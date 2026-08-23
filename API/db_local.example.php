<?php
/**
 * Copy to db_local.php on the server (db_local.php is gitignored).
 * Never commit real credentials to the repository.
 */
$dbHost = 'localhost';
$dbUser = 'your_db_username';
$dbPass = 'your_db_password';
$dbName = 'spllmgkn_posbill';

// Optional: absolute path to RSA private key for license payload signing (gitignored PEM).
// Default fallback: API/license_signing_private.pem
// $licenseSigningPrivateKeyPath = '/secure/path/license_signing_private.pem';
