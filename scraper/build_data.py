#!/usr/bin/env python3
"""
Build slim JSON data from scraper/hasil_ldii_indonesia.csv:
- scraper/places.json
- scraper/pengurus.json
"""
import csv
import json
import re
import sys
from pathlib import Path

# Increase CSV field size limit for large JSON cells
csv.field_size_limit(100_000_000)

PROVINCE_MAP = {
    'aceh': 'Aceh',
    'sumatera utara': 'Sumatera Utara',
    'north sumatra': 'Sumatera Utara',
    'sumatera barat': 'Sumatera Barat',
    'west sumatra': 'Sumatera Barat',
    'riau': 'Riau',
    'kepulauan riau': 'Kepulauan Riau',
    'riau islands': 'Kepulauan Riau',
    'jambi': 'Jambi',
    'sumatera selatan': 'Sumatera Selatan',
    'south sumatra': 'Sumatera Selatan',
    'kepulauan bangka belitung': 'Kepulauan Bangka Belitung',
    'bangka belitung islands': 'Kepulauan Bangka Belitung',
    'bangka belitung': 'Kepulauan Bangka Belitung',
    'bengkulu': 'Bengkulu',
    'lampung': 'Lampung',
    'dki jakarta': 'DKI Jakarta',
    'jakarta': 'DKI Jakarta',
    'jawa barat': 'Jawa Barat',
    'west java': 'Jawa Barat',
    'banten': 'Banten',
    'jawa tengah': 'Jawa Tengah',
    'central java': 'Jawa Tengah',
    'di yogyakarta': 'DI Yogyakarta',
    'daerah istimewa yogyakarta': 'DI Yogyakarta',
    'yogyakarta': 'DI Yogyakarta',
    'special region of yogyakarta': 'DI Yogyakarta',
    'jawa timur': 'Jawa Timur',
    'east java': 'Jawa Timur',
    'bali': 'Bali',
    'nusa tenggara barat': 'Nusa Tenggara Barat',
    'west nusa tenggara': 'Nusa Tenggara Barat',
    'nusa tenggara timur': 'Nusa Tenggara Timur',
    'east nusa tenggara': 'Nusa Tenggara Timur',
    'kalimantan barat': 'Kalimantan Barat',
    'west kalimantan': 'Kalimantan Barat',
    'kalimantan tengah': 'Kalimantan Tengah',
    'central kalimantan': 'Kalimantan Tengah',
    'kalimantan selatan': 'Kalimantan Selatan',
    'south kalimantan': 'Kalimantan Selatan',
    'kalimantan timur': 'Kalimantan Timur',
    'east kalimantan': 'Kalimantan Timur',
    'kalimantan utara': 'Kalimantan Utara',
    'north kalimantan': 'Kalimantan Utara',
    'sulawesi utara': 'Sulawesi Utara',
    'north sulawesi': 'Sulawesi Utara',
    'gorontalo': 'Gorontalo',
    'sulawesi tengah': 'Sulawesi Tengah',
    'central sulawesi': 'Sulawesi Tengah',
    'sulawesi barat': 'Sulawesi Barat',
    'west sulawesi': 'Sulawesi Barat',
    'sulawesi selatan': 'Sulawesi Selatan',
    'south sulawesi': 'Sulawesi Selatan',
    'sulawesi tenggara': 'Sulawesi Tenggara',
    'southeast sulawesi': 'Sulawesi Tenggara',
    'maluku': 'Maluku',
    'maluku utara': 'Maluku Utara',
    'north maluku': 'Maluku Utara',
    'papua': 'Papua',
    'papua barat': 'Papua Barat',
    'west papua': 'Papua Barat',
    'papua selatan': 'Papua Selatan',
    'papua tengah': 'Papua Tengah',
    'papua pegunungan': 'Papua Pegunungan',
    'papua barat daya': 'Papua Barat Daya',
}


def clean_phone(raw: str) -> str:
    """Normalize phone to digits starting with 62 (for WhatsApp and direct tel)."""
    if not raw:
        return ''
    digits = re.sub(r'\D', '', str(raw))
    if not digits:
        return ''
    if digits.startswith('0'):
        digits = '62' + digits[1:]
    elif digits.startswith('8'):
        digits = '62' + digits
    return digits


def parse_location(ca: dict, address: str, title: str) -> tuple[str, str]:
    raw_city = ca.get('city', '').strip()
    raw_state = ca.get('state', '').strip()

    province = ''
    if raw_state and raw_state.lower() in PROVINCE_MAP:
        province = PROVINCE_MAP[raw_state.lower()]

    if not province:
        for k in sorted(PROVINCE_MAP.keys(), key=lambda x: -len(x)):
            if re.search(r'\b' + re.escape(k) + r'\b', address, re.I):
                province = PROVINCE_MAP[k]
                break

    city = ''
    if raw_city:
        is_kota = bool(re.search(r'\b(city|kota)\b', raw_city, re.I))
        is_kab = bool(re.search(r'\b(regency|kabupaten|kab)\b', raw_city, re.I))
        clean_name = re.sub(r'(?i)\b(regency|city|kabupaten|kab\.?|kota)\b', '', raw_city).strip()
        if not is_kota and not is_kab:
            if re.search(r'\bKota\s+' + re.escape(clean_name) + r'\b', address, re.I) or re.search(
                re.escape(clean_name) + r'\s+City\b', address, re.I
            ):
                is_kota = True
            else:
                is_kab = True
        city = f'Kota {clean_name}' if is_kota else f'Kab. {clean_name}'

    if not city:
        m_kab = re.search(r'\b(?:Kabupaten|Kab\.)\s+([A-Za-z\s]+?)(?:,\s*|\s+\d{5}|\s+Indonesia)', address, re.I)
        m_kota = re.search(r'\bKota\s+([A-Za-z\s]+?)(?:,\s*|\s+\d{5}|\s+Indonesia)', address, re.I)
        m_reg = re.search(r'\b([A-Za-z\s]+?)\s+Regency\b', address, re.I)
        m_cit = re.search(r'\b([A-Za-z\s]+?)\s+City\b', address, re.I)

        if m_kab:
            c = re.sub(r'(?i)\b(regency|city|kabupaten|kab\.?|kota)\b', '', m_kab.group(1)).strip()
            city = f'Kab. {c}'
        elif m_kota:
            c = re.sub(r'(?i)\b(regency|city|kabupaten|kab\.?|kota)\b', '', m_kota.group(1)).strip()
            city = f'Kota {c}'
        elif m_reg:
            c = m_reg.group(1).split(',')[-1].strip()
            city = f'Kab. {c}'
        elif m_cit:
            c = m_cit.group(1).split(',')[-1].strip()
            city = f'Kota {c}'

    # Known edge cases
    if not city:
        if 'Simeulue' in title or 'Simeulue' in address:
            city = 'Kab. Simeulue'
            if not province:
                province = 'Aceh'
        elif 'Sawahlunto' in title or 'Sawahlunto' in address:
            city = 'Kota Sawahlunto'
            if not province:
                province = 'Sumatera Barat'

    return city, province


def main():
    base_dir = Path(__file__).resolve().parent
    csv_path = base_dir / 'hasil_ldii_indonesia.csv'
    places_out = base_dir / 'places.json'
    pengurus_out = base_dir / 'pengurus.json'

    if not csv_path.exists():
        print(f"Error: {csv_path} not found.")
        sys.exit(1)

    places = []
    undetermined_cities = []
    pengurus_map = {}  # key: (city, phone) -> dict

    with open(csv_path, mode='r', encoding='utf-8', errors='ignore') as f:
        reader = csv.DictReader(f)
        for idx, row in enumerate(reader, start=1):
            place_id = row.get('place_id', '').strip()
            if not place_id:
                place_id = f"custom_id_{idx}"

            title = row.get('title', '').strip()
            address = row.get('address', '').strip()

            lat_str = row.get('latitude', '').strip()
            lng_str = row.get('longitude', '').strip()
            try:
                lat = float(lat_str) if lat_str else 0.0
                lng = float(lng_str) if lng_str else 0.0
            except ValueError:
                lat, lng = 0.0, 0.0

            phone_raw = row.get('phone', '').strip()
            phone = clean_phone(phone_raw)

            ca_raw = row.get('complete_address', '{}')
            try:
                ca = json.loads(ca_raw) if ca_raw else {}
            except Exception:
                ca = {}

            city, province = parse_location(ca, address, title)

            if not city:
                undetermined_cities.append({
                    'row': idx,
                    'title': title,
                    'address': address
                })

            place_item = {
                'id': place_id,
                'name': title,
                'address': address,
                'city': city,
                'province': province,
                'lat': lat,
                'lng': lng,
                'phone': phone
            }
            places.append(place_item)

            if phone and city:
                key = (city, phone)
                if key not in pengurus_map:
                    pengurus_map[key] = {
                        'city': city,
                        'province': province,
                        'phone': phone
                    }

    # Generate pengurus list with sequential IDs
    pengurus_list = []
    # Sort pengurus alphabetically by province then city
    sorted_pengurus_keys = sorted(
        pengurus_map.keys(),
        key=lambda k: (pengurus_map[k]['province'], pengurus_map[k]['city'])
    )
    for idx, key in enumerate(sorted_pengurus_keys, start=1):
        item = pengurus_map[key]
        pengurus_list.append({
            'id': f"p-{idx:03d}",
            'city': item['city'],
            'province': item['province'],
            'phone': item['phone']
        })

    # Save to JSON
    with open(places_out, 'w', encoding='utf-8') as f:
        json.dump(places, f, ensure_ascii=False, indent=2)

    with open(pengurus_out, 'w', encoding='utf-8') as f:
        json.dump(pengurus_list, f, ensure_ascii=False, indent=2)

    print(f"=== Selesai memproses data ===")
    print(f"Total tempat (places): {len(places)} disimpan ke {places_out.name}")
    print(f"Total pengurus unik: {len(pengurus_list)} disimpan ke {pengurus_out.name}")
    print(f"Baris dengan kota tidak terdeteksi: {len(undetermined_cities)}")
    if undetermined_cities:
        print("\nDaftar tempat dengan kota tidak terdeteksi:")
        for item in undetermined_cities:
            print(f" - Baris {item['row']}: '{item['title']}' | Alamat: {item['address']}")
    else:
        print("Semua tempat (100%) berhasil ditentukan Kota/Kabupaten dan Provinsinya!")


if __name__ == '__main__':
    main()
