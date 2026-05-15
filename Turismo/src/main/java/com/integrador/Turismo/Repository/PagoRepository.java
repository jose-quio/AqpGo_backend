package com.integrador.Turismo.Repository;

import com.integrador.Turismo.Model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PagoRepository extends JpaRepository<Pago,String> {
    List<Pago> findByReservaId(String reservaId);
}
