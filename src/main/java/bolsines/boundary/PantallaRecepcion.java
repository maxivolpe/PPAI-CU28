package bolsines.boundary;

import bolsines.entity.ComisionMedica;
import bolsines.entity.DatosDeDocumentacion;
import bolsines.entity.OpcionRecepcion;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clase frontera del CU 28: los mensajes que el gestor le envía a la pantalla según el
 * diagrama de secuencia. Tiene dos implementaciones con el mismo gestor detrás:
 * PantallaRecepcionWeb (la interfaz web con React) y PantallaRecepcionConsola.
 */
public interface PantallaRecepcion {

    void mostrarCM(String nombreCMEmpleado);

    void mostrarMensaje(String mensaje);

    void solicitarSeleccionBolsin(List<ResumenDeBolsin> bolsinesEnviados);

    /** Por cada remito (número): la documentación que contiene. */
    void mostrarInfoBolsin(ResumenDeBolsin bolsin, Map<Integer, List<DatosDeDocumentacion>> informacionBolsin);

    /** La pantalla ofrece las cuatro opciones y marca como no disponibles las que no aplican a esa CM. */
    void solicitarOpcRecepcion(ComisionMedica comisionMedica);

    void solicitarConfirmacion(OpcionRecepcion opcion, Set<Integer> documentacionMarcada);

    void mostrarMensajeExito(int numeroBolsin, String estadoBolsin);
}
