package bolsines.entity.estado.bolsin;

import bolsines.entity.Bolsin;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Remito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * PATRÓN STATE - ESTADO CONCRETO "Enviado" del Bolsín.
 *
 * Es el único estado que admite el evento registrarBolsinRecibido (transición
 * Enviado -> Recibido en CM Destino). Toda la lógica de la transición vive acá, siguiendo
 * el diagrama de secuencia de diseño:
 *   1. propaga la recepción a cada remito (que delega en su propio estado),
 *   2. cierra el cambio de estado vigente,
 *   3. crea el estado siguiente y su cambio de estado con el responsable,
 *   4. le asigna ambos al Bolsín (el contexto).
 */
public class Enviado extends EstadoBolsin {

    @Override
    public String getNombre() {
        return "Enviado";
    }

    @Override
    public boolean sosEnviado() {
        return true;
    }

    @Override
    public void registrarBolsinRecibido(Bolsin bolsin, LocalDateTime fechaHoraActual,
                                        Empleado responsable, List<Remito> remitos,
                                        CambioEstadoBolsin cambioEstadoActual,
                                        OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        // Guarda [comisión destino igual CMC] de la máquina de estados (alternativo A5).
        // Se evalúa antes de tocar nada para no dejar la recepción a medias.
        if (opcion == OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR && !bolsin.getDestino().sosCentral()) {
            throw new IllegalStateException(
                    "Solo la Comisión Médica Central puede redirigir documentación a otra área.");
        }

        // loop de remitos para actualizar estado
        for (Remito remito : remitos) {
            remito.registrarBolsinRecibido(fechaHoraActual, responsable, opcion, documentacionMarcada);
        }

        cambioEstadoActual.setFechaHoraFin(fechaHoraActual);

        EstadoBolsin nuevoEstado = crearEstado();
        CambioEstadoBolsin nuevoCambioEstado = crearCambioEstadoBolsin(fechaHoraActual, responsable, nuevoEstado);

        bolsin.setEstado(nuevoEstado);
        bolsin.agregarCambioEstado(nuevoCambioEstado);
    }

    /** El estado concreto sabe cuál es su sucesor: el gestor ya no busca estados. */
    private EstadoBolsin crearEstado() {
        return new RecibidoEnCMDestino();
    }

    private CambioEstadoBolsin crearCambioEstadoBolsin(LocalDateTime fechaHoraActual,
                                                       Empleado responsable, EstadoBolsin estado) {
        return new CambioEstadoBolsin(fechaHoraActual, responsable, estado);
    }
}
