package com.bolivar.api_polizas.service;

import com.bolivar.api_polizas.exception.BusinessRuleException;
import com.bolivar.api_polizas.exception.ResourceNotFoundException;
import com.bolivar.api_polizas.model.dto.CrearRiesgoDTO;
import com.bolivar.api_polizas.model.entity.Poliza;
import com.bolivar.api_polizas.model.entity.Riesgo;
import com.bolivar.api_polizas.model.enums.EstadoPoliza;
import com.bolivar.api_polizas.model.enums.EstadoRiesgo;
import com.bolivar.api_polizas.model.enums.TipoPoliza;
import com.bolivar.api_polizas.repository.PolizaRepository;
import com.bolivar.api_polizas.repository.RiesgoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Servicio con logica de negocio para riesgos
@Service
@RequiredArgsConstructor
public class RiesgoService {

    private final RiesgoRepository riesgoRepository;
    private final PolizaRepository polizaRepository;
    private final CoreMockService coreMockService;

    // Lista los riesgos de una poliza existente
    @Transactional(readOnly = true)
    public List<Riesgo> listarPorPoliza(Long polizaId) {
        if (!polizaRepository.existsById(polizaId)) {
            throw new ResourceNotFoundException("Poliza no encontrada con ID: " + polizaId);
        }
        return riesgoRepository.findByPolizaId(polizaId);
    }

    // Agrega un nuevo riesgo validando tipo COLECTIVA
    @Transactional
    public Riesgo agregarRiesgo(Long polizaId, CrearRiesgoDTO dto) {
        Poliza poliza = polizaRepository.findById(polizaId)
                .orElseThrow(() -> new ResourceNotFoundException("Poliza no encontrada con ID: " + polizaId));

        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new BusinessRuleException("No se pueden agregar riesgos a una poliza en estado CANCELADA");
        }

        if (poliza.getTipo() == TipoPoliza.INDIVIDUAL) {
            throw new BusinessRuleException("No se pueden agregar riesgos adicionales a una poliza INDIVIDUAL");
        }

        Riesgo riesgo = Riesgo.builder()
                .identificadorRiesgo(dto.getIdentificadorRiesgo())
                .descripcion(dto.getDescripcion())
                .estado(EstadoRiesgo.ACTIVO)
                .poliza(poliza)
                .build();

        poliza.addRiesgo(riesgo);
        Riesgo guardado = riesgoRepository.save(riesgo);

        coreMockService.notificarCore("AGREGAR_RIESGO", poliza.getId());
        return guardado;
    }

    // Cancela un riesgo especifico por su id
    @Transactional
    public Riesgo cancelarRiesgo(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new ResourceNotFoundException("Riesgo no encontrado con ID: " + riesgoId));

        riesgo.setEstado(EstadoRiesgo.CANCELADO);
        Riesgo actualizado = riesgoRepository.save(riesgo);

        Long polizaId = (riesgo.getPoliza() != null) ? riesgo.getPoliza().getId() : null;
        coreMockService.notificarCore("CANCELACION_RIESGO", polizaId);
        return actualizado;
    }
}
