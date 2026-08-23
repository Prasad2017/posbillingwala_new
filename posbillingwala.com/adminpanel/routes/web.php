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
use App\Http\Controllers\ProductImportController;

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
	Route::get('all/{userId}/{categoryId}',[SubcategoryController::class, 'index']);
	Route::post('add/{userId}/{categoryId}',[SubcategoryController::class, 'store']);
	Route::get('delete/{id}',[SubcategoryController::class, 'deleteRecord']);
});

Route::group(['prefix' => 'portions', 'middleware' => ['auth']], function(){
	Route::get('all/{userId}/{productId}',[PortionController::class, 'index']);
	Route::post('add/{userId}/{productId}',[PortionController::class, 'store']);
	Route::get('delete/{id}',[PortionController::class, 'deleteRecord']);
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