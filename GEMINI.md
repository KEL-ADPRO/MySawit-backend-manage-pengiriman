# Prompt Penyelesaian Modul 4 MySawit (Managemen Pengiriman)

Bertindaklah sebagai Senior Backend Engineer yang sangat ahli dalam Java Spring Boot, arsitektur Microservices, gRPC, dan praktik *Clean Code*. Saya sedang mengerjakan proyek akhir Advanced Programming bernama MySawit. Tugas saya adalah mengimplementasikan **Modul 4: Managemen Pengiriman Hasil Panen Sawit**. 

Saya meminta kamu untuk membuatkan **FULL CODE DAN FULL IMPLEMENTASI** untuk modul ini secara menyeluruh dari awal hingga akhir, siap digunakan. Pastikan tidak ada *placeholder* logika.

## Konteks Modul 4 (Managemen Pengiriman Hasil Panen Sawit):
* **Tujuan:** Mengatur proses pengiriman hasil panen sawit valid dari kebun ke pabrik produksi.
* **Entitas Utama:** Pengiriman (Memiliki ID, ID Supir, ID Mandor, Daftar ID Hasil Panen, Total Berat, Status, Alasan Penolakan, Timestamp). Status pengiriman: `MEMUAT` (default), `MENGIRIM`, `TIBA_DI_TUJUAN`, `DISETUJUI_MANDOR`, `DITOLAK_MANDOR`, `DISETUJUI_ADMIN`, `DITOLAK_ADMIN`, `DITOLAK_PARSIAL_ADMIN`.
* **Batasan:** Berat total hasil panen dalam satu pengiriman tidak boleh melebihi 400 Kg.
* **Logika Peran:**
    * **Mandor:** Bisa melihat daftar supir di kebun yang sama, menugaskan supir untuk mengangkut hasil panen (Create Pengiriman), melihat status pengiriman, menyetujui (Approve) atau menolak (Reject dengan alasan) pengiriman yang berstatus `TIBA_DI_TUJUAN`.
    * **Supir Truk:** Bisa melihat daftar tugas pengiriman, mengubah status pengiriman secara berurutan (`MEMUAT` -> `MENGIRIM` -> `TIBA_DI_TUJUAN`), melihat riwayat pengiriman.
    * **Admin Utama:** Bisa melihat daftar pengiriman yang disetujui mandor. Bisa menyetujui, menolak penuh dengan alasan, atau menolak parsial (menyertakan kilogram sawit yang diakui dan alasan).
* **Integrasi (gRPC):** Setiap modul berjalan mandiri dan tidak berbagi *database*. Modul ini harus:
    1. Menyediakan gRPC server/endpoint yang bisa di-*consume* modul lain (jika diperlukan).
    2. Menjadi gRPC client untuk memanggil "User Module" (memverifikasi role dan assignment Mandor/Supir).
    3. Menjadi gRPC client untuk memanggil "Hasil Panen Module" (memverifikasi berat panen valid).
    4. Memicu "Payment Module" secara *asynchronous* (kirim event gRPC) setelah Mandor menyetujui pengiriman (untuk payroll supir) dan setelah Admin menyetujui pengiriman (untuk payroll mandor).

## Persyaratan Teknis Ketat:
1. **Framework:** Java Spring Boot 3.x, Hibernate/JPA, PostgreSQL.
2. **gRPC:** Implementasikan file `.proto` untuk modul ini dan integrasinya. Tuliskan implementasi *Service* gRPC-nya menggunakan `grpc-spring-boot-starter`.
3. **SOLID Principles:** Pisahkan *Controller* (REST/gRPC), *UseCase* (Interface), *Service* (Business Logic), dan *Repository* (Data Access).
4. **TDD (Test-Driven Development):** Sebelum menampilkan kode implementasi, JABARKAN test casenya (Red) menggunakan JUnit 5 dan Mockito. Lalu tuliskan implementasi *Service*-nya (Green). Minimal wajib buat *Unit Test* lengkap untuk logika bisnis utama (validasi berat > 400kg, transisi status yang tidak valid).
5. **CI Pipeline:** Tuliskan file `.github/workflows/ci.yml` lengkap yang berisi tahap *build*, *test*, dan *linting* untuk Gradle/Maven.
6. **Git Commits:** Berikan instruksi perintah `git commit -m "..."` secara kronologis di setiap akhir penyelesaian sebuah fitur/komponen untuk mensimulasikan alur kerja yang baik.

## Instruksi Output:
Keluarkan seluruh kode secara terstruktur. Mulai dari konfigurasi `.proto`, *Entity*, *Repository*, *Service/UseCase*, *Controller*, hingga *Unit Tests*. Jangan tinggalkan kode setengah jalan. Tuliskan setiap *file* lengkap dengan dependensi *import*-nya.