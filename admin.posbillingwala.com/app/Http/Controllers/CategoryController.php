<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Models\Category;
use App\Models\User;
use App\Models\FoodType;
use DataTables;
use Auth;

class CategoryController extends Controller
{
    public function getAllCategories(Request $request)
    {
    	if($request->ajax())
    	{
    		$data = Category::leftJoin('food_types', 'food_types.foodTypeId', '=', 'categories.foodTypeId')
                ->whereIn('categoryStatus',['active','inactive'])
                ->select('categories.*', 'food_types.foodTypeName');
            if($request->customer_id)
            {
                $data = $data->where('categories.userId', $request->customer_id);
            }
            elseif(Auth::user()->role_id==3)
            {
                $data = $data->where('categories.userId',Auth::user()->id);
            }
            elseif(Auth::user()->role_id==2)
            {
                $data = $data->where('categories.dealerId',Auth::user()->id);
            }
            return DataTables::of($data)->make(true);
        }

        $customers = User::where('is_active',1)->where('role_id',3);
        if(Auth::user()->role_id==2) {
            $customers = $customers->where('dealerId', Auth::id());
        }
        $customers = $customers->orderBy('name')->get();
        return view('categories.all', compact('customers'));
    }

    public function getAddRecordPage(){
    	$users = User::where('is_active',1)->where('role_id',3);
    	if(Auth::user()->role_id==2)
    	{
    		$users = $users->where('dealerId',Auth::user()->id);
    	}
    	$users = $users->get();
    	$foodTypes = FoodType::where('foodTypeStatus', 1)->orderBy('foodTypeSortOrder')->get();
    	return view('categories.add',compact('users','foodTypes'));
    }

    public function addCategoryRecord(Request $request)
    {
    	$validated = $request->validate([
         'category_name' => 'required|string'
     ]);

    	$data= new Category();
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

        $data->categoryName = $request->category_name??null;
        $data->foodTypeId = $request->food_type_id ?: null;
        $data->categoryNetworkStatus = substr(md5(time()), 0, 10);
        $data->save();

        return redirect('categories/all')->with('success','Category added successfully');
    }
    public function deleteRecord($id)
    {
      $data = Category::find($id);
      if($data)
      {
         if($data->categoryStatus == 'active')
         {
            $data->categoryStatus = 'inactive';
            $data->save();
            return redirect()->back()->with('success','Category deleted successfully');
        }
        else
        {
            $data->categoryStatus = 'active';
            $data->save();
            return redirect()->back()->with('success','Category activated successfully');
        }
    }
    return redirect()->back()->with('error','Unable to delete category');
}

public function getEditPage($id)
{
   $users = User::where('is_active',1)->where('role_id',3)->get();
   $data = Category::where('categoryId',$id)->first();
   $foodTypes = FoodType::where('foodTypeStatus', 1)->orderBy('foodTypeSortOrder')->get();
   if($data)
   {
     return view('categories.edit',compact('data','users','foodTypes'));
 }
 return redirect()->back()->with('error','Category details not found');
}

public function editCategoryRecord(Request $request)
{
   $validated = $request->validate([
     'category_name' => 'required|string'
 ]);

   $data=Category::where('categoryId',$request->id)->first();
   if($data)
   {
        $data->categoryName = $request->category_name??null;
        $data->foodTypeId = $request->food_type_id ?: null;
        if(Auth::user()->role_id==2)
        {
            $data->userId = $request->user_id ?? 0;
        }
        else
        {
            $data->userId = Auth::user()->id;
        }
        $data->save();
    }
return redirect('categories/all')->with('success','Category details updated successfully');
}
}

