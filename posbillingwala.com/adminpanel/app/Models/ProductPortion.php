<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class ProductPortion extends Model
{
    protected $primaryKey = 'portionId';
    protected $table = 'product_portions';
    public $timestamps = false;
}
