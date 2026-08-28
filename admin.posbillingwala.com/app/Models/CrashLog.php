<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class CrashLog extends Model
{
    protected $table = 'admin_crash_logs';
    protected $guarded = [];
    public $timestamps = false;
}
