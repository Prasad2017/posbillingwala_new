<?php

namespace App\Services;

class WebsiteMedia
{
    private const DIR = 'assets/website';

    public static function dir(string $subdir): string
    {
        $path = base_path(self::DIR . '/' . $subdir);
        if (!is_dir($path)) {
            mkdir($path, 0755, true);
        }

        return $path;
    }

    public static function save($uploadedFile, string $subdir, string $basename): string
    {
        $ext = strtolower((string) $uploadedFile->getClientOriginalExtension());
        if (!in_array($ext, ['png', 'jpg', 'jpeg', 'webp', 'svg'], true)) {
            $ext = 'jpg';
        }

        $filename = $basename . '.' . $ext;
        $uploadedFile->move(self::dir($subdir), $filename);

        return self::DIR . '/' . $subdir . '/' . $filename;
    }

    public static function url(?string $relativePath): ?string
    {
        if (!$relativePath) {
            return null;
        }

        $absolute = base_path($relativePath);
        if (!is_file($absolute)) {
            return null;
        }

        return asset($relativePath) . '?v=' . filemtime($absolute);
    }

    public static function delete(?string $relativePath): void
    {
        if (!$relativePath) {
            return;
        }

        $absolute = base_path($relativePath);
        if (is_file($absolute)) {
            @unlink($absolute);
        }
    }
}
