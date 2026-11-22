package bolsines.entity.estado.documentacion;

/**
 * PATRON STATE - estado concreto "De Baja".
 * Estado final para el CU 28: no redefine ningun evento, por lo que cualquiera es rechazado.
 */
public class DeBaja extends EstadoDocumentacion {

    @Override
    public String getNombre() {
        return "De Baja";
    }
}
