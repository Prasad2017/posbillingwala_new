<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use DataTables;
use Auth;
use App\Models\Invoice;
use PDF;

class InvoiceController extends Controller
{
	public function getEditPage($id)
	{
		$data = Invoice::join('licenses','licenses.id','invoice.licenseId')
        ->join('companys','invoice.licenseId','companys.licenseId')
        ->join('users','users.id','licenses.userId')
        ->where('invoice.invoiceId',$id)->with('products')->first();
		if($data)
		{
			return view('invoices.edit',compact('data'));
		}
	}

    public function getInvoicePage(Request $request)
    {
    	if($request->ajax())
    	{
    		$data = Invoice::join('licenses','licenses.id','invoice.licenseId')->where('licenses.userId',Auth::user()->id);
    		return DataTables::of($data)
            ->addColumn('discount', function($value){
                    if($value->discountType=='Percentage')
                    {
                        $amount = ($value->subTotal * $value->discount)/100;
                        return $amount;                        
                    }
                    else
                        return $value->discount;
                })
            ->addColumn('totalAmount', function($value){
                    if($value->discountType=='Percentage')
                    {
                        $amount = ($value->subTotal * $value->discount)/100;
                        $main_amount = $value->subTotal + $amount + $value->totalGSTAmount;
                    }
                    else
                    {
                        $main_amount = $value->subTotal - $value->discount + $value->totalGSTAmount;
                    }
                        return $main_amount;   
                })
                ->rawColumns(['discount','totalAmount'])->make(true);
    	}
    	return view('invoices.all');
    }

    public function downloadInvoice($id)
    {
        $data = Invoice::join('users','users.id','invoice.userId')
        ->join('companys','invoice.userId','companys.userId')
        ->where('invoice.invoiceId',$id)->with('products')->first();
        if($data)
        {
            // return view('invoices.invoice',compact('data'));
            $pdf = PDF::loadView('invoices.invoice',compact('data'))->setOptions(['defaultFont' => 'sans-serif']);
            return $pdf->download($data->invoiceNumber.'.pdf');
        }
    }
}
