<?php

namespace App\Services;

use Illuminate\Support\Facades\Schema;
use Illuminate\Database\Schema\Blueprint;

class AdminTables
{
    public static function ensure(): void
    {
        if (!Schema::hasTable('admin_support_tickets')) {
            Schema::create('admin_support_tickets', function (Blueprint $table) {
                $table->increments('id');
                $table->string('ticket_no', 40);
                $table->string('app_name', 40)->default('POS App');
                $table->string('category', 80)->default('General');
                $table->string('subject', 255);
                $table->text('description')->nullable();
                $table->string('status', 32)->default('Open');
                $table->unsignedInteger('licence_id')->nullable();
                $table->unsignedInteger('user_id')->nullable();
                $table->string('shop_name', 255)->default('');
                $table->string('device_name', 120)->default('');
                $table->string('device_id', 255)->default('');
                $table->dateTime('created_at')->useCurrent();
                $table->dateTime('updated_at')->useCurrent();
            });
        } else {
            if (!Schema::hasColumn('admin_support_tickets', 'licence_id')) {
                Schema::table('admin_support_tickets', function (Blueprint $table) {
                    $table->unsignedInteger('licence_id')->nullable()->after('status');
                    $table->unsignedInteger('user_id')->nullable()->after('licence_id');
                    $table->string('shop_name', 255)->default('')->after('user_id');
                    $table->string('device_name', 120)->default('')->after('shop_name');
                    $table->string('device_id', 255)->default('')->after('device_name');
                });
            }
        }

        if (!Schema::hasTable('admin_support_messages')) {
            Schema::create('admin_support_messages', function (Blueprint $table) {
                $table->increments('id');
                $table->unsignedInteger('ticket_id');
                $table->string('sender', 80)->default('Admin');
                $table->text('message');
                $table->dateTime('created_at')->useCurrent();
                $table->index('ticket_id');
            });
        }

        if (!Schema::hasTable('admin_crash_logs')) {
            Schema::create('admin_crash_logs', function (Blueprint $table) {
                $table->increments('id');
                $table->string('error_title', 255);
                $table->string('error_class', 255)->default('');
                $table->string('app_name', 40)->default('POS App');
                $table->string('status', 32)->default('New');
                $table->string('device_name', 120)->default('');
                $table->string('android_version', 40)->default('');
                $table->string('app_version', 40)->default('');
                $table->string('user_name', 120)->default('');
                $table->string('user_id', 40)->default('');
                $table->integer('occurrences')->default(1);
                $table->mediumText('stack_trace')->nullable();
                $table->string('source_fingerprint', 64)->nullable();
                $table->dateTime('created_at')->useCurrent();
                $table->dateTime('updated_at')->useCurrent();
                $table->index('source_fingerprint', 'idx_crash_fp');
            });
        } elseif (!Schema::hasColumn('admin_crash_logs', 'source_fingerprint')) {
            Schema::table('admin_crash_logs', function (Blueprint $table) {
                $table->string('source_fingerprint', 64)->nullable()->after('stack_trace');
                $table->index('source_fingerprint', 'idx_crash_fp');
            });
        }

        self::ensureWebsite();
    }

    public static function ensureWebsite(): void
    {
        if (!Schema::hasTable('website_pages')) {
            Schema::create('website_pages', function (Blueprint $table) {
                $table->increments('id');
                $table->string('slug', 80)->unique();
                $table->string('title', 255);
                $table->mediumText('body_html');
                $table->dateTime('updated_at')->useCurrent();
            });
        }

        if (!Schema::hasTable('website_clients')) {
            Schema::create('website_clients', function (Blueprint $table) {
                $table->increments('id');
                $table->string('business_name', 255);
                $table->string('subtitle', 255)->default('');
                $table->string('city', 120)->default('');
                $table->string('business_category', 120)->default('');
                $table->text('description')->nullable();
                $table->string('logo_path', 500)->default('');
                $table->string('photo_path', 500)->default('');
                $table->string('cta_url', 500)->default('');
                $table->unsignedInteger('sort_order')->default(0);
                $table->unsignedTinyInteger('is_published')->default(1);
                $table->dateTime('created_at')->useCurrent();
                $table->dateTime('updated_at')->useCurrent();
            });
        } elseif (!Schema::hasColumn('website_clients', 'city')) {
            Schema::table('website_clients', function (Blueprint $table) {
                $table->string('city', 120)->default('')->after('subtitle');
                $table->string('business_category', 120)->default('')->after('city');
            });
        }

        if (!Schema::hasTable('website_testimonials')) {
            Schema::create('website_testimonials', function (Blueprint $table) {
                $table->increments('id');
                $table->string('author_name', 255);
                $table->string('business_name', 255)->default('');
                $table->text('quote');
                $table->unsignedTinyInteger('rating')->default(5);
                $table->string('photo_path', 500)->default('');
                $table->unsignedInteger('sort_order')->default(0);
                $table->unsignedTinyInteger('is_published')->default(1);
                $table->dateTime('created_at')->useCurrent();
                $table->dateTime('updated_at')->useCurrent();
            });
        }

        if (!Schema::hasTable('website_contact_messages')) {
            Schema::create('website_contact_messages', function (Blueprint $table) {
                $table->increments('id');
                $table->string('name', 255);
                $table->string('email', 255);
                $table->string('subject', 255)->default('');
                $table->text('message');
                $table->string('status', 32)->default('New');
                $table->string('source_ip', 64)->default('');
                $table->dateTime('created_at')->useCurrent();
                $table->dateTime('updated_at')->useCurrent();
            });
        }

        if (!Schema::hasTable('website_dealers')) {
            Schema::create('website_dealers', function (Blueprint $table) {
                $table->increments('id');
                $table->string('area', 120);
                $table->string('dealer_name', 255);
                $table->string('contact_person', 255)->default('');
                $table->string('role_title', 255)->default('');
                $table->string('mobile', 32)->default('');
                $table->string('whatsapp', 32)->default('');
                $table->text('address')->nullable();
                $table->string('map_url', 500)->default('');
                $table->string('dealer_type', 32)->default('authorized_dealer');
                $table->unsignedInteger('sort_order')->default(0);
                $table->unsignedTinyInteger('is_published')->default(1);
                $table->dateTime('created_at')->useCurrent();
                $table->dateTime('updated_at')->useCurrent();
            });
        }

        if (!Schema::hasTable('website_pricing_plans')) {
            Schema::create('website_pricing_plans', function (Blueprint $table) {
                $table->increments('id');
                $table->string('plan_type', 32);
                $table->string('validity_label', 64);
                $table->decimal('price', 10, 2)->default(0);
                $table->string('gst_note', 120)->default('GST included');
                $table->string('description', 500)->default('');
                $table->unsignedInteger('sort_order')->default(0);
                $table->unsignedTinyInteger('is_published')->default(1);
                $table->dateTime('created_at')->useCurrent();
                $table->dateTime('updated_at')->useCurrent();
            });
        }

        if (!Schema::hasTable('website_products')) {
            Schema::create('website_products', function (Blueprint $table) {
                $table->increments('id');
                $table->string('name', 255);
                $table->string('category', 64);
                $table->text('description')->nullable();
                $table->string('icon', 16)->default('');
                $table->unsignedInteger('sort_order')->default(0);
                $table->unsignedTinyInteger('is_published')->default(1);
                $table->dateTime('created_at')->useCurrent();
                $table->dateTime('updated_at')->useCurrent();
            });
        }

        if (!Schema::hasTable('website_settings')) {
            Schema::create('website_settings', function (Blueprint $table) {
                $table->string('setting_key', 80)->primary();
                $table->text('setting_value')->nullable();
                $table->dateTime('updated_at')->useCurrent();
            });
        }
    }
}
