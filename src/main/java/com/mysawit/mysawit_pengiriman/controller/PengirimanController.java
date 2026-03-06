package com.mysawit.mysawit_pengiriman.controller;

import com.mysawit.mysawit_pengiriman.dto.AssignPengirimanRequest;
import com.mysawit.mysawit_pengiriman.dto.UpdateStatusRequest;
import com.mysawit.mysawit_pengiriman.dto.VerifikasiRequest;
import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.service.PengirimanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/pengiriman")
public class PengirimanController {

    @Autowired
    private PengirimanService pengirimanService;

    @PostMapping("/assign")
    public ResponseEntity<?> assignPengiriman(@RequestBody AssignPengirimanRequest request) {
        try {
            Pengiriman pengiriman = new Pengiriman();
            pengiriman.setSupirId(request.getSupirId());
            pengiriman.setMandorId(request.getMandorId());
            pengiriman.setTotalAngkutan(request.getTotalAngkutan());

            Pengiriman result = pengirimanService.assignPengiriman(pengiriman);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/status-supir")
    public ResponseEntity<?> updateStatusSupir(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        try {
            Pengiriman result = pengirimanService.updateStatusBySupir(id, request.getStatus());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/verifikasi-mandor")
    public ResponseEntity<?> verifikasiMandor(@PathVariable UUID id, @RequestBody VerifikasiRequest request) {
        try {
            Pengiriman result = pengirimanService.verifikasiOlehMandor(id, request.isApproved(), request.getAlasan());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/verifikasi-admin")
    public ResponseEntity<?> verifikasiAdmin(@PathVariable UUID id, @RequestBody VerifikasiRequest request) {
        try {
            Pengiriman result = pengirimanService.verifikasiOlehAdmin(id, request.isApproved(), request.getAlasan());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/tolak-parsial")
    public ResponseEntity<?> tolakParsialAdmin(@PathVariable UUID id, @RequestBody VerifikasiRequest request) {
        try {
            Pengiriman result = pengirimanService.tolakParsialOlehAdmin(id, request.getAlasan(), request.getAngkutanDiakui());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}