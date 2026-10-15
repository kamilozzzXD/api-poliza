package com.bolivar.api_polizas.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// DTO para recepcion de porcentaje IPC en renovacion
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenovarPolizaDTO {

    private BigDecimal ipc;
}
