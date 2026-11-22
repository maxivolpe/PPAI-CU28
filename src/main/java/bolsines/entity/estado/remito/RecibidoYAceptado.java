package bolsines.entity.estado.remito;

/**
 * PATRON STATE - estado concreto "Recibido y Aceptado".
 * Estado final para el CU 28: no redefine ningun evento, por lo que cualquiera es rechazado.
 */
public class RecibidoYAceptado extends EstadoRemito {

    @Override
    public String getNombre() {
        return "Recibido y Aceptado";
    }
}
