<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class FoodType extends Model
{
    protected $primaryKey = 'foodTypeId';
    protected $table = 'food_types';
    public $timestamps = false;
}
