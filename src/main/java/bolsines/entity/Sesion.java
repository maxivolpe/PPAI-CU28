package bolsines.entity;

import java.time.LocalDateTime;

/** Sesión activa: de acá el gestor obtiene el usuario logueado. */
public class Sesion {

    private final LocalDateTime fechaHoraInicio;
    private final Usuario usuario;

    public Sesion(LocalDateTime fechaHoraInicio, Usuario usuario) {
        this.fechaHoraInicio = fechaHoraInicio;
        this.usuario = usuario;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
