package com.integrador.Turismo.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Cada 30s hace un "SELECT 1" contra Supabase.
 * - Si falla UMBRAL_FALLOS veces seguidas -> conmuta a SECONDARY (Docker local).
 * - Si vuelve a responder -> conmuta de nuevo a PRIMARY.
 *
 * IMPORTANTE (documentar en la demo): la replica local puede tener hasta
 * 15 min de atraso respecto a Supabase (por el sync_15min.bat), y los
 * cambios hechos mientras se esta en modo SECONDARY no se escriben de
 * vuelta a Supabase automaticamente. Es un failover de solo lectura de
 * emergencia / demo, no una replicacion bidireccional real.
 */
@Component
public class DbHealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(DbHealthCheckService.class);
    private static final int UMBRAL_FALLOS = 2;

    private final DataSource primaryDataSource;
    private int fallosConsecutivos = 0;

    public DbHealthCheckService(@Qualifier("primaryDataSource") DataSource primaryDataSource) {
        this.primaryDataSource = primaryDataSource;
    }

    @Scheduled(fixedDelay = 30000)
    public void checkPrimaryHealth() {
        boolean primaryOk = ping(primaryDataSource);

        if (primaryOk) {
            if (DataSourceContextHolder.get() == DataSourceContextHolder.DataSourceType.SECONDARY) {
                log.warn("Supabase (PRIMARY) volvio a responder. Conmutando de vuelta a PRIMARY.");
            }
            fallosConsecutivos = 0;
            DataSourceContextHolder.set(DataSourceContextHolder.DataSourceType.PRIMARY);
        } else {
            fallosConsecutivos++;
            log.warn("Fallo de conexion a Supabase (PRIMARY). Intento {}/{}", fallosConsecutivos, UMBRAL_FALLOS);

            if (fallosConsecutivos >= UMBRAL_FALLOS
                    && DataSourceContextHolder.get() != DataSourceContextHolder.DataSourceType.SECONDARY) {
                log.error("Supabase no responde tras {} intentos. Conmutando a SECONDARY (replica local Docker).", UMBRAL_FALLOS);
                DataSourceContextHolder.set(DataSourceContextHolder.DataSourceType.SECONDARY);
            }
        }
    }

    private boolean ping(DataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(3);
            stmt.execute("SELECT 1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
