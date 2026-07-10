package com.integrador.Turismo.Config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * DataSource "fachada": cada vez que Hikari pide una conexion nueva,
 * Spring pregunta aqui cual usar. Segun lo que diga
 * DataSourceContextHolder, entrega conexiones de Supabase o del
 * Postgres local en Docker, sin que el resto del codigo se entere.
 */

public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.get();
    }
}
