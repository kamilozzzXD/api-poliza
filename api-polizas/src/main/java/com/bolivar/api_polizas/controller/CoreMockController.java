package com.bolivar.api_polizas.controller;

import com.bolivar.api_polizas.model.dto.CoreEventoRequest;
import com.bolivar.api_polizas.service.CoreMockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Controlador mock para eventos del CORE Weblogic
@RestController
@RequestMapping("/core-mock")
@RequiredArgsConstructor
public class CoreMockController {

    private final CoreMockService coreMockService;

    // Recibe y registra eventos del CORE legado
    @PostMapping("/evento")
    public ResponseEntity<Map<String, Object>> recibirEvento(@Valid @RequestBody CoreEventoRequest request) {
        coreMockService.notificarCore(request.getEvento(), request.getPolizaId());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Evento procesado y notificado al CORE Weblogic exitosamente",
                "evento", request.getEvento(),
                "polizaId", request.getPolizaId()
        ));
    }
}
