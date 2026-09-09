<?php

namespace App\Support;

use App\Models\License;

/**
 * Default licence PIN values and key format — keep in sync with API/licence_expiry.php.
 * Format: BW-XXXX-XXXX-XXXX (uppercase alnum, no I/O).
 */
class LicenceDefaults
{
    public static function defaultMpin(): string
    {
        return '9082';
    }

    public static function defaultReportPin(): string
    {
        return '9082';
    }

    /**
     * Generate a unique licence key like BW-CZBQ-0355-UWEE.
     */
    public static function generateUniqueKey(): string
    {
        $chars = '0123456789ABCDEFGHJKLMNPQRSTUVWXYZ';
        $max = strlen($chars) - 1;

        for ($attempt = 0; $attempt < 40; $attempt++) {
            $parts = [];
            for ($p = 0; $p < 3; $p++) {
                $segment = '';
                for ($i = 0; $i < 4; $i++) {
                    $segment .= $chars[random_int(0, $max)];
                }
                $parts[] = $segment;
            }
            $key = 'BW-' . implode('-', $parts);
            if (!License::where('licenseKey', $key)->exists()) {
                return $key;
            }
        }

        throw new \RuntimeException('Unable to generate a unique license key.');
    }
}
