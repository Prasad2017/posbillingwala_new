<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

/**
 * Portion Master — name only. Price belongs on ProductPortion.
 */
class PortionMaster extends Model
{
    protected $primaryKey = 'portionMasterId';
    protected $table = 'portion_master';
    public $timestamps = false;

    protected $fillable = [
        'userId',
        'portionName',
        'portionMasterNetworkStatus',
        'portionMasterStatus',
        'created_at',
        'updated_at',
    ];
}
