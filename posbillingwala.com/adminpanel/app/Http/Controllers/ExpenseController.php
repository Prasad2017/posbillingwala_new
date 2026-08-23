<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DataTables;
use Auth;
use App\Models\Expense;
use App\Models\Inventory;

class ExpenseController extends Controller
{
    public function getExpensePage(Request $request)
    {
    	if($request->ajax())
    	{
    		$data = Expense::where('userId',Auth::user()->id)->get();
    		return DataTables::of($data)->make(true);
    	}
    	return view('expenses.all');
    }

    public function getInventoryPage(Request $request)
    {
    	if($request->all())
    	{
    		$data = Inventory::join('products','products.productId','inventory.productId')
    		->where('inventory.userId',Auth::user()->id)
    		->select('inventory.*','products.productName')
    		->get();
    		return DataTables::of($data)->make(true);
    	}
    	return view('inventory.all');
    }
}
