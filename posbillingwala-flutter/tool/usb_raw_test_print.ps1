$printerName = 'POS-80-RAW'
$docName = 'POS Billingwala USB Test'

$init = [byte[]](0x1B, 0x40)
$text = [System.Text.Encoding]::ASCII.GetBytes(
@"

*** POS Billingwala ***
USB TEST PRINT
Printer: POS-80
VID_0418 PID_5011

If you see this, USB works.


"@
)
$feedCut = [byte[]](0x0A, 0x0A, 0x0A, 0x0A, 0x1D, 0x56, 0x00)
$payload = New-Object byte[] ($init.Length + $text.Length + $feedCut.Length)
[Array]::Copy($init, 0, $payload, 0, $init.Length)
[Array]::Copy($text, 0, $payload, $init.Length, $text.Length)
[Array]::Copy($feedCut, 0, $payload, $init.Length + $text.Length, $feedCut.Length)

$src = @'
using System;
using System.Runtime.InteropServices;
public class RawPrint {
  [StructLayout(LayoutKind.Sequential, CharSet=CharSet.Ansi)]
  public class DOCINFOA {
    [MarshalAs(UnmanagedType.LPStr)] public string pDocName;
    [MarshalAs(UnmanagedType.LPStr)] public string pOutputFile;
    [MarshalAs(UnmanagedType.LPStr)] public string pDataType;
  }
  [DllImport("winspool.Drv", EntryPoint="OpenPrinterA", SetLastError=true, CharSet=CharSet.Ansi, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]
  public static extern bool OpenPrinter([MarshalAs(UnmanagedType.LPStr)] string szPrinter, out IntPtr hPrinter, IntPtr pd);
  [DllImport("winspool.Drv", EntryPoint="ClosePrinter", SetLastError=true, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]
  public static extern bool ClosePrinter(IntPtr hPrinter);
  [DllImport("winspool.Drv", EntryPoint="StartDocPrinterA", SetLastError=true, CharSet=CharSet.Ansi, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]
  public static extern bool StartDocPrinter(IntPtr hPrinter, int level, [In, MarshalAs(UnmanagedType.LPStruct)] DOCINFOA di);
  [DllImport("winspool.Drv", EntryPoint="EndDocPrinter", SetLastError=true, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]
  public static extern bool EndDocPrinter(IntPtr hPrinter);
  [DllImport("winspool.Drv", EntryPoint="StartPagePrinter", SetLastError=true, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]
  public static extern bool StartPagePrinter(IntPtr hPrinter);
  [DllImport("winspool.Drv", EntryPoint="EndPagePrinter", SetLastError=true, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]
  public static extern bool EndPagePrinter(IntPtr hPrinter);
  [DllImport("winspool.Drv", EntryPoint="WritePrinter", SetLastError=true, ExactSpelling=true, CallingConvention=CallingConvention.StdCall)]
  public static extern bool WritePrinter(IntPtr hPrinter, IntPtr pBytes, int dwCount, out int dwWritten);

  public static bool SendBytes(string printer, string doc, byte[] bytes) {
    IntPtr hPrinter;
    if (!OpenPrinter(printer, out hPrinter, IntPtr.Zero)) return false;
    var di = new DOCINFOA();
    di.pDocName = doc;
    di.pDataType = "RAW";
    if (!StartDocPrinter(hPrinter, 1, di)) { ClosePrinter(hPrinter); return false; }
    if (!StartPagePrinter(hPrinter)) { EndDocPrinter(hPrinter); ClosePrinter(hPrinter); return false; }
    IntPtr p = Marshal.AllocCoTaskMem(bytes.Length);
    Marshal.Copy(bytes, 0, p, bytes.Length);
    int written;
    bool ok = WritePrinter(hPrinter, p, bytes.Length, out written);
    Marshal.FreeCoTaskMem(p);
    EndPagePrinter(hPrinter);
    EndDocPrinter(hPrinter);
    ClosePrinter(hPrinter);
    return ok && written == bytes.Length;
  }
}
'@

Add-Type -TypeDefinition $src -Language CSharp -ErrorAction Stop
$ok = [RawPrint]::SendBytes($printerName, $docName, $payload)
if ($ok) {
  Write-Host "RESULT: SUCCESS - raw ESC/POS sent to $printerName ($($payload.Length) bytes)"
  Write-Host 'Check the printer paper for the test slip.'
  exit 0
} else {
  Write-Host "RESULT: FAIL - WritePrinter error $($LASTEXITCODE) / lastWin32=$([Runtime.InteropServices.Marshal]::GetLastWin32Error())"
  exit 1
}
