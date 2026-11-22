package bolsines.entity;

/** El bolsín que se quiso seleccionar no está entre los bolsines ofrecidos a la CM del encargado. */
public class BolsinFueraDelListadoException extends ReglaDeNegocioException {

    public BolsinFueraDelListadoException(int numeroBolsin) {
        super("El bolsín " + numeroBolsin + " no está en el listado.");
    }
}
