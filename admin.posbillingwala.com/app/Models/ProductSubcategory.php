<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class ProductSubcategory extends Model
{
    protected $primaryKey = 'subcategoryId';
    protected $table = 'product_subcategories';
    public $timestamps = false;
}
