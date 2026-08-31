<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;

class LinkPublicAssets extends Command
{
    protected $signature = 'admin:link-assets';

    protected $description = 'Link public/assets to the project assets folder for CSS, JS, and images';

    public function handle(): int
    {
        if (admin_sync_public_assets()) {
            $this->info('public/assets is ready.');

            return self::SUCCESS;
        }

        $this->error('Could not create public/assets symlink. Create it manually: ln -s ../assets public/assets');

        return self::FAILURE;
    }
}
