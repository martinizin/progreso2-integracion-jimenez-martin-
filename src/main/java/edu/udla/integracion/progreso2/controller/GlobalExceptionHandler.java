package edu.udla.integracion.progreso2.controller;

import edu.udla.integracion.progreso2.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones (RF1 — RF5).
 *
 * Garantiza que el cliente reciba siempre una respuesta JSON estructurada
 * sin stack traces ni detalles internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Captura errores de Bean Validation (@Valid).
     * Devuelve lista de mensajes de campo para que el cliente sepa exactamente qué corregir.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {

        List<String> mensajes = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());

        log.warn("[Validación] Solicitud rechazada: {}", mensajes);

        return ResponseEntity.badRequest().body(new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Error de validación",
            mensajes
        ));
    }

    /**
     * Captura errores de validación de negocio (CitaValidationService).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[Negocio] Argumento inválido: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Solicitud inválida",
            List.of(ex.getMessage())
        ));
    }

    /**
     * Captura cuerpo de solicitud faltante o JSON malformado.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("[Solicitud] Cuerpo inválido o faltante: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Cuerpo de solicitud inválido",
            List.of("El cuerpo de la solicitud es requerido y debe ser JSON válido")
        ));
    }

    /**
     * Captura rutas inexistentes (Spring Boot 3.2+ lanza NoResourceFoundException
     * en vez de delegar al BasicErrorController como en versiones anteriores).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        log.warn("[404] Ruta no encontrada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "Recurso no encontrado",
            List.of("No existe un endpoint en la ruta solicitada. Usa POST /api/citas")
        ));
    }

    /**
     * Captura cualquier otra excepción no controlada.
     * Nunca expone detalles internos al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("[Error] Excepción inesperada: {}", ex.getMessage(), ex);

        return ResponseEntity.internalServerError().body(new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Error interno del servidor",
            List.of("Se produjo un error inesperado. Por favor intente nuevamente.")
        ));
    }
}
