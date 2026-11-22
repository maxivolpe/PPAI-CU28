package bolsines.entity.estado.remito;

import bolsines.entity.DetalleRemito;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Remito;
import bolsines.entity.TransicionNoPermitidaException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * PATRÓN STATE - rol ESTADO (State) del Remito.
 *
 * Por defecto todo evento es "transición no permitida"; cada estado concreto redefine solo
 * los eventos que salen de él en la máquina de estados de Remito. Para el CU 28 el único
 * evento es la recepción, y solo "En Bolsín Enviado" la admite.
 */
public abstract class EstadoRemito {

    /** Nombre del estado; también es el valor que se guarda en la base de datos. */
    public abstract String getNombre();

    public void registrarBolsinRecibido(Remito remito, List<DetalleRemito> detalles,
                                        LocalDateTime fechaHoraActual, Empleado responsable,
                                        OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        throw new TransicionNoPermitidaException("registrarBolsinRecibido", getNombre());
    }
}
