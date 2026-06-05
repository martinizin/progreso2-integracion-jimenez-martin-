package edu.udla.integracion.progreso2.controller;

import edu.udla.integracion.progreso2.service.CitaValidationService;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de capa web para CitaController (RF1).
 *
 * Verifica: 202 para payload válido, 400 para inválido.
 * ProducerTemplate y CitaValidationService son mocks (prueba de capa web aislada).
 * RabbitMQ y Camel se excluyen del contexto de prueba.
 */
@WebMvcTest(controllers = {CitaController.class, GlobalExceptionHandler.class},
    excludeAutoConfiguration = {RabbitAutoConfiguration.class, CamelAutoConfiguration.class})
class CitaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProducerTemplate producerTemplate;

    @MockBean
    private CitaValidationService citaValidationService;

    private static final String PAYLOAD_VALIDO = """
        {
            "idCita":      "CITA-1001",
            "paciente":    "Ana Torres",
            "correo":      "ana.torres@email.com",
            "especialidad":"Cardiología",
            "fechaCita":   "2026-06-15",
            "sede":        "Centro Norte",
            "valor":       45.50
        }
        """;

    private static final String PAYLOAD_VALOR_CERO = """
        {
            "idCita":      "CITA-1002",
            "paciente":    "Pedro Ruiz",
            "correo":      "pedro@email.com",
            "especialidad":"Dermatología",
            "fechaCita":   "2026-06-20",
            "sede":        "Centro Sur",
            "valor":       0
        }
        """;

    private static final String PAYLOAD_CORREO_VACIO = """
        {
            "idCita":      "CITA-1003",
            "paciente":    "Luis Gómez",
            "correo":      "",
            "especialidad":"Pediatría",
            "fechaCita":   "2026-06-25",
            "sede":        "Centro Este",
            "valor":       30.00
        }
        """;

    @Test
    @DisplayName("POST /api/citas con payload válido devuelve 202 y ACEPTADA")
    void testPostCitaValida() throws Exception {
        mockMvc.perform(post("/api/citas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYLOAD_VALIDO))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.estado").value("ACEPTADA"))
            .andExpect(jsonPath("$.idCita").value("CITA-1001"));
    }

    @Test
    @DisplayName("POST /api/citas con valor=0 devuelve 400")
    void testPostCitaValorCero() throws Exception {
        mockMvc.perform(post("/api/citas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYLOAD_VALOR_CERO))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/citas con correo vacío devuelve 400")
    void testPostCitaCorreoVacio() throws Exception {
        mockMvc.perform(post("/api/citas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYLOAD_CORREO_VACIO))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/citas sin body devuelve 400")
    void testPostSinBody() throws Exception {
        mockMvc.perform(post("/api/citas")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}
