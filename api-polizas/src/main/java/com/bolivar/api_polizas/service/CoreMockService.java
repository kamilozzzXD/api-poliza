package com.bolivar.api_polizas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Servicio de simulacion para integracion con CORE Weblogic
@Service
@Slf4j
public class CoreMockService {

    // Registra evento enviado al CORE legado Weblogic
    public void notificarCore(String evento, Long polizaId) {
        log.info(">>> [CORE-MOCK] Evento '{}' recibido para polizaId {}. Notificando a CORE en Weblogic.", evento, polizaId);
    }
}
