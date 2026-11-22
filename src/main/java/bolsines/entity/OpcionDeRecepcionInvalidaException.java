package bolsines.entity;

/** La opción de recepción elegida no es aplicable (falta documentación, no es de este bolsín, o no corresponde a la CM). */
public class OpcionDeRecepcionInvalidaException extends ReglaDeNegocioException {

    public OpcionDeRecepcionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
