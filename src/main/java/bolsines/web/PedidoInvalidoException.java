package bolsines.web;

/** El pedido HTTP no se puede atender tal como llegó (ruta inexistente, parámetro faltante, paso fuera de turno…). Se informa como 400. */
class PedidoInvalidoException extends RuntimeException {

    PedidoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
