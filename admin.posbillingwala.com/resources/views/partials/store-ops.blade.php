@php
    $storeOps = $storeOps ?? ['staff' => collect(), 'devices' => collect(), 'printers' => collect(), 'routes' => collect()];
@endphp
<div class="col-12">
    <div class="card border mt-2">
        <div class="card-body">
            <h6 class="text-primary mb-3"><i class="bx bx-group"></i> Store operations (users, devices, printers)</h6>
            <div class="row">
                <div class="col-lg-6 mb-3">
                    <strong>Users</strong>
                    <ul class="small mb-0 mt-2">
                        @forelse($storeOps['staff'] as $staff)
                            <li>{{ $staff->name }} — {{ $staff->role }} — {{ $staff->mobileNumber }} ({{ $staff->status }})</li>
                        @empty
                            <li class="text-secondary">No POS staff rows. Enable User Management and create users in POS.</li>
                        @endforelse
                    </ul>
                </div>
                <div class="col-lg-6 mb-3">
                    <strong>Devices</strong>
                    <ul class="small mb-0 mt-2">
                        @forelse($storeOps['devices'] as $device)
                            <li>{{ $device->deviceName ?: $device->deviceId }} — {{ $device->platform }} — {{ $device->status }}{{ (int) $device->isPrintHost === 1 ? ' — Print Host' : '' }}</li>
                        @empty
                            <li class="text-secondary">No extra POS devices registered.</li>
                        @endforelse
                    </ul>
                </div>
                <div class="col-lg-6 mb-3">
                    <strong>Printers</strong>
                    <ul class="small mb-0 mt-2">
                        @forelse($storeOps['printers'] as $printer)
                            <li>{{ $printer->printerName }} — {{ $printer->connectionType }} — {{ $printer->paperSize ?? '2-Inch' }} — {{ $printer->purpose }} {{ (int) $printer->enabled === 1 ? '(on)' : '(off)' }}</li>
                        @empty
                            <li class="text-secondary">Using existing bill/KOT printer settings.</li>
                        @endforelse
                    </ul>
                </div>
                <div class="col-lg-6 mb-3">
                    <strong>Printer routing</strong>
                    <ul class="small mb-0 mt-2">
                        @forelse($storeOps['routes'] as $route)
                            <li>{{ $route->documentType }} → printer #{{ $route->printerId }} {{ $route->foodTypeCode }}</li>
                        @empty
                            <li class="text-secondary">No category routes yet.</li>
                        @endforelse
                    </ul>
                </div>
            </div>
        </div>
    </div>
</div>
