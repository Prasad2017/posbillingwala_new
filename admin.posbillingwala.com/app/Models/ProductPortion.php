<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

/**
 * Product + Portion Master selling price.
 * Portion Master has no price — price lives here only.
 */
class ProductPortion extends Model
{
    protected $primaryKey = 'portionId';
    protected $table = 'product_portions';
    public $timestamps = false;

    protected $fillable = [
        'userId',
        'productId',
        'portionMasterId',
        'portionName',
        'portionPrice',
        'portionSortOrder',
        'portionNetworkStatus',
        'portionStatus',
        'created_at',
        'updated_at',
    ];
}
