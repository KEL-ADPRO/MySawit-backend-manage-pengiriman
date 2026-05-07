# Modul 4 Test Cases

Dokumen ini mendahulukan daftar test case utama sebelum implementasi service.

## Unit test prioritas

1. `createShipment_shouldRejectWhenTotalWeightExceedsTruckCapacity`
   - Mandor memilih beberapa hasil panen valid.
   - Berat total hasil panen lebih dari 400 kg.
   - Service harus melempar `BusinessRuleViolationException`.

2. `createShipment_shouldRejectWhenNoHarvestSelected`
   - Mandor mencoba membuat pengiriman tanpa hasil panen.
   - Service harus melempar `BusinessRuleViolationException`.

3. `updateDriverStatus_shouldRejectInvalidStatusJump`
   - Supir mencoba mengubah status dari `MEMUAT` langsung ke `TIBA_DI_TUJUAN`.
   - Service harus menolak transisi status.

4. `updateDriverStatus_shouldRejectWhenShipmentBelongsToAnotherDriver`
   - Supir yang bukan penanggung jawab mencoba memperbarui status.
   - Service harus melempar `AccessDeniedBusinessException`.

5. `mandorReview_shouldRejectBeforeShipmentArrives`
   - Mandor mencoba approval saat status belum `TIBA_DI_TUJUAN`.
   - Service harus melempar `BusinessRuleViolationException`.

6. `mandorReview_shouldApproveAndTriggerDriverPayroll`
   - Pengiriman sudah `TIBA_DI_TUJUAN`.
   - Mandor menyetujui pengiriman.
   - Status berubah menjadi `DISETUJUI_MANDOR` dan event payroll supir dipicu.

7. `adminReview_shouldRejectWhenShipmentNotApprovedByMandor`
   - Admin mencoba mereview pengiriman yang belum disetujui mandor.
   - Service harus menolak.

8. `adminReview_shouldPartiallyRejectWithRecognizedWeight`
   - Admin menolak parsial dengan berat yang diakui.
   - Status berubah menjadi `DITOLAK_PARSIAL_ADMIN`.
   - Event payroll mandor tetap dipicu dengan berat yang diakui.

9. `adminReview_shouldRejectRecognizedWeightAboveTotalWeight`
   - Berat yang diakui melebihi berat total pengiriman.
   - Service harus menolak.

10. `listShipments_shouldFilterByRoleAndDate`
    - Query riwayat pengiriman harus menghormati parameter `driverId`, `mandorId`, status, dan tanggal.

## Functional test prioritas

1. Membuat pengiriman via REST dan menerima respons `201 Created`.
2. Mengubah status supir via REST dan menerima payload status terbaru.
3. Mengambil daftar pengiriman supir via REST dengan filter tanggal.

## Rekomendasi urutan commit

1. `git commit -m "build: add grpc, protobuf, and quality tooling"`
2. `git commit -m "feat: add shipment domain model and repositories"`
3. `git commit -m "feat: implement shipment application services"`
4. `git commit -m "feat: expose shipment rest and grpc endpoints"`
5. `git commit -m "test: cover shipment business rules and controller flow"`
6. `git commit -m "ci: add gradle quality pipeline"`
