package bolsines.boundary;

import bolsines.entity.ComisionMedica;
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

    /** Por cada bolsín: {número de bolsín, CM origen, número de precinto}. */
    void solicitarSeleccionBolsin(List<String[]> bolsinesEnviados);

    /** Por cada remito (número): la documentación que contiene, {número, asunto, tipo}. */
    void mostrarInfoBolsin(int numeroBolsin, String cmOrigen, int numeroPrecinto,
                           Map<Integer, List<String[]>> informacionBolsin);

    /** Ofrece las cuatro opciones y marca como no disponibles las que no aplican a esa CM (A5). */
    void solicitarOpcRecepcion(ComisionMedica comisionMedica);

    void solicitarConfirmacion(OpcionRecepcion opcion, Set<Integer> documentacionMarcada);

    void mostrarMensajeExito(int numeroBolsin, String estadoBolsin);
}
