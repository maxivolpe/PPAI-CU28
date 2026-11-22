package bolsines.web;

import com.sun.net.httpserver.HttpExchange;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Una {@link SesionWeb} por navegador, para que dos personas puedan usar el sistema a la vez
 * sin pisarse: cada una tiene su gestor y su pantalla. El navegador se reconoce por la cookie
 * "sesion", que el servidor le entrega en su primer pedido a la API.
 *
 * Las sesiones viven mientras el servidor esté encendido (no vencen): alcanza para usar el
 * sistema en la presentación, no para un servidor que quede prendido meses.
 */
final class SesionesWeb {

    private static final String COOKIE = "sesion";

    private final Map<String, SesionWeb> sesiones = new ConcurrentHashMap<>();

    /** La sesión del navegador que hizo el pedido. Si todavía no tenía, se crea y se le envía la cookie. */
    SesionWeb de(HttpExchange pedido) {
        String id = idEnLaCookie(pedido);
        if (id != null && sesiones.containsKey(id)) {
            return sesiones.get(id);
        }
        String idNuevo = UUID.randomUUID().toString();
        SesionWeb nueva = new SesionWeb();
        sesiones.put(idNuevo, nueva);
        pedido.getResponseHeaders().add("Set-Cookie", COOKIE + "=" + idNuevo + "; Path=/; HttpOnly; SameSite=Strict");
        return nueva;
    }

    /** El valor de la cookie "sesion" del pedido, o null si no la trae. */
    private String idEnLaCookie(HttpExchange pedido) {
        for (String encabezado : pedido.getRequestHeaders().getOrDefault("Cookie", List.of())) {
            for (String cookie : encabezado.split(";")) {
                String[] nombreYValor = cookie.trim().split("=", 2);
                if (nombreYValor.length == 2 && nombreYValor[0].equals(COOKIE)) {
                    return nombreYValor[1];
                }
            }
        }
        return null;
    }
}
