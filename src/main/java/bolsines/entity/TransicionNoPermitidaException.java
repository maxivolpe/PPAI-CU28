package bolsines.entity;

/**
 * PATRÓN STATE: la lanzan los estados cuando reciben un evento que no tiene transición
 * definida desde ellos en la máquina de estados (por ejemplo, registrar la recepción de
 * un bolsín que no está "Enviado").
 */
public class TransicionNoPermitidaException extends ReglaDeNegocioException {

    public TransicionNoPermitidaException(String evento, String nombreEstado) {
        super("No se puede ejecutar '" + evento + "' en el estado '" + nombreEstado + "'.");
    }
}
