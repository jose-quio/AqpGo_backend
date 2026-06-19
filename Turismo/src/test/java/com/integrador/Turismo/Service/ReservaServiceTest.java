package com.integrador.Turismo.Service;

import com.integrador.Turismo.DTO.AcompananteDto;
import com.integrador.Turismo.DTO.ReservaRequest;
import com.integrador.Turismo.DTO.ReservaResponse;
import com.integrador.Turismo.Model.*;
import com.integrador.Turismo.Repository.*;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaService — Pruebas unitarias")
public class ReservaServiceTest {

    @Mock ReservaRepository reservaRepository;
    @Mock PaqueteRepository paqueteRepository;
    @Mock UsuarioRepository usuarioRepository;

    @InjectMocks ReservaService reservaService;

    private Usuario clienteMock;
    private Paquete paqueteMock;

    @BeforeEach
    void setUp() {
        clienteMock = Usuario.builder()
                .id("cliente-uuid")
                .nombreCompleto("Juan Pérez")
                .email("juan@test.com")
                .password("hashed")
                .rol(Usuario.Rol.CLIENTE)
                .build();

        paqueteMock = Paquete.builder()
                .id("paquete-uuid")
                .nombre("Arequipa + Colca")
                .precioBase(new BigDecimal("250.00"))
                .duracionDias(3)
                .duracionNoches(2)
                .activo(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── CREAR RESERVA ─────────────────────────────────────────

    @Test
    @DisplayName("Crear reserva exitosa — precio total = precioBase × personas")
    void crear_reservaExitosa_precioCalculadoCorrectamente() {
        ReservaRequest req = new ReservaRequest(
                "paquete-uuid",
                LocalDate.now().plusDays(30),
                2,
                List.of()
        );

        when(usuarioRepository.findById("cliente-uuid")).thenReturn(Optional.of(clienteMock));
        when(paqueteRepository.findById("paquete-uuid")).thenReturn(Optional.of(paqueteMock));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            // Verifica que el precio total se calculó correctamente
            assertThat(r.getPrecioTotal()).isEqualByComparingTo("500.00"); // 250 × 2
            assertThat(r.getEstado()).isEqualTo(Reserva.Estado.PENDIENTE_PAGO);
            return r;
        });

        ReservaResponse res = reservaService.crear(req, "cliente-uuid");

        assertThat(res).isNotNull();
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    @DisplayName("Crear reserva — paquete inactivo lanza excepción")
    void crear_paqueteInactivo_lanzaExcepcion() {
        paqueteMock.setActivo(false);

        ReservaRequest req = new ReservaRequest(
                "paquete-uuid", LocalDate.now().plusDays(10), 1, List.of()
        );

        when(usuarioRepository.findById("cliente-uuid")).thenReturn(Optional.of(clienteMock));
        when(paqueteRepository.findById("paquete-uuid")).thenReturn(Optional.of(paqueteMock));

        assertThatThrownBy(() -> reservaService.crear(req, "cliente-uuid"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está disponible");

        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear reserva — usuario no existe lanza excepción")
    void crear_usuarioNoExiste_lanzaExcepcion() {
        ReservaRequest req = new ReservaRequest(
                "paquete-uuid", LocalDate.now().plusDays(10), 1, List.of()
        );
        when(usuarioRepository.findById("noexiste")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.crear(req, "noexiste"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Crear reserva con acompañantes — se guardan correctamente")
    void crear_conAcompanantes_seGuardanCorrectamente() {
        List<AcompananteDto> acompanantes = List.of(
                new AcompananteDto("María García", "87654321", "Perú",
                        LocalDate.of(1995, 3, 20), "F", null)
        );

        ReservaRequest req = new ReservaRequest(
                "paquete-uuid", LocalDate.now().plusDays(15), 2, acompanantes
        );

        when(usuarioRepository.findById("cliente-uuid")).thenReturn(Optional.of(clienteMock));
        when(paqueteRepository.findById("paquete-uuid")).thenReturn(Optional.of(paqueteMock));
        when(reservaRepository.save(any())).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            assertThat(r.getAcompanantes()).hasSize(1);
            assertThat(r.getAcompanantes().get(0).getNombreCompleto()).isEqualTo("María García");
            return r;
        });

        reservaService.crear(req, "cliente-uuid");
        verify(reservaRepository).save(any());
    }

    // ── CANCELAR RESERVA ──────────────────────────────────────

    @Test
    @DisplayName("Cancelar reserva — cliente cancela su propia reserva")
    void cancelar_exitoso() {
        Reserva reserva = Reserva.builder()
                .id("reserva-uuid")
                .usuario(clienteMock)
                .paquete(paqueteMock)
                .estado(Reserva.Estado.PENDIENTE_PAGO)
                .numPersonas(1)
                .precioTotal(new BigDecimal("250"))
                .fechaSalida(LocalDate.now().plusDays(20))
                .createdAt(LocalDateTime.now())
                .build();

        when(reservaRepository.findById("reserva-uuid")).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenReturn(reserva);

        reservaService.cancelar("reserva-uuid", "cliente-uuid");

        assertThat(reserva.getEstado()).isEqualTo(Reserva.Estado.CANCELADA);
        verify(reservaRepository).save(reserva);
    }

    @Test
    @DisplayName("Cancelar reserva — cliente no puede cancelar reserva ajena")
    void cancelar_reservaAjena_lanzaExcepcion() {
        Usuario otroUsuario = Usuario.builder().id("otro-uuid").build();
        Reserva reserva = Reserva.builder()
                .id("reserva-uuid")
                .usuario(otroUsuario)  // pertenece a otro usuario
                .estado(Reserva.Estado.CONFIRMADA)
                .createdAt(LocalDateTime.now())
                .build();

        when(reservaRepository.findById("reserva-uuid")).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.cancelar("reserva-uuid", "cliente-uuid"))
                .isInstanceOf(SecurityException.class);

        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancelar reserva — no se puede cancelar una reserva completada")
    void cancelar_reservaCompletada_lanzaExcepcion() {
        Reserva reserva = Reserva.builder()
                .id("reserva-uuid")
                .usuario(clienteMock)
                .estado(Reserva.Estado.COMPLETADA)
                .createdAt(LocalDateTime.now())
                .build();

        when(reservaRepository.findById("reserva-uuid")).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.cancelar("reserva-uuid", "cliente-uuid"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completada");
    }
}
