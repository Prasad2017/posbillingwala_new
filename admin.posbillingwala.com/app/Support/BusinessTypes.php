<?php

namespace App\Support;

/* Mirrors Flutter BusinessType ids used by POS Billingwala. */
class BusinessTypes
{
    public static function options(): array
    {
        return [
            'retail_store' => 'Retail Store',
            'clothing_store' => 'Clothing Store',
            'supermarket' => 'Supermarket / Large Retail',
            'department_store' => 'Department Store',
            'grocery_store' => 'Grocery Store',
            'electronics_hardware' => 'Electronics / Hardware',
            'hotel' => 'Hotel',
            'restaurant' => 'Restaurant',
            'cafe' => 'Cafe',
            'cake_shop_bakery' => 'Cake Shop / Bakery',
            'bar' => 'Bar',
            'cold_drinks_beverage' => 'Cold Drinks / Beverage',
            'pharmacy' => 'Pharmacy',
            'footwear' => 'Footwear',
            'cosmetics' => 'Cosmetics',
            'home_kitchen' => 'Home & Kitchen',
            'other' => 'Other / Custom',
        ];
    }

    public static function ids(): array
    {
        return array_keys(self::options());
    }

    public static function normalize(?string $raw): string
    {
        $v = trim((string) $raw);
        if ($v === '' || !in_array($v, self::ids(), true)) {
            return '';
        }
        return $v;
    }

    public static function label(?string $raw): string
    {
        $id = self::normalize($raw);
        if ($id === '') {
            return 'Not set';
        }
        return self::options()[$id] ?? $id;
    }
}
