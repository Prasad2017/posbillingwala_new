<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('licenses', function (Blueprint $table) {
            $table->increments('id');
            $table->integer('userId');
            $table->text('licenseKey');
            $table->text('licenseValidity');
            $table->text('licenseType');
            $table->text('licenseStatus');
            $table->date('expiryDate');
            $table->text('paymentStatus');
            $table->integer('amount');
            $table->text('userType')->default('owner');
            $table->text('userName')->nullable();
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('licenses');
    }
};
