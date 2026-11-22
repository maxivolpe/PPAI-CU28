package bolsines.entity.estado.documentacion;

/**
 * PATRON STATE - estado concreto "En Remito".
 * Sus transiciones pertenecen a otros casos de uso: no redefine ningun evento del CU 28.
 */
public class EnRemito extends EstadoDocumentacion {

    @Override
    public String getNombre() {
        return "En Remito";
    }
}
