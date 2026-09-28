<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (!Schema::hasTable('licenses')) {
            return;
        }
        if (!Schema::hasColumn('licenses', 'businessType')) {
            Schema::table('licenses', function (Blueprint $table) {
                $table->string('businessType', 64)->default('')->after('mess');
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('licenses') && Schema::hasColumn('licenses', 'businessType')) {
            Schema::table('licenses', function (Blueprint $table) {
                $table->dropColumn('businessType');
            });
        }
    }
};
