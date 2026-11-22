package bolsines.entity;

import bolsines.entity.estado.bolsin.EstadoBolsin;

import java.time.LocalDateTime;

/** Registro histórico de un estado por el que pasó un Bolsín y quién lo provocó. */
public class CambioEstadoBolsin {

    private final LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private final Empleado responsable;
    private final EstadoBolsin estado;

    /** new(): crea el cambio de estado vigente (sin fecha de fin). */
    public CambioEstadoBolsin(LocalDateTime fechaHoraActual, Empleado responsable, EstadoBolsin estado) {
        this.fechaHoraInicio = fechaHoraActual;
        this.responsable = responsable;
        this.estado = estado;
    }

    public boolean sosActual() {
        return fechaHoraFin == null;
    }

    /** Delega en el estado (consulta polimórfica: solo Enviado responde verdadero). */
    public boolean sosEnviado() {
        return estado.sosEnviado();
    }

    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public Empleado getResponsable() {
        return responsable;
    }

    public EstadoBolsin getEstado() {
        return estado;
    }
}
