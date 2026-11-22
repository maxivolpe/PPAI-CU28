package bolsines.entity;

import java.time.LocalDateTime;
import java.util.Set;

public class DetalleRemito {

    private final String areaCMCDestino;
    private final Documentacion documentacion;

    public DetalleRemito(String areaCMCDestino, Documentacion documentacion) {
        this.areaCMCDestino = areaCMCDestino;
        this.documentacion = documentacion;
    }

    /**
     * Traduce la opción de recepción elegida por el EB en el evento que le corresponde a
     * ESTA documentación: la que fue marcada recibe el evento de la opción alternativa; el
     * resto se recibe normalmente (flujo principal). Qué hace cada evento (y si está
     * permitido) lo resuelve el estado actual de la Documentación (patrón State).
     *
     * @param documentacionMarcada números de la documentación a la que se aplica la opción
     *                             alternativa (no recibida, rechazada o a redirigir); vacío
     *                             en el flujo principal.
     */
    public void registrarBolsinRecibido(LocalDateTime fechaHoraActual, Empleado responsable,
                                        OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        OpcionRecepcion aplicable = documentacionMarcada.contains(documentacion.getNumero())
                ? opcion
                : OpcionRecepcion.CONTENIDO_IGUAL;
        aplicable.aplicarA(documentacion, fechaHoraActual, responsable);
    }

    /** Verdadero si la documentación no llegó o fue rechazada: el remito queda "parcial". */
    public boolean dejaRemitoIncompleto() {
        return documentacion.getEstadoActual().dejaRemitoIncompleto();
    }

    public Documentacion getDocumentacion() {
        return documentacion;
    }

    public String getAreaCMCDestino() {
        return areaCMCDestino;
    }
}
