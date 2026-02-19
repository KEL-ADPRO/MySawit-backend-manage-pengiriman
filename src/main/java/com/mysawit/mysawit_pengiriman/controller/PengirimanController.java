package com.mysawit.mysawit_pengiriman.controller;

import com.mysawit.mysawit_pengiriman.model.Pengiriman;
import com.mysawit.mysawit_pengiriman.service.PengirimanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/pengiriman")
public class PengirimanController {

    @Autowired
    private PengirimanService pengirimanService;

    @GetMapping
    public List<Pengiriman> getAllPengiriman() {
        return pengirimanService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pengiriman> getPengirimanById(@PathVariable UUID id) {
        Pengiriman pengiriman = pengirimanService.findById(id);
        return ResponseEntity.ok(pengiriman);
    }

    @PostMapping
    public ResponseEntity<Pengiriman> createPengiriman(@RequestBody Pengiriman pengiriman) {
        Pengiriman savedPengiriman = pengirimanService.create(pengiriman);
        return ResponseEntity.ok(savedPengiriman);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pengiriman> updatePengiriman(@PathVariable UUID id, @RequestBody Pengiriman pengirimanDetails) {
        Pengiriman updatedPengiriman = pengirimanService.update(id, pengirimanDetails);
        return ResponseEntity.ok(updatedPengiriman);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePengiriman(@PathVariable UUID id) {
        pengirimanService.delete(id);
        return ResponseEntity.ok("Data Pengiriman dengan ID " + id + " berhasil dihapus.");
    }
}