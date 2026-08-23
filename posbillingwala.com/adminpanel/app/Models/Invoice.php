<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Invoice extends Model
{
    protected $table = 'invoice';

    public function products()
    {
    	return $this->hasMany('App\Models\InvoiceFinalProduct','invoiceNumber','invoiceNumber');
    }
}
