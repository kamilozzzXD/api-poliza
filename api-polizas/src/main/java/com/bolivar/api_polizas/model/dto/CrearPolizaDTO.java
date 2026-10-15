package com.bolivar.api_polizas.model.dto;

import com.bolivar.api_polizas.model.enums.TipoPoliza;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// DTO para recepcion de creacion de poliza
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearPolizaDTO {

    @NotNull(message = "El tipo de poliza es obligatorio")
    private TipoPoliza tipo;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    @NotNull(message = "El canon mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El canon mensual debe ser mayor a cero")
    private BigDecimal canonMensual;

    @Valid
    @Builder.Default
    private List<CrearRiesgoDTO> riesgos = new ArrayList<>();
}
