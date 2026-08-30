<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class WebsiteProduct extends Model
{
    protected $table = 'website_products';

    public $timestamps = false;

    protected $fillable = [
        'name',
        'category',
        'description',
        'icon',
        'sort_order',
        'is_published',
        'created_at',
        'updated_at',
    ];

    protected $casts = [
        'is_published' => 'boolean',
        'sort_order' => 'integer',
    ];
}
