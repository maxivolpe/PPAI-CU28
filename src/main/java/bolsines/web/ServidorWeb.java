package bolsines.web;

import bolsines.control.RepositorioDeBolsines;
import bolsines.control.RepositorioDeEmpleados;
import bolsines.entity.ReglaDeNegocioException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Servidor HTTP (el que trae Java, sin frameworks). Hace dos cosas:
 *  - entrega los archivos de la página (ArchivosEstaticos);
 *  - recibe los pedidos de la página (/api/...), busca la sesión del navegador que los hizo
 *    (SesionesWeb) y los deriva al EnrutadorApi, que los traduce a mensajes al GestorRecepcion
 *    de esa sesión (ControladorRecepcionWeb).
 *
 * No se le asigna un executor: HttpServer atiende los pedidos de a uno, en orden de llegada.
 */
public class ServidorWeb {

    private static final System.Logger LOG = System.getLogger(ServidorWeb.class.getName());

    private final HttpServer servidor;
    private final ArchivosEstaticos archivos = new ArchivosEstaticos();
    private final SesionesWeb sesiones = new SesionesWeb();
    private final EnrutadorApi enrutador = new EnrutadorApi();

    public ServidorWeb(RepositorioDeBolsines repositorioDeBolsines, RepositorioDeEmpleados repositorioDeEmpleados,
                       int puerto) throws IOException {
        this.servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        this.servidor.createContext("/", this::atender);
        new ControladorRecepcionWeb(repositorioDeBolsines, repositorioDeEmpleados).registrarRutas(enrutador);
    }

    public void iniciar() {
        servidor.start();
    }

    public void detener() {
        servidor.stop(0);
    }

    public int getPuerto() {
        return servidor.getAddress().getPort();
    }

    // ---------------------------------------------------------------- atención de pedidos

    private void atender(HttpExchange pedido) throws IOException {
        String ruta = pedido.getRequestURI().getPath();
        if (!ruta.startsWith("/api/")) {
            entregarArchivo(pedido, ruta);
            return;
        }
        try {
            Object respuesta = enrutador.atender(pedido.getRequestMethod(), ruta, sesiones.de(pedido),
                    ParametrosHttp.desde(pedido.getRequestURI().getRawQuery()));
            responderJson(pedido, 200, respuesta);
        } catch (ReglaDeNegocioException | PedidoInvalidoException e) {
            // Reglas del caso de uso y pedidos mal formados: se le informan a la página.
            responderJson(pedido, 400, Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            // Cualquier otra cosa es un defecto del sistema: se registra y no se detalla al usuario.
            LOG.log(System.Logger.Level.ERROR, "Error inesperado en " + ruta, e);
            responderJson(pedido, 500, Map.of("error", "Error interno del servidor."));
        }
    }

    private void entregarArchivo(HttpExchange pedido, String ruta) throws IOException {
        Optional<ArchivosEstaticos.Archivo> archivo = archivos.buscar(ruta);
        if (archivo.isEmpty()) {
            responder(pedido, 404, "text/plain; charset=utf-8", "No encontrado".getBytes(StandardCharsets.UTF_8));
            return;
        }
        responder(pedido, 200, archivo.get().tipo(), archivo.get().contenido());
    }

    private void responderJson(HttpExchange pedido, int codigo, Object cuerpo) throws IOException {
        responder(pedido, codigo, "application/json; charset=utf-8",
                Json.escribir(cuerpo).getBytes(StandardCharsets.UTF_8));
    }

    private void responder(HttpExchange pedido, int codigo, String tipo, byte[] cuerpo) throws IOException {
        pedido.getResponseHeaders().set("Content-Type", tipo);
        pedido.getResponseHeaders().set("Cache-Control", "no-store");
        pedido.sendResponseHeaders(codigo, cuerpo.length);
        pedido.getResponseBody().write(cuerpo);
        pedido.close();
    }
}
