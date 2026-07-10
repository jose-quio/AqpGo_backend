package com.integrador.Turismo.Config;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Guarda cual DataSource esta activo en este momento (PRIMARY = Supabase,
 * SECONDARY = replica local en Docker). Lo actualiza DbHealthCheckService
 * y lo lee RoutingDataSource en cada conexion nueva.
 */

public class DataSourceContextHolder {

    public enum DataSourceType { PRIMARY, SECONDARY }

    private static final AtomicReference<DataSourceType> CURRENT =
            new AtomicReference<>(DataSourceType.PRIMARY);

    public static void set(DataSourceType type) {
        CURRENT.set(type);
    }

    public static DataSourceType get() {
        return CURRENT.get();
    }
}
