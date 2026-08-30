<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;
use App\Http\Controllers\Api\WebsitePublicController;

/*
|--------------------------------------------------------------------------
| API Routes
|--------------------------------------------------------------------------
|
| Here is where you can register API routes for your application. These
| routes are loaded by the RouteServiceProvider within a group which
| is assigned the "api" middleware group. Enjoy building your API!
|
*/

Route::middleware('auth:sanctum')->get('/user', function (Request $request) {
    return $request->user();
});

Route::prefix('website')->group(function () {
    Route::get('clients', [WebsitePublicController::class, 'clients']);
    Route::get('testimonials', [WebsitePublicController::class, 'testimonials']);
    Route::get('dealers', [WebsitePublicController::class, 'dealers']);
    Route::get('pricing', [WebsitePublicController::class, 'pricing']);
    Route::get('products', [WebsitePublicController::class, 'products']);
    Route::get('settings', [WebsitePublicController::class, 'settings']);
    Route::get('pages/{slug}', [WebsitePublicController::class, 'page']);
    Route::post('contact', [WebsitePublicController::class, 'submitContact']);
});
