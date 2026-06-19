package com.integrador.Turismo.Security;

import com.integrador.Turismo.Model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService — Pruebas unitarias")
public class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            "clave_secreta_de_prueba_que_tiene_mas_de_32_caracteres_para_hs256";
    private static final long EXPIRATION = 86400000L; // 24h

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);
    }

    private Usuario usuarioMock() {
        return Usuario.builder()
                .id("uuid-test")
                .nombreCompleto("Juan Pérez")
                .email("juan@test.com")
                .password("hashed")
                .rol(Usuario.Rol.CLIENTE)
                .build();
    }

    @Test
    @DisplayName("generateToken — genera token no nulo")
    void generateToken_devuelveTokenNoNulo() {
        String token = jwtService.generateToken(usuarioMock());
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("extractEmail — extrae el email correcto del token")
    void extractEmail_extraeEmailCorrecto() {
        Usuario u = usuarioMock();
        String token = jwtService.generateToken(u);
        String email = jwtService.extractEmail(token);
        assertThat(email).isEqualTo("juan@test.com");
    }

    @Test
    @DisplayName("isValid — token válido del mismo usuario devuelve true")
    void isValid_tokenValido_devuelveTrue() {
        Usuario u = usuarioMock();
        String token = jwtService.generateToken(u);
        assertThat(jwtService.isValid(token, u)).isTrue();
    }

    @Test
    @DisplayName("isValid — token de otro usuario devuelve false")
    void isValid_tokenDeOtroUsuario_devuelveFalse() {
        Usuario u1 = usuarioMock();
        Usuario u2 = Usuario.builder()
                .id("otro-uuid").email("otro@test.com")
                .password("hashed").rol(Usuario.Rol.CLIENTE).build();

        String tokenU1 = jwtService.generateToken(u1);

        // El token de u1 no es válido para u2
        assertThat(jwtService.isValid(tokenU1, u2)).isFalse();
    }

    @Test
    @DisplayName("isValid — token expirado devuelve false")
    void isValid_tokenExpirado_devuelveFalse() {
        // Configura expiración de 1ms para que expire inmediatamente
        ReflectionTestUtils.setField(jwtService, "expiration", 1L);
        Usuario u = usuarioMock();
        String token = jwtService.generateToken(u);

        // Espera a que expire
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(jwtService.isValid(token, u)).isFalse();
    }

    @Test
    @DisplayName("isValid — token manipulado devuelve false")
    void isValid_tokenManipulado_devuelveFalse() {
        Usuario u = usuarioMock();
        String token = jwtService.generateToken(u);
        String tokenManipulado = token + "manipulado";

        assertThat(jwtService.isValid(tokenManipulado, u)).isFalse();
    }
}
