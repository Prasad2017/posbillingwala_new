<?php

if (! function_exists('admin_asset')) {
    /**
     * Root-relative static asset URL (works on any host/path when /assets/ is web-accessible).
     */
    function admin_asset(string $path, bool $versioned = true): string
    {
        $path = ltrim($path, '/');
        if (! str_starts_with($path, 'assets/')) {
            $path = 'assets/' . $path;
        }

        $url = '/' . $path;

        if ($versioned) {
            $file = base_path($path);
            if (is_file($file)) {
                $mtime = @filemtime($file);
                if ($mtime !== false) {
                    $url .= '?v=' . $mtime;
                }
            }
        }

        return $url;
    }
}

if (! function_exists('admin_sync_public_assets')) {
    /**
     * Ensure public/assets exists (symlink to project assets/ for public/ document roots).
     */
    function admin_sync_public_assets(): bool
    {
        $publicAssets = public_path('assets');
        $sourceAssets = base_path('assets');

        if (! is_dir($sourceAssets)) {
            return false;
        }

        if (is_link($publicAssets)) {
            return true;
        }

        if (is_dir($publicAssets)) {
            return true;
        }

        if (file_exists($publicAssets)) {
            return false;
        }

        return @symlink($sourceAssets, $publicAssets);
    }
}
