<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class WebsiteDealer extends Model
{
    protected $table = 'website_dealers';

    public $timestamps = false;

    protected $fillable = [
        'area',
        'dealer_name',
        'contact_person',
        'role_title',
        'mobile',
        'whatsapp',
        'address',
        'map_url',
        'dealer_type',
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
