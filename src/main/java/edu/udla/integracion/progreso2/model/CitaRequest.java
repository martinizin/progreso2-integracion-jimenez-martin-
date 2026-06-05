package edu.udla.integracion.progreso2.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo de solicitud de cita médica (RF1).
 * Combina Bean Validation para validación declarativa en la capa web
 * con validación de negocio adicional en CitaValidationService.
 */
public class CitaRequest {

    @NotBlank(message = "idCita es obligatorio")
    private String idCita;

    @NotBlank(message = "paciente es obligatorio")
    private String paciente;

    @NotBlank(message = "correo es obligatorio")
    @Email(message = "correo debe tener formato de email válido")
    private String correo;

    @NotBlank(message = "especialidad es obligatoria")
    private String especialidad;

    @NotNull(message = "fechaCita es obligatoria")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaCita;

    @NotBlank(message = "sede es obligatoria")
    private String sede;

    @NotNull(message = "valor es obligatorio")
    @DecimalMin(value = "0.01", message = "valor debe ser mayor a 0")
    private BigDecimal valor;

    // ── Getters y Setters ───────────────────────────────────────────────────

    public String getIdCita() { return idCita; }
    public void setIdCita(String idCita) { this.idCita = idCita; }

    public String getPaciente() { return paciente; }
    public void setPaciente(String paciente) { this.paciente = paciente; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public LocalDate getFechaCita() { return fechaCita; }
    public void setFechaCita(LocalDate fechaCita) { this.fechaCita = fechaCita; }

    public String getSede() { return sede; }
    public void setSede(String sede) { this.sede = sede; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    @Override
    public String toString() {
        return String.format(
            "CitaRequest{idCita='%s', paciente='%s', correo='%s', " +
            "especialidad='%s', fechaCita='%s', sede='%s', valor=%s}",
            idCita, paciente, correo, especialidad, fechaCita, sede, valor
        );
    }
}
