package bolsines.entity;

/**
 * Al guardar la recepción, el bolsín ya no estaba en la base en el estado en que se leyó:
 * otro usuario lo modificó mientras tanto. No se pisa su registro.
 */
public class BolsinModificadoPorOtroUsuarioException extends ReglaDeNegocioException {

    public BolsinModificadoPorOtroUsuarioException(int numeroBolsin) {
        super("El bolsín " + numeroBolsin + " fue modificado por otro usuario mientras se registraba la recepción. "
                + "Vuelva a iniciar el caso de uso.");
    }
}
