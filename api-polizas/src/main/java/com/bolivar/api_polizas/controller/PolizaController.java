package com.bolivar.api_polizas.controller;

import com.bolivar.api_polizas.model.dto.CrearPolizaDTO;
import com.bolivar.api_polizas.model.dto.CrearRiesgoDTO;
import com.bolivar.api_polizas.model.dto.RenovarPolizaDTO;
import com.bolivar.api_polizas.model.entity.Poliza;
import com.bolivar.api_polizas.model.entity.Riesgo;
import com.bolivar.api_polizas.model.enums.EstadoPoliza;
import com.bolivar.api_polizas.model.enums.TipoPoliza;
import com.bolivar.api_polizas.service.PolizaService;
import com.bolivar.api_polizas.service.RiesgoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

// Controlador para administracion de polizas y riesgos
@RestController
@RequestMapping("/polizas")
@RequiredArgsConstructor
public class PolizaController {

    private final PolizaService polizaService;
    private final RiesgoService riesgoService;

    // Obtiene polizas con filtros opcionales
    @GetMapping
    public ResponseEntity<List<Poliza>> listarPolizas(
            @RequestParam(name = "tipo", required = false) TipoPoliza tipo,
            @RequestParam(name = "estado", required = false) EstadoPoliza estado) {
        List<Poliza> resultado = polizaService.listarPolizas(tipo, estado);
        return ResponseEntity.ok(resultado);
    }

    // Obtiene una poliza por su id
    @GetMapping("/{id}")
    public ResponseEntity<Poliza> obtenerPorId(@PathVariable("id") Long id) {
        Poliza poliza = polizaService.obtenerPorId(id);
        return ResponseEntity.ok(poliza);
    }

    // Crea una nueva poliza en el sistema
    @PostMapping
    public ResponseEntity<Poliza> crearPoliza(@Valid @RequestBody CrearPolizaDTO dto) {
        Poliza creada = polizaService.crearPoliza(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // Lista todos los riesgos de una poliza
    @GetMapping("/{id}/riesgos")
    public ResponseEntity<List<Riesgo>> listarRiesgos(@PathVariable("id") Long id) {
        List<Riesgo> riesgos = riesgoService.listarPorPoliza(id);
        return ResponseEntity.ok(riesgos);
    }

    // Renueva una poliza aplicando incremento de IPC
    @PostMapping("/{id}/renovar")
    public ResponseEntity<Poliza> renovarPoliza(
            @PathVariable("id") Long id,
            @RequestBody(required = false) RenovarPolizaDTO body,
            @RequestParam(name = "ipc", required = false) BigDecimal queryIpc) {
        BigDecimal ipc = (body != null && body.getIpc() != null) ? body.getIpc() : queryIpc;
        Poliza renovada = polizaService.renovarPoliza(id, ipc);
        return ResponseEntity.ok(renovada);
    }

    // Cancela una poliza y sus riesgos asociados
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Poliza> cancelarPoliza(@PathVariable("id") Long id) {
        Poliza cancelada = polizaService.cancelarPoliza(id);
        return ResponseEntity.ok(cancelada);
    }

    // Agrega un riesgo a una poliza colectiva
    @PostMapping("/{id}/riesgos")
    public ResponseEntity<Riesgo> agregarRiesgo(
            @PathVariable("id") Long id,
            @Valid @RequestBody CrearRiesgoDTO dto) {
        Riesgo nuevoRiesgo = riesgoService.agregarRiesgo(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoRiesgo);
    }
}
