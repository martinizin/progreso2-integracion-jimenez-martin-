package edu.udla.integracion.progreso2.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de error estructurada para el cliente.
 * Nunca expone stack traces ni detalles internos.
 */
public class ErrorResponse {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private int status;
    private String error;
    private List<String> mensajes;

    public ErrorResponse(LocalDateTime timestamp, int status, String error, List<String> mensajes) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.mensajes = mensajes;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public List<String> getMensajes() { return mensajes; }
}
