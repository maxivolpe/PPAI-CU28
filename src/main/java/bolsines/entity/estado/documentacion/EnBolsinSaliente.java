package bolsines.entity.estado.documentacion;

/**
 * PATRON STATE - estado concreto "En Bolsin Saliente".
 * Sus transiciones pertenecen a otros casos de uso: no redefine ningun evento del CU 28.
 */
public class EnBolsinSaliente extends EstadoDocumentacion {

    @Override
    public String getNombre() {
        return "En Bolsin Saliente";
    }
}
