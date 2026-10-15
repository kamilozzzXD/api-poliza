package com.bolivar.api_polizas;

import com.bolivar.api_polizas.config.ApiKeyAuthFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de integracion para endpoints y reglas
@SpringBootTest
class PolizaIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ApiKeyAuthFilter apiKeyAuthFilter;

    private MockMvc mockMvc;

    // Configura MockMvc con el filtro de seguridad
    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(apiKeyAuthFilter)
                .build();
    }

    // Valida rechazo de peticion sin header de autenticacion
    @Test
    @DisplayName("Debe retornar 401 si falta el header de seguridad")
    void testSeguridadSinHeader() throws Exception {
        mockMvc.perform(get("/polizas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // Valida acceso permitido con x-api-key
    @Test
    @DisplayName("Debe permitir acceso con header x-api-key")
    void testSeguridadConXApiKey() throws Exception {
        mockMvc.perform(get("/polizas")
                        .header("x-api-key", "123456"))
                .andExpect(status().isOk());
    }

    // Valida acceso permitido con api-key
    @Test
    @DisplayName("Debe permitir acceso con header api-key")
    void testSeguridadConApiKey() throws Exception {
        mockMvc.perform(get("/polizas")
                        .header("api-key", "123456"))
                .andExpect(status().isOk());
    }

    // Valida listado de riesgos de una poliza existente
    @Test
    @DisplayName("Debe listar riesgos asociados a una poliza")
    void testListarRiesgosPorPoliza() throws Exception {
        mockMvc.perform(get("/polizas/1/riesgos")
                        .header("x-api-key", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].identificadorRiesgo").exists());
    }

    // Valida renovacion e incremento de IPC
    @Test
    @DisplayName("Debe renovar poliza e incrementar canon e IPC")
    void testRenovarPoliza() throws Exception {
        mockMvc.perform(post("/polizas/1/renovar")
                        .header("x-api-key", "123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ipc\": 10.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RENOVADA"));
    }

    // Valida interpretacion inequivoca de IPC como porcentaje
    @Test
    @DisplayName("Debe interpretar 1.0 como 1 por ciento")
    void testRenovarPolizaConIpcUnoPorCiento() throws Exception {
        mockMvc.perform(post("/polizas/1/renovar")
                        .header("x-api-key", "123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ipc\": 1.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RENOVADA"));
    }

    // Valida que poliza cancelada no puede renovarse
    @Test
    @DisplayName("No debe permitir renovar una poliza cancelada")
    void testNoRenovarPolizaCancelada() throws Exception {
        mockMvc.perform(post("/polizas/2/cancelar")
                        .header("x-api-key", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        mockMvc.perform(post("/polizas/2/renovar")
                        .header("x-api-key", "123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se puede renovar una poliza en estado CANCELADA"));
    }

    // Valida rechazo de riesgo adicional en poliza individual
    @Test
    @DisplayName("Debe rechazar agregar riesgo a poliza individual")
    void testRechazarRiesgoEnPolizaIndividual() throws Exception {
        String nuevoRiesgoJson = "{\"identificadorRiesgo\":\"Cra 15 # 80-20\",\"descripcion\":\"Local comercial\"}";

        mockMvc.perform(post("/polizas/1/riesgos")
                        .header("x-api-key", "123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nuevoRiesgoJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se pueden agregar riesgos adicionales a una poliza INDIVIDUAL"));
    }

    // Valida cancelacion de un riesgo especifico
    @Test
    @DisplayName("Debe cancelar un riesgo especifico por id")
    void testCancelarRiesgo() throws Exception {
        mockMvc.perform(post("/riesgos/1/cancelar")
                        .header("x-api-key", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    // Valida procesamiento del evento mock CORE
    @Test
    @DisplayName("Debe recibir y procesar evento mock CORE")
    void testCoreMockEvento() throws Exception {
        String eventoJson = "{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}";

        mockMvc.perform(post("/core-mock/evento")
                        .header("x-api-key", "123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Evento procesado y notificado al CORE Weblogic exitosamente"));
    }
}
