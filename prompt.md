# TASK: Build the Android app "Tempat Sambung" (Kotlin + Jetpack Compose)

## 0. How to work
- Start in Planning mode: show a short plan and file layout first, then implement.
- Priorities, in order: (1) simplest possible implementation, (2) effortless, fast UI/UX, (3) full compliance with Google Play and AdMob policies.
- Keep the tech minimal: single module, no DI framework (no Hilt/Koin), no Room, no Retrofit, no Firebase.
- Use the latest stable versions of everything. For SDK APIs (AdMob, UMP, Compose, AGP), check the current official docs (developer.android.com, developers.google.com/admob/android) instead of relying on memory.
- The workspace is the existing `jokam-app` repo (it already has `scraper/` and `wallpaper/result/`). Do not modify or delete existing folders. Create the Android project in `./android/` (Gradle root there) and helper scripts in `./tools/`.
- When finished: run `./gradlew assembleDebug` and `./gradlew lint` in `./android`, fix all errors, then report what you built and what I must do manually.

## 1. Product
Travelers need to find the nearest "tempat sambung" (community gathering place) fast. Core data is latitude/longitude. Goal: a genuinely useful utility that earns via AdMob. There is NO in-app map UI; Google Maps is only the navigation target. No accounts, no backend of our own, no analytics.

## 2. Project setup
- applicationId / package: `com.jokam.tempatsambung`. App name: "Tempat Sambung" (both languages).
- compileSdk = 36, targetSdk = 36, minSdk = 26. Gradle Kotlin DSL (`build.gradle.kts`) with a version catalog.
- Compose + Material 3. Single Activity: `AppCompatActivity` hosting Compose (so `AppCompatDelegate.setApplicationLocales` works on all API levels). Theme parent must be AppCompat-compatible (e.g. Theme.Material3.DayNight.NoActionBar).
- Dependencies: navigation-compose, lifecycle-viewmodel-compose, datastore-preferences, coil-compose, okhttp, kotlinx-serialization-json, play-services-location, and `com.google.android.gms:play-services-ads` (declare explicitly in build.gradle.kts). Verify `com.google.android.ump:user-messaging-platform` is on the classpath; add it if not.
- API 36 requirements: correct edge-to-edge insets, predictive back (BackHandler where needed), do not lock orientation, no native libs of our own (16 KB page size safe), recent AGP.
- Release build: R8 minify + shrinkResources, with ProGuard rules for kotlinx.serialization.
- Manifest permissions, nothing else: INTERNET, ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION, SET_WALLPAPER, WRITE_EXTERNAL_STORAGE (`android:maxSdkVersion="28"`). Keep `com.google.android.gms.permission.AD_ID` as merged from the ads SDK. HTTPS only (`usesCleartextTraffic=false`).
- AdMob App ID in AndroidManifest.xml as `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" ...>` with the SAMPLE ID `ca-app-pub-3940256099942544~3347511713`, via `manifestPlaceholders` so swapping to the production ID later is a one-line change.

## 3. Remote config and data (all URLs are constants in one file)
### Ads config (already exists, do NOT rename existing keys)
`ADS_CONFIG_URL = "https://raw.githubusercontent.com/susantohenri/admob-remote-configs/refs/heads/main/tempatsambung/ads_config.json"`
Existing keys: `appOpenAdUnitId`, `bannerAdUnitId`, `interstitialAdUnitId`, `rewardedAdUnitId`, `nativeAdUnitId`, `isAdsEnabled`.
New OPTIONAL boolean keys I will add: `isBannerEnabled` (default true), `isRewardedRouteEnabled`, `isRewardedContactEnabled`, `isRewardedWallpaperEnabled` (all default false).
- The app uses only `bannerAdUnitId`, `rewardedAdUnitId`, `isAdsEnabled` and the four new flags; ignore all other keys (appOpen, interstitial, native are NOT used, no interstitials anywhere).
- `isAdsEnabled=false` → no ad is requested or shown anywhere and all gates open immediately.
- Fetch once per app start, 5 s timeout, off the main thread, never block the UI. Parse leniently (ignore unknown keys, missing keys use defaults).
- If the fetch fails: debug builds fall back to Google's test ad unit IDs (BuildConfig); release builds show NO ads (never ship test IDs in release).

### App data (slim JSON, produced by scripts, hosted in this repo)
`DATA_BASE_URL = "https://raw.githubusercontent.com/susantohenri/jokam-app/refs/heads/main/data/"` containing `places.json`, `pengurus.json`, `wallpapers.json`. The app must NEVER download the raw CSV.
```json
// places.json
[ { "id": "ChIJ...", "name": "...", "address": "...", "city": "Kota Banda Aceh", "province": "Aceh", "lat": 5.57, "lng": 95.34, "phone": "0852..." } ]
// pengurus.json
[ { "id": "p-001", "city": "Kota Banda Aceh", "province": "Aceh", "phone": "0852..." } ]
// wallpapers.json
[ { "id": "03454130", "thumb_url": "https://raw.githubusercontent.com/susantohenri/jokam-app/refs/heads/main/wallpaper/thumbs/03454130.webp", "full_url": "https://raw.githubusercontent.com/susantohenri/jokam-app/refs/heads/main/wallpaper/result/03454130.webp" } ]
```
- Places/pengurus/wallpapers are fetched when their screen is first needed and kept in memory for the session only (no disk cache). Loading = spinner; failure = friendly message + Retry button. Parse leniently and skip malformed items.

### Data scripts (Python 3, in ./tools/, stdlib + Pillow only)
1. `tools/build_data.py` reads `scraper/hasil_ldii_indonesia.csv` (Google Maps scrape, ~36 columns; some cells contain huge JSON, so raise `csv.field_size_limit`; stream the file) and writes `data/places.json` and `data/pengurus.json`.
   - places: `id`=`place_id` (dedupe by it; drop rows without valid latitude/longitude or whose `status` says permanently/temporarily closed), `name`=`title`, `address`, `lat`/`lng`=`latitude`/`longitude`, `phone` normalized to local format `08xxxxxxxxxx` (empty if none).
   - `city` and `province`: prefer `complete_address` (JSON: `city`, `state`); when empty, derive from `address` (segments like "Kabupaten X", "X Regency", "X City", "Kota X", followed by the province). Normalize to Indonesian labels: "X City"/"Kota X" → "Kota X", "X Regency"/"Kabupaten X" → "Kab. X". Print a report of rows where the city could not be determined, and write those with an empty city (the app hides them from city search but still lists them by distance).
   - pengurus: distinct (city, phone) pairs from places that have a phone; ids `p-001`...
   - NEVER copy reviews, reviewer names/photos/links, images, ratings, opening hours or popular times into the output.
2. `tools/build_wallpapers.py` scans `wallpaper/result/*.webp`, creates 480 px-wide `.webp` thumbnails in `wallpaper/thumbs/`, and writes `data/wallpapers.json` (URLs as in the example above).
- Add `tools/README.md` with the 2 commands to re-run after updating the CSV or adding wallpapers, then commit + push.

## 4. Navigation
Bottom NavigationBar, 4 tabs: Home, Pengurus, Wallpaper, Settings. Material icons (Home, Chat, Image, Settings). Do NOT bundle any WhatsApp or Google Maps logo.

## 5. Home (core feature)
1. On open, if location permission is granted, get the current location (FusedLocationProvider, balanced accuracy). Approximate-only permission must work.
2. If permission is not granted: a short friendly card explaining why location is needed + button [Izinkan Lokasi / Allow location].
3. If permission is granted but device location is off: [Aktifkan Lokasi / Enable location], which triggers Google's in-app resolvable "turn on location" dialog (SettingsClient + ResolvableApiException).
4. If location is still unavailable (denied, dialog refused, timeout ~8 s): show a city/kabupaten autocomplete. Suggestions = distinct (`city`, `province`) pairs from places.json, shown as "Kota Banda Aceh, Aceh", case-insensitive "contains" match.
5. List sorted nearest → farthest (Haversine on device), distance as "850 m" or "2,3 km" (locale-aware).
6. In city mode (no GPS): sort by distance from the centroid of the chosen city's places, do NOT show km, show the city name on each item instead.
7. Each item: name, address (max 2 lines), distance, heart icon (favorite toggle), copy-address icon button, and the main button [Buka Tempat Sambung / Get directions].
8. Main button opens Google Maps directions by coordinates: ACTION_VIEW `https://www.google.com/maps/dir/?api=1&destination=LAT,LNG` (prefer the Google Maps package if available, else default handler; catch ActivityNotFoundException). No Maps SDK dependency.
- Favorites: DataStore (string set of place `id`s). If any exist, show a "Favorit / Favorites" section at the very top of Home with the same item layout; a favorited place must not repeat in the main list; hide the section when empty; ids missing from places.json are silently ignored; works without GPS (no distance).
- No manual "change city" button when GPS works. No map UI.

## 6. Pengurus tab
Rows: city (+ province), phone number (selectable text + copy icon), button [Hubungi Pengurus / Contact]. The button opens WhatsApp via ACTION_VIEW `https://wa.me/<number>` (digits only; leading "0" → "62"; leading "62" unchanged). Order: if user location or a chosen city is known, nearest city first (city position = centroid of that city's places; cities without places last); otherwise alphabetical by province then city.

## 7. Wallpaper tab
- 2-column grid of thumbnails (`thumb_url`, Coil, placeholder + crossfade; no titles). Tap → full-screen preview (`full_url`) with [Pasang sebagai wallpaper / Set as wallpaper] (WallpaperManager, home + lock screen) and [Simpan / Save] (MediaStore into Pictures/Tempat Sambung; on API 26–28 request WRITE_EXTERNAL_STORAGE at that moment; on 29+ no permission). Snackbar for success/failure. Download/decoding off the main thread.
- Both buttons are gated by `isRewardedWallpaperEnabled` (section 8).

## 8. Ads
### Consent (Google UMP), must come first
- On every app start: `requestConsentInfoUpdate` → `loadAndShowConsentFormIfRequired` → only when `canRequestAds()` is true, initialize `MobileAds` (exactly once, off the main thread). Also initialize immediately if `canRequestAds()` is already true from a previous session. No ad request before that.
- Debug builds only: ConsentDebugSettings with a placeholder for my test device hash and a constant to force the EEA geography for testing.
- Settings → Legal & info: a row "Ad privacy settings / Pengaturan privasi iklan" shown ONLY when `privacyOptionsRequirementStatus == REQUIRED`; it calls `showPrivacyOptionsForm`.

### Banner
- Anchored adaptive banner (follow current docs), unit ID from `bannerAdUnitId`, shown only if `isAdsEnabled` and `isBannerEnabled` and consent allows, on Home, Pengurus and the Wallpaper grid. NOT on Settings, NOT on the full-screen preview.
- Place it directly above the bottom navigation with at least 8 dp spacing and a divider (no accidental taps). Reserve its height to avoid layout jumps. Handle AdView lifecycle (pause/resume/destroy).

### Rewarded (opt-in gate)
Unit ID from `rewardedAdUnitId`. Applied to: [Buka Tempat Sambung] (`isRewardedRouteEnabled`), [Hubungi Pengurus] (`isRewardedContactEnabled`), wallpaper Set/Save (`isRewardedWallpaperEnabled`). If `isAdsEnabled` is false or the flag is false, the action runs immediately. Otherwise, on every tap:
1. Show a dialog with a clear message + [Tonton iklan / Watch ad] and [Batal / Cancel]. Cancel does nothing.
2. On Watch: if the rewarded ad is loaded, show it; run the original action only after the reward was earned and the ad dismissed. If the user closes the ad early (no reward), do NOT run the action.
3. If the ad is not loaded or fails to show, run the action directly (never leave the user stuck).
- Preload the rewarded ad once consent allows it and preload the next after each show (single small manager class).
- Address, phone number and city text always stay visible and copyable regardless of gates.
- Dialog texts, EN: "Watch a short ad to open the route / to contact the coordinator / to use this wallpaper". ID: "Tonton iklan singkat untuk membuka rute / untuk menghubungi pengurus / untuk memakai wallpaper ini".

## 9. Settings
- **Preferences**: (a) language as a two-option segmented control "Bahasa Indonesia | English"; (b) theme as a THREE-option segmented control: System | Light | Dark (ID: "Ikuti sistem | Terang | Gelap"), default System.
- **Legal & info**: an About item showing app name, "Version x.y (build)" from the package info, and a [Privacy Policy] button that opens `https://tokiocv.blogspot.com/2026/07/privacy-policy.html` in the external browser (ACTION_VIEW). Plus the conditional ad-privacy row from section 8.

## 10. Languages and theme
- English (default resources) + Indonesian (`values-in`). Auto-detect from the OS language: Indonesian if the OS is Indonesian, otherwise English.
- The Settings language choice uses `AppCompatDelegate.setApplicationLocales`; enable AppCompat's `autoStoreLocales` service so it persists on API < 33; add `res/xml/locales_config.xml` (en, id) + `android:localeConfig`; set `androidResources.localeFilters` to en and in.
- Theme mode (system/light/dark) persisted in DataStore, applied at startup. Static green Material 3 color scheme (light + dark), no dynamic color.
- No hardcoded UI strings; complete EN and ID translations. Indonesian labels to use: Beranda, Pengurus, Wallpaper, Pengaturan, Buka Tempat Sambung, Hubungi Pengurus, Aktifkan Lokasi, Izinkan Lokasi, Favorit, Salin alamat, Coba lagi, Batal.

## 11. Branding assets
- Create `./store-assets/` with:
  - `app-icon-512.png`: 512×512, 32-bit PNG, < 1 MB, full-bleed square (no rounded corners, no shadow).
  - `feature-graphic-1024x500.png`: 1024×500, 24-bit PNG without alpha; shows "Tempat Sambung" + Indonesian tagline "Temukan tempat sambung terdekat, di mana pun kamu berada"; text inside safe margins.
  - Editable SVG sources for both.
- Design: a location pin containing a simple dome silhouette, deep green + white, flat, no text in the icon. Do NOT use any organization's logo, trademark, or third-party brand asset.
- Put the icon in the app as an adaptive icon (foreground + background vector drawables + monochrome layer) in `mipmap-anydpi-v26` (`ic_launcher`, `ic_launcher_round`).
- Generate the PNGs from the SVGs with whatever tool works here (image generation or an SVG rasterizer).

## 12. Policy checklist (verify each at the end)
- Rewarded ads are opt-in only, clearly labeled, with Cancel; no forced or hidden ad triggers; no interstitials.
- Banner separated from clickable elements; none on Settings or the full-screen preview.
- UMP consent runs before any ad request; ad-privacy entry point exists when required.
- No production ad unit ID in code; release builds show no ads if the remote config is unavailable.
- Location is requested in context with a rationale, used only on-device to compute distance, never stored or sent by our code; foreground only.
- Privacy Policy reachable inside the app.
- No third-party trademarks in the UI. The app never shows reviews, reviewer info, photos or ratings from the scraped data, and the raw CSV is never bundled or downloaded by the app.
- No accounts, no analytics, no data collection of our own.

## 13. Definition of done
- Debug build runs, lint is clean, the minified release build assembles.
- Both languages and all three theme modes work; language and theme survive an app restart.
- The rewarded gate works with flags on/off and falls back correctly when no ad is loaded.
- `tools/build_data.py` and `tools/build_wallpapers.py` run successfully; generated `data/*.json` and `wallpaper/thumbs/` exist; show me the report of rows with an undetermined city.
- Write `android/SETUP.md` (short) with my manual steps: add the 4 new keys to ads_config.json, commit + push `data/` and `wallpaper/thumbs/`, replace the AdMob App ID with the production one, put production ad unit IDs into ads_config.json, create the GDPR message in AdMob (Privacy & messaging), add my test device hash, and fill the Data safety form in Play Console (approximate/precise location, advertising ID).

## 14. OVERRIDES — these take precedence over any conflicting text above

- **Project location:** create the Android project at the REPO ROOT (`settings.gradle.kts` at the root), not in `./android/`. Keep `scraper/`, `wallpaper/`, `tools/`, `data/` untouched, add a standard Android `.gitignore`, run `./gradlew` from the root, and put `SETUP.md` at the root. Ignore every mention of `./android/` above.
- **Ads flags:** `ads_config.json` will NOT be modified. Every optional flag defaults to TRUE when the key is missing: `isBannerEnabled`, `isNativeEnabled`, `isRewardedRouteEnabled`, `isRewardedContactEnabled`, `isRewardedWallpaperEnabled` (a flag is off only if the key exists and is false). `isAdsEnabled` stays the master switch. Remove the "add the 4 new keys" step from SETUP.md.
- **Native ads (now used):** use `nativeAdUnitId` from the config (debug fallback: Google's test native ID `ca-app-pub-3940256099942544/2247696110`). Ignore the earlier statement that native is unused; appOpen and interstitial remain unused.
  - Placement: in the Home main list and the Pengurus list, between items. First slot after the 3rd item, then every 8th item, max 3 per screen. Never as the first item, never inside the Favorites section, never on Wallpaper or Settings.
  - Loading: after consent, request up to 3 ads at once with `AdLoader.loadAds`. If none loaded, render no slot at all (no empty gap). Show only if `isAdsEnabled` and `isNativeEnabled`.
  - Rendering: `NativeAdView` inside `AndroidView` with MediaView, icon, headline, body, call-to-action. Register every asset view, never modify asset text/images, show the AdChoices overlay, and add a visible "Iklan / Ad" badge.
  - Must be clearly distinguishable from our cards: different background color, border, and smaller height, with at least 12 dp spacing to neighbouring items, so it can never be mistaken for a tempat sambung or pengurus card.
  - Destroy native ads properly when the screen/activity is destroyed.
- Add to section 12 checklist: native ads are labeled, visually distinct from content, and never adjacent to a gated button without spacing.