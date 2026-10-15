package com.bolivar.api_polizas.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para recepcion de eventos en mock CORE
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoreEventoRequest {

    @NotBlank(message = "El nombre del evento es obligatorio")
    private String evento;

    @NotNull(message = "El id de la poliza es obligatorio")
    private Long polizaId;
}
