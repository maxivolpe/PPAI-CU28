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
import bolsines.persistencia.BaseDeDatos;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servidor HTTP (el que trae Java, sin frameworks). Hace dos cosas:
 *  - entrega los archivos de la página (carpeta resources/static);
 *  - recibe los pedidos de la página (/api/...) y los traduce a mensajes al GestorRecepcion.
 * Después de cada pedido devuelve la "vista" que el gestor dejó en PantallaRecepcionWeb.
 *
 * Atiende a un único usuario a la vez: alcanza para la demo del caso de uso.
 */
public class ServidorWeb {

    private final BaseDeDatos baseDeDatos;
    private final HttpServer servidor;
    private final PantallaRecepcionWeb pantalla = new PantallaRecepcionWeb();

    private Empleado empleadoLogueado;
    private InterfazServidorCorreo servidorCorreo;
    private GestorRecepcion gestor;

    public ServidorWeb(BaseDeDatos baseDeDatos, int puerto) throws IOException {
        this.baseDeDatos = baseDeDatos;
        this.servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        this.servidor.createContext("/", this::atender);
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
        try {
            if (!ruta.startsWith("/api/")) {
                entregarArchivo(pedido, ruta);
                return;
            }
            Object respuesta;
            synchronized (this) {
                respuesta = atenderApi(pedido.getRequestMethod() + " " + ruta, parametros(pedido));
            }
            responder(pedido, 200, "application/json", Json.escribir(respuesta).getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            // Validaciones del gestor y transiciones no permitidas: se informan a la página.
            String mensaje = e.getMessage() == null ? "Error inesperado." : e.getMessage();
            responder(pedido, 400, "application/json",
                    Json.escribir(Map.of("error", mensaje)).getBytes(StandardCharsets.UTF_8));
        }
    }

    private Object atenderApi(String pedido, Map<String, String> parametros) {
        switch (pedido) {
            case "GET /api/estado":
                return estado();

            case "POST /api/sesion":
                iniciarSesion(parametros.get("usuario"));
                return estado();

            case "POST /api/recepcion/iniciar":            // paso 1 del CU
                exigirPaso("inicio", "exito");
                pantalla.habilitarPantalla();
                gestor.opcRegistrarRecepcion(pantalla);
                return estado();

            case "GET /api/recepcion/filtro":              // paso 4, alternativos A1 y A2
                exigirPaso("seleccionBolsin");
                return filtrar(parametros.get("precinto"), parametros.get("cmOrigen"));

            case "POST /api/recepcion/seleccion":          // paso 5
                exigirPaso("seleccionBolsin");
                pantalla.limpiarMensaje();
                gestor.tomarSeleccionBolsin(entero(parametros, "bolsin"));
                return estado();

            case "POST /api/recepcion/opcion":             // paso 8, alternativos A3, A4 y A5
                exigirPaso("opcion");
                pantalla.limpiarMensaje();
                gestor.tomarOpcRecepcion(OpcionRecepcion.desdeNumero(entero(parametros, "numero")),
                        numeros(parametros.get("documentacion")));
                return estado();

            case "POST /api/recepcion/confirmacion": {     // paso 10, alternativo A6
                exigirPaso("confirmacion");
                pantalla.limpiarMensaje();
                int emailsPrevios = servidorCorreo.getEmailsEnviados().size();
                gestor.tomarConfirmacion("true".equals(parametros.get("confirma")));
                Map<String, Object> estado = estado();
                estado.put("emails", emails(emailsPrevios));
                return estado;
            }

            case "POST /api/recepcion/cancelar":           // observación 1
                exigirPaso("seleccionBolsin", "opcion", "confirmacion");
                gestor.cancelarCasoDeUso();
                pantalla.volverAlInicio();
                return estado();

            case "GET /api/consulta":
                return consultarEstados();

            default:
                throw new IllegalArgumentException("Pedido inexistente: " + pedido);
        }
    }

    // ---------------------------------------------------------------- sesión

    /** Simula el login (CU 5): crea la Sesión del empleado elegido y un gestor para ella. */
    private void iniciarSesion(String nombreUsuario) {
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

    /** Aplica los filtros del gestor. Sin resultados informa qué flujo alternativo ocurrió. */
    private Map<String, Object> filtrar(String precinto, String cmOrigen) {
        List<Map<String, Object>> bolsines = null;
        String flujoAlternativo = null;

        if (precinto != null && !precinto.isBlank()) {
            List<String[]> porPrecinto;
            try {
                porPrecinto = gestor.tomarFiltroPrecinto(Integer.parseInt(precinto.trim()));
            } catch (NumberFormatException e) {
                porPrecinto = List.of();
            }
            bolsines = PantallaRecepcionWeb.bolsines(porPrecinto);
            if (bolsines.isEmpty()) {
                flujoAlternativo = "A1";
            }
        }
        if (cmOrigen != null && !cmOrigen.isBlank()) {
            List<Map<String, Object>> porOrigen = PantallaRecepcionWeb.bolsines(gestor.tomarFiltroCMOrigen(cmOrigen));
            if (porOrigen.isEmpty() && flujoAlternativo == null) {
                flujoAlternativo = "A2";
            }
            if (bolsines == null) {
                bolsines = porOrigen;
            } else {
                bolsines.retainAll(porOrigen);   // cumplen los dos filtros
            }
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("bolsines", bolsines == null ? pantalla.getVista().get("bolsines") : bolsines);
        respuesta.put("flujoAlternativo", flujoAlternativo);
        return respuesta;
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

    /**
     * No es parte del CU 28: lee todo de la base de datos para verificar cómo quedaron
     * guardados los estados y sus historiales (con fecha y responsable).
     */
    private List<Map<String, Object>> consultarEstados() {
        List<Map<String, Object>> bolsines = new ArrayList<>();
        for (Bolsin bolsin : baseDeDatos.buscarBolsines()) {
            List<Map<String, Object>> cambiosBolsin = new ArrayList<>();
            for (CambioEstadoBolsin cambio : bolsin.getCambioEstados()) {
                cambiosBolsin.add(cambio(cambio.getFechaHoraInicio(), cambio.getFechaHoraFin(),
                        cambio.getEstado().getNombre(), cambio.getResponsable()));
            }
            List<Map<String, Object>> remitos = new ArrayList<>();
            for (Remito remito : bolsin.getRemitos()) {
                List<Map<String, Object>> documentaciones = new ArrayList<>();
                for (DetalleRemito detalle : remito.getDetallesRemito()) {
                    Documentacion documentacion = detalle.getDocumentacion();
                    List<Map<String, Object>> cambiosDocumentacion = new ArrayList<>();
                    for (CambioEstadoDocumentacion cambio : documentacion.getCambioEstadoDocumentacion()) {
                        cambiosDocumentacion.add(cambio(cambio.getFechaHoraInicio(), cambio.getFechaHoraFin(),
                                cambio.getEstado().getNombre(), cambio.getResponsable()));
                    }
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("numero", documentacion.getNumero());
                    item.put("asunto", documentacion.getAsunto());
                    item.put("tipo", documentacion.getTipoDocumentacion());
                    item.put("estado", documentacion.getEstadoActual().getNombre());
                    item.put("cambios", cambiosDocumentacion);
                    documentaciones.add(item);
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("numero", remito.getNumero());
                item.put("estado", remito.getEstadoActual().getNombre());
                item.put("documentacion", documentaciones);
                remitos.add(item);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("numero", bolsin.getNumeroBolsin());
            item.put("cmOrigen", bolsin.getCMOrigen());
            item.put("cmDestino", bolsin.getDestino().getNombre());
            item.put("precinto", bolsin.getNumPrecinto());
            item.put("estado", bolsin.getEstadoActual().getNombre());
            item.put("cambios", cambiosBolsin);
            item.put("remitos", remitos);
            bolsines.add(item);
        }
        return bolsines;
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
        if (texto != null && !texto.isBlank()) {
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
        String archivo = ruta.equals("/") ? "/index.html" : ruta;
        InputStream contenido = archivo.contains("..") ? null : ServidorWeb.class.getResourceAsStream("/static" + archivo);
        if (contenido == null) {
            responder(pedido, 404, "text/plain; charset=utf-8", "No encontrado".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String tipo = archivo.endsWith(".html") ? "text/html; charset=utf-8"
                : archivo.endsWith(".css") ? "text/css; charset=utf-8"
                : archivo.endsWith(".js") ? "text/javascript; charset=utf-8"
                : "application/octet-stream";
        try (contenido) {
            responder(pedido, 200, tipo, contenido.readAllBytes());
        }
    }

    private void responder(HttpExchange pedido, int codigo, String tipo, byte[] cuerpo) throws IOException {
        pedido.getResponseHeaders().set("Content-Type", tipo.startsWith("application/json") ? tipo + "; charset=utf-8" : tipo);
        pedido.getResponseHeaders().set("Cache-Control", "no-store");
        pedido.sendResponseHeaders(codigo, cuerpo.length);
        pedido.getResponseBody().write(cuerpo);
        pedido.close();
    }
}
