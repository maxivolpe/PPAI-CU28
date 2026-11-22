package bolsines.entity.estado.documentacion;

/**
 * PATRÓN STATE - estado concreto "Recibida y Rechazada" (alternativo A4).
 * Su transición de salida es del CU 31, fuera de alcance: no redefine ningún evento.
 */
public class RecibidaYRechazada extends EstadoDocumentacion {

    @Override
    public String getNombre() {
        return "Recibida y Rechazada";
    }

    @Override
    public boolean dejaRemitoIncompleto() {
        return true;
    }
}
