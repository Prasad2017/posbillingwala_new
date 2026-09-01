/**
 * Build XAMPP-ready SQL from server dump.
 * Usage: node scripts/build-xampp-sql.js
 */
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..");
const sources = [
  path.join(root, "rgusomuk_posbilling (2).sql"),
  path.join(process.env.USERPROFILE || "", "Downloads", "rgusomuk_posbilling (2).sql"),
];

const outFile = path.join(root, "database", "posbillingwala_xampp.sql");
const dbName = "posbillingwala";

let input = "";
for (const src of sources) {
  if (fs.existsSync(src)) {
    input = fs.readFileSync(src, "utf8");
    console.log("Source:", src);
    break;
  }
}

if (!input) {
  console.error("Source SQL not found.");
  process.exit(1);
}

let sql = input;

// MySQL uses 0/1 — not ON/OFF
sql = sql.replace(/FOREIGN_KEY_CHECKS\s*=\s*ON\b/gi, "FOREIGN_KEY_CHECKS = 1");
sql = sql.replace(/FOREIGN_KEY_CHECKS\s*=\s*OFF\b/gi, "FOREIGN_KEY_CHECKS = 0");

// Rename database references in comments / USE statements
sql = sql.replace(/`rgusomuk_posbilling`/g, "`" + dbName + "`");
sql = sql.replace(/Database: `rgusomuk_posbilling`/g, "Database: `" + dbName + "` (XAMPP local)");

const xamppHeader = `-- POS Billingwala — XAMPP import (fixed from server dump)
-- Database: ${dbName}
-- Import: mysql -u root ${dbName} < posbillingwala_xampp.sql

CREATE DATABASE IF NOT EXISTS \`${dbName}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE \`${dbName}\`;

SET FOREIGN_KEY_CHECKS = 0;
`;

// Insert XAMPP header after phpMyAdmin SET NAMES block
const marker = "/*!40101 SET NAMES utf8mb4 */;";
const idx = sql.indexOf(marker);
if (idx !== -1) {
  const insertAt = idx + marker.length;
  sql = sql.slice(0, insertAt) + "\n\n" + xamppHeader + sql.slice(insertAt);
} else {
  sql = xamppHeader + "\n" + sql;
}

// Ensure FOREIGN_KEY_CHECKS restored before COMMIT
if (!/SET FOREIGN_KEY_CHECKS\s*=\s*1/.test(sql)) {
  sql = sql.replace(/\nCOMMIT;\s*\n/, "\nSET FOREIGN_KEY_CHECKS = 1;\n\nCOMMIT;\n\n");
}

const banner = `-- ============================================================
-- XAMPP LOCAL IMPORT — ${dbName}
-- Generated: ${new Date().toISOString()}
-- ============================================================

`;

fs.mkdirSync(path.dirname(outFile), { recursive: true });
fs.writeFileSync(outFile, banner + sql, "utf8");

const mb = (fs.statSync(outFile).size / (1024 * 1024)).toFixed(2);
console.log("Output:", outFile);
console.log("Size:", mb, "MB");
console.log("Done.");
