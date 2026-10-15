package com.bolivar.api_polizas.config;

import com.bolivar.api_polizas.model.entity.Poliza;
import com.bolivar.api_polizas.model.entity.Riesgo;
import com.bolivar.api_polizas.model.enums.EstadoPoliza;
import com.bolivar.api_polizas.model.enums.EstadoRiesgo;
import com.bolivar.api_polizas.model.enums.TipoPoliza;
import com.bolivar.api_polizas.repository.PolizaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

// Inicializador de datos de prueba al arrancar
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PolizaRepository polizaRepository;

    // Carga polizas y riesgos iniciales de prueba
    @Override
    public void run(String... args) {
        if (polizaRepository.count() > 0) {
            return;
        }

        Poliza polizaIndividual = Poliza.builder()
                .tipo(TipoPoliza.INDIVIDUAL)
                .estado(EstadoPoliza.ACTIVA)
                .fechaInicio(LocalDate.of(2026, 1, 1))
                .fechaFin(LocalDate.of(2027, 1, 1))
                .canonMensual(new BigDecimal("1500000.00"))
                .primaTotal(new BigDecimal("18000000.00"))
                .build();

        Riesgo riesgo1 = Riesgo.builder()
                .identificadorRiesgo("Calle 100 # 15-20 Apto 301, Bogota")
                .descripcion("Inmueble residencial arrendatario Pedro Perez")
                .estado(EstadoRiesgo.ACTIVO)
                .build();
        polizaIndividual.addRiesgo(riesgo1);

        Poliza polizaColectiva = Poliza.builder()
                .tipo(TipoPoliza.COLECTIVA)
                .estado(EstadoPoliza.ACTIVA)
                .fechaInicio(LocalDate.of(2026, 3, 1))
                .fechaFin(LocalDate.of(2027, 3, 1))
                .canonMensual(new BigDecimal("3200000.00"))
                .primaTotal(new BigDecimal("38400000.00"))
                .build();

        Riesgo riesgo2 = Riesgo.builder()
                .identificadorRiesgo("Carrera 7 # 72-10 Oficina 502, Bogota")
                .descripcion("Oficina comercial inmobiliaria ABC")
                .estado(EstadoRiesgo.ACTIVO)
                .build();

        Riesgo riesgo3 = Riesgo.builder()
                .identificadorRiesgo("Carrera 7 # 72-10 Oficina 503, Bogota")
                .descripcion("Oficina comercial consultores XYZ")
                .estado(EstadoRiesgo.ACTIVO)
                .build();

        polizaColectiva.addRiesgo(riesgo2);
        polizaColectiva.addRiesgo(riesgo3);

        polizaRepository.save(polizaIndividual);
        polizaRepository.save(polizaColectiva);

        log.info(">>> Datos de prueba inicializados exitosamente (Polizas 1 y 2 creadas)");
    }
}
