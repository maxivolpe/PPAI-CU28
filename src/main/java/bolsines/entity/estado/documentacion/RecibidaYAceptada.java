package bolsines.entity.estado.documentacion;

/**
 * PATRON STATE - estado concreto "Recibida y Aceptada".
 * Estado final para el CU 28: no redefine ningun evento, por lo que cualquiera es rechazado.
 */
public class RecibidaYAceptada extends EstadoDocumentacion {

    @Override
    public String getNombre() {
        return "Recibida y Aceptada";
    }
}
