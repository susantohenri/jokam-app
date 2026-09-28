#!/usr/bin/env bash
# ==============================================================================
# Script Automasi Scraper Google Maps LDII Indonesia
# Lingkungan: Linux / WSL / Git Bash / macOS
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

BINARY="./google-maps-scraper"
if [ ! -f "$BINARY" ] && [ -f "./google-maps-scraper.exe" ]; then
    BINARY="./google-maps-scraper.exe"
fi

QUERIES_FILE="queries.txt"
BATCH_DIR="batches"
RAW_PROXY_FILE="proxies.txt"
CLEAN_PROXY_FILE="active_proxies.tmp"
OUTPUT_FILE="hasil_ldii_indonesia.csv"
CHECKPOINT_FILE="checkpoint.txt"
LOG_FILE="scraper.log"
STATUS_FILE="status.json"
SLEEP_SECONDS=5400

log() {
    local level="$1"
    local msg="$2"
    local timestamp
    timestamp="$(date '+%Y-%m-%d %H:%M:%S')"
    echo "[$timestamp] [$level] $msg" | tee -a "$LOG_FILE"
}

update_status() {
    local status="$1"
    local cur_batch="$2"
    local tot_batch="$3"
    local comp_count="$4"
    local cur_file="$5"
    local next_time="${6:-}"
    local records=0
    if [ -f "$OUTPUT_FILE" ]; then
        records=$(( $(wc -l < "$OUTPUT_FILE") - 1 ))
        [ "$records" -lt 0 ] && records=0
    fi
    local pct=0
    if [ "$tot_batch" -gt 0 ]; then
        pct=$(awk "BEGIN {printf \"%.2f\", ($comp_count/$tot_batch)*100}")
    fi
    cat <<EOF > "$STATUS_FILE"
{
  "status": "$status",
  "current_batch": $cur_batch,
  "total_batches": $tot_batch,
  "completed_batches": $comp_count,
  "percent_complete": $pct,
  "total_records": $records,
  "current_file": "$cur_file",
  "next_run_at": "$next_time",
  "last_updated": "$(date '+%Y-%m-%d %H:%M:%S')"
}
EOF
}

echo ""
echo "===================================================================="
echo "  AUTOMASI GOOGLE MAPS SCRAPER - LDII INDONESIA (514 WILAYAH)       "
echo "===================================================================="
echo ""

# 1. Cek Binary
if [ ! -f "$BINARY" ]; then
    log "ERROR" "File binary '$BINARY' tidak ditemukan!"
    exit 1
fi
chmod +x "$BINARY" 2>/dev/null || true

# 2. Cek & Siapkan Proxy
if [ ! -f "$RAW_PROXY_FILE" ]; then
    log "ERROR" "File '$RAW_PROXY_FILE' tidak ditemukan!"
    exit 1
fi

# Format proxy otomatis (mendukung http://user:pass@ip:port maupun ip:port:user:pass)
> "$CLEAN_PROXY_FILE"
while IFS= read -r line || [ -n "$line" ]; do
    line="$(echo "$line" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
    [ -z "$line" ] && continue
    [[ "$line" =~ ^# ]] && continue
    
    # Deteksi ip:port:user:pass
    if [[ "$line" =~ ^([^:@]+):([0-9]+):([^:@]+):([^:@]+)$ ]]; then
        echo "http://${BASH_REMATCH[3]}:${BASH_REMATCH[4]}@${BASH_REMATCH[1]}:${BASH_REMATCH[2]}" >> "$CLEAN_PROXY_FILE"
    # Deteksi ip:port tanpa auth
    elif [[ "$line" =~ ^([^:@]+):([0-9]+)$ ]]; then
        echo "http://${BASH_REMATCH[1]}:${BASH_REMATCH[2]}" >> "$CLEAN_PROXY_FILE"
    else
        echo "$line" >> "$CLEAN_PROXY_FILE"
    fi
done < "$RAW_PROXY_FILE"

PROXY_COUNT=$(wc -l < "$CLEAN_PROXY_FILE" | tr -d ' ')

if [ "$PROXY_COUNT" -eq 0 ]; then
    log "ERROR" "File 'proxies.txt' belum diisi proxy valid! Masukkan proxy Anda terlebih dahulu."
    exit 1
fi
log "SUCCESS" "Proxy terdeteksi: $PROXY_COUNT proxy aktif siap digunakan."

# 3. Cek Batch
if [ ! -d "$BATCH_DIR" ] || [ -z "$(ls -A "$BATCH_DIR"/*.txt 2>/dev/null)" ]; then
    log "WARN" "Folder batch belum ada. Menjalankan python generate_queries.py..."
    python3 generate_queries.py || python generate_queries.py
fi

BATCH_FILES=( $(ls "$BATCH_DIR"/batch_*.txt | sort) )
TOTAL_BATCHES=${#BATCH_FILES[@]}

if [ "$TOTAL_BATCHES" -eq 0 ]; then
    log "ERROR" "Tidak ada file batch di folder '$BATCH_DIR'!"
    exit 1
fi

touch "$CHECKPOINT_FILE"

# Hitung batch selesai
COMPLETED_COUNT=$(wc -l < "$CHECKPOINT_FILE" | tr -d ' ')
echo "Total Batch  : $TOTAL_BATCHES batch (~10 kueri/batch)"
echo "Selesai      : $COMPLETED_COUNT batch"
echo "Output CSV   : $OUTPUT_FILE"
echo "Log File     : $LOG_FILE"
echo "--------------------------------------------------------------------"

BATCH_IDX=0
for batch_path in "${BATCH_FILES[@]}"; do
    BATCH_IDX=$((BATCH_IDX + 1))
    batch_name="$(basename "$batch_path")"

    # Cek apakah batch sudah ada di checkpoint
    if grep -Fxq "$batch_name" "$CHECKPOINT_FILE"; then
        continue
    fi

    query_count=$(grep -cve '^[[:space:]]*$' "$batch_path" || true)
    pct=$(awk "BEGIN {printf \"%.1f\", (($BATCH_IDX-1)/$TOTAL_BATCHES)*100}")

    echo ""
    echo "===================================================================="
    echo " [BATCH $BATCH_IDX / $TOTAL_BATCHES] ($pct% SELESAI)"
    echo " Memproses : $batch_name ($query_count kueri)"
    echo " Target CSV: $OUTPUT_FILE"
    echo "===================================================================="

    log "INFO" "Memulai scraping untuk $batch_name ($query_count kueri)..."
    update_status "scraping" "$BATCH_IDX" "$TOTAL_BATCHES" "$((BATCH_IDX - 1))" "$batch_name"

    # Jalankan Scraper
    "$BINARY" -input "$batch_path" -results "$OUTPUT_FILE" -proxies-file "$CLEAN_PROXY_FILE" -c 1 -depth 10 -pages-per-browser 2 -resume

    # Simpan Checkpoint
    echo "$batch_name" >> "$CHECKPOINT_FILE"
    COMPLETED_COUNT=$((COMPLETED_COUNT + 1))

    current_rows=0
    if [ -f "$OUTPUT_FILE" ]; then
        current_rows=$(( $(wc -l < "$OUTPUT_FILE") - 1 ))
        [ "$current_rows" -lt 0 ] && current_rows=0
    fi
    log "SUCCESS" "$batch_name selesai. Total data terkumpul saat ini: $current_rows baris."

    # Jeda sleep jika bukan batch terakhir
    if [ "$BATCH_IDX" -lt "$TOTAL_BATCHES" ]; then
        NEXT_TIME=$(date -d "+$SLEEP_SECONDS seconds" '+%Y-%m-%d %H:%M:%S' 2>/dev/null || date -v "+${SLEEP_SECONDS}S" '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo "dalam $SLEEP_SECONDS detik")
        log "INFO" "Jeda $SLEEP_SECONDS detik (90 menit) mendinginkan proxy. Lanjut pada: $NEXT_TIME"
        update_status "sleeping" "$BATCH_IDX" "$TOTAL_BATCHES" "$COMPLETED_COUNT" "$batch_name" "$NEXT_TIME"

        remaining=$SLEEP_SECONDS
        while [ "$remaining" -gt 0 ]; do
            hours=$((remaining / 3600))
            minutes=$(((remaining % 3600) / 60))
            seconds=$((remaining % 60))
            printf "\r[JEDA PROXY] Sisa waktu: %02d:%02d:%02d | Target mulai: %s  " "$hours" "$minutes" "$seconds" "$NEXT_TIME"
            sleep 1
            remaining=$((remaining - 1))
        done
        echo ""
    fi
done

rm -f "$CLEAN_PROXY_FILE"

echo ""
echo "===================================================================="
echo "  SEMUA BATCH TELAH SELESAI DISCRAPING DENGAN SUKSES!               "
echo "===================================================================="
final_rows=0
if [ -f "$OUTPUT_FILE" ]; then
    final_rows=$(( $(wc -l < "$OUTPUT_FILE") - 1 ))
    [ "$final_rows" -lt 0 ] && final_rows=0
fi
echo "Total data tersimpan di '$OUTPUT_FILE': $final_rows baris."
log "SUCCESS" "Semua batch selesai. Total tersimpan: $final_rows baris."
update_status "finished" "$TOTAL_BATCHES" "$TOTAL_BATCHES" "$TOTAL_BATCHES" "none"
