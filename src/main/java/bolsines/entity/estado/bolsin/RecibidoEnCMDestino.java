package bolsines.entity.estado.bolsin;

/**
 * PATRON STATE - estado concreto "Recibido en CM Destino".
 * Estado final para el CU 28: no redefine ningun evento, por lo que cualquiera es rechazado.
 */
public class RecibidoEnCMDestino extends EstadoBolsin {

    @Override
    public String getNombre() {
        return "Recibido en CM Destino";
    }
}
