package bolsines.entity.estado.documentacion;

import bolsines.entity.CambioEstadoDocumentacion;
import bolsines.entity.Documentacion;
import bolsines.entity.Empleado;
import bolsines.entity.TransicionNoPermitidaException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PATRÓN STATE - rol ESTADO (State) de la Documentación.
 *
 * Declara un método por cada evento de la máquina de estados de Documentación. La
 * implementación por defecto de todos es "transición no permitida": así, cada estado
 * concreto redefine ÚNICAMENTE los eventos que tienen una flecha saliendo de él en el
 * diagrama, y cualquier otro evento queda rechazado sin escribir un solo if.
 *
 * Los estados no guardan datos propios (el contexto llega por parámetro), por eso una
 * misma instancia puede compartirse entre muchas documentaciones. Por lo mismo, dos
 * estados son iguales cuando son del mismo estado concreto.
 */
public abstract class EstadoDocumentacion {

    /** Nombre del estado; también es el valor que se guarda en la base de datos. */
    public abstract String getNombre();

    // ---------------------------------------------------------------- consultas sobre el estado

    /**
     * Consulta polimórfica: verdadero si, estando la documentación en este estado, el remito
     * que la contiene no puede quedar "Recibido y Aceptado" completo (no llegó o fue rechazada).
     */
    public boolean dejaRemitoIncompleto() {
        return false;
    }

    // ---------------------------------------------------------------- eventos del CU 28

    public void registrarBolsinRecibido(Documentacion documentacion,
                                        List<CambioEstadoDocumentacion> historialCambioEstado,
                                        LocalDateTime fechaHoraActual, Empleado responsable) {
        throw noPermitida("registrarBolsinRecibido");
    }

    public void registrarDocumentacionNoRecibida(Documentacion documentacion,
                                                 List<CambioEstadoDocumentacion> historialCambioEstado,
                                                 LocalDateTime fechaHoraActual, Empleado responsable) {
        throw noPermitida("registrarDocumentacionNoRecibida");
    }

    public void registrarRechazoDeDocumentacion(Documentacion documentacion,
                                                List<CambioEstadoDocumentacion> historialCambioEstado,
                                                LocalDateTime fechaHoraActual, Empleado responsable) {
        throw noPermitida("registrarRechazoDeDocumentacion");
    }

    public void registrarDocumentacionParaRedirigir(Documentacion documentacion,
                                                    List<CambioEstadoDocumentacion> historialCambioEstado,
                                                    LocalDateTime fechaHoraActual, Empleado responsable) {
        throw noPermitida("registrarDocumentacionParaRedirigir");
    }

    // ---------------------------------------------------------------- eventos de otros casos de uso
    // Figuran en el diagrama de clases de diseño pero pertenecen a otros CU (15, 17, 19, 20,
    // 21, 27, 31 y 9). Quedan declarados con el comportamiento por defecto para mostrar la
    // interfaz completa del State; implementarlos sería redefinirlos en el estado que corresponda.

    /** Recepción por el área interna de la CMC (sale de "Para Redirigir"). */
    public void registrarRecepcionDeDocumentacion() {
        throw noPermitida("registrarRecepcionDeDocumentacion");
    }

    public void remitar() {
        throw noPermitida("remitar");
    }

    public void cancelarRemito() {
        throw noPermitida("cancelarRemito");
    }

    public void modificarBolsin() {
        throw noPermitida("modificarBolsin");
    }

    public void actualizarBolsin() {
        throw noPermitida("actualizarBolsin");
    }

    public void registrarRetiroBolsin() {
        throw noPermitida("registrarRetiroBolsin");
    }

    public void registrarDocumentacionParaReenvio() {
        throw noPermitida("registrarDocumentacionParaReenvio");
    }

    public void darDeBaja() {
        throw noPermitida("darDeBaja");
    }

    // ---------------------------------------------------------------- utilidades

    /** La excepción por defecto de todo evento sin transición definida desde este estado. */
    protected final TransicionNoPermitidaException noPermitida(String evento) {
        return new TransicionNoPermitidaException(evento, getNombre());
    }

    @Override
    public final boolean equals(Object otro) {
        return otro != null && getClass() == otro.getClass();
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public final String toString() {
        return getNombre();
    }
}
