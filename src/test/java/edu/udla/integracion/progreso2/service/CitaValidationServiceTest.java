package edu.udla.integracion.progreso2.service;

import edu.udla.integracion.progreso2.model.CitaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para CitaValidationService (RF1).
 *
 * Test unitario puro: CitaValidationService no tiene dependencias Spring,
 * se instancia directamente para no requerir contexto ni RabbitMQ.
 * Cubre: campos faltantes, valor <= 0, correo inválido → rechazo con motivo.
 */
class CitaValidationServiceTest {

    private CitaValidationService validationService;

    private CitaRequest citaValida;

    @BeforeEach
    void setUp() {
        validationService = new CitaValidationService();
        citaValida = new CitaRequest();
        citaValida.setIdCita("CITA-1001");
        citaValida.setPaciente("Ana Torres");
        citaValida.setCorreo("ana.torres@email.com");
        citaValida.setEspecialidad("Cardiología");
        citaValida.setFechaCita(LocalDate.of(2026, 6, 15));
        citaValida.setSede("Centro Norte");
        citaValida.setValor(new BigDecimal("45.50"));
    }

    @Test
    @DisplayName("Cita con todos los campos válidos no lanza excepción")
    void testCitaValida() {
        assertDoesNotThrow(() -> validationService.validar(citaValida));
    }

    @Test
    @DisplayName("idCita vacío lanza excepción con motivo")
    void testIdCitaVacio() {
        citaValida.setIdCita("");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validationService.validar(citaValida));
        assertTrue(ex.getMessage().contains("idCita"),
            "El mensaje debe mencionar 'idCita'");
    }

    @Test
    @DisplayName("paciente nulo lanza excepción con motivo")
    void testPacienteNulo() {
        citaValida.setPaciente(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validationService.validar(citaValida));
        assertTrue(ex.getMessage().contains("paciente"));
    }

    @Test
    @DisplayName("correo con formato inválido lanza excepción")
    void testCorreoInvalido() {
        citaValida.setCorreo("no-es-un-email");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validationService.validar(citaValida));
        assertTrue(ex.getMessage().contains("correo"));
    }

    @Test
    @DisplayName("correo vacío lanza excepción")
    void testCorreoVacio() {
        citaValida.setCorreo("  ");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validationService.validar(citaValida));
        assertTrue(ex.getMessage().contains("correo"));
    }

    @Test
    @DisplayName("valor cero lanza excepción")
    void testValorCero() {
        citaValida.setValor(BigDecimal.ZERO);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validationService.validar(citaValida));
        assertTrue(ex.getMessage().contains("valor"));
    }

    @Test
    @DisplayName("valor negativo lanza excepción")
    void testValorNegativo() {
        citaValida.setValor(new BigDecimal("-10.00"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validationService.validar(citaValida));
        assertTrue(ex.getMessage().contains("valor"));
    }

    @Test
    @DisplayName("especialidad vacía lanza excepción")
    void testEspecialidadVacia() {
        citaValida.setEspecialidad("");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validationService.validar(citaValida));
        assertTrue(ex.getMessage().contains("especialidad"));
    }

    @Test
    @DisplayName("sede nula lanza excepción")
    void testSedeNula() {
        citaValida.setSede(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validationService.validar(citaValida));
        assertTrue(ex.getMessage().contains("sede"));
    }

    @Test
    @DisplayName("fechaCita nula lanza excepción")
    void testFechaCitaNula() {
        citaValida.setFechaCita(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> validationService.validar(citaValida));
        assertTrue(ex.getMessage().contains("fechaCita"));
    }

    @Test
    @DisplayName("cita nula lanza excepción")
    void testCitaNula() {
        assertThrows(IllegalArgumentException.class,
            () -> validationService.validar(null));
    }
}
