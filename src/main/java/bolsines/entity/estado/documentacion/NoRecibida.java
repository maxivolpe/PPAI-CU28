package bolsines.entity.estado.documentacion;

/**
 * PATRÓN STATE - estado concreto "No Recibida" (alternativo A3).
 * Sus transiciones de salida son del CU 31, fuera de alcance: no redefine ningún evento.
 */
public class NoRecibida extends EstadoDocumentacion {

    @Override
    public String getNombre() {
        return "No Recibida";
    }

    @Override
    public boolean esNoRecibida() {
        return true;
    }
}
