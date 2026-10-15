package com.bolivar.api_polizas.controller;

import com.bolivar.api_polizas.model.entity.Riesgo;
import com.bolivar.api_polizas.service.RiesgoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controlador para operaciones sobre riesgos asegurables
@RestController
@RequestMapping("/riesgos")
@RequiredArgsConstructor
public class RiesgoController {

    private final RiesgoService riesgoService;

    // Cancela un riesgo especifico por su identificador
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Riesgo> cancelarRiesgo(@PathVariable("id") Long id) {
        Riesgo cancelado = riesgoService.cancelarRiesgo(id);
        return ResponseEntity.ok(cancelado);
    }
}
