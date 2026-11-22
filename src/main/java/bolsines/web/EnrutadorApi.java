package bolsines.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tabla de rutas de la API: "MÉTODO /ruta" -> manejador. Agregar un endpoint es registrar
 * una entrada, sin tocar el enrutador.
 */
final class EnrutadorApi {

    /** Atiende un pedido de un navegador (su sesión) y devuelve el objeto que se escribe como JSON. */
    @FunctionalInterface
    interface Manejador {
        Object atender(SesionWeb sesion, ParametrosHttp parametros);
    }

    private final Map<String, Manejador> rutas = new LinkedHashMap<>();

    void agregar(String metodoYRuta, Manejador manejador) {
        rutas.put(metodoYRuta, manejador);
    }

    Object atender(String metodo, String ruta, SesionWeb sesion, ParametrosHttp parametros) {
        Manejador manejador = rutas.get(metodo + " " + ruta);
        if (manejador == null) {
            throw new PedidoInvalidoException("Pedido inexistente: " + metodo + " " + ruta);
        }
        return manejador.atender(sesion, parametros);
    }
}
