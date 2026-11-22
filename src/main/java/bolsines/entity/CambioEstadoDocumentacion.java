package bolsines.entity;

import bolsines.entity.estado.documentacion.EstadoDocumentacion;

import java.time.LocalDateTime;

/** Registro histórico de un estado por el que pasó una Documentación y quién lo provocó. */
public class CambioEstadoDocumentacion {

    private final LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private final Empleado responsable;
    private final EstadoDocumentacion estado;

    /** new(): crea el cambio de estado vigente (sin fecha de fin). */
    public CambioEstadoDocumentacion(LocalDateTime fechaHoraActual, Empleado responsable,
                                     EstadoDocumentacion estado) {
        this.fechaHoraInicio = fechaHoraActual;
        this.responsable = responsable;
        this.estado = estado;
    }

    public boolean sosActual() {
        return fechaHoraFin == null;
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

    public EstadoDocumentacion getEstado() {
        return estado;
    }
}
