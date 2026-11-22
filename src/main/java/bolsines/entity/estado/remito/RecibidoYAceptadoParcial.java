package bolsines.entity.estado.remito;

/**
 * PATRON STATE - estado concreto "Recibido y Aceptado Parcial".
 * Estado final para el CU 28: no redefine ningun evento, por lo que cualquiera es rechazado.
 */
public class RecibidoYAceptadoParcial extends EstadoRemito {

    @Override
    public String getNombre() {
        return "Recibido y Aceptado Parcial";
    }
}
