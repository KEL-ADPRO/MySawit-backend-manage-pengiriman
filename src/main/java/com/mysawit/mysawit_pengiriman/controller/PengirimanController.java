package com.mysawit.mysawit_pengiriman.controller;

import com.mysawit.mysawit_pengiriman.dto.AssignPengirimanRequest;
import com.mysawit.mysawit_pengiriman.dto.UpdateStatusRequest;
import com.mysawit.mysawit_pengiriman.dto.VerifikasiRequest;
import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.service.PengirimanService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/pengiriman")
public class PengirimanController {

    private final PengirimanService pengirimanService;

    public PengirimanController(PengirimanService pengirimanService) {
        this.pengirimanService = pengirimanService;
    }

    @PostMapping("/assign")
    public ResponseEntity<Pengiriman> assignPengiriman(@Valid @RequestBody AssignPengirimanRequest request) {
        Pengiriman pengiriman = new Pengiriman();
        pengiriman.setSupirId(request.getSupirId());
        pengiriman.setMandorId(request.getMandorId());
        pengiriman.setSupirNama(request.getSupirNama());
        pengiriman.setMandorNama(request.getMandorNama());
        pengiriman.setTotalAngkutan(request.getTotalAngkutan());
        pengiriman.setHasilPanenIds(request.getHasilPanenIds());

        return ResponseEntity.ok(pengirimanService.assignPengiriman(pengiriman));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pengiriman> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(pengirimanService.getDetail(id));
    }

    @GetMapping("/supir/{supirId}")
    public ResponseEntity<List<Pengiriman>> getDaftarPengirimanSupir(
        @PathVariable UUID supirId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(pengirimanService.getDaftarPengirimanSupir(supirId, startDate, endDate));
    }

    @GetMapping("/supir/{supirId}/riwayat")
    public ResponseEntity<List<Pengiriman>> getRiwayatPengirimanSupir(
        @PathVariable UUID supirId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggal
    ) {
        return ResponseEntity.ok(pengirimanService.getRiwayatPengirimanSupir(supirId, tanggal));
    }

    @GetMapping("/mandor/{mandorId}/berlangsung")
    public ResponseEntity<List<Pengiriman>> getPengirimanBerlangsungMandor(
        @PathVariable UUID mandorId,
        @RequestParam(required = false) String supirNama
    ) {
        return ResponseEntity.ok(pengirimanService.getPengirimanBerlangsungMandor(mandorId, supirNama));
    }

    @GetMapping("/mandor/{mandorId}/supir/{supirId}")
    public ResponseEntity<List<Pengiriman>> getDaftarPengirimanSupirMandor(
        @PathVariable UUID mandorId,
        @PathVariable UUID supirId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggal
    ) {
        return ResponseEntity.ok(pengirimanService.getDaftarPengirimanSupirMandor(mandorId, supirId, tanggal));
    }

    @GetMapping("/admin/persetujuan")
    public ResponseEntity<List<Pengiriman>> getDaftarPersetujuanAdmin(
        @RequestParam(required = false) String mandorNama,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tanggal
    ) {
        return ResponseEntity.ok(pengirimanService.getDaftarPersetujuanAdmin(mandorNama, tanggal));
    }

    @PatchMapping("/{id}/status-supir")
    public ResponseEntity<Pengiriman> updateStatusSupir(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(pengirimanService.updateStatusBySupir(id, request.getStatus()));
    }

    @PostMapping("/{id}/verifikasi-mandor")
    public ResponseEntity<Pengiriman> verifikasiMandor(@PathVariable UUID id, @Valid @RequestBody VerifikasiRequest request) {
        return ResponseEntity.ok(pengirimanService.verifikasiOlehMandor(id, request.getApproved(), request.getAlasan()));
    }

    @PostMapping("/{id}/verifikasi-admin")
    public ResponseEntity<Pengiriman> verifikasiAdmin(@PathVariable UUID id, @Valid @RequestBody VerifikasiRequest request) {
        return ResponseEntity.ok(pengirimanService.verifikasiOlehAdmin(id, request.getApproved(), request.getAlasan()));
    }

    @PostMapping("/{id}/tolak-parsial")
    public ResponseEntity<Pengiriman> tolakParsialAdmin(@PathVariable UUID id, @Valid @RequestBody VerifikasiRequest request) {
        return ResponseEntity.ok(pengirimanService.tolakParsialOlehAdmin(id, request.getAlasan(), request.getAngkutanDiakui()));
    }
}
