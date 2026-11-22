package bolsines.entity.estado.documentacion;

import bolsines.entity.CambioEstadoDocumentacion;
import bolsines.entity.Documentacion;
import bolsines.entity.Empleado;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PATRÓN STATE - ESTADO CONCRETO (ConcreteState) "En Bolsín Enviado" de la Documentación.
 *
 * Es el estado en que está la documentación cuando llega el bolsín, y del que salen las
 * cuatro transiciones del CU 28. Cada método redefinido es una flecha de la máquina de
 * estados: el propio estado conoce cuál es el estado siguiente y lo crea.
 */
public class EnBolsinEnviado extends EstadoDocumentacion {

    @Override
    public String getNombre() {
        return "En Bolsin Enviado";
    }

    /** [contenido bolsín es igual contenido registrado] -> Recibida y Aceptada. */
    @Override
    public void registrarBolsinRecibido(Documentacion documentacion,
                                        List<CambioEstadoDocumentacion> historialCambioEstado,
                                        LocalDateTime fechaHoraActual, Empleado responsable) {
        // Mismo orden que el diagrama de secuencia de diseño.
        CambioEstadoDocumentacion actualCambioEstado = buscarCambioEstadoActual(historialCambioEstado);
        actualCambioEstado.setFechaHoraFin(fechaHoraActual);

        EstadoDocumentacion estadoNuevo = crearEstado();
        CambioEstadoDocumentacion nuevoCambioEstado =
                crearCambioEstadoDocumentacion(fechaHoraActual, responsable, estadoNuevo);

        documentacion.setEstado(estadoNuevo);
        documentacion.agregarCambioEstado(nuevoCambioEstado);
    }

    /** Alternativo A3 -> No Recibida. */
    @Override
    public void registrarDocumentacionNoRecibida(Documentacion documentacion,
                                                 List<CambioEstadoDocumentacion> historialCambioEstado,
                                                 LocalDateTime fechaHoraActual, Empleado responsable) {
        cambiarEstado(documentacion, historialCambioEstado, fechaHoraActual, responsable, new NoRecibida());
    }

    /** Alternativo A4 [comisión médica destino incorrecta] -> Recibida y Rechazada. */
    @Override
    public void registrarRechazoDeDocumentacion(Documentacion documentacion,
                                                List<CambioEstadoDocumentacion> historialCambioEstado,
                                                LocalDateTime fechaHoraActual, Empleado responsable) {
        cambiarEstado(documentacion, historialCambioEstado, fechaHoraActual, responsable, new RecibidaYRechazada());
    }

    /** Alternativo A5 [comisión destino igual CMC] -> Para Redirigir. La guarda la evalúa el estado del Bolsín. */
    @Override
    public void registrarDocumentacionParaRedirigir(Documentacion documentacion,
                                                    List<CambioEstadoDocumentacion> historialCambioEstado,
                                                    LocalDateTime fechaHoraActual, Empleado responsable) {
        cambiarEstado(documentacion, historialCambioEstado, fechaHoraActual, responsable, new ParaRedirigir());
    }

    // ---------------------------------------------------------------- métodos propios (diagrama de clases)

    /** *sosActual(): recorre el historial hasta encontrar el cambio de estado vigente. */
    private CambioEstadoDocumentacion buscarCambioEstadoActual(
            List<CambioEstadoDocumentacion> historialCambioEstado) {
        for (CambioEstadoDocumentacion cambioEstado : historialCambioEstado) {
            if (cambioEstado.sosActual()) {
                return cambioEstado;
            }
        }
        throw new IllegalStateException("La documentación no tiene un cambio de estado vigente.");
    }

    /** El estado siguiente del flujo principal: el estado concreto conoce a su sucesor. */
    private EstadoDocumentacion crearEstado() {
        return new RecibidaYAceptada();
    }

    private CambioEstadoDocumentacion crearCambioEstadoDocumentacion(
            LocalDateTime fechaHoraActual, Empleado responsable, EstadoDocumentacion estado) {
        return new CambioEstadoDocumentacion(fechaHoraActual, responsable, estado);
    }

    /** Los flujos alternativos repiten los pasos del principal con otro estado destino. */
    private void cambiarEstado(Documentacion documentacion,
                               List<CambioEstadoDocumentacion> historialCambioEstado,
                               LocalDateTime fechaHoraActual, Empleado responsable,
                               EstadoDocumentacion estadoNuevo) {
        buscarCambioEstadoActual(historialCambioEstado).setFechaHoraFin(fechaHoraActual);
        documentacion.setEstado(estadoNuevo);
        documentacion.agregarCambioEstado(
                crearCambioEstadoDocumentacion(fechaHoraActual, responsable, estadoNuevo));
    }
}
