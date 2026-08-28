<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\HomeController;
use App\Http\Controllers\DealerController;
use App\Http\Controllers\CategoryController;
use App\Http\Controllers\ProductController;
use App\Http\Controllers\CustomerController;
use App\Http\Controllers\ExpenseController;
use App\Http\Controllers\InvoiceController;
use App\Http\Controllers\SubcategoryController;
use App\Http\Controllers\PortionController;
use App\Http\Controllers\PortionMasterController;
use App\Http\Controllers\ProductImportController;
use App\Http\Controllers\SalesController;
use App\Http\Controllers\ReportController;
use App\Http\Controllers\DeviceController;
use App\Http\Controllers\CrashController;
use App\Http\Controllers\SupportController;
use App\Http\Controllers\SettingsController;
use App\Http\Controllers\WebsiteContentController;

/*
|--------------------------------------------------------------------------
| Web Routes
|--------------------------------------------------------------------------
|
| Here is where you can register web routes for your application. These
| routes are loaded by the RouteServiceProvider within a group which
| contains the "web" middleware group. Now create something great!
|
*/

Route::get('/', function () {
    return redirect('login');
});

Route::get('customer/login', function () {
    return view('customers.login');
});

Route::get('dealer/login', function () {
    return view('dealer.login');
});
Route::post('dealer/login', [DealerController::class, 'login'])->name('dealer.login');
Route::post('customer/login', [CustomerController::class, 'login'])->name('customer.login');

Auth::routes();
Route::get('/home', [HomeController::class, 'index'])->name('home');

Route::group(['prefix' => 'dealer', 'middleware' => ['auth']], function(){
	Route::get('all',[DealerController::class, 'getAllRecord']);
	Route::get('add',[DealerController::class, 'getAddRecordPage']);
	Route::get('delete/{id}',[DealerController::class, 'deleteRecord']);
	Route::post('add',[DealerController::class, 'addDealerRecord']);
	Route::get('edit/{id}',[DealerController::class, 'getEditRecordPage']);
	Route::post('edit/{id}',[DealerController::class, 'editDealerRecord']);

});

Route::group(['prefix' => 'categories', 'middleware' => ['auth']], function(){
	Route::get('all',[CategoryController::class, 'getAllCategories']);
	Route::get('add',[CategoryController::class, 'getAddRecordPage']);
	Route::get('edit/{id}',[CategoryController::class, 'getEditPage']);
	Route::post('edit/{id}',[CategoryController::class, 'editCategoryRecord']);
	Route::get('delete/{id}',[CategoryController::class, 'deleteRecord']);
	Route::post('add',[CategoryController::class, 'addCategoryRecord']);
});

Route::group(['prefix' => 'products', 'middleware' => ['auth']], function(){
	Route::get('all',[ProductController::class, 'getAllProducts']);
	Route::get('add',[ProductController::class, 'getAddRecordPage']);
	Route::get('edit/{id}',[ProductController::class, 'getEditPage']);
	Route::post('edit/{id}',[ProductController::class, 'editProductRecord']);
	Route::get('get-categories',[ProductController::class, 'getCategories']);
	Route::get('get-subcategories',[ProductController::class, 'getSubcategories']);
	Route::get('delete/{id}',[ProductController::class, 'deleteRecord']);
	Route::post('add',[ProductController::class, 'addProductRecord']);
});

Route::group(['prefix' => 'subcategories', 'middleware' => ['auth']], function(){
	Route::get('all',[SubcategoryController::class, 'getAllSubcategories']);
	Route::get('add',[SubcategoryController::class, 'getAddRecordPage']);
	Route::post('add',[SubcategoryController::class, 'addSubcategoryRecord']);
	Route::get('all/{userId}/{categoryId}',[SubcategoryController::class, 'index']);
	Route::post('add/{userId}/{categoryId}',[SubcategoryController::class, 'store']);
	Route::get('delete/{id}',[SubcategoryController::class, 'deleteRecord']);
});

Route::group(['prefix' => 'portions', 'middleware' => ['auth']], function(){
	Route::get('all/{userId}/{productId}',[PortionController::class, 'index']);
	Route::post('add/{userId}/{productId}',[PortionController::class, 'store']);
	Route::get('delete/{id}',[PortionController::class, 'deleteRecord']);
});

Route::group(['prefix' => 'portion-masters', 'middleware' => ['auth']], function(){
	Route::get('all',[PortionMasterController::class, 'getAll']);
	Route::get('add',[PortionMasterController::class, 'getAddPage']);
	Route::post('add',[PortionMasterController::class, 'store']);
	Route::get('toggle/{id}',[PortionMasterController::class, 'toggle']);
});

Route::group(['prefix' => 'product-import', 'middleware' => ['auth']], function(){
	Route::get('/',[ProductImportController::class, 'index']);
	Route::post('upload',[ProductImportController::class, 'import']);
});

Route::group(['prefix' => 'customers', 'middleware' => ['auth']], function(){
	Route::get('all',[CustomerController::class, 'getAllCustomers']);
	Route::get('add',[CustomerController::class, 'getAddRecordPage']);
	Route::get('edit/{id}',[CustomerController::class, 'getEditPage']);
	Route::post('edit/{id}',[CustomerController::class, 'editCustomerRecord']);
	Route::get('delete/{id}',[CustomerController::class, 'deleteRecord']);
	Route::post('add',[CustomerController::class, 'addCustomerRecord']);

	Route::get('all-license',[CustomerController::class, 'getLicenseList']);
	Route::get('add-license/{id}',[CustomerController::class, 'addLicensePage']);
	Route::post('add-license/{id}',[CustomerController::class, 'addLicenseData']);
	Route::get('edit-license/{id}',[CustomerController::class, 'editLicensePage']);
	Route::get('delete-license/{id}',[CustomerController::class, 'deleteLicenseData']);
	Route::post('edit-license/{id}',[CustomerController::class, 'editLicenseData']);

});

Route::group(['prefix' => 'expenses', 'middleware' => ['auth']], function(){
	Route::get('all',[ExpenseController::class, 'getExpensePage']);
	Route::get('edit/{id}',[ExpenseController::class, 'getEditPage']);
});
Route::group(['prefix' => 'inventory', 'middleware' => ['auth']], function(){
	Route::get('all',[ExpenseController::class, 'getInventoryPage']);
	Route::get('edit/{id}',[ExpenseController::class, 'getEditPage']);
});
Route::group(['prefix' => 'invoices', 'middleware' => ['auth']], function(){
	Route::get('all',[InvoiceController::class, 'getInvoicePage']);
	Route::get('edit/{id}',[InvoiceController::class, 'getEditPage']);
	Route::get('download/{id}',[InvoiceController::class, 'downloadInvoice']);
});

Route::middleware(['auth'])->group(function () {
	Route::get('sales/dashboard', [SalesController::class, 'dashboard']);
	Route::get('sales/overview', [SalesController::class, 'overview']);
	Route::get('sales/invoices', [SalesController::class, 'invoices']);
	Route::get('sales/invoices/{id}', [SalesController::class, 'invoiceDetails']);

	Route::get('reports', [ReportController::class, 'hub']);
	Route::get('reports/customers', [ReportController::class, 'customers']);
	Route::get('reports/licenses', [ReportController::class, 'licenses']);
	Route::get('reports/dealers', [ReportController::class, 'dealers']);
	Route::get('reports/branches', [ReportController::class, 'branches']);
	Route::get('reports/devices', [ReportController::class, 'devices']);

	Route::get('devices', [DeviceController::class, 'index']);

	Route::get('crashes', [CrashController::class, 'index']);
	Route::get('crashes/analytics', [CrashController::class, 'analytics']);
	Route::get('crashes/errors', [CrashController::class, 'errors']);
	Route::get('crashes/errors/{id}', [CrashController::class, 'errorShow']);
	Route::post('crashes/errors/{id}/resolve', [CrashController::class, 'resolveError']);
	Route::get('crashes/{id}', [CrashController::class, 'show']);
	Route::post('crashes/{id}/status', [CrashController::class, 'updateStatus']);

	Route::get('support', [SupportController::class, 'hub']);
	Route::get('support/faq', [SupportController::class, 'faq']);
	Route::get('support/tickets', [SupportController::class, 'tickets']);
	Route::get('support/tickets/create', [SupportController::class, 'create']);
	Route::post('support/tickets', [SupportController::class, 'store']);
	Route::get('support/tickets/{id}', [SupportController::class, 'show']);
	Route::post('support/tickets/{id}/reply', [SupportController::class, 'reply']);
	Route::post('support/tickets/{id}/status', [SupportController::class, 'updateStatus']);

	Route::get('settings', [SettingsController::class, 'hub']);
	Route::get('settings/profile', [SettingsController::class, 'profile']);
	Route::post('settings/profile', [SettingsController::class, 'updateProfile']);
	Route::get('settings/password', [SettingsController::class, 'password']);
	Route::post('settings/password', [SettingsController::class, 'updatePassword']);
	Route::get('settings/logo', [SettingsController::class, 'logo']);
	Route::post('settings/logo', [SettingsController::class, 'updateLogo']);
	Route::get('settings/favicon', [SettingsController::class, 'favicon']);
	Route::post('settings/favicon', [SettingsController::class, 'updateFavicon']);

	Route::get('website', [WebsiteContentController::class, 'hub']);
	Route::get('website/privacy', [WebsiteContentController::class, 'privacy']);
	Route::post('website/privacy', [WebsiteContentController::class, 'updatePrivacy']);
	Route::get('website/about', [WebsiteContentController::class, 'about']);
	Route::post('website/about', [WebsiteContentController::class, 'updateAbout']);
	Route::get('website/clients', [WebsiteContentController::class, 'clients']);
	Route::get('website/clients/add', [WebsiteContentController::class, 'clientAdd']);
	Route::post('website/clients/add', [WebsiteContentController::class, 'clientStore']);
	Route::get('website/clients/edit/{id}', [WebsiteContentController::class, 'clientEdit']);
	Route::post('website/clients/edit/{id}', [WebsiteContentController::class, 'clientUpdate']);
	Route::get('website/clients/toggle/{id}', [WebsiteContentController::class, 'clientToggle']);
	Route::get('website/clients/delete/{id}', [WebsiteContentController::class, 'clientDelete']);
	Route::get('website/testimonials', [WebsiteContentController::class, 'testimonials']);
	Route::get('website/testimonials/add', [WebsiteContentController::class, 'testimonialAdd']);
	Route::post('website/testimonials/add', [WebsiteContentController::class, 'testimonialStore']);
	Route::get('website/testimonials/edit/{id}', [WebsiteContentController::class, 'testimonialEdit']);
	Route::post('website/testimonials/edit/{id}', [WebsiteContentController::class, 'testimonialUpdate']);
	Route::get('website/testimonials/toggle/{id}', [WebsiteContentController::class, 'testimonialToggle']);
	Route::get('website/testimonials/delete/{id}', [WebsiteContentController::class, 'testimonialDelete']);
	Route::get('website/contacts', [WebsiteContentController::class, 'contacts']);
	Route::get('website/contacts/{id}', [WebsiteContentController::class, 'contactShow']);
	Route::post('website/contacts/{id}/status', [WebsiteContentController::class, 'contactUpdateStatus']);
});