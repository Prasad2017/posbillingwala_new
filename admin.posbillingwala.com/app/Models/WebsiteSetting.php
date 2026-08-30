<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class WebsiteSetting extends Model
{
    protected $table = 'website_settings';

    protected $primaryKey = 'setting_key';

    public $incrementing = false;

    protected $keyType = 'string';

    public $timestamps = false;

    protected $fillable = [
        'setting_key',
        'setting_value',
        'updated_at',
    ];

    public static function getValue(string $key, string $default = ''): string
    {
        $row = static::find($key);

        return $row ? (string) $row->setting_value : $default;
    }

    public static function setValue(string $key, ?string $value): void
    {
        static::updateOrCreate(
            ['setting_key' => $key],
            ['setting_value' => $value ?? '', 'updated_at' => now()]
        );
    }

    public static function allMap(): array
    {
        return static::query()->pluck('setting_value', 'setting_key')->all();
    }
}
