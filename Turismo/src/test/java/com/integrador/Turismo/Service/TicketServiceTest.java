package com.integrador.Turismo.Service;

import com.integrador.Turismo.DTO.CrearTicketRequest;
import com.integrador.Turismo.DTO.ResponderTicketRequest;
import com.integrador.Turismo.DTO.TicketDetalleDto;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService — Pruebas unitarias")
public class TicketServiceTest {

    @Mock TicketRepository        ticketRepository;
    @Mock TicketMensajeRepository mensajeRepository;
    @Mock UsuarioRepository       usuarioRepository;
    @Mock ReservaRepository       reservaRepository;

    @InjectMocks TicketService ticketService;

    private Usuario clienteMock;
    private Usuario adminMock;
    private Ticket  ticketMock;

    @BeforeEach
    void setUp() {
        clienteMock = Usuario.builder()
                .id("cliente-uuid")
                .nombreCompleto("Juan Pérez")
                .email("juan@test.com")
                .rol(Usuario.Rol.CLIENTE)
                .build();

        adminMock = Usuario.builder()
                .id("admin-uuid")
                .nombreCompleto("Admin AQP")
                .email("admin@aqpgo.com")
                .rol(Usuario.Rol.ADMIN)
                .build();

        ticketMock = Ticket.builder()
                .id("ticket-uuid")
                .usuario(clienteMock)
                .asunto("Quiero cambiar la fecha")
                .tipo(Ticket.Tipo.SERVICIO)
                .estado(Ticket.Estado.ABIERTO)
                .prioridad(Ticket.Prioridad.MEDIA)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── CREAR TICKET ──────────────────────────────────────────

    @Test
    @DisplayName("Crear ticket — se crea con estado ABIERTO y primer mensaje")
    void crear_ticketValido_estadoAbiertoConMensaje() {
        CrearTicketRequest req = new CrearTicketRequest(
                Ticket.Tipo.SERVICIO,
                "Quiero cambiar la fecha de mi reserva",
                "Necesito cambiar al 20 de agosto",
                null
        );

        when(usuarioRepository.findById("cliente-uuid")).thenReturn(Optional.of(clienteMock));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            assertThat(t.getEstado()).isEqualTo(Ticket.Estado.ABIERTO);
            assertThat(t.getMensajes()).hasSize(1);
            assertThat(t.getMensajes().get(0).getMensaje())
                    .isEqualTo("Necesito cambiar al 20 de agosto");
            assertThat(t.getMensajes().get(0).isEsAdmin()).isFalse();
            return t;
        });

        ticketService.crear(req, "cliente-uuid");
        verify(ticketRepository).save(any());
    }

    @Test
    @DisplayName("Crear ticket con reserva — verifica que pertenece al usuario")
    void crear_conReservaAjena_lanzaExcepcion() {
        Usuario otroUsuario = Usuario.builder().id("otro-uuid").build();
        Paquete paquete = Paquete.builder()
                .id("paquete-uuid").nombre("Tour").precioBase(BigDecimal.TEN)
                .duracionDias(1).activo(true).createdAt(LocalDateTime.now()).build();

        Reserva reservaAjena = Reserva.builder()
                .id("reserva-uuid")
                .usuario(otroUsuario) // pertenece a otro
                .paquete(paquete)
                .fechaSalida(LocalDate.now().plusDays(10))
                .numPersonas(1)
                .precioTotal(BigDecimal.TEN)
                .estado(Reserva.Estado.CONFIRMADA)
                .createdAt(LocalDateTime.now())
                .build();

        CrearTicketRequest req = new CrearTicketRequest(
                Ticket.Tipo.SERVICIO, "Asunto", "Mensaje", "reserva-uuid"
        );

        when(usuarioRepository.findById("cliente-uuid")).thenReturn(Optional.of(clienteMock));
        when(reservaRepository.findById("reserva-uuid")).thenReturn(Optional.of(reservaAjena));

        assertThatThrownBy(() -> ticketService.crear(req, "cliente-uuid"))
                .isInstanceOf(SecurityException.class);

        verify(ticketRepository, never()).save(any());
    }

    // ── RESPONDER TICKET (CLIENTE) ────────────────────────────

    @Test
    @DisplayName("Cliente responde ticket — mensaje se agrega como no admin")
    void responderCliente_agregaMensajeNoAdmin() {
        ResponderTicketRequest req = new ResponderTicketRequest("Gracias por la respuesta");

        when(ticketRepository.findByIdWithMensajes("ticket-uuid"))
                .thenReturn(Optional.of(ticketMock));
        when(usuarioRepository.findById("cliente-uuid")).thenReturn(Optional.of(clienteMock));
        when(ticketRepository.save(any())).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            // El último mensaje no debe ser de admin
            TicketMensaje ultimo = t.getMensajes().get(t.getMensajes().size() - 1);
            assertThat(ultimo.isEsAdmin()).isFalse();
            assertThat(ultimo.getMensaje()).isEqualTo("Gracias por la respuesta");
            return t;
        });

        ticketService.responderCliente("ticket-uuid", "cliente-uuid", req);
        verify(ticketRepository).save(any());
    }

    @Test
    @DisplayName("Cliente no puede responder ticket ajeno")
    void responderCliente_ticketAjeno_lanzaExcepcion() {
        ResponderTicketRequest req = new ResponderTicketRequest("Mensaje");

        when(ticketRepository.findByIdWithMensajes("ticket-uuid"))
                .thenReturn(Optional.of(ticketMock));
        // El ticket pertenece a clienteMock pero intenta responder "otro-uuid"

        assertThatThrownBy(() ->
                ticketService.responderCliente("ticket-uuid", "otro-uuid", req))
                .isInstanceOf(SecurityException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cliente no puede responder ticket cerrado")
    void responderCliente_ticketCerrado_lanzaExcepcion() {
        ticketMock.setEstado(Ticket.Estado.CERRADO);
        ResponderTicketRequest req = new ResponderTicketRequest("Quiero reabrir");

        when(ticketRepository.findByIdWithMensajes("ticket-uuid"))
                .thenReturn(Optional.of(ticketMock));

        assertThatThrownBy(() ->
                ticketService.responderCliente("ticket-uuid", "cliente-uuid", req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cerrado");
    }

    @Test
    @DisplayName("Cliente responde ticket RESUELTO — vuelve a EN_PROCESO")
    void responderCliente_ticketResuelto_vuelveAEnProceso() {
        ticketMock.setEstado(Ticket.Estado.RESUELTO);
        ResponderTicketRequest req = new ResponderTicketRequest("Sigue sin funcionar");

        when(ticketRepository.findByIdWithMensajes("ticket-uuid"))
                .thenReturn(Optional.of(ticketMock));
        when(usuarioRepository.findById("cliente-uuid")).thenReturn(Optional.of(clienteMock));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ticketService.responderCliente("ticket-uuid", "cliente-uuid", req);

        assertThat(ticketMock.getEstado()).isEqualTo(Ticket.Estado.EN_PROCESO);
    }

    // ── RESPONDER TICKET (ADMIN) ──────────────────────────────

    @Test
    @DisplayName("Admin responde — mensaje marcado como esAdmin=true")
    void responderAdmin_mensajeMarcadoComoAdmin() {
        ResponderTicketRequest req = new ResponderTicketRequest("Le ayudamos con su solicitud");

        when(ticketRepository.findByIdWithMensajes("ticket-uuid"))
                .thenReturn(Optional.of(ticketMock));
        when(usuarioRepository.findById("admin-uuid")).thenReturn(Optional.of(adminMock));
        when(ticketRepository.save(any())).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            TicketMensaje ultimo = t.getMensajes().get(t.getMensajes().size() - 1);
            assertThat(ultimo.isEsAdmin()).isTrue();
            return t;
        });

        ticketService.responderAdmin("ticket-uuid", "admin-uuid", req);
        verify(ticketRepository).save(any());
    }

    @Test
    @DisplayName("Admin responde ticket ABIERTO — cambia a EN_PROCESO automáticamente")
    void responderAdmin_ticketAbierto_cambiaAEnProceso() {
        assertThat(ticketMock.getEstado()).isEqualTo(Ticket.Estado.ABIERTO);
        ResponderTicketRequest req = new ResponderTicketRequest("Revisando su caso");

        when(ticketRepository.findByIdWithMensajes("ticket-uuid"))
                .thenReturn(Optional.of(ticketMock));
        when(usuarioRepository.findById("admin-uuid")).thenReturn(Optional.of(adminMock));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ticketService.responderAdmin("ticket-uuid", "admin-uuid", req);

        assertThat(ticketMock.getEstado()).isEqualTo(Ticket.Estado.EN_PROCESO);
    }

    @Test
    @DisplayName("Admin responde — se asigna como responsable si no había admin")
    void responderAdmin_seAsignaComoResponsable() {
        assertThat(ticketMock.getAdminAsignado()).isNull();
        ResponderTicketRequest req = new ResponderTicketRequest("En proceso");

        when(ticketRepository.findByIdWithMensajes("ticket-uuid"))
                .thenReturn(Optional.of(ticketMock));
        when(usuarioRepository.findById("admin-uuid")).thenReturn(Optional.of(adminMock));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ticketService.responderAdmin("ticket-uuid", "admin-uuid", req);

        assertThat(ticketMock.getAdminAsignado()).isEqualTo(adminMock);
    }

    // ── CAMBIAR ESTADO ────────────────────────────────────────

    @Test
    @DisplayName("Cambiar estado — actualiza correctamente")
    void cambiarEstado_actualizaEstado() {
        when(ticketRepository.findByIdWithMensajes("ticket-uuid"))
                .thenReturn(Optional.of(ticketMock));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ticketService.cambiarEstado("ticket-uuid", Ticket.Estado.RESUELTO);

        assertThat(ticketMock.getEstado()).isEqualTo(Ticket.Estado.RESUELTO);
        verify(ticketRepository).save(ticketMock);
    }

    // ── DETALLE ───────────────────────────────────────────────

    @Test
    @DisplayName("Obtener detalle — cliente solo puede ver su ticket")
    void obtenerDetalle_clienteSoloVeSuTicket() {
        when(ticketRepository.findByIdWithMensajes("ticket-uuid"))
                .thenReturn(Optional.of(ticketMock));

        // El ticket pertenece a clienteMock — debe funcionar
        TicketDetalleDto detalle = ticketService.obtenerDetalle(
                "ticket-uuid", "cliente-uuid", false
        );
        assertThat(detalle).isNotNull();
    }

    @Test
    @DisplayName("Obtener detalle — cliente no puede ver ticket ajeno")
    void obtenerDetalle_clienteNoVeTicketAjeno() {
        when(ticketRepository.findByIdWithMensajes("ticket-uuid"))
                .thenReturn(Optional.of(ticketMock));

        assertThatThrownBy(() ->
                ticketService.obtenerDetalle("ticket-uuid", "otro-uuid", false))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("Obtener detalle — admin puede ver cualquier ticket")
    void obtenerDetalle_adminVeCualquierTicket() {
        when(ticketRepository.findByIdWithMensajes("ticket-uuid"))
                .thenReturn(Optional.of(ticketMock));

        // esAdmin = true, no importa el usuarioId
        TicketDetalleDto detalle = ticketService.obtenerDetalle(
                "ticket-uuid", "admin-uuid", true
        );
        assertThat(detalle).isNotNull();
    }
}
