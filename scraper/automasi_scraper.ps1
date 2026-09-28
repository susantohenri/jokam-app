# ==============================================================================
# Script Automasi Scraper Google Maps LDII Indonesia
# Lingkungan: Windows PowerShell
# ==============================================================================
param(
    [int]$SleepSeconds = 5400,     # 90 menit jeda antar batch
    [switch]$ResetCheckpoint = $false
)

$ErrorActionPreference = "Stop"

# Pindah ke direktori script
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$BinaryName = "google-maps-scraper.exe"
$BinaryPath = Join-Path $ScriptDir $BinaryName
$QueriesFile = Join-Path $ScriptDir "queries.txt"
$BatchDir = Join-Path $ScriptDir "batches"
$RawProxyFile = Join-Path $ScriptDir "proxies.txt"
$CleanProxyFile = Join-Path $ScriptDir "active_proxies.tmp"
$OutputFile = Join-Path $ScriptDir "hasil_ldii_indonesia.csv"
$CheckpointFile = Join-Path $ScriptDir "checkpoint.txt"
$LogFile = Join-Path $ScriptDir "scraper.log"
$StatusFile = Join-Path $ScriptDir "status.json"

function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    $logLine = "[$timestamp] [$Level] $Message"
    Add-Content -Path $LogFile -Value $logLine
    
    switch ($Level) {
        "ERROR"   { Write-Host $logLine -ForegroundColor Red }
        "WARN"    { Write-Host $logLine -ForegroundColor Yellow }
        "SUCCESS" { Write-Host $logLine -ForegroundColor Green }
        default   { Write-Host $logLine -ForegroundColor Cyan }
    }
}

function Update-Status {
    param(
        [string]$Status,
        [int]$CurrentBatch,
        [int]$TotalBatches,
        [int]$CompletedCount,
        [string]$CurrentBatchFile,
        [string]$NextRunTime = ""
    )
    $recordCount = 0
    if (Test-Path $OutputFile) {
        $recordCount = [Math]::Max(0, (Get-Content $OutputFile).Count - 1)
    }
    $percent = 0
    if ($TotalBatches -gt 0) {
        $percent = [Math]::Round(($CompletedCount / $TotalBatches) * 100, 2)
    }
    
    $statusObj = [PSCustomObject]@{
        status            = $Status
        current_batch     = $CurrentBatch
        total_batches     = $TotalBatches
        completed_batches = $CompletedCount
        percent_complete  = $percent
        total_records     = $recordCount
        current_file      = $CurrentBatchFile
        next_run_at       = $NextRunTime
        last_updated      = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    }
    
    $statusObj | ConvertTo-Json -Depth 2 | Set-Content -Path $StatusFile -Encoding utf8
}

Write-Host ""
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "  AUTOMASI GOOGLE MAPS SCRAPER - LDII INDONESIA (514 WILAYAH)       " -ForegroundColor Yellow
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Cek Binary
if (-not (Test-Path $BinaryPath)) {
    Write-Log "File binary '$BinaryName' tidak ditemukan di $ScriptDir!" "ERROR"
    exit 1
}

# 2. Cek & Siapkan Proxy
if (-not (Test-Path $RawProxyFile)) {
    Write-Log "File '$RawProxyFile' tidak ditemukan!" "ERROR"
    exit 1
}

# Filter & format proxy otomatis (mendukung http://user:pass@ip:port maupun ip:port:user:pass)
$validProxies = @()
foreach ($line in (Get-Content $RawProxyFile)) {
    $trimmed = $line.Trim()
    if ($trimmed -eq "" -or $trimmed.StartsWith("#")) { continue }
    
    # Deteksi format IP:PORT:USER:PASS
    if ($trimmed -match '^([^:@]+):(\d+):([^:@]+):([^:@]+)$') {
        $validProxies += "http://$($Matches[3]):$($Matches[4])@$($Matches[1]):$($Matches[2])"
    }
    # Deteksi format IP:PORT tanpa auth
    elseif ($trimmed -match '^([^:@]+):(\d+)$') {
        $validProxies += "http://$($Matches[1]):$($Matches[2])"
    }
    # Format yang sudah memiliki skema protocol (http://, socks5://, dll)
    else {
        $validProxies += $trimmed
    }
}

if ($validProxies.Count -eq 0) {
    Write-Log "File 'proxies.txt' belum diisi proxy yang valid!" "ERROR"
    Write-Host "Silakan buka 'proxies.txt' dan masukkan 10 proxy Anda (1 proxy per baris), lalu jalankan script ini kembali." -ForegroundColor Yellow
    exit 1
}

$validProxies | Set-Content -Path $CleanProxyFile -Encoding ascii
Write-Log "Proxy terdeteksi: $($validProxies.Count) proxy aktif siap digunakan." "SUCCESS"

# 3. Cek Batch Directory
if (-not (Test-Path $BatchDir) -or (Get-ChildItem $BatchDir -Filter "*.txt").Count -eq 0) {
    Write-Log "Folder batch belum ada atau kosong. Menjalankan generate_queries.py..." "WARN"
    python (Join-Path $ScriptDir "generate_queries.py")
}

$batchFiles = Get-ChildItem -Path $BatchDir -Filter "batch_*.txt" | Sort-Object Name
$totalBatches = $batchFiles.Count

if ($totalBatches -eq 0) {
    Write-Log "Tidak ada file batch di folder '$BatchDir'!" "ERROR"
    exit 1
}

# 4. Handle Checkpoint
if ($ResetCheckpoint -and (Test-Path $CheckpointFile)) {
    Remove-Item $CheckpointFile -Force
    Write-Log "Checkpoint direset sesuai parameter -ResetCheckpoint." "WARN"
}

$completedBatches = @()
if (Test-Path $CheckpointFile) {
    $completedBatches = Get-Content $CheckpointFile | Where-Object { $_.Trim() -ne "" }
}

$remainingBatches = $batchFiles | Where-Object { $completedBatches -notcontains $_.Name }
$completedCount = $completedBatches.Count

Write-Host "Total Batch : $totalBatches batch (~10 kueri per batch)" -ForegroundColor White
Write-Host "Selesai     : $completedCount batch" -ForegroundColor Green
Write-Host "Sisa        : $($remainingBatches.Count) batch" -ForegroundColor Yellow
Write-Host "Jeda Cooldown: $([Math]::Round($SleepSeconds / 60)) menit per batch" -ForegroundColor Gray
Write-Host "Output File : $OutputFile" -ForegroundColor White
Write-Host "Log File    : $LogFile" -ForegroundColor White
Write-Host "--------------------------------------------------------------------" -ForegroundColor Gray
Write-Host ""

if ($remainingBatches.Count -eq 0) {
    Write-Log "Semua $totalBatches batch sudah selesai dikerjakan! Tidak ada tugas tersisa." "SUCCESS"
    Update-Status -Status "finished" -CurrentBatch $totalBatches -TotalBatches $totalBatches -CompletedCount $totalBatches -CurrentBatchFile "none"
    exit 0
}

# 5. Looping Batch
$batchIndex = $completedCount

foreach ($batch in $remainingBatches) {
    $batchIndex++
    $batchName = $batch.Name
    $batchFullPath = $batch.FullName
    $queryCount = (Get-Content $batchFullPath | Where-Object { $_.Trim() -ne "" }).Count
    
    $percent = [Math]::Round((($batchIndex - 1) / $totalBatches) * 100, 1)
    
    Write-Host ""
    Write-Host "====================================================================" -ForegroundColor DarkCyan
    Write-Host " [BATCH $batchIndex / $totalBatches] ($percent% SELESAI)" -ForegroundColor Yellow
    Write-Host " Memproses : $batchName ($queryCount kueri)" -ForegroundColor White
    Write-Host " Target CSV: $OutputFile" -ForegroundColor Gray
    Write-Host "====================================================================" -ForegroundColor DarkCyan
    
    Write-Log "Memulai scraping untuk $batchName ($queryCount kueri)..." "INFO"
    Update-Status -Status "scraping" -CurrentBatch $batchIndex -TotalBatches $totalBatches -CompletedCount ($batchIndex - 1) -CurrentBatchFile $batchName
    
    # Eksekusi Scraper
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    
    $processArgs = @(
        "-input", "`"$batchFullPath`"",
        "-results", "`"$OutputFile`"",
        "-proxies-file", "`"$CleanProxyFile`"",
        "-c", "1",
        "-depth", "10",
        "-pages-per-browser", "2",
        "-resume"
    )
    
    $cmdLine = "& `"$BinaryPath`" $($processArgs -join ' ')"
    Invoke-Expression $cmdLine
    $exitCode = $LASTEXITCODE
    $sw.Stop()
    
    if ($exitCode -ne 0) {
        Write-Log "Batch $batchName gagal atau dihentikan dengan exit code $exitCode." "ERROR"
        Update-Status -Status "error" -CurrentBatch $batchIndex -TotalBatches $totalBatches -CompletedCount ($batchIndex - 1) -CurrentBatchFile $batchName
        Write-Host "Scraper terhenti. Anda dapat menjalankan ulang script ini untuk melanjutkan dari batch $batchName." -ForegroundColor Yellow
        exit $exitCode
    }
    
    # Catat checkpoint jika sukses
    Add-Content -Path $CheckpointFile -Value $batchName
    $completedCount++
    
    $currentRows = 0
    if (Test-Path $OutputFile) {
        $currentRows = [Math]::Max(0, (Get-Content $OutputFile).Count - 1)
    }
    
    Write-Log "SUKSES: $batchName selesai dalam $($sw.Elapsed.ToString('mm\:ss')). Total data tersimpan saat ini: $currentRows baris." "SUCCESS"
    
    # Jika masih ada batch berikutnya, lakukan sleep dengan countdown interaktif
    if ($batchIndex -lt $totalBatches) {
        $nextRun = (Get-Date).AddSeconds($SleepSeconds)
        $nextRunStr = $nextRun.ToString("HH:mm:ss")
        Write-Log "Memasuki periode jeda $SleepSeconds detik ($([Math]::Round($SleepSeconds/60)) menit) untuk pendinginan proxy." "INFO"
        Write-Log "Batch berikutnya ($($batchIndex + 1)/$totalBatches) akan dimulai pukul $nextRunStr WIB." "INFO"
        
        Update-Status -Status "sleeping" -CurrentBatch $batchIndex -TotalBatches $totalBatches -CompletedCount $batchIndex -CurrentBatchFile $batchName -NextRunTime $nextRun.ToString("yyyy-MM-dd HH:mm:ss")
        
        # Countdown loop di terminal
        $sleepRemaining = $SleepSeconds
        while ($sleepRemaining -gt 0) {
            $ts = [TimeSpan]::FromSeconds($sleepRemaining)
            $timeFmt = "{0:D2}:{1:D2}:{2:D2}" -f [int]$ts.TotalHours, $ts.Minutes, $ts.Seconds
            
            $progressPct = [Math]::Round((($SleepSeconds - $sleepRemaining) / $SleepSeconds) * 100)
            $barFilled = [int]($progressPct / 5)
            $progressBar = ("=" * $barFilled) + (" " * (20 - $barFilled))
            
            $msg = "`r[JEDA PROXY] Sisa waktu: $timeFmt [$progressBar] $progressPct% | Lanjut pukul $nextRunStr WIB  "
            Write-Host -NoNewline $msg -ForegroundColor DarkYellow
            
            $step = [Math]::Min(1, $sleepRemaining)
            Start-Sleep -Seconds $step
            $sleepRemaining -= $step
        }
        Write-Host ""
    }
}

# Bersihkan file proxy temporary
if (Test-Path $CleanProxyFile) {
    Remove-Item $CleanProxyFile -Force
}

Write-Host ""
Write-Host "====================================================================" -ForegroundColor Green
Write-Host "  SEMUA BATCH (514 WILAYAH) TELAH SELESAI DISCRAPING DENGAN SUKSES!  " -ForegroundColor Green
Write-Host "====================================================================" -ForegroundColor Green
$finalRows = 0
if (Test-Path $OutputFile) {
    $finalRows = [Math]::Max(0, (Get-Content $OutputFile).Count - 1)
}
Write-Host "Total data tersimpan di '$OutputFile': $finalRows baris." -ForegroundColor Yellow
Write-Log "Proses automasi selesai penuh. Total $finalRows data berhasil disimpan di $OutputFile." "SUCCESS"
Update-Status -Status "finished" -CurrentBatch $totalBatches -TotalBatches $totalBatches -CompletedCount $totalBatches -CurrentBatchFile "none"
