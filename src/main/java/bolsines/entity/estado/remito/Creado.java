package bolsines.entity.estado.remito;

/**
 * PATRON STATE - estado concreto "Creado".
 * Sus transiciones pertenecen a otros casos de uso: no redefine ningun evento del CU 28.
 */
public class Creado extends EstadoRemito {

    @Override
    public String getNombre() {
        return "Creado";
    }
}
