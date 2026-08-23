<?php
include_once "config.php";
require_once __DIR__ . '/../auth_tokens.php';

mysqli_query($con, "set names utf8");

if ($_SERVER["REQUEST_METHOD"] == "POST") {
    $userName = $_POST["userName"];
    $password = $_POST["password"];

    date_default_timezone_set("Asia/Calcutta");
    $date = date("Y-m-d");

    $hashed_password = password_hash($password, PASSWORD_DEFAULT);

    $sth = "SELECT * FROM `users` WHERE `contact_number`='$userName'";

    if ($result = mysqli_query($con, $sth)) {
        if (mysqli_num_rows($result) > 0) {
            while ($row = mysqli_fetch_assoc($result)) {
                  
                if (isset($row["password"]) && $row["password"] !== null) {
                    $passwords = $row["password"];
                    if (password_verify($password, $passwords)) {
                        $userId = $row["id"];

                        header("Content-type: application/json; charset=utf-8");
                        $json2 = [
                            "status" => "true",
                            "message" => "Login Sucessfully.",
                            "userId" => $userId,
                        ];
                        auth_token_append_response($con, $json2, 'dealer', $userId);
                    } else {
                        $json2 = [
                            "status" => "false",
                            "message" => "Login failed.",
                        ];
                    }
                }
            }
        } else {
            $json2 = [
                "status" => "false",
                "message" => "Login failed.",
            ];
        }
    }

    print_r(json_encode($json2));
    unset($json2);
}
?>
