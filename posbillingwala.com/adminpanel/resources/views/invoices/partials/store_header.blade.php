{{-- Structured store header for invoices (skips blank lines) --}}
@php
    $shopName1 = trim((string)($data->shopName1 ?? ''));
    if ($shopName1 === '') {
        $shopName1 = trim((string)($data->companyName ?? ''));
    }
    $shopName2 = trim((string)($data->shopName2 ?? ''));
    $addressLines = [];
    foreach (['addressLine1', 'addressLine2', 'addressLine3'] as $field) {
        $line = trim((string)($data->{$field} ?? ''));
        if ($line !== '') {
            $addressLines[] = $line;
        }
    }
    if (count($addressLines) === 0) {
        $legacy = trim((string)($data->companyAddress ?? ''));
        if ($legacy !== '') {
            $addressLines[] = $legacy;
        }
    }
    $phone1 = trim((string)($data->phoneNo1 ?? ''));
    if ($phone1 === '') {
        $phone1 = trim((string)($data->companyMobile ?? ''));
    }
    $phone2 = trim((string)($data->phoneNo2 ?? ''));
@endphp
@if($shopName1 !== '')
    <h2 class="name">
        <a target="_blank" href="javascript:;">{{ $shopName1 }}</a>
    </h2>
@endif
@if($shopName2 !== '')
    <div>{{ $shopName2 }}</div>
@endif
@foreach($addressLines as $line)
    <div>{{ $line }}</div>
@endforeach
@if($phone1 !== '')
    <div>{{ $phone1 }}</div>
@endif
@if($phone2 !== '')
    <div>{{ $phone2 }}</div>
@endif
