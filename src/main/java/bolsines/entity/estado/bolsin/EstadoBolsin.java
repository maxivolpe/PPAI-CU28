package bolsines.entity.estado.bolsin;

import bolsines.entity.Bolsin;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Remito;
import bolsines.entity.TransicionNoPermitidaException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * PATRÓN STATE - rol ESTADO (State) del Bolsín.
 *
 * Define la interfaz común de todos los estados del Bolsín. La implementación por defecto
 * de cada evento es "transición no permitida": los estados concretos redefinen solo los
 * eventos que tienen una transición de salida en la máquina de estados de Bolsín.
 */
public abstract class EstadoBolsin {

    /** Nombre del estado; también es el valor que se guarda en la base de datos. */
    public abstract String getNombre();

    /** Consulta polimórfica: solo el estado concreto Enviado responde verdadero. */
    public boolean sosEnviado() {
        return false;
    }

    /** Evento del CU 28. Solo el estado Enviado lo redefine. */
    public void registrarBolsinRecibido(Bolsin bolsin, LocalDateTime fechaHoraActual,
                                        Empleado responsable, List<Remito> remitos,
                                        CambioEstadoBolsin cambioEstadoActual,
                                        OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        throw new TransicionNoPermitidaException("registrarBolsinRecibido", getNombre());
    }
}
