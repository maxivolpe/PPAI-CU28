package bolsines.entity.estado.documentacion;

/**
 * PATRON STATE - estado concreto "Registrada".
 * Sus transiciones pertenecen a otros casos de uso: no redefine ningun evento del CU 28.
 */
public class Registrada extends EstadoDocumentacion {

    @Override
    public String getNombre() {
        return "Registrada";
    }
}
