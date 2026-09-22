package com.pos_billingwala.Extra;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Offline-first pending queue for error logs.
 * Fatal crashes use {@link #enqueueSync} (atomic disk write + fsync).
 */
public final class ErrorLogQueue {

    private static final String TAG = "POS_ERR_QUEUE";
    private static final String DIR = "error_log_queue";
    private static final String ARCHIVE_DIR = "error_log_archive";
    private static final int MAX_PENDING_FILES = 300;
    private static final int MAX_ARCHIVE_FILES = 200;

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ErrorLogQueue");
        t.setDaemon(true);
        return t;
    });

    private static final AtomicBoolean flushing = new AtomicBoolean(false);
    private static volatile Context appContext;

    private ErrorLogQueue() {
    }

    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
            restorePendingFromArchive(appContext);
            cleanupStaleTempFiles(appContext);
        }
    }

    public static void enqueue(ErrorLogPayload payload) {
        if (payload == null) {
            return;
        }
        Context ctx = appContext;
        if (ctx == null) {
            return;
        }
        EXEC.execute(() -> {
            try {
                writePayload(ctx, payload);
                ErrorLogUploader.flushPending(ctx);
            } catch (Throwable t) {
                Log.e(TAG, "enqueue failed: " + t.getMessage());
            }
        });
    }

    /**
     * Synchronous durable write for fatal crashes — must complete before process exit.
     * @return true if the payload was written to disk
     */
    public static boolean enqueueSync(ErrorLogPayload payload) {
        Context ctx = appContext;
        if (ctx == null || payload == null) {
            return false;
        }
        try {
            writePayload(ctx, payload);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "enqueueSync failed: " + t.getMessage());
            return false;
        }
    }

    public static void flushAsync() {
        Context ctx = appContext;
        if (ctx == null) {
            return;
        }
        EXEC.execute(() -> {
            try {
                ErrorLogUploader.flushPending(ctx);
            } catch (Throwable t) {
                Log.e(TAG, "flushAsync failed: " + t.getMessage());
            }
        });
    }

    public static int pendingCount(Context ctx) {
        if (ctx == null) {
            return 0;
        }
        return listPending(ctx).size();
    }

    /** Copy any archive files missing from the live queue (recovery after partial crash write). */
    public static void restorePendingFromArchive(Context ctx) {
        if (ctx == null) {
            return;
        }
        try {
            File queue = queueDir(ctx);
            File archive = archiveDir(ctx);
            File[] archived = archive.listFiles((d, name) -> name != null && name.endsWith(".json"));
            if (archived == null || archived.length == 0) {
                return;
            }
            Set<String> queuedNames = new HashSet<>();
            File[] pending = queue.listFiles((d, name) -> name != null && name.endsWith(".json"));
            if (pending != null) {
                for (File f : pending) {
                    queuedNames.add(f.getName());
                }
            }
            int restored = 0;
            for (File src : archived) {
                if (queuedNames.contains(src.getName())) {
                    continue;
                }
                File dest = new File(queue, src.getName());
                if (copyFile(src, dest)) {
                    restored++;
                }
            }
            if (restored > 0) {
                Log.i(TAG, "Restored " + restored + " error log(s) from archive");
            }
        } catch (Throwable t) {
            Log.w(TAG, "restorePendingFromArchive: " + t.getMessage());
        }
    }

    static List<File> listPending(Context ctx) {
        List<File> out = new ArrayList<>();
        File dir = queueDir(ctx);
        File[] files = dir.listFiles((d, name) -> name != null && name.endsWith(".json"));
        if (files == null) {
            return out;
        }
        for (File f : files) {
            out.add(f);
        }
        out.sort((a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        return out;
    }

    static ErrorLogPayload readFile(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return ErrorLogPayload.fromJson(sb.toString());
        } catch (Exception e) {
            return null;
        }
    }

    static boolean deleteFile(File file) {
        try {
            return file != null && file.delete();
        } catch (Exception e) {
            return false;
        }
    }

    static boolean tryBeginFlush() {
        return flushing.compareAndSet(false, true);
    }

    static void endFlush() {
        flushing.set(false);
    }

    private static void writePayload(Context ctx, ErrorLogPayload payload) throws Exception {
        File dir = queueDir(ctx);
        trimIfNeeded(dir, MAX_PENDING_FILES);
        String name = "err_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 100000) + ".json";
        byte[] bytes = payload.toJson().getBytes(StandardCharsets.UTF_8);
        File dest = new File(dir, name);
        writeAtomic(dest, bytes);
        try {
            File archive = new File(archiveDir(ctx), name);
            writeAtomic(archive, bytes);
            trimIfNeeded(archiveDir(ctx), MAX_ARCHIVE_FILES);
        } catch (Throwable t) {
            Log.w(TAG, "archive copy failed: " + t.getMessage());
        }
    }

    /** Write to .tmp first, fsync, then atomic rename — avoids corrupt half-files on crash. */
    private static void writeAtomic(File dest, byte[] bytes) throws Exception {
        File dir = dest.getParentFile();
        if (dir != null && !dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        File tmp = new File(dir, dest.getName() + ".tmp");
        writeSynced(tmp, bytes);
        if (tmp.renameTo(dest)) {
            return;
        }
        copyFile(tmp, dest);
        //noinspection ResultOfMethodCallIgnored
        tmp.delete();
    }

    private static void writeSynced(File file, byte[] bytes) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            fos.write(bytes);
            fos.flush();
            fos.getFD().sync();
        } finally {
            try {
                fos.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean copyFile(File src, File dest) {
        if (src == null || dest == null || !src.exists()) {
            return false;
        }
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            FileChannel inCh = in.getChannel();
            FileChannel outCh = out.getChannel();
            inCh.transferTo(0, inCh.size(), outCh);
            out.getFD().sync();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void cleanupStaleTempFiles(Context ctx) {
        try {
            long cutoff = System.currentTimeMillis() - (24L * 60 * 60 * 1000);
            for (File dir : new File[]{queueDir(ctx), archiveDir(ctx)}) {
                File[] tmpFiles = dir.listFiles((d, name) -> name != null && name.endsWith(".tmp"));
                if (tmpFiles == null) {
                    continue;
                }
                for (File tmp : tmpFiles) {
                    if (tmp.lastModified() < cutoff) {
                        //noinspection ResultOfMethodCallIgnored
                        tmp.delete();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static File queueDir(Context ctx) {
        return ensureDir(new File(ctx.getFilesDir(), DIR));
    }

    private static File archiveDir(Context ctx) {
        return ensureDir(new File(ctx.getFilesDir(), ARCHIVE_DIR));
    }

    private static File ensureDir(File dir) {
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    private static void trimIfNeeded(File dir, int maxFiles) {
        File[] files = dir.listFiles((d, name) -> name != null && name.endsWith(".json"));
        if (files == null || files.length < maxFiles) {
            return;
        }
        List<File> list = new ArrayList<>();
        for (File f : files) {
            list.add(f);
        }
        list.sort((a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        int remove = list.size() - maxFiles + 1;
        for (int i = 0; i < remove && i < list.size(); i++) {
            //noinspection ResultOfMethodCallIgnored
            list.get(i).delete();
        }
    }
}
