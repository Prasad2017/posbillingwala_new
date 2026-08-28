<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class WebsitePage extends Model
{
    protected $table = 'website_pages';

    public $timestamps = false;

    protected $fillable = ['slug', 'title', 'body_html', 'updated_at'];

    protected $casts = [
        'updated_at' => 'datetime',
    ];
}
