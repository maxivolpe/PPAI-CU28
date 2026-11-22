package bolsines.entity;

/**
 * Base de los errores que el sistema le informa al usuario porque violó una regla del
 * caso de uso. Todo lo demás (NullPointerException, errores de base de datos…) es un
 * defecto del sistema y los adaptadores no deben presentarlo como un error de validación.
 */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
