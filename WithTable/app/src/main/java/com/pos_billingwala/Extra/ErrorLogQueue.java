package com.pos_billingwala.Extra;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Offline-first pending queue for error logs. Never blocks the caller thread.
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
        }
    }

    /** Enqueue asynchronously — safe to call from UI or crash path after writing. */
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

    /** Synchronous disk write for fatal crashes (before process exit). */
    public static void enqueueSync(ErrorLogPayload payload) {
        Context ctx = appContext;
        if (ctx == null || payload == null) {
            return;
        }
        try {
            writePayload(ctx, payload);
        } catch (Throwable t) {
            Log.e(TAG, "enqueueSync failed: " + t.getMessage());
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
        writeSynced(new File(dir, name), bytes);
        try {
            File archive = new File(archiveDir(ctx), name);
            writeSynced(archive, bytes);
            trimIfNeeded(archiveDir(ctx), MAX_ARCHIVE_FILES);
        } catch (Throwable t) {
            Log.w(TAG, "archive copy failed: " + t.getMessage());
        }
    }

    /** Durably flush so a fatal crash cannot lose the file before process death. */
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
