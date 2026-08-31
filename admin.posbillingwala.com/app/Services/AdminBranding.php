<?php

namespace App\Services;

class AdminBranding
{
    private const DIR = 'assets/branding';
    private const LOGO = 'admin-logo';
    private const FAVICON = 'admin-favicon';
    private const DEFAULT_LOGO = 'assets/images/pos_billingwala_logo.png';
    private const DEFAULT_FAVICON = 'assets/images/app_logo.png';

    private static function extensions(): array
    {
        return ['png', 'jpg', 'jpeg', 'webp', 'ico', 'svg'];
    }

    public static function brandingDir(): string
    {
        return base_path(self::DIR);
    }

    public static function ensureDir(): void
    {
        $dir = self::brandingDir();
        if (!is_dir($dir)) {
            mkdir($dir, 0755, true);
        }
    }

    private static function findFile(string $base): ?string
    {
        foreach (self::extensions() as $ext) {
            $path = self::brandingDir() . '/' . $base . '.' . $ext;
            if (is_file($path)) {
                return $path;
            }
        }

        return null;
    }

    private static function assetWithVersion(string $relativePath, string $absolutePath): string
    {
        $path = ltrim($relativePath, '/');
        if (str_starts_with($path, 'assets/')) {
            $path = substr($path, 7);
        }

        return admin_asset($path);
    }

    public static function logoUrl(): string
    {
        $file = self::findFile(self::LOGO);
        if ($file) {
            return self::assetWithVersion(self::DIR . '/' . basename($file), $file);
        }

        return admin_asset('images/pos_billingwala_logo.png');
    }

    public static function faviconUrl(): string
    {
        $file = self::findFile(self::FAVICON);
        if ($file) {
            return self::assetWithVersion(self::DIR . '/' . basename($file), $file);
        }

        return admin_asset('images/app_logo.png');
    }

    public static function hasCustomLogo(): bool
    {
        return self::findFile(self::LOGO) !== null;
    }

    public static function hasCustomFavicon(): bool
    {
        return self::findFile(self::FAVICON) !== null;
    }

    public static function saveLogo($uploadedFile): void
    {
        self::ensureDir();
        self::removeExisting(self::LOGO);
        $ext = strtolower((string) $uploadedFile->getClientOriginalExtension());
        if (!in_array($ext, ['png', 'jpg', 'jpeg', 'webp', 'svg'], true)) {
            $ext = 'png';
        }
        $uploadedFile->move(self::brandingDir(), self::LOGO . '.' . $ext);
    }

    public static function saveFavicon($uploadedFile): void
    {
        self::ensureDir();
        self::removeExisting(self::FAVICON);
        $ext = strtolower((string) $uploadedFile->getClientOriginalExtension());
        if (!in_array($ext, ['png', 'jpg', 'jpeg', 'webp', 'ico', 'svg'], true)) {
            $ext = 'png';
        }
        $uploadedFile->move(self::brandingDir(), self::FAVICON . '.' . $ext);
    }

    private static function removeExisting(string $base): void
    {
        foreach (self::extensions() as $ext) {
            $path = self::brandingDir() . '/' . $base . '.' . $ext;
            if (is_file($path)) {
                @unlink($path);
            }
        }
    }
}
