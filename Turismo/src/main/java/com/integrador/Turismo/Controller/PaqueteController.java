package com.integrador.Turismo.Controller;

import com.integrador.Turismo.Model.Paquete;
import com.integrador.Turismo.Service.PaqueteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paquetes")
@RequiredArgsConstructor
public class PaqueteController {
    private final PaqueteService paqueteService;

    // ── Públicos (sin login) ──────────────────────────────────

    // GET /api/paquetes
    @GetMapping
    public ResponseEntity<List<Paquete>> listar() {
        return ResponseEntity.ok(paqueteService.listarActivos());
    }

    // GET /api/paquetes/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Paquete> detalle(@PathVariable String id) {
        return ResponseEntity.ok(paqueteService.obtenerPorId(id));
    }

    // ── Solo ADMIN ────────────────────────────────────────────

    // POST /api/paquetes
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Paquete> crear(@RequestBody Paquete paquete) {
        return ResponseEntity.ok(paqueteService.crear(paquete));
    }

    // PUT /api/paquetes/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Paquete> actualizar(@PathVariable String id,
                                              @RequestBody Paquete datos) {
        return ResponseEntity.ok(paqueteService.actualizar(id, datos));
    }

    // PATCH /api/paquetes/{id}/toggle
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Paquete> toggleActivo(@PathVariable String id) {
        return ResponseEntity.ok(paqueteService.toggleActivo(id));
    }

    // DELETE /api/paquetes/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        paqueteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
