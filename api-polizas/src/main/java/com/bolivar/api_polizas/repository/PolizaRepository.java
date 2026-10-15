package com.bolivar.api_polizas.repository;

import com.bolivar.api_polizas.model.entity.Poliza;
import com.bolivar.api_polizas.model.enums.EstadoPoliza;
import com.bolivar.api_polizas.model.enums.TipoPoliza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repositorio de acceso a datos para polizas
@Repository
public interface PolizaRepository extends JpaRepository<Poliza, Long> {

    List<Poliza> findByTipo(TipoPoliza tipo);

    List<Poliza> findByEstado(EstadoPoliza estado);

    List<Poliza> findByTipoAndEstado(TipoPoliza tipo, EstadoPoliza estado);
}
