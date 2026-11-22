package bolsines.entity.estado.bolsin;

/**
 * PATRON STATE - estado concreto "Cerrado".
 * Sus transiciones pertenecen a otros casos de uso: no redefine ningun evento del CU 28.
 */
public class Cerrado extends EstadoBolsin {

    @Override
    public String getNombre() {
        return "Cerrado";
    }
}
