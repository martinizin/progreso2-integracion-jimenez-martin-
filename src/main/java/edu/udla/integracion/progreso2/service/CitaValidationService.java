package edu.udla.integracion.progreso2.service;

import edu.udla.integracion.progreso2.model.CitaRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Servicio de validación de negocio para CitaRequest (RF1).
 *
 * Aplica reglas programáticas que complementan las anotaciones de Bean Validation.
 * Se llama también desde la ruta Camel como defensa en profundidad, garantizando
 * que ninguna cita inválida llegue a los sistemas destino.
 */
@Service
public class CitaValidationService {

    // Patrón básico de email (complementa @Email de Bean Validation)
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Valida todos los campos de una CitaRequest.
     *
     * @param cita la solicitud a validar
     * @throws IllegalArgumentException si uno o más campos no cumplen las reglas
     */
    public void validar(CitaRequest cita) {
        if (cita == null) {
            throw new IllegalArgumentException("La solicitud de cita no puede ser nula");
        }

        List<String> errores = new ArrayList<>();

        if (esBlanco(cita.getIdCita())) {
            errores.add("idCita es obligatorio");
        }
        if (esBlanco(cita.getPaciente())) {
            errores.add("paciente es obligatorio");
        }
        if (esBlanco(cita.getCorreo())) {
            errores.add("correo es obligatorio");
        } else if (!EMAIL_PATTERN.matcher(cita.getCorreo()).matches()) {
            errores.add("correo debe tener formato de email válido");
        }
        if (esBlanco(cita.getEspecialidad())) {
            errores.add("especialidad es obligatoria");
        }
        if (cita.getFechaCita() == null) {
            errores.add("fechaCita es obligatoria");
        }
        if (esBlanco(cita.getSede())) {
            errores.add("sede es obligatoria");
        }
        if (cita.getValor() == null || cita.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            errores.add("valor debe ser mayor a 0");
        }

        if (!errores.isEmpty()) {
            throw new IllegalArgumentException("Validación fallida: " + String.join("; ", errores));
        }
    }

    private boolean esBlanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
