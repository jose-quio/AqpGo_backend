package com.integrador.Turismo.Service;

import com.integrador.Turismo.Model.Paquete;
import com.integrador.Turismo.Repository.PaqueteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class PaqueteService {
    private final PaqueteRepository paqueteRepository;

    // Público — lista todos los paquetes activos
    public List<Paquete> listarActivos() {
        return paqueteRepository.findByActivoTrue();
    }

    // Público — detalle de un paquete por ID
    public Paquete obtenerPorId(String id) {
        return paqueteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paquete no encontrado: " + id));
    }

    // Admin — crear paquete
    public Paquete crear(Paquete paquete) {
        return paqueteRepository.save(paquete);
    }

    // Admin — actualizar paquete
    public Paquete actualizar(String id, Paquete datos) {
        Paquete existente = obtenerPorId(id);
        existente.setNombre(datos.getNombre());
        existente.setSubtitulo(datos.getSubtitulo());
        existente.setResumenMd(datos.getResumenMd());
        existente.setRecomendacionesMd(datos.getRecomendacionesMd());
        existente.setIncluyeMd(datos.getIncluyeMd());
        existente.setNoIncluyeMd(datos.getNoIncluyeMd());
        existente.setPreguntasMd(datos.getPreguntasMd());
        existente.setPrecioBase(datos.getPrecioBase());
        existente.setDuracionDias(datos.getDuracionDias());
        existente.setDuracionNoches(datos.getDuracionNoches());
        existente.setMapaUrl(datos.getMapaUrl());
        return paqueteRepository.save(existente);
    }

    // Admin — activar/desactivar
    public Paquete toggleActivo(String id) {
        Paquete paquete = obtenerPorId(id);
        paquete.setActivo(!paquete.getActivo());
        return paqueteRepository.save(paquete);
    }

    // Admin — eliminar
    public void eliminar(String id) {
        paqueteRepository.deleteById(id);
    }
}
