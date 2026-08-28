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
        Schema::create('products', function (Blueprint $table) {
            $table->increments('productId');
            $table->integer('userId');
            $table->integer('categoryId');
            $table->text('productName')->nullable();
            $table->text('productUnit');
            $table->text('productPrice');
            $table->integer('productCGST')->nullable();
            $table->integer('productSGST')->nullable();
            $table->text('productStatus')->default('active');
            $table->text('productNetworkStatus');
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
        Schema::dropIfExists('products');
    }
};
