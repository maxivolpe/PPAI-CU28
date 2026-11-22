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
 * misma instancia puede compartirse entre muchas documentaciones.
 */
public abstract class EstadoDocumentacion {

    /** Nombre del estado; también es el valor que se guarda en la base de datos. */
    public abstract String getNombre();

    // ---------------------------------------------------------------- consultas sobre el estado

    public boolean esNoRecibida() {
        return false;
    }

    public boolean esRecibidaYRechazada() {
        return false;
    }

    // ---------------------------------------------------------------- eventos del CU 28

    public void registrarBolsinRecibido(Documentacion documentacion,
                                        List<CambioEstadoDocumentacion> historialCambioEstado,
                                        LocalDateTime fechaHoraActual, Empleado responsable) {
        throw new TransicionNoPermitidaException("registrarBolsinRecibido", getNombre());
    }

    public void registrarDocumentacionNoRecibida(Documentacion documentacion,
                                                 List<CambioEstadoDocumentacion> historialCambioEstado,
                                                 LocalDateTime fechaHoraActual, Empleado responsable) {
        throw new TransicionNoPermitidaException("registrarDocumentacionNoRecibida", getNombre());
    }

    public void registrarRechazoDeDocumentacion(Documentacion documentacion,
                                                List<CambioEstadoDocumentacion> historialCambioEstado,
                                                LocalDateTime fechaHoraActual, Empleado responsable) {
        throw new TransicionNoPermitidaException("registrarRechazoDeDocumentacion", getNombre());
    }

    public void registrarDocumentacionParaRedirigir(Documentacion documentacion,
                                                    List<CambioEstadoDocumentacion> historialCambioEstado,
                                                    LocalDateTime fechaHoraActual, Empleado responsable) {
        throw new TransicionNoPermitidaException("registrarDocumentacionParaRedirigir", getNombre());
    }

    // ---------------------------------------------------------------- eventos de otros casos de uso
    // Figuran en el diagrama de clases de diseño pero pertenecen a otros CU (15, 17, 19, 20,
    // 21, 27, 31 y 9). Quedan declarados con el comportamiento por defecto para mostrar la
    // interfaz completa del State; implementarlos sería redefinirlos en el estado que corresponda.

    /** Recepción por el área interna de la CMC (sale de "Para Redirigir"). */
    public void registrarRecepcionDeDocumentacion() {
        throw new TransicionNoPermitidaException("registrarRecepcionDeDocumentacion", getNombre());
    }

    public void remitar() {
        throw new TransicionNoPermitidaException("remitar", getNombre());
    }

    public void cancelarRemito() {
        throw new TransicionNoPermitidaException("cancelarRemito", getNombre());
    }

    public void modificarBolsin() {
        throw new TransicionNoPermitidaException("modificarBolsin", getNombre());
    }

    public void actualizarBolsin() {
        throw new TransicionNoPermitidaException("actualizarBolsin", getNombre());
    }

    public void registrarRetiroBolsin() {
        throw new TransicionNoPermitidaException("registrarRetiroBolsin", getNombre());
    }

    public void registrarDocumentacionParaReenvio() {
        throw new TransicionNoPermitidaException("registrarDocumentacionParaReenvio", getNombre());
    }

    public void darDeBaja() {
        throw new TransicionNoPermitidaException("darDeBaja", getNombre());
    }
}
