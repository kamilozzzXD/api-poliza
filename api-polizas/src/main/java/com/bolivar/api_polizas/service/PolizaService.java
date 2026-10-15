package com.bolivar.api_polizas.service;

import com.bolivar.api_polizas.exception.BusinessRuleException;
import com.bolivar.api_polizas.exception.ResourceNotFoundException;
import com.bolivar.api_polizas.model.dto.CrearPolizaDTO;
import com.bolivar.api_polizas.model.dto.CrearRiesgoDTO;
import com.bolivar.api_polizas.model.entity.Poliza;
import com.bolivar.api_polizas.model.entity.Riesgo;
import com.bolivar.api_polizas.model.enums.EstadoPoliza;
import com.bolivar.api_polizas.model.enums.EstadoRiesgo;
import com.bolivar.api_polizas.model.enums.TipoPoliza;
import com.bolivar.api_polizas.repository.PolizaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Servicio con logica de negocio para polizas
@Service
@RequiredArgsConstructor
public class PolizaService {

    private final PolizaRepository polizaRepository;
    private final CoreMockService coreMockService;

    @Value("${app.ipc:9.28}")
    private BigDecimal defaultIpc;

    // Obtiene polizas aplicando filtros opcionales
    @Transactional(readOnly = true)
    public List<Poliza> listarPolizas(TipoPoliza tipo, EstadoPoliza estado) {
        if (tipo != null && estado != null) {
            return polizaRepository.findByTipoAndEstado(tipo, estado);
        } else if (tipo != null) {
            return polizaRepository.findByTipo(tipo);
        } else if (estado != null) {
            return polizaRepository.findByEstado(estado);
        }
        return polizaRepository.findAll();
    }

    // Busca una poliza por id
    @Transactional(readOnly = true)
    public Poliza obtenerPorId(Long id) {
        return polizaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Poliza no encontrada con ID: " + id));
    }

    // Renueva una poliza aplicando incremento de IPC
    @Transactional
    public Poliza renovarPoliza(Long id, BigDecimal customIpc) {
        Poliza poliza = obtenerPorId(id);

        if (poliza.getEstado() == EstadoPoliza.CANCELADA) {
            throw new BusinessRuleException("No se puede renovar una poliza en estado CANCELADA");
        }

        BigDecimal ipcAplicado = (customIpc != null) ? customIpc : defaultIpc;
        if (ipcAplicado.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("El valor de IPC no puede ser negativo");
        }

        BigDecimal factor = ipcAplicado.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal multiplicador = BigDecimal.ONE.add(factor);
        BigDecimal nuevoCanon = poliza.getCanonMensual().multiply(multiplicador).setScale(2, RoundingMode.HALF_UP);

        LocalDate inicioAnterior = poliza.getFechaInicio();
        LocalDate finAnterior = poliza.getFechaFin();
        Period periodo = Period.between(inicioAnterior, finAnterior);

        LocalDate nuevaFechaInicio;
        LocalDate nuevaFechaFin;

        if (ChronoUnit.MONTHS.between(inicioAnterior, finAnterior.plusDays(1)) > ChronoUnit.MONTHS.between(inicioAnterior, finAnterior)) {
            nuevaFechaInicio = finAnterior.plusDays(1);
            long meses = ChronoUnit.MONTHS.between(inicioAnterior, finAnterior.plusDays(1));
            nuevaFechaFin = nuevaFechaInicio.plusMonths(meses).minusDays(1);
        } else {
            nuevaFechaInicio = finAnterior;
            if (periodo.getYears() > 0 || periodo.getMonths() > 0 || periodo.getDays() > 0) {
                nuevaFechaFin = nuevaFechaInicio.plus(periodo);
            } else {
                long meses = calcularMesesVigencia(inicioAnterior, finAnterior);
                nuevaFechaFin = nuevaFechaInicio.plusMonths(meses);
            }
        }

        poliza.setFechaInicio(nuevaFechaInicio);
        poliza.setFechaFin(nuevaFechaFin);
        poliza.setCanonMensual(nuevoCanon);
        poliza.setPrimaTotal(calcularPrimaTotal(nuevoCanon, nuevaFechaInicio, nuevaFechaFin));
        poliza.setEstado(EstadoPoliza.RENOVADA);

        Poliza actualizada = polizaRepository.save(poliza);
        coreMockService.notificarCore("RENOVACION", actualizada.getId());
        return actualizada;
    }

    // Cancela una poliza y sus riesgos asociados
    @Transactional
    public Poliza cancelarPoliza(Long id) {
        Poliza poliza = obtenerPorId(id);
        poliza.setEstado(EstadoPoliza.CANCELADA);

        if (poliza.getRiesgos() != null) {
            for (Riesgo riesgo : poliza.getRiesgos()) {
                riesgo.setEstado(EstadoRiesgo.CANCELADO);
            }
        }

        Poliza cancelada = polizaRepository.save(poliza);
        coreMockService.notificarCore("CANCELACION_POLIZA", cancelada.getId());
        return cancelada;
    }

    // Crea una nueva poliza con validaciones
    @Transactional
    public Poliza crearPoliza(CrearPolizaDTO dto) {
        if (dto.getFechaFin().isBefore(dto.getFechaInicio())) {
            throw new BusinessRuleException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        if (dto.getTipo() == TipoPoliza.INDIVIDUAL && dto.getRiesgos() != null && dto.getRiesgos().size() > 1) {
            throw new BusinessRuleException("Una poliza de tipo INDIVIDUAL solo puede tener como maximo 1 riesgo");
        }

        Poliza poliza = Poliza.builder()
                .tipo(dto.getTipo())
                .estado(EstadoPoliza.ACTIVA)
                .fechaInicio(dto.getFechaInicio())
                .fechaFin(dto.getFechaFin())
                .canonMensual(dto.getCanonMensual().setScale(2, RoundingMode.HALF_UP))
                .primaTotal(calcularPrimaTotal(dto.getCanonMensual(), dto.getFechaInicio(), dto.getFechaFin()))
                .build();

        if (dto.getRiesgos() != null) {
            for (CrearRiesgoDTO rDto : dto.getRiesgos()) {
                Riesgo riesgo = Riesgo.builder()
                        .identificadorRiesgo(rDto.getIdentificadorRiesgo())
                        .descripcion(rDto.getDescripcion())
                        .estado(EstadoRiesgo.ACTIVO)
                        .build();
                poliza.addRiesgo(riesgo);
            }
        }

        Poliza guardada = polizaRepository.save(poliza);
        coreMockService.notificarCore("CREACION_POLIZA", guardada.getId());
        return guardada;
    }

    // Calcula meses de vigencia entre dos fechas
    public long calcularMesesVigencia(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) return 1;
        long meses = ChronoUnit.MONTHS.between(inicio, fin);
        if (meses == 0 || (ChronoUnit.MONTHS.between(inicio, fin.plusDays(1)) > meses)) {
            meses = ChronoUnit.MONTHS.between(inicio, fin.plusDays(1));
        }
        return meses > 0 ? meses : 1;
    }

    // Calcula la prima total de la poliza
    public BigDecimal calcularPrimaTotal(BigDecimal canonMensual, LocalDate inicio, LocalDate fin) {
        long meses = calcularMesesVigencia(inicio, fin);
        return canonMensual.multiply(BigDecimal.valueOf(meses)).setScale(2, RoundingMode.HALF_UP);
    }
}
