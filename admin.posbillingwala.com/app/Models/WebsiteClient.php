<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class WebsiteClient extends Model
{
    protected $table = 'website_clients';

    public $timestamps = false;

    protected $fillable = [
        'business_name',
        'subtitle',
        'description',
        'logo_path',
        'photo_path',
        'cta_url',
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
