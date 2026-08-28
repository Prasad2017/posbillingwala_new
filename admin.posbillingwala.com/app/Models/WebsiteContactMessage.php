<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class WebsiteContactMessage extends Model
{
    protected $table = 'website_contact_messages';

    public $timestamps = false;

    protected $fillable = [
        'name',
        'email',
        'subject',
        'message',
        'status',
        'source_ip',
        'created_at',
        'updated_at',
    ];

    protected $casts = [
        'created_at' => 'datetime',
        'updated_at' => 'datetime',
    ];
}
