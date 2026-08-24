<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Company extends Model
{
    use HasFactory;

    protected $table = 'companys';

    protected $fillable = [
        'licenseId',
        'companyName',
        'cashierName',
        'companyMobile',
        'companyAddress',
        'shopName1',
        'shopName2',
        'addressLine1',
        'addressLine2',
        'addressLine3',
        'phoneNo1',
        'phoneNo2',
        'currencyName',
        'countryName',
        'stateName',
        'tableStatus',
        'noOfTable',
        'gstStatus',
        'gstNumber',
        'shopCGST',
        'shopSGST',
        'panNumber',
        'companyFssis',
        'companyLogo',
        'paymentLogo',
        'companyStatus',
    ];

    public function resolveShopName1(): string
    {
        $name1 = trim((string) ($this->shopName1 ?? ''));
        if ($name1 !== '') {
            return $name1;
        }
        return trim((string) ($this->companyName ?? ''));
    }

    public function resolveShopName2(): string
    {
        return trim((string) ($this->shopName2 ?? ''));
    }

    /** @return string[] */
    public function resolveAddressLines(): array
    {
        $lines = [];
        foreach (['addressLine1', 'addressLine2', 'addressLine3'] as $field) {
            $value = trim((string) ($this->{$field} ?? ''));
            if ($value !== '') {
                $lines[] = $value;
            }
        }
        if (count($lines) === 0) {
            $legacy = trim((string) ($this->companyAddress ?? ''));
            if ($legacy !== '') {
                $lines[] = $legacy;
            }
        }
        return $lines;
    }

    public function resolvePhone1(): string
    {
        $phone = trim((string) ($this->phoneNo1 ?? ''));
        if ($phone !== '') {
            return $phone;
        }
        return trim((string) ($this->companyMobile ?? ''));
    }

    public function resolvePhone2(): string
    {
        return trim((string) ($this->phoneNo2 ?? ''));
    }

    public function displayAddressOneline(): string
    {
        $lines = $this->resolveAddressLines();
        return count($lines) > 0 ? implode(', ', $lines) : '-';
    }
}
