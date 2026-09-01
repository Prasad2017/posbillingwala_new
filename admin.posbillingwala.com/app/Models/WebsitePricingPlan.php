<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class WebsitePricingPlan extends Model
{
    protected $table = 'website_pricing_plans';

    public $timestamps = false;

    protected $fillable = [
        'plan_type',
        'validity_label',
        'price',
        'gst_note',
        'description',
        'sort_order',
        'is_published',
        'is_featured',
        'created_at',
        'updated_at',
    ];

    protected $casts = [
        'is_published' => 'boolean',
        'is_featured' => 'boolean',
        'sort_order' => 'integer',
        'price' => 'float',
    ];
}
