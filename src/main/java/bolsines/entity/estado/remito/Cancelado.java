package bolsines.entity.estado.remito;

/**
 * PATRON STATE - estado concreto "Cancelado".
 * Estado final para el CU 28: no redefine ningun evento, por lo que cualquiera es rechazado.
 */
public class Cancelado extends EstadoRemito {

    @Override
    public String getNombre() {
        return "Cancelado";
    }
}
