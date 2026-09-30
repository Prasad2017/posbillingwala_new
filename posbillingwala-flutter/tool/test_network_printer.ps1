$ip = '172.20.100.51'
$port = 9100
Write-Host "Testing TCP ${ip}:${port} ..."
try {
  $client = New-Object System.Net.Sockets.TcpClient
  $iar = $client.BeginConnect($ip, $port, $null, $null)
  $ok = $iar.AsyncWaitHandle.WaitOne(5000, $false)
  if (-not $ok) {
    $client.Close()
    Write-Host 'RESULT: TIMEOUT (5s) - printer not reachable on port 9100'
    exit 1
  }
  $client.EndConnect($iar)
  Write-Host "RESULT: CONNECTED"

  $stream = $client.GetStream()
  $stream.WriteTimeout = 5000

  $init = [byte[]](0x1B, 0x40)
  $text = [System.Text.Encoding]::ASCII.GetBytes(
    "`n*** POS Billingwala ***`nNetwork test print`nIP: 172.20.100.51`nPort: 9100`n`n"
  )
  $feedCut = [byte[]](0x0A, 0x0A, 0x0A, 0x0A, 0x1D, 0x56, 0x00)
  $bytes = New-Object byte[] ($init.Length + $text.Length + $feedCut.Length)
  [Array]::Copy($init, 0, $bytes, 0, $init.Length)
  [Array]::Copy($text, 0, $bytes, $init.Length, $text.Length)
  [Array]::Copy($feedCut, 0, $bytes, $init.Length + $text.Length, $feedCut.Length)

  $stream.Write($bytes, 0, $bytes.Length)
  $stream.Flush()
  Write-Host 'RESULT: TEST BYTES SENT'
  Start-Sleep -Milliseconds 800
  $stream.Close()
  $client.Close()
  Write-Host 'RESULT: DONE - check printer paper'
  exit 0
}
catch {
  Write-Host ("RESULT: FAIL - " + $_.Exception.Message)
  exit 1
}
