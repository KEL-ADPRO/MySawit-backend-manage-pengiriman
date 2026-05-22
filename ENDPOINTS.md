# API Endpoints — MySawit Manage Pengiriman

## Base URL
- **REST:** `http://<host>:8084`
- **gRPC:** `<host>:9094`

---

## REST API — `/api/v1/shipments`

### 1. Buat Pengiriman Baru
```
POST /api/v1/shipments
```
**Request Body:**
```json
{
  "mandorId": "uuid-mandor",
  "driverId": "uuid-driver",
  "harvestIds": ["uuid-harvest-1", "uuid-harvest-2"]
}
```
**Yang terjadi:**
- Validasi mandor & driver satu estate (gRPC → User service)
- Validasi harvest sudah approved & total berat ≤ 400 kg (gRPC → Harvest service)
- Simpan pengiriman dengan status `MEMUAT`

**Response:** `201 Created` → ShipmentResponse

---

### 2. Ambil Daftar Pengiriman
```
GET /api/v1/shipments
```
**Query Params (semua opsional):**
| Param | Contoh | Keterangan |
|---|---|---|
| `driverId` | `uuid-driver` | Filter by driver |
| `mandorId` | `uuid-mandor` | Filter by mandor |
| `status` | `MEMUAT` | Filter by status |
| `date` | `2026-05-22` | Filter by tanggal buat (ISO 8601) |

**Response:** `200 OK` → `List<ShipmentResponse>`

---

### 3. Ambil Detail Pengiriman
```
GET /api/v1/shipments/{shipmentId}
```
**Path Variable:** `shipmentId` (UUID)

**Response:** `200 OK` → ShipmentResponse | `404 Not Found`

---

### 4. Ambil Pengiriman yang Disetujui Mandor (Antrean Admin)
```
GET /api/v1/shipments/approved-by-mandor
```
**Query Params (semua opsional):**
| Param | Contoh | Keterangan |
|---|---|---|
| `mandorId` | `uuid-mandor` | Filter by mandor |
| `date` | `2026-05-22` | Filter by tanggal |

**Yang terjadi:** Mengembalikan pengiriman berstatus `DISETUJUI_MANDOR` yang menunggu review Admin

**Response:** `200 OK` → `List<ShipmentResponse>`

---

### 5. Ambil Daftar Driver (untuk Mandor)
```
GET /api/v1/shipments/drivers?mandorId={mandorId}&search={search}
```
**Query Params:**
| Param | Wajib | Keterangan |
|---|---|---|
| `mandorId` | ✅ | UUID mandor yang mencari driver |
| `search` | ❌ | Kata kunci pencarian nama driver |

**Yang terjadi:** Query ke User service via gRPC untuk mendapatkan driver di estate yang sama

**Response:** `200 OK` → `List<DriverSummaryResponse>` (id, name, estateId)

---

### 6. Driver Update Status Perjalanan
```
PATCH /api/v1/shipments/{shipmentId}/driver-status
```
**Request Body:**
```json
{
  "driverId": "uuid-driver",
  "newStatus": "MENGIRIM"
}
```
**Alur transisi yang diizinkan:**
```
MEMUAT → MENGIRIM → TIBA_DI_TUJUAN
```
**Response:** `200 OK` → ShipmentResponse | `400` (transisi invalid) | `403` (bukan driver shipment ini)

---

### 7. Mandor Review Pengiriman
```
PATCH /api/v1/shipments/{shipmentId}/mandor-review
```
**Request Body:**
```json
{
  "mandorId": "uuid-mandor",
  "approved": true,
  "rejectionReason": null
}
```
> Jika `approved: false`, field `rejectionReason` **wajib diisi**

**Yang terjadi setelah approve:**
- Status → `DISETUJUI_MANDOR`
- Trigger payroll Driver secara async ke Payment service (gRPC)

**Response:** `200 OK` → ShipmentResponse | `400` (status bukan TIBA_DI_TUJUAN) | `403` (bukan mandor shipment ini)

---

### 8. Admin Review Pengiriman
```
PATCH /api/v1/shipments/{shipmentId}/admin-review
```
**Request Body:**
```json
{
  "adminId": "uuid-admin",
  "decision": "APPROVE",
  "recognizedWeightKg": null,
  "rejectionReason": null
}
```
**Pilihan `decision`:**
| Decision | `recognizedWeightKg` | `rejectionReason` | Status Hasil |
|---|---|---|---|
| `APPROVE` | Tidak perlu | Tidak perlu | `DISETUJUI_ADMIN` |
| `REJECT` | Tidak perlu | **Wajib** | `DITOLAK_ADMIN` |
| `PARTIAL_REJECT` | **Wajib** (> 0, < total) | **Wajib** | `DITOLAK_PARSIAL_ADMIN` |

**Yang terjadi setelah APPROVE / PARTIAL_REJECT:**
- Trigger payroll Mandor secara async ke Payment service (gRPC)

**Response:** `200 OK` → ShipmentResponse | `400` | `403` (bukan Admin)

---

## gRPC RPCs — `port 9094`

### 1. GetShipmentById
```protobuf
rpc GetShipmentById (GetShipmentByIdRequest) returns (ShipmentSummary)
```
**Request:** `shipment_id` (string UUID)

**Response (ShipmentSummary):**
```
id, status, supir_user_id, mandor_user_id,
delivered_kg, recognized_kg,
created_at, mandor_reviewed_at, admin_reviewed_at
```
> Digunakan modul **Payment** untuk verifikasi source payroll

---

### 2. GetRecognizedWeight
```protobuf
rpc GetRecognizedWeight (GetShipmentByIdRequest) returns (RecognizedWeightResponse)
```
**Request:** `shipment_id` (string UUID)

**Response:** `shipment_id`, `recognized_kg` (empty string jika belum Admin review)

> Lightweight endpoint untuk kalkulasi payroll Mandor di modul Payment

---

### 3. ListShipmentsByDriver
```protobuf
rpc ListShipmentsByDriver (ListShipmentsByDriverRequest) returns (ShipmentListMessage)
```
**Request:** `driver_id`, `date` (opsional, format `YYYY-MM-DD`)

**Response:** List `ShipmentMessage` (pesan internal lengkap)

---

### 4. UpdateDriverStatus
```protobuf
rpc UpdateDriverStatus (UpdateDriverStatusGrpcRequest) returns (ShipmentMessage)
```
**Request:** `shipment_id`, `driver_id`, `new_status` (ShipmentStatusGrpc enum)

**Response:** `ShipmentMessage` dengan status terbaru

---

## Alur Status Lengkap

```
[Mandor buat]
POST /shipments → MEMUAT
                    │
       PATCH /driver-status
                    ↓
                 MENGIRIM
                    │
       PATCH /driver-status
                    ↓
            TIBA_DI_TUJUAN
            /              \
PATCH /mandor-review     PATCH /mandor-review
  (approved=true)          (approved=false)
          │                       │
   DISETUJUI_MANDOR          DITOLAK_MANDOR ← (FINAL)
          │
  PATCH /admin-review
  /        |         \
APPROVE  REJECT   PARTIAL_REJECT
   │        │           │
DISETUJUI  DITOLAK  DITOLAK_PARSIAL
_ADMIN     _ADMIN   _ADMIN
(FINAL)   (FINAL)   (FINAL)
```

---

## Response Shape (ShipmentResponse)

```json
{
  "id": "uuid",
  "driverId": "uuid",
  "mandorId": "uuid",
  "harvestIds": ["uuid1", "uuid2"],
  "totalWeightKg": 200.0,
  "recognizedWeightKg": 150.0,
  "status": "DISETUJUI_ADMIN",
  "rejectionReason": null,
  "createdAt": "2026-05-22T04:00:00Z",
  "updatedAt": "2026-05-22T05:00:00Z",
  "mandorReviewedAt": "2026-05-22T04:30:00Z",
  "adminReviewedAt": "2026-05-22T05:00:00Z"
}
```

---

## Error Responses

| HTTP Status | Kondisi |
|---|---|
| `400 Bad Request` | Validasi gagal, transisi status tidak valid, berat melebihi kapasitas |
| `403 Forbidden` | Bukan aktor yang berwenang (driver/mandor/admin tidak cocok) |
| `404 Not Found` | Shipment dengan ID tersebut tidak ditemukan |
