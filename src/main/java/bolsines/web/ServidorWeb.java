package bolsines.web;

import bolsines.boundary.InterfazServidorCorreo;
import bolsines.boundary.PantallaRecepcionWeb;
import bolsines.control.GestorRecepcion;
import bolsines.entity.Bolsin;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.CambioEstadoDocumentacion;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Remito;
import bolsines.entity.Sesion;
import bolsines.entity.TransicionNoPermitidaException;
import bolsines.persistencia.BaseDeDatos;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Servidor HTTP (el que trae Java, sin frameworks). Hace dos cosas:
 *  - entrega los archivos de la página (carpeta resources/static);
 *  - recibe los pedidos de la página (/api/...) y los traduce a mensajes al GestorRecepcion.
 * Después de cada pedido devuelve la "vista" que el gestor dejó en PantallaRecepcionWeb.
 *
 * Atiende a un único usuario a la vez: alcanza para la demo del caso de uso.
 */
public class ServidorWeb {

    private static final System.Logger LOG = System.getLogger(ServidorWeb.class.getName());

    private final BaseDeDatos baseDeDatos;
    private final HttpServer servidor;
    private final PantallaRecepcionWeb pantalla = new PantallaRecepcionWeb();
    /** Tabla de rutas: "MÉTODO /ruta" -> qué hacer con los parámetros del pedido. */
    private final Map<String, Function<Map<String, String>, Object>> rutas = new LinkedHashMap<>();

    private Empleado empleadoLogueado;
    private InterfazServidorCorreo servidorCorreo;
    private GestorRecepcion gestor;

    public ServidorWeb(BaseDeDatos baseDeDatos, int puerto) throws IOException {
        this.baseDeDatos = baseDeDatos;
        this.servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        this.servidor.createContext("/", this::atender);
        registrarRutas();
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

    /** Agregar un pedido nuevo a la API es agregar una línea acá. */
    private void registrarRutas() {
        rutas.put("GET /api/estado", parametros -> estado());
        rutas.put("POST /api/sesion", this::iniciarSesion);
        rutas.put("POST /api/recepcion/iniciar", this::iniciarRecepcion);        // paso 1 del CU
        rutas.put("GET /api/recepcion/filtro", this::filtrar);                   // paso 4, alternativos A1 y A2
        rutas.put("POST /api/recepcion/seleccion", this::seleccionarBolsin);     // paso 5
        rutas.put("POST /api/recepcion/opcion", this::elegirOpcion);             // paso 8, alternativos A3, A4 y A5
        rutas.put("POST /api/recepcion/confirmacion", this::confirmar);          // paso 10, alternativo A6
        rutas.put("POST /api/recepcion/cancelar", this::cancelar);               // observación 1
        rutas.put("GET /api/consulta", parametros -> consultarEstados());
    }

    // ---------------------------------------------------------------- atención de pedidos

    private void atender(HttpExchange pedido) throws IOException {
        String ruta = pedido.getRequestURI().getPath();
        if (!ruta.startsWith("/api/")) {
            entregarArchivo(pedido, ruta);
            return;
        }
        try {
            Object respuesta;
            synchronized (this) {
                respuesta = atenderApi(pedido.getRequestMethod() + " " + ruta, parametros(pedido));
            }
            responderJson(pedido, 200, respuesta);
        } catch (IllegalArgumentException | IllegalStateException | TransicionNoPermitidaException e) {
            // Validaciones del gestor, pasos fuera de orden y transiciones no permitidas: se informan a la página.
            responderJson(pedido, 400, Map.of("error", e.getMessage() == null ? "Pedido no válido." : e.getMessage()));
        } catch (RuntimeException e) {
            // Cualquier otra cosa es un defecto del sistema: se registra y no se detalla al usuario.
            LOG.log(System.Logger.Level.ERROR, "Error inesperado en " + ruta, e);
            responderJson(pedido, 500, Map.of("error", "Error interno del servidor."));
        }
    }

    private Object atenderApi(String pedido, Map<String, String> parametros) {
        Function<Map<String, String>, Object> manejador = rutas.get(pedido);
        if (manejador == null) {
            throw new IllegalArgumentException("Pedido inexistente: " + pedido);
        }
        return manejador.apply(parametros);
    }

    // ---------------------------------------------------------------- pasos del caso de uso

    private Object iniciarSesion(Map<String, String> parametros) {
        crearSesionPara(parametros.get("usuario"));
        return estado();
    }

    private Object iniciarRecepcion(Map<String, String> parametros) {
        exigirPaso("inicio", "exito");
        pantalla.habilitarPantalla();
        gestor.opcRegistrarRecepcion(pantalla);
        return estado();
    }

    private Object seleccionarBolsin(Map<String, String> parametros) {
        exigirPaso("seleccionBolsin");
        pantalla.limpiarMensaje();
        gestor.tomarSeleccionBolsin(entero(parametros, "bolsin"));
        return estado();
    }

    private Object elegirOpcion(Map<String, String> parametros) {
        exigirPaso("opcion");
        pantalla.limpiarMensaje();
        gestor.tomarOpcRecepcion(OpcionRecepcion.desdeNumero(entero(parametros, "numero")),
                numeros(parametros.get("documentacion")));
        return estado();
    }

    private Object confirmar(Map<String, String> parametros) {
        exigirPaso("confirmacion");
        pantalla.limpiarMensaje();
        int emailsPrevios = servidorCorreo.getEmailsEnviados().size();
        gestor.tomarConfirmacion("true".equals(parametros.get("confirma")));
        Map<String, Object> estado = estado();
        estado.put("emails", emails(emailsPrevios));
        return estado;
    }

    private Object cancelar(Map<String, String> parametros) {
        exigirPaso("seleccionBolsin", "opcion", "confirmacion");
        gestor.cancelarCasoDeUso();
        pantalla.volverAlInicio();
        return estado();
    }

    // ---------------------------------------------------------------- sesión

    /** Simula el login (CU 5): crea la Sesión del empleado elegido y un gestor para ella. */
    private void crearSesionPara(String nombreUsuario) {
        for (Empleado empleado : baseDeDatos.buscarEmpleados()) {
            if (empleado.getUsuario().getNombre().equals(nombreUsuario)) {
                empleadoLogueado = empleado;
                Sesion sesion = new Sesion(LocalDateTime.now(), empleado.getUsuario());
                servidorCorreo = new InterfazServidorCorreo(System.out);
                gestor = new GestorRecepcion(baseDeDatos, sesion, servidorCorreo);
                pantalla.habilitarPantalla();
                return;
            }
        }
        throw new IllegalArgumentException("No existe el usuario " + nombreUsuario + ".");
    }

    private void exigirPaso(String... pasosPermitidos) {
        if (gestor == null) {
            throw new IllegalStateException("Primero hay que iniciar sesión.");
        }
        for (String paso : pasosPermitidos) {
            if (paso.equals(pantalla.getPaso())) {
                return;
            }
        }
        throw new IllegalStateException("La acción no corresponde a este momento del caso de uso.");
    }

    // ---------------------------------------------------------------- filtros (paso 4, A1 y A2)

    /** Aplica los filtros del gestor. Sin resultados informa qué flujo alternativo ocurrió. */
    private Object filtrar(Map<String, String> parametros) {
        exigirPaso("seleccionBolsin");
        String precinto = parametros.get("precinto");
        String cmOrigen = parametros.get("cmOrigen");
        List<Map<String, Object>> porPrecinto = tieneValor(precinto) ? filtrarPorPrecinto(precinto) : null;
        List<Map<String, Object>> porOrigen = tieneValor(cmOrigen)
                ? PantallaRecepcionWeb.bolsines(gestor.tomarFiltroCMOrigen(cmOrigen)) : null;

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("bolsines", combinar(porPrecinto, porOrigen));
        respuesta.put("flujoAlternativo", flujoAlternativo(porPrecinto, porOrigen));
        return respuesta;
    }

    /** Un precinto que no es número no coincide con ninguno (A1). */
    private List<Map<String, Object>> filtrarPorPrecinto(String precinto) {
        try {
            return PantallaRecepcionWeb.bolsines(gestor.tomarFiltroPrecinto(Integer.parseInt(precinto.trim())));
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }
    }

    /** Los bolsines que cumplen los filtros pedidos; sin filtros, el listado completo. */
    private Object combinar(List<Map<String, Object>> porPrecinto, List<Map<String, Object>> porOrigen) {
        if (porPrecinto == null && porOrigen == null) {
            return pantalla.getVista().get("bolsines");
        }
        if (porPrecinto == null) {
            return porOrigen;
        }
        if (porOrigen != null) {
            porPrecinto.retainAll(porOrigen);   // cumplen los dos filtros
        }
        return porPrecinto;
    }

    private String flujoAlternativo(List<Map<String, Object>> porPrecinto, List<Map<String, Object>> porOrigen) {
        if (porPrecinto != null && porPrecinto.isEmpty()) {
            return "A1";
        }
        if (porOrigen != null && porOrigen.isEmpty()) {
            return "A2";
        }
        return null;
    }

    // ---------------------------------------------------------------- respuestas

    /** Lo que la página necesita para dibujarse: usuarios, sesión actual y vista del CU. */
    private Map<String, Object> estado() {
        List<Map<String, Object>> empleados = new ArrayList<>();
        for (Empleado empleado : baseDeDatos.buscarEmpleados()) {
            empleados.add(datos(empleado));
        }
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("empleados", empleados);
        estado.put("sesion", empleadoLogueado == null ? null : datos(empleadoLogueado));
        estado.put("vista", pantalla.getVista());
        return estado;
    }

    private Map<String, Object> datos(Empleado empleado) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("usuario", empleado.getUsuario().getNombre());
        datos.put("nombre", empleado.getNombre() + " " + empleado.getApellido());
        datos.put("cm", empleado.getCM().getNombre());
        return datos;
    }

    private List<Map<String, Object>> emails(int desde) {
        List<Map<String, Object>> emails = new ArrayList<>();
        List<String[]> enviados = servidorCorreo.getEmailsEnviados();
        for (String[] email : enviados.subList(desde, enviados.size())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("para", email[0]);
            item.put("asunto", email[1]);
            item.put("cuerpo", email[2]);
            emails.add(item);
        }
        return emails;
    }

    // ---------------------------------------------------------------- consulta de estados

    /**
     * No es parte del CU 28: lee todo de la base de datos para verificar cómo quedaron
     * guardados los estados y sus historiales (con fecha y responsable).
     */
    private List<Map<String, Object>> consultarEstados() {
        List<Map<String, Object>> bolsines = new ArrayList<>();
        for (Bolsin bolsin : baseDeDatos.buscarBolsines()) {
            bolsines.add(vistaDeBolsin(bolsin));
        }
        return bolsines;
    }

    private Map<String, Object> vistaDeBolsin(Bolsin bolsin) {
        List<Map<String, Object>> cambios = new ArrayList<>();
        for (CambioEstadoBolsin cambio : bolsin.getCambioEstados()) {
            cambios.add(cambio(cambio.getFechaHoraInicio(), cambio.getFechaHoraFin(),
                    cambio.getEstado().getNombre(), cambio.getResponsable()));
        }
        List<Map<String, Object>> remitos = new ArrayList<>();
        for (Remito remito : bolsin.getRemitos()) {
            remitos.add(vistaDeRemito(remito));
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("numero", bolsin.getNumeroBolsin());
        item.put("cmOrigen", bolsin.getCMOrigen());
        item.put("cmDestino", bolsin.getDestino().getNombre());
        item.put("precinto", bolsin.getNumPrecinto());
        item.put("estado", bolsin.getEstadoActual().getNombre());
        item.put("cambios", cambios);
        item.put("remitos", remitos);
        return item;
    }

    private Map<String, Object> vistaDeRemito(Remito remito) {
        List<Map<String, Object>> documentaciones = new ArrayList<>();
        for (DetalleRemito detalle : remito.getDetallesRemito()) {
            documentaciones.add(vistaDeDocumentacion(detalle.getDocumentacion()));
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("numero", remito.getNumero());
        item.put("estado", remito.getEstadoActual().getNombre());
        item.put("documentacion", documentaciones);
        return item;
    }

    private Map<String, Object> vistaDeDocumentacion(Documentacion documentacion) {
        List<Map<String, Object>> cambios = new ArrayList<>();
        for (CambioEstadoDocumentacion cambio : documentacion.getCambioEstadoDocumentacion()) {
            cambios.add(cambio(cambio.getFechaHoraInicio(), cambio.getFechaHoraFin(),
                    cambio.getEstado().getNombre(), cambio.getResponsable()));
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("numero", documentacion.getNumero());
        item.put("asunto", documentacion.getAsunto());
        item.put("tipo", documentacion.getTipoDocumentacion());
        item.put("estado", documentacion.getEstadoActual().getNombre());
        item.put("cambios", cambios);
        return item;
    }

    private Map<String, Object> cambio(LocalDateTime inicio, LocalDateTime fin, String estado, Empleado responsable) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("inicio", inicio.toString());
        item.put("fin", fin == null ? null : fin.toString());
        item.put("estado", estado);
        item.put("responsable", responsable.getNombre() + " " + responsable.getApellido());
        return item;
    }

    // ---------------------------------------------------------------- utilidades HTTP

    /** Parámetros que llegan en la dirección: /api/algo?nombre=valor&otro=valor. */
    private Map<String, String> parametros(HttpExchange pedido) {
        Map<String, String> parametros = new LinkedHashMap<>();
        String consulta = pedido.getRequestURI().getRawQuery();
        if (consulta != null) {
            for (String par : consulta.split("&")) {
                int igual = par.indexOf('=');
                if (igual > 0) {
                    parametros.put(URLDecoder.decode(par.substring(0, igual), StandardCharsets.UTF_8),
                            URLDecoder.decode(par.substring(igual + 1), StandardCharsets.UTF_8));
                }
            }
        }
        return parametros;
    }

    private boolean tieneValor(String texto) {
        return texto != null && !texto.isBlank();
    }

    private int entero(Map<String, String> parametros, String nombre) {
        try {
            return Integer.parseInt(parametros.get(nombre).trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Falta el número '" + nombre + "'.");
        }
    }

    /** "5001,5002" -> {5001, 5002}. */
    private Set<Integer> numeros(String texto) {
        Set<Integer> numeros = new LinkedHashSet<>();
        if (tieneValor(texto)) {
            for (String parte : texto.split(",")) {
                try {
                    numeros.add(Integer.valueOf(parte.trim()));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Número de documentación no válido: " + parte);
                }
            }
        }
        return numeros;
    }

    private void entregarArchivo(HttpExchange pedido, String ruta) throws IOException {
        String recurso = recursoDeLaPagina(ruta);
        InputStream contenido = recurso == null ? null : ServidorWeb.class.getResourceAsStream(recurso);
        if (contenido == null) {
            responder(pedido, 404, "text/plain; charset=utf-8", "No encontrado".getBytes(StandardCharsets.UTF_8));
            return;
        }
        try (contenido) {
            responder(pedido, 200, tipoDeContenido(recurso), contenido.readAllBytes());
        }
    }

    /** La ruta del archivo dentro de /static, o null si el pedido intenta salir de esa carpeta (por ejemplo con ".."). */
    private String recursoDeLaPagina(String ruta) {
        String archivo = ruta.equals("/") ? "/index.html" : ruta;
        try {
            String normalizada = URI.create("/static" + archivo).normalize().getPath();
            return normalizada != null && normalizada.startsWith("/static/") ? normalizada : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String tipoDeContenido(String archivo) {
        if (archivo.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (archivo.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (archivo.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        return "application/octet-stream";
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
