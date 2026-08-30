<?php

namespace App\Support;

/**
 * Default licence PIN values — keep in sync with API/licence_expiry.php.
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
}
