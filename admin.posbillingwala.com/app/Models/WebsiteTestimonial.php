<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class WebsiteTestimonial extends Model
{
    protected $table = 'website_testimonials';

    public $timestamps = false;

    protected $fillable = [
        'author_name',
        'business_name',
        'quote',
        'rating',
        'photo_path',
        'sort_order',
        'is_published',
        'created_at',
        'updated_at',
    ];

    protected $casts = [
        'is_published' => 'boolean',
        'sort_order' => 'integer',
        'rating' => 'integer',
    ];
}
