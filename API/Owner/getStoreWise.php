<?php
include_once('config.php');
include_once(__DIR__ . '/../licence_expiry.php');
require_once __DIR__ . '/../company_store_fields.php';

/**
 * P4-5: Owner store-wise list — all branches (owner + franchise) for a shop user.
 */
$response = array('licensesResponse' => array(), 'storeCount' => '0');
mysqli_query($con, 'set names utf8');

if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    $userId = isset($_GET['userId']) ? $_GET['userId'] : '';
    $userIdEsc = mysqli_real_escape_string($con, (string) $userId);
    $today = licence_today();

    $sql = "SELECT `licenses`.*, `companys`.`companyName`, `companys`.`companyAddress`, `companys`.`companyMobile`,
                   `companys`.`shopName1`, `companys`.`shopName2`,
                   `companys`.`addressLine1`, `companys`.`addressLine2`, `companys`.`addressLine3`,
                   `companys`.`phoneNo1`, `companys`.`phoneNo2`, `companys`.`currencyName`
            FROM `licenses`
            LEFT JOIN `companys` ON `companys`.`licenseId` = `licenses`.`id`
            WHERE `licenses`.`userId`='$userIdEsc'
            ORDER BY CASE WHEN LOWER(`licenses`.`userType`) = 'owner' THEN 0 ELSE 1 END,
                     `licenses`.`id` ASC";

    $result = mysqli_query($con, $sql);
    if ($result && mysqli_num_rows($result) > 0) {
        while ($row = mysqli_fetch_assoc($result)) {
            $branch = licence_branch_fields($row);
            $sales = licence_store_sales($con, $row['id'], $today);
            $deviceName = isset($row['android_device_name']) && $row['android_device_name'] !== null && $row['android_device_name'] !== ''
                ? $row['android_device_name']
                : 'Not bound';

            $store = company_structured_fields($row);
            $companyAddress = company_display_address_oneline($row);

            $response['licensesResponse'][] = array(
                'licenses_id' => $row['id'],
                'organizationId' => (string) $row['userId'],
                'branchId' => (string) $row['id'],
                'deviceId' => isset($row['android_device_id']) ? $row['android_device_id'] : '',
                'companyAddress' => $companyAddress,
                'shopName1' => $store['shopName1'],
                'shopName2' => $store['shopName2'],
                'addressLine1' => $store['addressLine1'],
                'addressLine2' => $store['addressLine2'],
                'addressLine3' => $store['addressLine3'],
                'phoneNo1' => $store['phoneNo1'],
                'phoneNo2' => $store['phoneNo2'],
                'licenseKey' => $row['licenseKey'],
                'androidDeviceName' => $deviceName,
                'androidDeviceId' => isset($row['android_device_id']) ? $row['android_device_id'] : '',
                'userType' => $branch['userType'],
                'userName' => $branch['userName'],
                'branchLabel' => $branch['branchLabel'],
                'licenseType' => isset($row['licenseType']) ? $row['licenseType'] : '',
                'licenseStatus' => isset($row['licenseStatus']) ? $row['licenseStatus'] : '',
                'expiryDate' => isset($row['expiryDate']) ? $row['expiryDate'] : '',
                'currencyName' => licence_format_currency_name(isset($row['currencyName']) ? $row['currencyName'] : null),
                'totalSale' => $sales['totalSale'],
                'todaySale' => $sales['todaySale'],
            );
        }
        $response['storeCount'] = (string) count($response['licensesResponse']);
    }
}

header('Content-type: application/json; charset=utf-8');
echo json_encode($response);
?>
