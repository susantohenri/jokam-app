# ==============================================================================
# Script Monitoring Scraper LDII
# Jalankan ini di terminal terpisah untuk melihat status & progres real-time.
# Penggunaan:
#   .\monitor.ps1         (Cek status sekali)
#   .\monitor.ps1 -Watch  (Pantau live setiap 3 detik)
# ==============================================================================
param(
    [switch]$Watch = $false
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$StatusFile = Join-Path $ScriptDir "status.json"
$OutputFile = Join-Path $ScriptDir "hasil_ldii_indonesia.csv"
$LogFile = Join-Path $ScriptDir "scraper.log"

function Show-Dashboard {
    Clear-Host
    Write-Host "====================================================================" -ForegroundColor Cyan
    Write-Host "           DASHBOARD MONITORING SCRAPER LDII INDONESIA              " -ForegroundColor Yellow
    Write-Host "====================================================================" -ForegroundColor Cyan
    Write-Host "Waktu Sekarang: $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss')) WIB" -ForegroundColor Gray
    Write-Host ""

    if (Test-Path $StatusFile) {
        try {
            $raw = Get-Content $StatusFile -Raw -Encoding utf8
            $s = $raw | ConvertFrom-Json
            
            $statusColor = "White"
            if ($s.status -eq "scraping") { $statusColor = "Green" }
            elseif ($s.status -eq "sleeping") { $statusColor = "Yellow" }
            elseif ($s.status -eq "finished") { $statusColor = "Cyan" }
            elseif ($s.status -eq "error") { $statusColor = "Red" }
            
            Write-Host "Status Saat Ini      : " -NoNewline
            Write-Host ($s.status.ToUpper()) -ForegroundColor $statusColor
            
            Write-Host "Batch Berjalan       : Batch $($s.current_batch) dari $($s.total_batches)" -ForegroundColor White
            Write-Host "Batch Selesai        : $($s.completed_batches) batch ($($s.percent_complete)%)" -ForegroundColor White
            
            # Progress bar
            $barFilled = [int]($s.percent_complete / 5)
            $progressBar = ("#" * $barFilled) + ("-" * (20 - $barFilled))
            Write-Host "Progress Bar         : [$progressBar] $($s.percent_complete)%" -ForegroundColor Cyan
            
            Write-Host "File Batch Aktif     : $($s.current_file)" -ForegroundColor Gray
            if ($s.status -eq "sleeping" -and $s.next_run_at) {
                Write-Host "Jeda Berakhir Pada   : $($s.next_run_at) WIB" -ForegroundColor Yellow
            }
            Write-Host "Total Data CSV       : $($s.total_records) baris tersimpan" -ForegroundColor Green
            Write-Host "Update Terakhir      : $($s.last_updated)" -ForegroundColor Gray
        } catch {
            Write-Host "Sedang memperbarui status file..." -ForegroundColor Gray
        }
    } else {
        Write-Host "Scraper belum pernah dijalankan atau status.json belum dibuat." -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "--------------------------------------------------------------------" -ForegroundColor Gray
    Write-Host " Log Aktivitas Terakhir (5 baris terakhir scraper.log):" -ForegroundColor DarkYellow
    Write-Host "--------------------------------------------------------------------" -ForegroundColor Gray
    if (Test-Path $LogFile) {
        Get-Content $LogFile -Tail 5 | ForEach-Object {
            Write-Host "  $_" -ForegroundColor Gray
        }
    } else {
        Write-Host "  (Belum ada file log)" -ForegroundColor DarkGray
    }
    Write-Host "--------------------------------------------------------------------" -ForegroundColor Gray
    Write-Host ""
    if ($Watch) {
        Write-Host "Mode Live Watch aktif (tekan Ctrl+C untuk keluar)..." -ForegroundColor DarkGray
    }
}

if ($Watch) {
    while ($true) {
        Show-Dashboard
        Start-Sleep -Seconds 3
    }
} else {
    Show-Dashboard
    Write-Host "Tips: Gunakan '.\monitor.ps1 -Watch' untuk update otomatis setiap 3 detik." -ForegroundColor DarkCyan
}
