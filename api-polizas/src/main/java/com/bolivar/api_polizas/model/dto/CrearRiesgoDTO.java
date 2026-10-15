package com.bolivar.api_polizas.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para recepcion de nuevo riesgo
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearRiesgoDTO {

    @NotBlank(message = "El identificador del riesgo es obligatorio")
    private String identificadorRiesgo;

    private String descripcion;
}
