package edu.udla.integracion.progreso2.controller;

import edu.udla.integracion.progreso2.model.CitaRequest;
import edu.udla.integracion.progreso2.service.CitaValidationService;
import jakarta.validation.Valid;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador REST para el registro de citas médicas (RF1).
 *
 * Flujo:
 *  1. @Valid realiza Bean Validation — si falla → GlobalExceptionHandler devuelve 400
 *  2. CitaValidationService aplica reglas de negocio adicionales
 *  3. ProducerTemplate envía la cita a la ruta Camel direct:procesarCita
 *  4. Responde 202 Accepted con idCita y estado
 */
@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private static final Logger log = LoggerFactory.getLogger(CitaController.class);

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private CitaValidationService citaValidationService;

    @PostMapping
    public ResponseEntity<?> registrarCita(@Valid @RequestBody CitaRequest request) {

        log.info("[CitaController] Solicitud recibida: idCita={}", request.getIdCita());

        // Validación de negocio (complementa Bean Validation)
        citaValidationService.validar(request);

        // Enviar al flujo de integración Camel
        producerTemplate.sendBody("direct:procesarCita", request);

        log.info("[CitaController] Cita {} aceptada y enviada al flujo de integración",
            request.getIdCita());

        return ResponseEntity.accepted().body(Map.of(
            "idCita",  request.getIdCita(),
            "estado",  "ACEPTADA",
            "mensaje", "Solicitud de cita registrada y procesada exitosamente"
        ));
    }
}
