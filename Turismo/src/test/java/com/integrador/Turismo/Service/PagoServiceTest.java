package com.integrador.Turismo.Service;

import com.integrador.Turismo.DTO.PagoRequest;
import com.integrador.Turismo.DTO.PagoResponse;
import com.integrador.Turismo.Model.*;
import com.integrador.Turismo.Repository.PagoRepository;
import com.integrador.Turismo.Repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoService — Pruebas unitarias")
public class PagoServiceTest {

    @Mock PagoRepository    pagoRepository;
    @Mock ReservaRepository reservaRepository;

    @InjectMocks PagoService pagoService;

    private Reserva reservaMock;

    @BeforeEach
    void setUp() {
        Usuario cliente = Usuario.builder()
                .id("cliente-uuid").email("juan@test.com")
                .nombreCompleto("Juan Pérez").rol(Usuario.Rol.CLIENTE).build();

        Paquete paquete = Paquete.builder()
                .id("paquete-uuid").nombre("Arequipa + Colca")
                .precioBase(new BigDecimal("250")).duracionDias(3)
                .activo(true).createdAt(LocalDateTime.now()).build();

        reservaMock = Reserva.builder()
                .id("reserva-uuid")
                .usuario(cliente)
                .paquete(paquete)
                .numPersonas(2)
                .precioTotal(new BigDecimal("500.00"))
                .estado(Reserva.Estado.PENDIENTE_PAGO)
                .fechaSalida(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Procesar pago — monto correcto resulta en VERIFICADO y reserva CONFIRMADA")
    void procesarPago_montoExacto_verificadoYReservaConfirmada() {
        PagoRequest req = new PagoRequest(
                "reserva-uuid",
                new BigDecimal("500.00"),
                Pago.Metodo.YAPE,
                "OP-123456"
        );

        when(reservaRepository.findById("reserva-uuid"))
                .thenReturn(Optional.of(reservaMock));
        when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PagoResponse res = pagoService.procesarPago(req);

        assertThat(res.estado()).isEqualTo("VERIFICADO");
        assertThat(reservaMock.getEstado()).isEqualTo(Reserva.Estado.CONFIRMADA);
        verify(reservaRepository).save(reservaMock);
    }

    @Test
    @DisplayName("Procesar pago — monto incorrecto resulta en RECHAZADO")
    void procesarPago_montoIncorrecto_rechazado() {
        PagoRequest req = new PagoRequest(
                "reserva-uuid",
                new BigDecimal("100.00"), // monto incorrecto — debería ser 500
                Pago.Metodo.TARJETA,
                "OP-999"
        );

        when(reservaRepository.findById("reserva-uuid"))
                .thenReturn(Optional.of(reservaMock));
        when(pagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PagoResponse res = pagoService.procesarPago(req);

        assertThat(res.estado()).isEqualTo("RECHAZADO");
        // La reserva NO debe confirmarse si el pago fue rechazado
        assertThat(reservaMock.getEstado()).isEqualTo(Reserva.Estado.PENDIENTE_PAGO);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Procesar pago — reserva ya confirmada lanza excepción")
    void procesarPago_reservaYaConfirmada_lanzaExcepcion() {
        reservaMock.setEstado(Reserva.Estado.CONFIRMADA);

        PagoRequest req = new PagoRequest(
                "reserva-uuid", new BigDecimal("500"),
                Pago.Metodo.YAPE, "OP-111"
        );

        when(reservaRepository.findById("reserva-uuid"))
                .thenReturn(Optional.of(reservaMock));

        assertThatThrownBy(() -> pagoService.procesarPago(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya está pagada");

        verify(pagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Procesar pago — guarda referencia de operación")
    void procesarPago_guardaReferencia() {
        PagoRequest req = new PagoRequest(
                "reserva-uuid", new BigDecimal("500.00"),
                Pago.Metodo.TRANSFERENCIA, "TRF-2024-001"
        );

        when(reservaRepository.findById("reserva-uuid"))
                .thenReturn(Optional.of(reservaMock));
        when(pagoRepository.save(any())).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            assertThat(p.getReferencia()).isEqualTo("TRF-2024-001");
            assertThat(p.getMetodo()).isEqualTo(Pago.Metodo.TRANSFERENCIA);
            return p;
        });
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pagoService.procesarPago(req);
        verify(pagoRepository).save(any());
    }
}
