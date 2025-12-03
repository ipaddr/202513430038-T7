<div align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="MedZone Logo" width="150"/>
  
  # MedZone - Aplikasi Konsultasi Keluhan Ringan

  ![Version](https://img.shields.io/badge/version-1.0.0-blue)
  ![Platform](https://img.shields.io/badge/platform-Android-green)
  ![MinSDK](https://img.shields.io/badge/MinSDK-26-orange)
</div>

**MedZone** adalah aplikasi Android yang membantu pengguna menemukan rekomendasi obat yang tepat untuk keluhan kesehatan ringan menggunakan teknologi AI.

---

## 📱 Tentang Aplikasi

MedZone dirancang untuk memberikan rekomendasi obat yang sesuai berdasarkan keluhan kesehatan ringan yang dialami pengguna. Aplikasi ini menggunakan AI untuk menganalisis keluhan dan memberikan saran obat beserta informasi dosis yang tepat.

### ⚠️ Disclaimer
Rekomendasi yang diberikan bersifat **informasi umum** dan **bukan diagnosis medis**. Selalu konsultasikan dengan dokter atau apoteker untuk kondisi kesehatan yang serius.

---

## ✨ Fitur Utama

### 1. 🤖 Konsultasi AI
- Input keluhan kesehatan ringan (sakit kepala, batuk, demam, dll)
- Analisis AI untuk rekomendasi obat yang sesuai
- Informasi lengkap tentang obat dan dosisnya
- Quick chips untuk keluhan umum

### 2. 📋 Riwayat Konsultasi
- Lihat kembali konsultasi sebelumnya
- Detail keluhan dan rekomendasi obat
- Tanggal dan waktu konsultasi

### 3. 💊 Pengingat Obat
- Atur jadwal penggunaan obat
- Frekuensi per hari (1x, 2x, 3x, dst)
- Durasi pengobatan (1-14 hari)
- Notifikasi tepat waktu menggunakan `AlarmManager.setAlarmClock()`
- Pengingat otomatis setiap hari

### 4. 🔔 Pengaturan Notifikasi
- Aktifkan/nonaktifkan notifikasi
- Menggunakan nada dering default sistem
- Respek mode DND dan Silent

### 5. 👤 Manajemen Profil
- Edit informasi pribadi
- Upload foto profil
- Statistik konsultasi
- Sinkronisasi dengan Firebase

### 6. 📞 Bantuan & Dukungan
- FAQ (Frequently Asked Questions)
- Panduan penggunaan aplikasi
- Kontak support (WhatsApp, Email, Telepon)
- Informasi privasi dan disclaimer

---

## 🛠️ Teknologi yang Digunakan

### Platform & Framework
- **Android SDK** - MinSDK 26 (Android 8.0 Oreo)
- **Java** - Bahasa pemrograman utama
- **Gradle 8.x** - Build system

### Libraries & Dependencies
- **Firebase**
  - Firebase Authentication - Login/Register user
  - Firebase Firestore - Database cloud
  - Firebase Storage - Penyimpanan foto profil
  
- **Networking**
  - Retrofit 2 - HTTP client
  - OkHttp - Network layer
  - Gson - JSON serialization
  
- **UI/UX**
  - Material Design Components
  - RecyclerView - List rendering
  - ViewPager2 - Swipeable views
  - Glide - Image loading & caching
  
- **Other**
  - SharedPreferences - Local storage
  - AlarmManager - Scheduled notifications
  - NotificationCompat - Android notifications

---

## 📋 Persyaratan Sistem

### Minimum Requirements
- Android 8.0 (Oreo) atau lebih tinggi
- RAM minimal 2GB
- Storage minimal 100MB
- Koneksi internet (untuk fitur konsultasi AI)

### Permissions
- `INTERNET` - Koneksi ke server AI
- `POST_NOTIFICATIONS` - Menampilkan notifikasi (Android 13+)
- `SCHEDULE_EXACT_ALARM` - Alarm tepat waktu
- `RECEIVE_BOOT_COMPLETED` - Restore alarm setelah reboot
- `READ_MEDIA_IMAGES` - Upload foto profil

---

## 🚀 Instalasi & Setup

### 1. Clone Repository
```bash
git clone https://github.com/yourusername/MedZone.git
cd MedZone
```

### 2. Konfigurasi Firebase
1. Buat project baru di [Firebase Console](https://console.firebase.google.com)
2. Download file `google-services.json`
3. Letakkan di folder `app/`
4. Enable Authentication (Email/Password)
5. Enable Firestore Database
6. Enable Storage

### 3. Build & Run
```bash
# Via Android Studio
1. Open project di Android Studio
2. Sync Gradle
3. Run pada emulator atau device

# Via Command Line
./gradlew assembleDebug
./gradlew installDebug
```

---

## 📱 Cara Penggunaan

### Pertama Kali
1. **Registrasi** - Buat akun dengan email dan password
2. **Login** - Masuk dengan akun yang sudah dibuat
3. **Lengkapi Profil** - Upload foto dan lengkapi data diri

### Konsultasi
1. Klik tab **Beranda**
2. Ketik keluhan Anda (atau pilih quick chips)
3. Klik **Cari Obat yang Sesuai**
4. Tunggu hasil rekomendasi dari AI
5. Lihat detail obat dan dosisnya

### Atur Pengingat Obat
1. Setelah mendapat rekomendasi, klik tombol **Atur Pengingat**
2. Pilih frekuensi per hari (misalnya 3× sehari)
3. Atur waktu untuk setiap dosis
4. Pilih durasi pengobatan
5. Klik **Simpan Jadwal**
6. Notifikasi akan muncul tepat waktu sesuai jadwal

### Lihat Riwayat
1. Klik tab **Riwayat**
2. Lihat semua konsultasi sebelumnya
3. Klik item untuk melihat detail

### Pengaturan
1. Klik tab **Profil**
2. Akses berbagai menu:
   - **Pengaturan Akun** - Edit profil
   - **Notifikasi** - Atur preferensi notifikasi
   - **Bantuan & Dukungan** - FAQ dan kontak

---

## 🏗️ Arsitektur Aplikasi

### Package Structure
```
com.example.medzone/
├── activities/           # Activity screens
│   ├── LoginActivity
│   ├── RegisterActivity
│   ├── AccountSettingsActivity
│   ├── NotificationSettingsActivity
│   └── HelpSupportActivity
│
├── ui/                   # Fragment UI
│   ├── home/            # Home Fragment
│   ├── history/         # History Fragment
│   └── profile/         # Profile Fragment
│
├── reminder/            # Notification system
│   ├── ReminderReceiver
│   ├── ReminderScheduler
│   └── BootReceiver
│
├── models/              # Data models
│   ├── ConsultationHistory
│   └── ApiResponse
│
├── adapters/            # RecyclerView adapters
│   ├── HistoryAdapter
│   └── RecommendationAdapter
│
├── utils/               # Utilities
│   ├── NotificationPreferences
│   └── ValidationUtils
│
└── MyApplication        # Application class
```

### Notification System
- Menggunakan `AlarmManager.setAlarmClock()` untuk timing tepat waktu
- Notification channel dengan importance HIGH
- Support untuk vibration dan custom pattern
- Auto-reschedule setelah device reboot
- Duration-based reminder dengan auto-stop

---


## 🔐 Keamanan & Privacy

### Data User
- Password di-hash dengan Firebase Authentication
- Data pribadi disimpan terenkripsi di Firestore
- Foto profil disimpan di Firebase Storage dengan akses terbatas

### Network Security
- Menggunakan HTTPS untuk semua komunikasi
- Network Security Config untuk additional protection
- Certificate pinning (optional)

### Permissions
Aplikasi hanya meminta permission yang benar-benar diperlukan dan menjelaskan tujuan penggunaannya kepada user.

---

## 🐛 Troubleshooting

### Notifikasi Tidak Muncul
1. Pastikan notifikasi diaktifkan di Settings aplikasi
2. Cek pengaturan notifikasi sistem Android
3. Pastikan battery optimization tidak membatasi aplikasi
4. Untuk OEM tertentu (Xiaomi, OPPO), cek autostart permission


### Konsultasi Gagal
1. Cek koneksi internet
2. Pastikan server AI tidak down
3. Cek format input keluhan sudah benar
4. Restart aplikasi dan coba lagi

---

## 📝 Changelog

### Version 1.0.0 (2025-12-03)
- ✅ Initial release
- ✅ Fitur konsultasi AI dengan rekomendasi obat
- ✅ Sistem pengingat obat dengan notifikasi
- ✅ Manajemen profil user
- ✅ Riwayat konsultasi
- ✅ Bantuan & dukungan
- ✅ Build production-ready

---

## 🤝 Kontribusi

Kontribusi selalu diterima! Jika Anda ingin berkontribusi:

1. Fork repository
2. Buat branch baru (`git checkout -b feature/AmazingFeature`)
3. Commit perubahan (`git commit -m 'Add some AmazingFeature'`)
4. Push ke branch (`git push origin feature/AmazingFeature`)
5. Buat Pull Request

---

## 👥 Tim Pengembang

**Grup 7 - Mobile Programming**

| Role | Name           | GitHub |
|------|----------------|--------|
| Developer | Rafid Hilmi    | [@RazorPG](https://github.com/RazorPG) |
| Developer | Tiara Utari    | [@23346022](https://github.com/23346022) |
| Developer | Rahmi Fah Riza | [@rahmifahriza](https://github.com/rahmifahriza) |

---

## 📄 Lisensi

Aplikasi ini dibuat untuk keperluan edukasi dan pembelajaran.

---

## 📞 Kontak & Support

Jika ada pertanyaan atau masalah:

- **WhatsApp**: 0812-6665-4510
- **Email**: rafidhilmi0504@gmail.com
- **Phone**: 0812-6665-4510

---

## ⚠️ Disclaimer Final

MedZone adalah aplikasi **informasi kesehatan umum** dan **bukan pengganti konsultasi medis profesional**.

- ❌ Jangan gunakan untuk kondisi darurat medis
- ❌ Jangan gunakan untuk diagnosis penyakit serius
- ❌ Jangan gunakan sebagai satu-satunya sumber informasi obat
- ✅ Selalu konsultasikan dengan dokter atau apoteker
- ✅ Baca aturan pakai obat dengan teliti
- ✅ Perhatikan efek samping dan kontraindikasi

**Segera hubungi dokter jika:**
- Gejala tidak membaik dalam 2-3 hari
- Gejala memburuk
- Demam tinggi tidak turun
- Ada tanda-tanda reaksi alergi obat

---

**© 2025 MedZone. All rights reserved.**

