<?php
/**
 * Shared helpers for structured Store Details on `companys`.
 * Safe when new columns are missing or empty (falls back to legacy fields).
 */

if (!function_exists('company_trim')) {
    function company_trim($value)
    {
        if ($value === null) {
            return '';
        }
        return trim((string)$value);
    }
}

if (!function_exists('company_first_non_empty')) {
    function company_first_non_empty($primary, $fallback)
    {
        $first = company_trim($primary);
        if ($first !== '') {
            return $first;
        }
        return company_trim($fallback);
    }
}

/**
 * Extract structured store fields from a companys (or joined) row array.
 *
 * @param array $row
 * @return array
 */
if (!function_exists('company_structured_fields')) {
    function company_structured_fields($row)
    {
        if (!is_array($row)) {
            $row = array();
        }
        $companyName = isset($row['companyName']) ? $row['companyName'] : '';
        $companyAddress = isset($row['companyAddress']) ? $row['companyAddress'] : '';
        $companyMobile = isset($row['companyMobile']) ? $row['companyMobile'] : '';

        $shopName1 = company_first_non_empty(isset($row['shopName1']) ? $row['shopName1'] : '', $companyName);
        $shopName2 = company_trim(isset($row['shopName2']) ? $row['shopName2'] : '');
        $addressLine1 = company_trim(isset($row['addressLine1']) ? $row['addressLine1'] : '');
        $addressLine2 = company_trim(isset($row['addressLine2']) ? $row['addressLine2'] : '');
        $addressLine3 = company_trim(isset($row['addressLine3']) ? $row['addressLine3'] : '');
        $phoneNo1 = company_first_non_empty(isset($row['phoneNo1']) ? $row['phoneNo1'] : '', $companyMobile);
        $phoneNo2 = company_trim(isset($row['phoneNo2']) ? $row['phoneNo2'] : '');

        if ($addressLine1 === '' && $addressLine2 === '' && $addressLine3 === '') {
            $addressLine1 = company_trim($companyAddress);
        }

        return array(
            'shopName1' => $shopName1,
            'shopName2' => $shopName2,
            'addressLine1' => $addressLine1,
            'addressLine2' => $addressLine2,
            'addressLine3' => $addressLine3,
            'phoneNo1' => $phoneNo1,
            'phoneNo2' => $phoneNo2,
            'companyName' => company_first_non_empty($companyName, $shopName1),
            'companyAddress' => company_compose_address($addressLine1, $addressLine2, $addressLine3, $companyAddress),
            'companyMobile' => company_first_non_empty($companyMobile, $phoneNo1),
        );
    }
}

if (!function_exists('company_compose_address')) {
    function company_compose_address($line1, $line2, $line3, $legacyAddress = '')
    {
        $parts = array();
        foreach (array($line1, $line2, $line3) as $line) {
            $trimmed = company_trim($line);
            if ($trimmed !== '') {
                $parts[] = $trimmed;
            }
        }
        if (count($parts) > 0) {
            return implode("\n", $parts);
        }
        return company_trim($legacyAddress);
    }
}

/**
 * Multi-line shop details block for bills/lists (skips blanks).
 */
if (!function_exists('company_shop_details_block')) {
    function company_shop_details_block($row, $includeShopName2 = true, $includePhones = true, $includeGst = true, $includeFssai = true)
    {
        $fields = company_structured_fields($row);
        $lines = array();
        if ($includeShopName2 && $fields['shopName2'] !== '') {
            $lines[] = $fields['shopName2'];
        }
        foreach (array('addressLine1', 'addressLine2', 'addressLine3') as $key) {
            if ($fields[$key] !== '') {
                $lines[] = $fields[$key];
            }
        }
        if ($includePhones) {
            if ($fields['phoneNo1'] !== '') {
                $lines[] = $fields['phoneNo1'];
            }
            if ($fields['phoneNo2'] !== '') {
                $lines[] = $fields['phoneNo2'];
            }
        }
        $gstStatus = isset($row['gstStatus']) ? $row['gstStatus'] : '';
        if ($includeGst && strcasecmp(company_trim($gstStatus), 'on') === 0) {
            $gst = company_trim(isset($row['gstNumber']) ? $row['gstNumber'] : '');
            if ($gst !== '') {
                $lines[] = 'GSTIN: ' . $gst;
            }
        }
        if ($includeFssai) {
            $fssai = company_trim(isset($row['companyFssis']) ? $row['companyFssis'] : '');
            if ($fssai !== '') {
                $lines[] = 'FSSAI No: ' . $fssai;
            }
        }
        return implode("\n", $lines);
    }
}

/**
 * Single-line address for compact list UIs (comma-separated).
 */
if (!function_exists('company_display_address_oneline')) {
    function company_display_address_oneline($row)
    {
        $fields = company_structured_fields($row);
        $parts = array();
        foreach (array('addressLine1', 'addressLine2', 'addressLine3') as $key) {
            if ($fields[$key] !== '') {
                $parts[] = $fields[$key];
            }
        }
        if (count($parts) === 0) {
            return '-';
        }
        return implode(', ', $parts);
    }
}
