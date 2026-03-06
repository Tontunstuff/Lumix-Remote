# 📷 Lumix Remote — DMC-G7K
Remote shutter WiFi untuk Panasonic Lumix DMC-G7K

---

## 🚀 CARA BUILD APK OTOMATIS (Tanpa Komputer/Android Studio)

### LANGKAH 1 — Daftar GitHub (gratis)
1. Buka **github.com** di browser HP atau komputer
2. Klik **"Sign up"** → isi email, password, username
3. Verifikasi email

### LANGKAH 2 — Buat Repository Baru
1. Klik tombol **"+"** pojok kanan atas → **"New repository"**
2. Nama: `lumix-remote` → pilih **Public** → **Create repository**

### LANGKAH 3 — Upload Semua File
1. Klik **"uploading an existing file"**
2. Extract ZIP dari Claude → drag & drop SEMUA FILE termasuk folder `.github`
3. Klik **"Commit changes"**

### LANGKAH 4 — Tunggu Build Selesai
1. Klik tab **"Actions"**
2. Tunggu 5-10 menit sampai icon jadi ✅

### LANGKAH 5 — Download APK
1. Klik job ✅ → scroll ke **"Artifacts"** → klik **"LumixRemote-APK"**

### LANGKAH 6 — Install di HP
1. Buka APK → Settings → Allow install → **Install**

---

# 📷 Lumix Remote — DMC-G7K WiFi Controller (original)
Aplikasi Android native untuk mengontrol kamera Panasonic Lumix DMC-G7K via WiFi.

## 🚀 Cara Build APK

### Metode 1: Android Studio (Direkomendasikan)
1. Download & install **Android Studio** (gratis) dari https://developer.android.com/studio
2. Buka Android Studio → **File → Open** → pilih folder `LumixRemote`
3. Tunggu Gradle sync selesai (otomatis download dependencies)
4. Klik **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. APK tersimpan di: `app/build/outputs/apk/debug/app-debug.apk`
6. Transfer APK ke HP → install (aktifkan "Unknown Sources" di Settings)

### Metode 2: Command Line (jika sudah ada Android SDK)
```bash
cd LumixRemote
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## 📱 Cara Pakai Aplikasi

### Koneksi Kamera
1. Di kamera G7K: **Menu → WiFi → Remote Shooting**
2. Kamera buat hotspot WiFi (nama: `G7K_XXXXXX`)
3. Di HP: sambungkan ke WiFi tersebut
4. Buka app → masukkan IP `192.168.54.1` → tap **KONEK**

### Fitur
| Fitur | Keterangan |
|-------|-----------|
| 📸 Shutter | Tap tombol bulat besar untuk foto |
| 🔴 Rekam Video | Mulai/stop rekam + timer |
| 👁 Live View | Preview real-time ~10fps |
| ◎ Autofocus | Trigger AF |
| ISO/SS/F/EV | Atur exposure settings |
| Zoom W/T | Kontrol zoom optis |

## ⚙️ Requirements
- Android 7.0+ (API 24+)
- WiFi tersambung ke kamera
- Kamera dalam mode Remote Shooting

## 🔧 API Protocol
Menggunakan Lumix HTTP CGI API:
- `GET http://192.168.54.1/cam.cgi?mode=camcmd&value=capture`
- `GET http://192.168.54.1/cam.cgi?mode=getliveviewimage`
- `GET http://192.168.54.1/cam.cgi?mode=accctrl&type=req_acc&...`

## 📦 Dependencies
- OkHttp 4.12 (HTTP client)
- AndroidX AppCompat 1.6.1
- Material Components 1.11.0
