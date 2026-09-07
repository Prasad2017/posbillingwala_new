<?php

namespace App\Support;

use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

/**
 * Web-admin business template upsert — mirrors API/business_template_ops.php
 * (userId column = licence id).
 */
class BusinessTemplateSupport
{
    /**
     * @return array<int, array{type:string,templateId:string,label:string}>
     */
    public static function choices(): array
    {
        return [
            ['type' => 'restaurant', 'templateId' => 'restaurant_default', 'label' => 'Restaurant / Food [Live]'],
            ['type' => 'bar_restaurant', 'templateId' => 'bar_restaurant_default', 'label' => 'Bar + Restaurant [Live]'],
            ['type' => 'mess', 'templateId' => 'mess_focused', 'label' => 'Mess / Tiffin [Live]'],
            ['type' => 'retail', 'templateId' => 'retail_default', 'label' => 'Retail / General Shop [Live]'],
            ['type' => 'grocery', 'templateId' => 'grocery_default', 'label' => 'Grocery / Kirana [Live]'],
            ['type' => 'weight_fresh', 'templateId' => 'weight_fresh_default', 'label' => 'Weight / Fresh [Live]'],
            ['type' => 'wholesale', 'templateId' => 'wholesale_default', 'label' => 'Wholesale [Live]'],
            ['type' => 'fashion', 'templateId' => 'fashion_default', 'label' => 'Clothing / Footwear [Live]'],
            ['type' => 'jewellery', 'templateId' => 'jewellery_default', 'label' => 'Jewellery [Live]'],
            ['type' => 'salon', 'templateId' => 'salon_default', 'label' => 'Salon / Beauty / Spa [Live]'],
            ['type' => 'bakery', 'templateId' => 'bakery_default', 'label' => 'Bakery / Cake Shop [Live]'],
            ['type' => 'electronics', 'templateId' => 'retail_default', 'label' => 'Electronics / Mobile [Partial → retail]'],
            ['type' => 'hardware', 'templateId' => 'retail_default', 'label' => 'Hardware [Partial → retail]'],
            ['type' => 'stationery', 'templateId' => 'retail_default', 'label' => 'Stationery [Partial → retail]'],
            ['type' => 'pet_shop', 'templateId' => 'retail_default', 'label' => 'Pet Shop [Partial → retail]'],
            ['type' => 'rental', 'templateId' => 'retail_default', 'label' => 'Rental [Partial → retail]'],
            ['type' => 'laundry', 'templateId' => 'salon_default', 'label' => 'Laundry [Partial → salon]'],
            ['type' => 'car_wash', 'templateId' => 'salon_default', 'label' => 'Car Wash [Partial → salon]'],
            ['type' => 'repair', 'templateId' => 'salon_default', 'label' => 'Repair [Partial → salon]'],
            ['type' => 'healthcare', 'templateId' => 'salon_default', 'label' => 'Healthcare-ready [Partial → salon]'],
            ['type' => 'custom', 'templateId' => 'restaurant_default', 'label' => 'Custom Business [Partial → restaurant]'],
        ];
    }

    public static function choiceKey(string $type, string $templateId): string
    {
        return $type . '|' . $templateId;
    }

    /**
     * @return array{type:string,templateId:string}
     */
    public static function parseChoice(?string $raw): array
    {
        $raw = trim((string) $raw);
        if ($raw === '' || strpos($raw, '|') === false) {
            return ['type' => 'restaurant', 'templateId' => 'restaurant_default'];
        }
        [$type, $templateId] = explode('|', $raw, 2);
        $type = strtolower(trim($type));
        $templateId = strtolower(trim($templateId));
        if ($type === '' || $templateId === '') {
            return ['type' => 'restaurant', 'templateId' => 'restaurant_default'];
        }
        return ['type' => $type, 'templateId' => $templateId];
    }

    /**
     * @return array{businessType:string,businessTemplateId:string}
     */
    public static function fetchForLicence($licenceId): array
    {
        $defaults = ['businessType' => 'restaurant', 'businessTemplateId' => 'restaurant_default'];
        if (!Schema::hasTable('company_business_templates') || $licenceId === null || $licenceId === '') {
            return $defaults;
        }
        $row = DB::table('company_business_templates')
            ->where('userId', (string) $licenceId)
            ->first();
        if (!$row) {
            return $defaults;
        }
        return [
            'businessType' => $row->businessType ?: 'restaurant',
            'businessTemplateId' => $row->businessTemplateId ?: 'restaurant_default',
        ];
    }

    public static function upsertForLicence($licenceId, string $businessType, string $businessTemplateId, string $source = 'web_admin'): bool
    {
        if ($licenceId === null || $licenceId === '') {
            return false;
        }
        if (!Schema::hasTable('company_business_templates')) {
            return false;
        }
        $licenceId = (string) $licenceId;
        $now = now();
        $existing = DB::table('company_business_templates')->where('userId', $licenceId)->first();
        $payload = [
            'businessType' => $businessType,
            'businessTemplateId' => $businessTemplateId,
            'templateNetworkStatus' => $source,
            'updatedAt' => $now,
        ];
        if ($existing) {
            DB::table('company_business_templates')->where('userId', $licenceId)->update($payload);
        } else {
            $payload['userId'] = $licenceId;
            $payload['createdAt'] = $now;
            DB::table('company_business_templates')->insert($payload);
        }
        return true;
    }

    /**
     * @return array{fastBilling:int,takeAway:int,dineIn:int,mess:int}
     */
    public static function defaultModules(string $businessType, string $templateId): array
    {
        $type = strtolower(trim($businessType));
        $tid = strtolower(trim($templateId));
        if ($type === 'mess' || $tid === 'mess_focused') {
            return ['fastBilling' => 1, 'takeAway' => 0, 'dineIn' => 0, 'mess' => 1];
        }
        if ($type === 'restaurant' || $type === 'bar_restaurant' || $type === 'custom'
            || $tid === 'restaurant_default' || $tid === 'bar_restaurant_default') {
            return ['fastBilling' => 1, 'takeAway' => 1, 'dineIn' => 1, 'mess' => 1];
        }
        return ['fastBilling' => 1, 'takeAway' => 0, 'dineIn' => 0, 'mess' => 0];
    }

    public static function applyFromRequest($licenceId, $request): void
    {
        if (!$request->filled('business_template_choice')) {
            return;
        }
        $parsed = self::parseChoice($request->input('business_template_choice'));
        self::upsertForLicence($licenceId, $parsed['type'], $parsed['templateId'], 'web_admin');

        $sync = $request->input('sync_modules', '1') === '1' || $request->boolean('sync_modules');
        if ($sync && Schema::hasTable('licenses')) {
            $mods = self::defaultModules($parsed['type'], $parsed['templateId']);
            DB::table('licenses')->where('id', $licenceId)->update([
                'fastBilling' => $mods['fastBilling'],
                'takeAway' => $mods['takeAway'],
                'dineIn' => $mods['dineIn'],
                'mess' => $mods['mess'],
            ]);
        }
    }
}
