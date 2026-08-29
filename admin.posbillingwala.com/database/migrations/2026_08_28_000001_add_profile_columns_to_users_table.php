<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            if (!Schema::hasColumn('users', 'address')) {
                $table->text('address')->nullable()->after('contact_number');
            }
            if (!Schema::hasColumn('users', 'shopName')) {
                $table->string('shopName')->nullable()->after('address');
            }
            if (!Schema::hasColumn('users', 'dealerId')) {
                $table->unsignedInteger('dealerId')->default(0)->after('role_id');
            }
        });
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            if (Schema::hasColumn('users', 'address')) {
                $table->dropColumn('address');
            }
            if (Schema::hasColumn('users', 'shopName')) {
                $table->dropColumn('shopName');
            }
            if (Schema::hasColumn('users', 'dealerId')) {
                $table->dropColumn('dealerId');
            }
        });
    }
};
