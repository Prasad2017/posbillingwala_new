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

if (! function_exists('admin_assets_ready')) {
    function admin_assets_ready(string $publicAssets): bool
    {
        return is_file($publicAssets . DIRECTORY_SEPARATOR . 'css' . DIRECTORY_SEPARATOR . 'app.css');
    }
}

if (! function_exists('admin_remove_public_assets')) {
    function admin_remove_public_assets(string $publicAssets): void
    {
        if (! file_exists($publicAssets) && ! is_link($publicAssets)) {
            return;
        }

        if (is_link($publicAssets)) {
            @unlink($publicAssets);

            return;
        }

        if (! is_dir($publicAssets)) {
            @unlink($publicAssets);

            return;
        }

        $iterator = new RecursiveIteratorIterator(
            new RecursiveDirectoryIterator($publicAssets, RecursiveDirectoryIterator::SKIP_DOTS),
            RecursiveIteratorIterator::CHILD_FIRST
        );

        foreach ($iterator as $item) {
            if ($item->isDir()) {
                @rmdir($item->getPathname());
            } else {
                @unlink($item->getPathname());
            }
        }

        @rmdir($publicAssets);
    }
}

if (! function_exists('admin_sync_public_assets')) {
    /**
     * Ensure public/assets serves project assets/ (symlink or Windows junction).
     */
    function admin_sync_public_assets(): bool
    {
        $publicAssets = public_path('assets');
        $sourceAssets = base_path('assets');

        if (! is_dir($sourceAssets)) {
            return false;
        }

        if (admin_assets_ready($publicAssets)) {
            return true;
        }

        admin_remove_public_assets($publicAssets);

        if (@symlink($sourceAssets, $publicAssets) && admin_assets_ready($publicAssets)) {
            return true;
        }

        if (DIRECTORY_SEPARATOR === '\\') {
            $link = str_replace('/', '\\', $publicAssets);
            $target = str_replace('/', '\\', $sourceAssets);
            @exec(
                'cmd /c mklink /J ' . escapeshellarg($link) . ' ' . escapeshellarg($target),
                $output,
                $code
            );

            if ($code === 0 && admin_assets_ready($publicAssets)) {
                return true;
            }

            @exec(
                'powershell -NoProfile -Command "if (Test-Path '
                . escapeshellarg($link)
                . ') { Remove-Item '
                . escapeshellarg($link)
                . ' -Recurse -Force }; New-Item -ItemType Junction -Path '
                . escapeshellarg($link)
                . ' -Target '
                . escapeshellarg($target)
                . ' | Out-Null"',
                $psOutput,
                $psCode
            );
        }

        return admin_assets_ready($publicAssets);
    }
}
