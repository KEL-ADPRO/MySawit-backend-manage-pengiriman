# Struktur Proyek MySawit - Manage Pengiriman

Dokumen ini menjelaskan struktur lengkap dari direktori dan file yang ada pada proyek `MySawit-backend-manage-pengiriman`. Proyek ini dibangun menggunakan Spring Boot dengan arsitektur berbasis *Clean Architecture* dan *Domain-Driven Design (DDD)*, serta menggunakan gRPC untuk komunikasi antar-layanan (microservices).

## Direktori Root

- **`.github/workflows/`**: Berisi konfigurasi GitHub Actions (seperti `cicd.yml`) untuk otomatisasi *Continuous Integration* dan *Continuous Deployment* (CI/CD).
- **`bin/` & `build/`**: Direktori *output* hasil kompilasi program dan *build* dari Gradle.
- **`config/checkstyle/`**: Berisi file `checkstyle.xml` untuk mengatur standar penulisan kode Java (linter) di proyek ini.
- **`docs/`**: Berisi dokumentasi teknis tambahan, seperti `modul4-test-cases.md`.
- **`gradle/`**: Direktori bawaan dari Gradle Wrapper.
- **`src/`**: Direktori utama yang menyimpan *source code* aplikasi dan pengujian (*testing*).

## File Root
- **`build.gradle.kts` & `settings.gradle.kts`**: Konfigurasi manajemen dependensi dan *build* proyek menggunakan Gradle dengan Kotlin DSL.
- **`Dockerfile` & `.dockerignore`**: Konfigurasi untuk membuat Docker image dari aplikasi ini (containerization).
- **`.env` & `.env.example`**: Tempat menyimpan variabel lingkungan (*environment variables*) seperti kredensial database.
- **`ENDPOINTS.md`**: File dokumentasi yang menjelaskan daftar *endpoint* REST API yang tersedia.
- **`Procfile` & `system.properties`**: Konfigurasi yang biasanya digunakan untuk *deployment* ke *platform-as-a-service* seperti Heroku.
- **`gradlew` & `gradlew.bat`**: Skrip eksekusi Gradle Wrapper untuk Unix dan Windows.
- **`README.md`**: Informasi umum mengenai proyek.

---

## Direktori `src/main/`

Ini adalah direktori utama kode aplikasi yang akan dijalankan.

### 1. `src/main/proto/`
Berisi file `.proto` (Protobuf) yang mendefinisikan *contract* untuk komunikasi gRPC antar-microservice:
- `harvest.proto`: Kontrak untuk layanan Panen.
- `payment.proto`: Kontrak untuk layanan Pembayaran (Payroll).
- `shipment.proto`: Kontrak layanan Pengiriman (disediakan oleh modul ini untuk diakses layanan lain).
- `user.proto`: Kontrak untuk layanan Pengguna (Driver, Admin, Mandor).

### 2. `src/main/resources/`
- **`application.properties`**: File konfigurasi utama Spring Boot (port server, koneksi database PostgreSQL, URL gRPC, dll).

### 3. `src/main/java/com/mysawit/pengiriman/`
Paket dasar (*base package*) dari aplikasi backend pengiriman.

#### File Utama:
- **`PengirimanApplication.java`**: Titik masuk (*entry point*) utama aplikasi Spring Boot.

#### Direktori Konfigurasi (`config/`):
- **`AsyncConfig.java`**: Mengatur konfigurasi eksekusi proses secara asinkron (*asynchronous*), misalnya untuk pengiriman *event* atau proses *background*.

#### Direktori Antarmuka Pengguna/API (`controller/`):
- **`ShipmentController.java`**: Menerima *request* HTTP (REST API) dari *frontend* atau *client*, memproses input, dan memanggil *usecase/service*, lalu mengembalikan *response*.

#### Direktori DTO (`dto/`):
Berisi objek *Data Transfer Object* yang digunakan untuk mentransfer data dari/ke *client* REST API tanpa mengekspos *Entity* database.
- **`AdminReviewRequest.java`, `MandorReviewRequest.java`**: DTO untuk input form tinjauan/review.
- **`CreateShipmentRequest.java`, `DriverStatusUpdateRequest.java`, `ShipmentQueryRequest.java`**: DTO untuk input pengiriman.
- **`DriverSummaryResponse.java`, `ShipmentResponse.java`**: DTO untuk format *output* respons REST.

#### Direktori Entitas Database (`entity/`):
- **`Shipment.java`**: Representasi tabel database untuk data Pengiriman (JPA Entity). Berisi atribut seperti berat, status, ID driver, dll.

#### Direktori Enumerasi (`enums/`):
- **`AdminReviewDecision.java`**: Pilihan keputusan Admin (APPROVE, REJECT, PARTIAL_REJECT).
- **`ShipmentStatus.java`**: Status siklus hidup pengiriman (CREATED, IN_TRANSIT, DELIVERED, dll).

#### Direktori Event Driven (`event/`):
Mengatur *Domain Events* untuk memisahkan proses (Decoupling) ketika sesuatu terjadi pada *Shipment*.
- **`ShipmentApprovedByAdminEvent.java`, `ShipmentApprovedByMandorEvent.java`, `ShipmentPartialRejectedByAdminEvent.java`**: Kelas objek *event*.
- **`PayrollEventListener.java`**: Pendengar (*listener*) yang akan menangkap *event* *ShipmentApproved* dan memicu logika untuk memanggil microservice *Payment* melalui gRPC.

#### Direktori Exception & Error Handling (`exception/`):
- **`ApiExceptionHandler.java`**: *Global Exception Handler* (menggunakan `@ControllerAdvice`) untuk menangkap error dan mengembalikan *response* JSON yang rapi.
- **`AccessDeniedBusinessException.java`, `BusinessRuleViolationException.java`, `ResourceNotFoundException.java`**: Kelas-kelas *custom error* (karena aturan bisnis dilanggar, data tidak ditemukan, dll).

#### Direktori gRPC Server (`grpc/`):
- **`ShipmentGrpcService.java`**: Implementasi *server* gRPC yang menerima *request* dari layanan MySawit lainnya (jika mereka butuh data pengiriman).
- **`ShipmentGrpcMapper.java`**: Memetakan data dari *Entity* ke objek Protobuf.

#### Direktori Integrasi gRPC Client (`integration/`):
Digunakan untuk memanggil/berkomunikasi dengan *microservices* lain secara gRPC.
- **`gateway/*Gateway.java`**: Interface *Gateway* (*Port*) ke sistem eksternal (Harvest, Payment, User).
- **`client/Grpc*Gateway.java`**: Implementasi *Gateway* (*Adapter*) menggunakan *client* gRPC.
- **`dto/*Summary.java`**: Objek lokal untuk menampung respons dari *microservice* lain.

#### Direktori Mapper (`mapper/`):
- **`ShipmentMapper.java`**: Berfungsi untuk mengubah objek Entity ke DTO dan sebaliknya.

#### Direktori Profiling Performa (`profiling/`):
Sistem pemantauan kinerja aplikasi (metrik durasi eksekusi).
- **`Profiled.java`**: Anotasi kustom `@Profiled` yang bisa ditaruh di *method*.
- **`ProfilingAspect.java`**: Implementasi *Aspect-Oriented Programming (AOP)* yang menangkap eksekusi *method* dengan `@Profiled` dan mencatat waktunya ke Micrometer/Prometheus.

#### Direktori Akses Database (`repository/`):
- **`ShipmentRepository.java`**: Interface *Spring Data JPA* untuk melakukan operasi *CRUD* ke database pengiriman.
- **`ShipmentSpecifications.java`**: Membantu membangun *query* pencarian dinamis (misal filter berdasarkan status atau driver).

#### Direktori Logika Bisnis (`service/` & `usecase/`):
Pemisahan CQRS (Command Query Responsibility Segregation).
- **`usecase/ShipmentCommandUseCase.java` & `ShipmentQueryUseCase.java`**: Interface yang menjabarkan *behavior* aplikasi (Clean Architecture).
- **`service/ShipmentCommandService.java`**: Implementasi logika bisnis untuk operasi Write/Ubah (Create, Update Status, Review).
- **`service/ShipmentQueryService.java`**: Implementasi logika bisnis untuk operasi Read/Baja (Get All, Get By ID).

#### Direktori Design Pattern Strategy (`service/strategy/`):
Menerapkan *Strategy Pattern* untuk menangani logika berbagai keputusan Admin agar tidak menumpuk dalam instruksi `if-else`.
- **`AdminReviewStrategy.java`**: Interface strategi.
- **`AdminReviewStrategyFactory.java`**: *Factory* untuk mengambil strategi yang tepat berdasarkan keputusan Admin.
- **`ApproveAdminStrategy.java`, `RejectAdminStrategy.java`, `PartialRejectAdminStrategy.java`**: Implementasi logika masing-masing keputusan.

---

## Direktori `src/test/`

Berisi kode pengujian (Unit Test & Functional Test). Strukturnya persis meniru `src/main/` untuk memudahkan pemetaan.
- **`controller/`**: *Test* untuk API HTTP.
- **`entity/`, `enums/`**: *Test* logika model dasar.
- **`event/`**: *Test* apakah pendengar *event* berjalan dengan benar.
- **`exception/`**: *Test* perilaku *Global Exception Handler*.
- **`grpc/`**: *Test* layanan pemetaan dan server gRPC.
- **`mapper/`**: *Test* konversi Entity <-> DTO.
- **`profiling/`**: *Test* apakah *AOP Aspect* memonitor dengan benar.
- **`service/` & `service/strategy/`**: *Test* logika bisnis untuk command, query, dan strategi.
- **`resources/application.properties`**: Konfigurasi database *in-memory* (H2) untuk *testing*.
