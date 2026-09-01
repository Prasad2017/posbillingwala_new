<?php

namespace App\Console\Commands;

use Database\Seeders\WebsiteMockDataSeeder;
use Illuminate\Console\Command;

class SeedWebsiteMockData extends Command
{
    protected $signature = 'website:seed-mock {--fresh : Clear existing website catalog data before seeding} {--force : Skip confirmation when using --fresh}';

    protected $description = 'Seed all POS Billingwala website mock data (products, pricing, dealers, customers, testimonials, settings, pages)';

    public function handle(): int
    {
        $fresh = (bool) $this->option('fresh');
        $force = (bool) $this->option('force');

        if ($fresh && !$force && !$this->confirm('This will delete all website products, pricing, dealers, customers, testimonials, settings and CMS pages. Continue?')) {
            $this->warn('Cancelled.');

            return self::SUCCESS;
        }

        $this->info($fresh ? 'Refreshing website mock data…' : 'Seeding website mock data (skips tables that already have rows)…');

        (new WebsiteMockDataSeeder())->run($fresh);

        $this->newLine();
        $this->info('Website mock data seeded successfully.');
        $this->line('  • Settings, products, pricing, dealers');
        $this->line('  • Customers & testimonials');
        $this->line('  • CMS pages (about, privacy, terms, support, company, refund)');

        return self::SUCCESS;
    }
}
