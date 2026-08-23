<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Models\User;
use Hash;
use DataTables;
use Auth;

class DealerController extends Controller
{
	public function getEditRecordPage($id)
	{
		$data = User::find($id);
		if($data)
		{
			return view('dealer.edit',compact('data'));
		}
		else{
			return redirect()->back()->with('error','Dealer not found');
		}
	}
	public function login(Request $request)
	{
		$validated = $request->validate([
			'aadhar_number' => 'required|digits:12',
			'password' => 'required',
		]);

		$user = User::where('aadhar_number',$request->aadhar_number)->where('is_active',1)->first();
		if($user)
		{
			if(Hash::check($request->password, $user->password)) 
			{
				Auth::login($user);
				return redirect('home');
			} 
			else 
			{
				return redirect()->back()->withErrors(['password'=>'Password does not match'])->withInput();
			}
		}
		else
		{
				return redirect()->back()->withErrors(['aadhar_number'=>'Dealer not found'])->withInput();
		}

	}

	public function getAllRecord(Request $request)
	{
		if($request->ajax())
		{
			$data = User::where('role_id',2)->withCount('customers')->get();
			return DataTables::of($data)->make(true);
		}
		return view('dealer.all');
	}

	public function getAddRecordPage()
	{
		return view('dealer.add');
	}
	public function addDealerRecord(Request $request)
	{
		$validated = $request->validate([
			'name' => 'required',
			'contact_number' => 'required|digits:10',
			'aadhar_number' => 'required|digits:12|unique:users',
			'password' => 'required|string|min:8|confirmed',
			'address' => 'required'
		]);

		$user = new User();
		$user->name = $request->name??null;
		$user->contact_number = $request->contact_number??null;
		$user->aadhar_number = $request->aadhar_number??null;
		$user->password = Hash::make($request->password);
		$user->address = $request->address??null;
		$user->role_id = 2;
		$user->save();

		return redirect('dealer/all')->with('success','Dealer registered successfully');
	}

	public function deleteRecord($id)
	{
		$data = User::find($id);
		if($data)
		{
			if($data->is_active == 1)
			{
				$data->is_active = 0;
				$data->save();
				return redirect()->back()->with('success','Dealer deleted successfully');
			}
			else
			{
				$data->is_active = 1;
				$data->save();
				return redirect()->back()->with('success','Dealer activated successfully');
			}
		}
			return redirect()->back()->with('error','Unable to delete dealer');
	}

	public function editDealerRecord(Request $request)
	{
		$validated = $request->validate([
			'name' => 'required',
			'contact_number' => 'required|digits:10',
			'aadhar_number' => 'required|digits:12',
			'password' => 'nullable|string|min:8|confirmed',
			'address' => 'required'
		]);


		$user = User::find($request->id);
		if($user)
		{
			$check_user = User::where('id','!=',$request->id)->where('aadhar_number',$request->aadhar_number)->first();
			if($check_user)
			{
				return redirect()->back()->withInput($request->input())->withErrors(['aadhar_number'=>'Aadhar number already taken by another dealer']);
			}
		}

		$user->name = $request->name??null;
		$user->contact_number = $request->contact_number??null;
		$user->aadhar_number = $request->aadhar_number??null;
		if($request->password)
		{
			$user->password = Hash::make($request->password);
		}
		$user->address = $request->address??null;
		$user->save();
		return redirect('dealer/all')->with('success','Dealer details updated successfully');

	}
}
