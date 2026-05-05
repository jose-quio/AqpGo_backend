package com.integrador.Turismo.Service;

import com.integrador.Turismo.DTO.AcompananteDto;
import com.integrador.Turismo.DTO.ReservaRequest;
import com.integrador.Turismo.Model.*;
import com.integrador.Turismo.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaService {
    private final ReservaRepository reservaRepository;
    private final PaqueteRepository paqueteRepository;
    private final UsuarioRepository usuarioRepository;

    // Cliente — crear reserva
    @Transactional
    public Reserva crear(ReservaRequest req, String usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Paquete paquete = paqueteRepository.findById(req.paqueteId())
                .orElseThrow(() -> new RuntimeException("Paquete no encontrado"));

        if (!paquete.getActivo()) {
            throw new IllegalStateException("El paquete no está disponible");
        }

        // Precio total = precio base * número de personas
        Reserva reserva = Reserva.builder()
                .usuario(usuario)
                .paquete(paquete)
                .fechaSalida(req.fechaSalida())
                .numPersonas(req.numPersonas())
                .precioTotal(paquete.getPrecioBase()
                        .multiply(java.math.BigDecimal.valueOf(req.numPersonas())))
                .estado(Reserva.Estado.PENDIENTE_PAGO)
                .build();

        // Agregar acompañantes si los hay
        if (req.acompanantes() != null) {
            for (AcompananteDto dto : req.acompanantes()) {
                Acompanante a = Acompanante.builder()
                        .reserva(reserva)
                        .nombreCompleto(dto.nombreCompleto())
                        .dniPasaporte(dto.dniPasaporte())
                        .pais(dto.pais())
                        .fechaNacimiento(dto.fechaNacimiento())
                        .genero(dto.genero())
                        .datosAdicionales(dto.datosAdicionales())
                        .build();
                reserva.getAcompanantes().add(a);
            }
        }

        return reservaRepository.save(reserva);
    }

    // Cliente — mis reservas
    public List<Reserva> misReservas(String usuarioId) {
        return reservaRepository.findByUsuarioId(usuarioId);
    }

    // Cliente/Admin — detalle de una reserva
    public Reserva obtenerPorId(String id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
    }

    // Admin — todas las reservas
    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    // Admin — cambiar estado manualmente
    public Reserva cambiarEstado(String id, Reserva.Estado nuevoEstado) {
        Reserva reserva = obtenerPorId(id);
        reserva.setEstado(nuevoEstado);
        return reservaRepository.save(reserva);
    }

    // Cliente — cancelar su propia reserva
    public Reserva cancelar(String id, String usuarioId) {
        Reserva reserva = obtenerPorId(id);
        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para cancelar esta reserva");
        }
        if (reserva.getEstado() == Reserva.Estado.COMPLETADA) {
            throw new IllegalStateException("No se puede cancelar una reserva completada");
        }
        reserva.setEstado(Reserva.Estado.CANCELADA);
        return reservaRepository.save(reserva);
    }
}
