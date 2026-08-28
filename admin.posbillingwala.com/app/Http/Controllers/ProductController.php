<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DataTables;
use App\Models\Product;
use App\Models\Category;
use App\Models\User;
use App\Models\Unit;
use App\Models\ProductSubcategory;
use Auth;

class ProductController extends Controller
{
	public function getCategories(Request $request)
	{
		if($request->ajax())
		{
			$categories = Category::where('userId',$request->user_id)->where('categoryStatus','active')->get();
			$html='<option value="">select</option>';
			foreach ($categories as $key => $value) 
			{
				$html.='<option value="'.$value->categoryId.'">'.$value->categoryName.'</option>';
			}
			return $html;
		}
	}

	public function getSubcategories(Request $request)
	{
		if($request->ajax())
		{
			$subcategories = ProductSubcategory::where('userId',$request->user_id)
                ->where('categoryId',$request->category_id)
                ->where('subcategoryStatus','active')
                ->get();
			$html='<option value="">select</option>';
			foreach ($subcategories as $value)
			{
				$html.='<option value="'.$value->subcategoryId.'">'.$value->subcategoryName.'</option>';
			}
			return $html;
		}
	}

	public function getAllProducts(Request $request)
	{
		if($request->ajax())
		{
			$data = Product::join('users','users.id','products.userId')->join('categories','categories.categoryId','products.categoryId');
			if($request->customer_id)
            {
                $data = $data->where('products.userId', $request->customer_id);
            }
			elseif(Auth::user()->role_id==3)
            {
                $data = $data->where('products.userId',Auth::user()->id);
            }
            elseif(Auth::user()->role_id==2)
            {
                $data = $data->where('products.dealerId',Auth::user()->id);
            }
			$data = $data->select('products.*', 'categories.categoryName','users.name as userName')
			->get();
			return DataTables::of($data)->make(true);
		}

        $customers = User::where('is_active',1)->where('role_id',3);
        if(Auth::user()->role_id==2) {
            $customers = $customers->where('dealerId', Auth::id());
        }
        $customers = $customers->orderBy('name')->get();
		return view('products.all', compact('customers'));
	}

	public function getAddRecordPage(Request $request){
		$categories = Category::where('categoryStatus','active')->get();
    	$users = User::where('is_active',1)->where('role_id',3);
    	if(Auth::user()->role_id==2)
    	{
    		$users = $users->where('dealerId',Auth::user()->id);
    	}
    	$users = $users->get();
		$units = Unit::where('is_active',1)->orderBy('name','ASC')->get();
		$selectedUserId = $request->user_id;
		return view('products.add',compact('categories','units','users','selectedUserId'));
	}

	public function addProductRecord(Request $request)
	{
		$validated = $request->validate([
			'category_id' => 'required|exists:categories,categoryId',
			'unit_id' => 'required|exists:units,name',
			'product_name' => 'required',
			'price' => 'required|numeric',
			'sgst' => 'nullable|numeric',
			'cgst' => 'nullable|numeric',
		]);

		$data= new Product();
		if(Auth::user()->role_id==2 || Auth::user()->role_id==1)
        {
            $data->userId = $request->user_id ?? 0;
            $data->dealerId = Auth::user()->role_id == 2 ? Auth::user()->id : (User::find($request->user_id)->dealerId ?? 0);
        }
        else
        {
            $data->userId = Auth::user()->id;
            $data->dealerId = Auth::user()->dealerId ?? 0;
        }
		$data->categoryId = $request->category_id ?? null;
		$data->subcategoryId = $request->subcategory_id ?: null;
		$data->productUnit = $request->unit_id ?? null;
		$data->productName = $request->product_name ?? null;
		$data->productPrice = $request->price ?? null;
		$data->productCGST = $request->cgst ?? null;
		$data->productSGST = $request->sgst ?? null;
		$data->productNetworkStatus = substr(md5(time()), 0, 10);
		$data->save();

		return redirect('products/all')->with('success','Product added successfully');
	}
	public function deleteRecord($id)
	{
		$data = Product::where('productId',$id)->first();
		if($data)
		{
			if($data->productStatus == 'active')
			{
				$data->productStatus = 'inactive';
				$data->save();
				return redirect()->back()->with('success','Product deleted successfully');
			}
			else
			{
				$data->productStatus = 'active';
				$data->save();
				return redirect()->back()->with('success','Product activated successfully');
			}
		}
		return redirect()->back()->with('error','Unable to delete product');
	}

	public function getEditPage($id)
	{
		$data = Product::where('productId',$id)->first();
		$categories = Category::where('categoryStatus','active')->get();
		$units = Unit::where('is_active',1)->orderBy('name','ASC')->get();
    	$users = User::where('is_active',1)->where('role_id',3)->get();
		$subcategories = [];
		if ($data && $data->categoryId) {
			$subcategories = ProductSubcategory::where('userId', $data->userId)
				->where('categoryId', $data->categoryId)
				->where('subcategoryStatus', 'active')
				->orderBy('subcategoryName')
				->get();
		}

		if($data)
		{
			return view('products.edit',compact('data','categories','units','users','subcategories'));
		}
		return redirect()->back()->with('error','Product details not found');
	}

	public function editProductRecord(Request $request)
	{
		$validated = $request->validate([
			'category_id' => 'required|exists:categories,categoryId',
			'unit_id' => 'required|exists:units,name',
			'product_name' => 'required',
			'price' => 'required|numeric',
			'sgst' => 'nullable|numeric',
			'cgst' => 'nullable|numeric',
		]);

		$data=Product::where('productId',$request->id)->first();
		if($data)
		{
			if(Auth::user()->role_id==2)
			{
				$data->userId = $request->user_id ?? 0;
			}
			$data->categoryId = $request->category_id ?? null;
			$data->subcategoryId = $request->subcategory_id ?: null;
			$data->productUnit = $request->unit_id ?? null;
			$data->productName = $request->product_name ?? null;
			$data->productPrice = $request->price ?? null;
			$data->productCGST = $request->cgst ?? null;
			$data->productSGST = $request->sgst ?? null;
			$data->save();
		}
		return redirect('products/all')->with('success','Product details updated successfully');
	}
}
