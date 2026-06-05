package edu.udla.integracion.progreso2.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.udla.integracion.progreso2.config.RabbitMQConfig;
import edu.udla.integracion.progreso2.model.CitaRequest;
import edu.udla.integracion.progreso2.service.CitaValidationService;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ruta de integración principal de Salud360 (RF2, RF3, RF4, RF5).
 *
 * Punto de entrada: direct:procesarCita
 *
 * Flujo:
 *  1. Revalidación defensiva (CitaValidationService)
 *  2. Multicast hacia tres ramas:
 *     a) direct:enviarFacturacion  → billing.exchange (Point-to-Point, RF2)
 *     b) direct:publicarEvento     → appointments.events (Pub/Sub fanout, RF3)
 *     c) direct:generarCSV         → data/outbox/auditoria-citas.csv (File Transfer, RF4)
 *  3. onException → log de rechazos en data/errors/citas-rechazadas.log (RF5)
 */
@Component
public class CitaIntegrationRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(CitaIntegrationRoute.class);

    @Value("${app.csv.outbox-path}")
    private String csvOutboxPath;

    @Value("${app.csv.file-name}")
    private String csvFileName;

    @Value("${app.errors.log-path}")
    private String errorsLogPath;

    @Autowired
    private CitaValidationService citaValidationService;

    @Autowired
    private ObjectMapper objectMapper;

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void configure() {

        // ── Manejo global de errores (RF5) ─────────────────────────────────────
        onException(Exception.class)
            .handled(true)
            .process(exchange -> {
                Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

                // Intentar recuperar idCita y payload del cuerpo del exchange
                Object body = exchange.getIn().getBody();
                String idCita = "N/A";
                String payload = "N/A";

                if (body instanceof CitaRequest cita) {
                    idCita  = cita.getIdCita() != null ? cita.getIdCita() : "N/A";
                    payload = cita.toString();
                } else {
                    // El cuerpo puede haber sido transformado (ej. JSON string)
                    String propId = exchange.getProperty("citaId", String.class);
                    if (propId != null) idCita = propId;
                    payload = body != null ? body.toString() : "N/A";
                }

                String motivo    = ex != null ? ex.getMessage() : "Error desconocido";
                String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                String linea = String.format("[%s] idCita=%s | motivo=%s | payload=%s%n",
                    timestamp, idCita, motivo, payload);

                // Escribir en el log de rechazos
                try {
                    Path logPath = Paths.get(errorsLogPath);
                    logPath.getParent().toFile().mkdirs();
                    Files.write(logPath, linea.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    log.warn("[Error] Rechazo registrado: {}", linea.trim());
                } catch (Exception ioEx) {
                    log.error("[Error] No se pudo escribir en el log de errores: {}",
                        ioEx.getMessage());
                }
            })
            .log("Error capturado en ruta Camel: ${exception.message}");

        // ── Ruta principal ──────────────────────────────────────────────────────
        from("direct:procesarCita")
            .routeId("ruta-procesar-cita")
            .log("Procesando cita: ${body.idCita}")
            .process(exchange -> {
                CitaRequest cita = exchange.getIn().getBody(CitaRequest.class);
                // Guardar idCita como propiedad para uso en onException
                exchange.setProperty("citaId", cita.getIdCita());
                // Defensa en profundidad: revalidar aunque el controller ya validó
                citaValidationService.validar(cita);
            })
            .multicast()
                .to("direct:enviarFacturacion",
                    "direct:publicarEvento",
                    "direct:generarCSV")
            .end()
            .log("Cita ${exchangeProperty.citaId} procesada exitosamente en los tres sistemas");

        // ── Sub-ruta: Facturación — Point-to-Point (RF2) ────────────────────────
        from("direct:enviarFacturacion")
            .routeId("ruta-facturacion-p2p")
            .log("Enviando comando de facturación: ${body.idCita}")
            .process(exchange -> {
                CitaRequest cita = exchange.getIn().getBody(CitaRequest.class);

                Map<String, Object> mensaje = new LinkedHashMap<>();
                mensaje.put("idCita",       cita.getIdCita());
                mensaje.put("paciente",     cita.getPaciente());
                mensaje.put("especialidad", cita.getEspecialidad());
                mensaje.put("valor",        cita.getValor());
                mensaje.put("tipoMensaje",  "COMANDO_FACTURAR_CITA");

                exchange.getIn().setBody(objectMapper.writeValueAsString(mensaje));
                exchange.getIn().setHeader("Content-Type", "application/json");
            })
            // Point-to-Point: billing.exchange (DirectExchange) → billing.queue
            .to("spring-rabbitmq:" + RabbitMQConfig.BILLING_EXCHANGE
                + "?routingKey=" + RabbitMQConfig.BILLING_ROUTING_KEY)
            .log("Comando COMANDO_FACTURAR_CITA enviado a billing.queue");

        // ── Sub-ruta: Evento Pub/Sub — Notificaciones y Analítica (RF3) ─────────
        from("direct:publicarEvento")
            .routeId("ruta-evento-pubsub")
            .log("Publicando evento CITA_CONFIRMADA: ${body.idCita}")
            .process(exchange -> {
                CitaRequest cita = exchange.getIn().getBody(CitaRequest.class);

                Map<String, Object> evento = new LinkedHashMap<>();
                evento.put("idCita",       cita.getIdCita());
                evento.put("paciente",     cita.getPaciente());
                evento.put("correo",       cita.getCorreo());
                evento.put("especialidad", cita.getEspecialidad());
                evento.put("fechaCita",    cita.getFechaCita() != null
                    ? cita.getFechaCita().toString() : null);
                evento.put("sede",         cita.getSede());
                evento.put("tipoEvento",   "CITA_CONFIRMADA");

                exchange.getIn().setBody(objectMapper.writeValueAsString(evento));
                exchange.getIn().setHeader("Content-Type", "application/json");
            })
            // Pub/Sub: appointments.events (FanoutExchange) → notifications.queue + analytics.queue
            .to("spring-rabbitmq:" + RabbitMQConfig.EVENTS_EXCHANGE)
            .log("Evento CITA_CONFIRMADA publicado en appointments.events (fanout)");

        // ── Sub-ruta: Auditoría CSV — File Transfer para sistema legado (RF4) ───
        from("direct:generarCSV")
            .routeId("ruta-auditoria-csv")
            .log("Generando línea CSV para auditoría: ${body.idCita}")
            .process(exchange -> {
                CitaRequest cita = exchange.getIn().getBody(CitaRequest.class);

                Path csvPath = Paths.get(csvOutboxPath, csvFileName);
                csvPath.getParent().toFile().mkdirs();

                // Escribir cabecera solo si el archivo no existe o está vacío
                boolean needsHeader = !csvPath.toFile().exists()
                    || csvPath.toFile().length() == 0;

                // Nota: se asume que los campos no contienen comas.
                // En producción se usaría entrecomillado RFC-4180.
                String valorFormateado = cita.getValor()
                    .setScale(2, RoundingMode.HALF_UP)
                    .toPlainString();

                String lineaDatos = String.join(",",
                    cita.getIdCita(),
                    cita.getPaciente(),
                    cita.getCorreo(),
                    cita.getEspecialidad(),
                    cita.getFechaCita().toString(),
                    cita.getSede(),
                    valorFormateado
                ) + System.lineSeparator();

                String contenido = (needsHeader
                    ? "idCita,paciente,correo,especialidad,fechaCita,sede,valor"
                        + System.lineSeparator()
                    : "")
                    + lineaDatos;

                Files.write(csvPath, contenido.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                log.info("[CSV] Línea escrita en {}: {}", csvPath, lineaDatos.trim());
                exchange.getIn().setBody("CSV escrito para: " + cita.getIdCita());
            })
            .log("Línea CSV de auditoría generada correctamente");
    }
}
