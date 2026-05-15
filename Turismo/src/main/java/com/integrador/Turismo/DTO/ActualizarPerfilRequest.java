package com.integrador.Turismo.DTO;

public record ActualizarPerfilRequest(
        String telefono,
        String dniPasaporte,
        String pais,
        String genero
) {
}
