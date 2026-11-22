package bolsines.entity.estado.remito;

import bolsines.entity.DetalleRemito;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Remito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * PATRÓN STATE - ESTADO CONCRETO "En Bolsín Enviado" del Remito.
 *
 * Único estado desde el que se puede registrar la recepción. Propaga el evento a cada
 * detalle (y este a su Documentación, que a su vez delega en SU estado) y después decide
 * a qué estado pasa el remito según cómo quedó su documentación.
 */
public class EnBolsinEnviado extends EstadoRemito {

    @Override
    public String getNombre() {
        return "En Bolsin Enviado";
    }

    @Override
    public void registrarBolsinRecibido(Remito remito, List<DetalleRemito> detalles,
                                        LocalDateTime fechaHoraActual, Empleado responsable,
                                        OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        // loop de detalles de remito
        for (DetalleRemito detalle : detalles) {
            detalle.registrarBolsinRecibido(fechaHoraActual, responsable, opcion, documentacionMarcada);
        }

        EstadoRemito estadoNuevo = crearEstado(detalles);
        remito.setEstado(estadoNuevo);
    }

    /**
     * Las dos transiciones de salida de la máquina de estados de Remito:
     * [el contenido es igual al registrado] -> Recibido y Aceptado;
     * [no se acepta toda la documentación]  -> Recibido y Aceptado Parcial.
     */
    private EstadoRemito crearEstado(List<DetalleRemito> detalles) {
        for (DetalleRemito detalle : detalles) {
            if (detalle.dejaRemitoIncompleto()) {
                return new RecibidoYAceptadoParcial();
            }
        }
        return new RecibidoYAceptado();
    }
}
