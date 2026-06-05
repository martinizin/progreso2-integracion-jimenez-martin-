# Integración de Sistemas — Salud360

**Examen Práctico Progreso 2**

---

## 1. Nombre del estudiante

**Jiménez M.**
Universidad UDLA · Integración de Sistemas

---

## 2. Descripción de la solución

Solución mínima funcional de integración de sistemas para **Salud360**. Automatiza el flujo de registro de citas médicas confirmadas desde una API REST hacia cuatro sistemas destino, aplicando los patrones de integración empresarial (EIP) **Point-to-Point**, **Publish/Subscribe** y **File Transfer**.

```
POST /api/citas
      │
      ▼
 CitaController  ──validates──► GlobalExceptionHandler (400)
      │
      ▼ (202 Accepted)
 direct:procesarCita  (Apache Camel)
      │
      ├──► billing.exchange → billing.queue         [Point-to-Point]
      ├──► appointments.events → notifications.queue  [Pub/Sub]
      │                        → analytics.queue
      └──► data/outbox/auditoria-citas.csv           [File Transfer]
```

---

## 3. Tecnologías utilizadas

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 17 (LTS) | Lenguaje |
| Spring Boot | 3.2.5 | Framework base |
| Apache Camel | 4.4.0 | Orquestación / rutas de integración |
| RabbitMQ | 3-management | Broker de mensajería |
| Docker Compose | — | Infraestructura local |
| Maven | 3.x | Build |
| JUnit 5 | — | Pruebas |

---

## 4. Levantar RabbitMQ

```bash
docker compose up -d
```

- **Consola de administración:** http://localhost:15672
- **Credenciales:** `guest` / `guest`
- **Puerto AMQP:** `5672`

Verificar que las colas `billing.queue`, `notifications.queue` y `analytics.queue` aparezcan en la consola (se crean automáticamente al arrancar la aplicación).

---

## 5. Ejecutar la aplicación

**Con Maven instalado:**
```bash
mvn spring-boot:run
```

**En Windows desde VS Code (terminal integrada):**
```bash
mvnw.cmd spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080`.

> **Nota de versión:** el sistema de desarrollo usa Java 25; el código compila a bytecode Java 17 (`--release 17`). Spring Boot 3.2.x y Camel 4.4.x son compatibles con Java 17+.

---

## 6. Endpoint disponible

| Método | URL | Descripción |
|---|---|---|
| `POST` | `/api/citas` | Registra una cita médica e inicia el flujo de integración |

---

## 7. Ejemplos de uso

### Request válido

```bash
curl -X POST http://localhost:8080/api/citas \
  -H "Content-Type: application/json" \
  -d '{
    "idCita":      "CITA-1001",
    "paciente":    "Ana Torres",
    "correo":      "ana.torres@email.com",
    "especialidad":"Cardiología",
    "fechaCita":   "2026-06-15",
    "sede":        "Centro Norte",
    "valor":       45.50
  }'
```

**Respuesta esperada (HTTP 202 Accepted):**
```json
{
  "idCita":  "CITA-1001",
  "estado":  "ACEPTADA",
  "mensaje": "Solicitud de cita registrada y procesada exitosamente"
}
```

### Request inválido (valor = 0 y correo vacío)

```bash
curl -X POST http://localhost:8080/api/citas \
  -H "Content-Type: application/json" \
  -d '{
    "idCita":      "CITA-1002",
    "paciente":    "Pedro Ruiz",
    "correo":      "",
    "especialidad":"Dermatología",
    "fechaCita":   "2026-06-20",
    "sede":        "Centro Sur",
    "valor":       0
  }'
```

**Respuesta esperada (HTTP 400 Bad Request):**
```json
{
  "timestamp": "2026-06-04T10:30:00",
  "status":    400,
  "error":     "Error de validación",
  "mensajes":  [
    "correo es obligatorio",
    "valor debe ser mayor a 0"
  ]
}
```

---

## 8. Patrones de integración aplicados

### Point-to-Point Channel — Facturación

- **Dónde:** `billing.exchange` (DirectExchange) → `billing.queue`
- **Por qué:** la orden de cobro debe procesarse **una sola vez** por **un único consumidor**. Una cola con consumidor competidor garantiza que, aunque haya varios procesadores activos, solo uno recibe cada mensaje. El fanout sería inadecuado porque entregaría la factura a múltiples consumidores, generando doble cobro.

### Publish/Subscribe Channel — Notificaciones y Analítica

- **Dónde:** `appointments.events` (FanoutExchange) → `notifications.queue` + `analytics.queue`
- **Por qué:** el mismo evento de cita confirmada debe llegar a **todos** los sistemas interesados simultáneamente. El fanout entrega una copia a cada cola enlazada sin que el productor necesite conocer a los suscriptores. Agregar un nuevo sistema consumidor no requiere modificar el productor.

### File Transfer — Sistema legado de Auditoría

- **Dónde:** `data/outbox/auditoria-citas.csv`
- **Por qué:** el sistema legado no cuenta con API ni conexión a mensajería. Solo puede leer archivos `.csv` desde una carpeta compartida. Esta es la única forma de integrarlo sin modificar su código.

### Manejo de errores

- **Bean Validation (`@Valid`)** → campos inválidos devuelven 400 con lista de mensajes (sin stack traces).
- **`GlobalExceptionHandler`** → captura `MethodArgumentNotValidException` e `IllegalArgumentException`; nunca filtra trazas internas al cliente.
- **`onException` en Camel** → cualquier excepción dentro de la ruta queda registrada en `data/errors/citas-rechazadas.log` con timestamp, idCita, motivo y payload. La aplicación continúa procesando sin detenerse.

---

## 9. Estructura del proyecto

```
JimenezM_examenP2/
├── README.md
├── docker-compose.yml
├── pom.xml
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/edu/udla/integracion/progreso2/
│   │   │   ├── Progreso2Application.java
│   │   │   ├── config/
│   │   │   │   └── RabbitMQConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── CitaController.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── model/
│   │   │   │   ├── CitaRequest.java
│   │   │   │   └── ErrorResponse.java
│   │   │   ├── routes/
│   │   │   │   └── CitaIntegrationRoute.java
│   │   │   └── service/
│   │   │       └── CitaValidationService.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/edu/udla/integracion/progreso2/
│           ├── controller/CitaControllerTest.java
│           └── service/CitaValidationServiceTest.java
├── data/
│   ├── outbox/
│   │   └── auditoria-citas.csv
│   └── errors/
│       └── citas-rechazadas.log   (generado en runtime)
└── docs/
    └── capturas/
```

---

## 10. Evidencia esperada

Para verificar el funcionamiento completo, capturar:

1. **Aplicación iniciada** — consola de Spring Boot mostrando rutas Camel registradas sin errores.
2. **Request válido** — Postman/curl con el payload de ejemplo.
3. **Respuesta 202** — cuerpo JSON con `estado: "ACEPTADA"`.
4. **`billing.queue`** — consola RabbitMQ muestra 1 mensaje encolado.
5. **`notifications.queue` y `analytics.queue`** — cada una con 1 mensaje (mismo evento fanout).
6. **`auditoria-citas.csv`** — archivo con cabecera + línea de datos.
7. **`citas-rechazadas.log`** — entrada con timestamp, idCita y motivo ante una solicitud inválida.

---

## 11. Ejecutar las pruebas

```bash
mvn test
```

Las pruebas cubren:
- `CitaValidationServiceTest` — campos faltantes, `valor <= 0`, correo inválido.
- `CitaControllerTest` — 202 para payload válido, 400 para inválido (MockMvc).
