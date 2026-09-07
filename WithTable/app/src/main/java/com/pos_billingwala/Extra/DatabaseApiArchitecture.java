package com.pos_billingwala.Extra;

/**
 * Database / API architecture notes for Universal POS.
 * Phase 20: live stack — SQLite POS + PHP API + MySQL; additive migrations only.
 */
public final class DatabaseApiArchitecture {

    public static final String POS_DB = "pos_billingwala_db (SQLiteOpenHelper)";
    public static final String API_ROOT = "API/*.php (androidApp)";
    public static final String MYSQL = "Shared MySQL (companys, products, invoices, licences, …)";
    public static final String MIGRATIONS = "API/migrations/*.sql + POS addColumnIfNotExists";

    private DatabaseApiArchitecture() {
    }

    public static String moduleSummary() {
        return "POS: " + POS_DB
                + "\nAPI: " + API_ROOT
                + "\nServer: " + MYSQL
                + "\nMigrations: " + MIGRATIONS
                + "\nRule: additive only — no DROP TABLE in upgrades"
                + "\nAuth: auth_tokens.php + Bearer on write endpoints";
    }
}
