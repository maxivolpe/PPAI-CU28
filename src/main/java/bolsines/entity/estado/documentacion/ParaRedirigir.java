package bolsines.entity.estado.documentacion;

/**
 * PATRON STATE - estado concreto "Para Redirigir".
 * Alternativo A5. La aceptacion o rechazo posterior por el area interna de la CMC queda fuera del flujo del CU 28 implementado.
 */
public class ParaRedirigir extends EstadoDocumentacion {

    @Override
    public String getNombre() {
        return "Para Redirigir";
    }
}
