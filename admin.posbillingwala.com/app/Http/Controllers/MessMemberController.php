<?php

namespace App\Http\Controllers;

use App\Models\License;
use App\Models\User;
use App\Support\MessMemberBridge;
use Auth;
use Illuminate\Http\Request;

class MessMemberController extends Controller
{
    public function __construct()
    {
        $this->middleware('auth');
    }

    public function index(Request $request, $customerId)
    {
        $customerId = (int) $customerId;
        $ctx = $this->bootContext($customerId);
        $customer = $ctx['customer'];
        $con = $ctx['con'];
        $allowed = $ctx['allowed'];

        $licences = $this->licencesForCustomer($customerId, $allowed);
        $licenceId = $this->resolveSelectedLicenceId($request->query('licenceId'), $allowed, $licences);

        $members = [];
        if ($licenceId > 0) {
            $members = mess_remote_list_members($con, [$licenceId]);
        }

        return view('mess-members.index', [
            'customer' => $customer,
            'customerId' => $customerId,
            'licences' => $licences,
            'licenceId' => $licenceId,
            'members' => $members,
        ]);
    }

    public function create($customerId)
    {
        $customerId = (int) $customerId;
        $ctx = $this->bootContext($customerId);
        $allowed = $ctx['allowed'];
        $licences = $this->licencesForCustomer($customerId, $allowed);
        $licenceId = $this->resolveSelectedLicenceId(request()->query('licenceId'), $allowed, $licences);

        return view('mess-members.form', [
            'customer' => $ctx['customer'],
            'customerId' => $customerId,
            'licences' => $licences,
            'licenceId' => $licenceId,
            'member' => null,
            'isEdit' => false,
        ]);
    }

    public function store(Request $request, $customerId)
    {
        $customerId = (int) $customerId;
        $ctx = $this->bootContext($customerId);
        $con = $ctx['con'];
        $allowed = $ctx['allowed'];

        $validated = $request->validate([
            'licenceId' => 'required|integer',
            'memberName' => 'required|string|max:255',
            'memberMobileNumber' => 'required|string|max:20',
            'memberAltenetMobileNumber' => 'nullable|string|max:20',
            'memberAddress' => 'nullable|string|max:500',
            'memberType' => 'required|in:student,working',
            'rollNo' => 'nullable|string|max:100',
            'college' => 'nullable|string|max:255',
            'studentYear' => 'nullable|string|max:50',
            'company' => 'nullable|string|max:255',
            'registrationNo' => 'nullable|string|max:100',
        ]);

        $licenceId = (int) $validated['licenceId'];
        if (!mess_remote_assert_licence_access($allowed, $licenceId)) {
            abort(403, 'Licence not allowed for this customer.');
        }

        $fields = $this->memberFieldsFromRequest($validated);
        $fields['memberStatus'] = 'active';
        $fields['memberNetworkStatus'] = 'REMOTE-' . strtoupper(bin2hex(random_bytes(8)));

        $result = mess_remote_save_member($con, $licenceId, $fields);
        if (($result['status'] ?? '0') !== '1') {
            return redirect()
                ->back()
                ->withInput()
                ->with('error', $result['message'] ?? 'Failed to save member.');
        }

        return redirect()
            ->route('mess-members.index', ['customerId' => $customerId, 'licenceId' => $licenceId])
            ->with('success', $result['message'] ?? 'Member saved.');
    }

    public function edit($customerId, $memberId)
    {
        $customerId = (int) $customerId;
        $memberId = (int) $memberId;
        $ctx = $this->bootContext($customerId);
        $con = $ctx['con'];
        $allowed = $ctx['allowed'];

        $member = $this->findMember($con, $allowed, $memberId);
        if ($member === null) {
            abort(404, 'Member not found.');
        }

        $licences = $this->licencesForCustomer($customerId, $allowed);
        $licenceId = (int) ($member['licenceId'] ?? 0);

        return view('mess-members.form', [
            'customer' => $ctx['customer'],
            'customerId' => $customerId,
            'licences' => $licences,
            'licenceId' => $licenceId,
            'member' => $member,
            'isEdit' => true,
        ]);
    }

    public function update(Request $request, $customerId, $memberId)
    {
        $customerId = (int) $customerId;
        $memberId = (int) $memberId;
        $ctx = $this->bootContext($customerId);
        $con = $ctx['con'];
        $allowed = $ctx['allowed'];

        $existing = $this->findMember($con, $allowed, $memberId);
        if ($existing === null) {
            abort(404, 'Member not found.');
        }

        $validated = $request->validate([
            'licenceId' => 'required|integer',
            'memberName' => 'required|string|max:255',
            'memberMobileNumber' => 'required|string|max:20',
            'memberAltenetMobileNumber' => 'nullable|string|max:20',
            'memberAddress' => 'nullable|string|max:500',
            'memberType' => 'required|in:student,working',
            'rollNo' => 'nullable|string|max:100',
            'college' => 'nullable|string|max:255',
            'studentYear' => 'nullable|string|max:50',
            'company' => 'nullable|string|max:255',
            'registrationNo' => 'nullable|string|max:100',
        ]);

        $licenceId = (int) $validated['licenceId'];
        if (!mess_remote_assert_licence_access($allowed, $licenceId)) {
            abort(403, 'Licence not allowed for this customer.');
        }

        // Keep member on its original licence
        $licenceId = (int) ($existing['licenceId'] ?? $licenceId);

        $fields = $this->memberFieldsFromRequest($validated);
        $fields['memberId'] = $memberId;
        $fields['memberStatus'] = $existing['memberStatus'] ?? 'active';
        $fields['memberNetworkStatus'] = $existing['memberNetworkStatus'] ?? '';

        $result = mess_remote_save_member($con, $licenceId, $fields);
        if (($result['status'] ?? '0') !== '1') {
            return redirect()
                ->back()
                ->withInput()
                ->with('error', $result['message'] ?? 'Failed to update member.');
        }

        return redirect()
            ->route('mess-members.index', ['customerId' => $customerId, 'licenceId' => $licenceId])
            ->with('success', $result['message'] ?? 'Member updated.');
    }

    public function payments(Request $request, $customerId, $memberId)
    {
        $customerId = (int) $customerId;
        $memberId = (int) $memberId;
        $ctx = $this->bootContext($customerId);
        $con = $ctx['con'];
        $allowed = $ctx['allowed'];

        $member = $this->findMember($con, $allowed, $memberId);
        if ($member === null) {
            abort(404, 'Member not found.');
        }

        $licenceId = (int) ($member['licenceId'] ?? 0);
        $payments = mess_remote_list_payments($con, [$licenceId], $memberId);

        return view('mess-members.payments', [
            'customer' => $ctx['customer'],
            'customerId' => $customerId,
            'member' => $member,
            'licenceId' => $licenceId,
            'payments' => $payments,
        ]);
    }

    public function storePayment(Request $request, $customerId, $memberId)
    {
        $customerId = (int) $customerId;
        $memberId = (int) $memberId;
        $ctx = $this->bootContext($customerId);
        $con = $ctx['con'];
        $allowed = $ctx['allowed'];

        $member = $this->findMember($con, $allowed, $memberId);
        if ($member === null) {
            abort(404, 'Member not found.');
        }

        $validated = $request->validate([
            'paymentMessAmount' => 'required|string|max:50',
            'paymentPaidAmount' => 'required|string|max:50',
            'messTotalDays' => 'nullable|string|max:50',
            'paymentDate' => 'nullable|date',
        ]);

        $licenceId = (int) ($member['licenceId'] ?? 0);
        if (!mess_remote_assert_licence_access($allowed, $licenceId)) {
            abort(403);
        }

        $result = mess_remote_save_payment($con, $licenceId, [
            'memberId' => (string) $memberId,
            'memberName' => $member['memberName'] ?? '',
            'paymentMessAmount' => $validated['paymentMessAmount'],
            'paymentPaidAmount' => $validated['paymentPaidAmount'],
            'messTotalDays' => $validated['messTotalDays'] ?? '',
            'paymentDate' => $validated['paymentDate'] ?? date('Y-m-d'),
            'paymentStatus' => 'active',
            'paymentNetworkStatus' => 'REMOTE-PAY-' . strtoupper(bin2hex(random_bytes(8))),
        ]);

        if (($result['status'] ?? '0') !== '1') {
            return redirect()
                ->back()
                ->withInput()
                ->with('error', $result['message'] ?? 'Failed to save payment.');
        }

        return redirect()
            ->route('mess-members.payments', ['customerId' => $customerId, 'memberId' => $memberId])
            ->with('success', $result['message'] ?? 'Payment saved.');
    }

    public function excel($customerId)
    {
        $customerId = (int) $customerId;
        $ctx = $this->bootContext($customerId);
        $allowed = $ctx['allowed'];
        $licences = $this->licencesForCustomer($customerId, $allowed);
        $licenceId = $this->resolveSelectedLicenceId(request()->query('licenceId'), $allowed, $licences);

        return view('mess-members.excel', [
            'customer' => $ctx['customer'],
            'customerId' => $customerId,
            'licences' => $licences,
            'licenceId' => $licenceId,
        ]);
    }

    public function template()
    {
        MessMemberBridge::bootstrap();
        mess_remote_stream_template('mess_member_template.xlsx');
        exit;
    }

    public function export(Request $request, $customerId)
    {
        $customerId = (int) $customerId;
        $ctx = $this->bootContext($customerId);
        $con = $ctx['con'];
        $allowed = $ctx['allowed'];

        $request->validate([
            'licenceId' => 'required|integer',
        ]);

        $licenceId = (int) $request->input('licenceId');
        if (!mess_remote_assert_licence_access($allowed, $licenceId)) {
            abort(403, 'Licence not allowed for this customer.');
        }

        mess_remote_export_xlsx($con, [$licenceId], true, 'mess_members_' . $licenceId . '.xlsx');
        exit;
    }

    public function import(Request $request, $customerId)
    {
        $customerId = (int) $customerId;
        $ctx = $this->bootContext($customerId);
        $con = $ctx['con'];
        $allowed = $ctx['allowed'];

        $request->validate([
            'licenceId' => 'required|integer',
            'import_file' => 'required|file|mimes:xlsx|max:10240',
        ]);

        $licenceId = (int) $request->input('licenceId');
        if (!mess_remote_assert_licence_access($allowed, $licenceId)) {
            abort(403, 'Licence not allowed for this customer.');
        }

        $file = $request->file('import_file');
        $result = mess_remote_import_xlsx($con, $licenceId, $file->getRealPath());

        $redirect = redirect()->route('mess-members.excel', [
            'customerId' => $customerId,
            'licenceId' => $licenceId,
        ]);

        if (($result['status'] ?? '0') !== '1') {
            return $redirect->with('error', $result['message'] ?? 'Import failed.');
        }

        $message = $result['message'] ?? 'Import completed.';
        if (!empty($result['errors'])) {
            $firstErrors = array_slice($result['errors'], 0, 5);
            $extra = [];
            foreach ($firstErrors as $err) {
                $extra[] = ($err['sheet'] ?? '') . ' row ' . ($err['row'] ?? '?') . ': ' . ($err['message'] ?? '');
            }
            $message .= ' ' . implode(' | ', $extra);
        }

        return $redirect->with('success', $message);
    }

    /**
     * @return array{customer:User,con:\mysqli,allowed:int[]}
     */
    private function bootContext(int $customerId): array
    {
        if ($customerId <= 0 || !MessMemberBridge::authorizeCustomer($customerId)) {
            abort(403);
        }

        $customer = User::where('id', $customerId)->where('role_id', 3)->first();
        if ($customer === null) {
            abort(404, 'Customer not found.');
        }

        MessMemberBridge::bootstrap();
        $con = MessMemberBridge::connection();
        $allowed = $this->resolveAllowedLicenceIds($con, $customerId);
        if ($allowed === null) {
            abort(403, 'Customer not accessible.');
        }

        return [
            'customer' => $customer,
            'con' => $con,
            'allowed' => $allowed,
        ];
    }

    /**
     * @return int[]|null
     */
    private function resolveAllowedLicenceIds($con, int $customerId): ?array
    {
        $roleId = (int) Auth::user()->role_id;
        if ($roleId === 1) {
            return mess_remote_resolve_licence_ids_for_admin_customer($con, $customerId);
        }
        if ($roleId === 2) {
            return mess_remote_resolve_licence_ids_for_dealer_customer($con, (int) Auth::id(), $customerId);
        }

        return null;
    }

    /**
     * @param int[] $allowed
     * @return \Illuminate\Support\Collection
     */
    private function licencesForCustomer(int $customerId, array $allowed)
    {
        $query = License::where('userId', $customerId)->orderBy('id');
        if (!empty($allowed)) {
            $query->whereIn('id', $allowed);
        } else {
            $query->whereRaw('1 = 0');
        }

        return $query->get();
    }

    /**
     * @param int[] $allowed
     * @param \Illuminate\Support\Collection $licences
     */
    private function resolveSelectedLicenceId($requested, array $allowed, $licences): int
    {
        $licenceId = (int) $requested;
        if ($licenceId > 0 && mess_remote_assert_licence_access($allowed, $licenceId)) {
            return $licenceId;
        }
        if ($licences->isNotEmpty()) {
            return (int) $licences->first()->id;
        }

        return 0;
    }

    /**
     * @param int[] $allowed
     */
    private function findMember($con, array $allowed, int $memberId): ?array
    {
        if ($memberId <= 0 || empty($allowed)) {
            return null;
        }
        $members = mess_remote_list_members($con, $allowed);
        foreach ($members as $member) {
            if ((int) ($member['memberId'] ?? 0) === $memberId) {
                return $member;
            }
        }

        return null;
    }

    private function memberFieldsFromRequest(array $validated): array
    {
        return [
            'memberName' => $validated['memberName'],
            'memberMobileNumber' => $validated['memberMobileNumber'],
            'memberAltenetMobileNumber' => $validated['memberAltenetMobileNumber'] ?? '',
            'memberAddress' => $validated['memberAddress'] ?? '',
            'memberType' => $validated['memberType'],
            'rollNo' => $validated['rollNo'] ?? '',
            'college' => $validated['college'] ?? '',
            'studentYear' => $validated['studentYear'] ?? '',
            'company' => $validated['company'] ?? '',
            'registrationNo' => $validated['registrationNo'] ?? '',
        ];
    }
}
