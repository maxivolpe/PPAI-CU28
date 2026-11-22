package bolsines.web;

import bolsines.boundary.Email;
import bolsines.boundary.PantallaRecepcionWeb;
import bolsines.boundary.ResumenDeBolsin;
import bolsines.control.ConsultarEstadosGuardados;
import bolsines.control.RepositorioDeBolsines;
import bolsines.control.RepositorioDeEmpleados;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduce los pedidos de la página (/api/...) a mensajes al GestorRecepcion de esa sesión y
 * devuelve la "vista" que el gestor dejó en su PantallaRecepcionWeb. No tiene lógica de negocio.
 */
final class ControladorRecepcionWeb {

    private final RepositorioDeBolsines repositorioDeBolsines;
    private final RepositorioDeEmpleados repositorioDeEmpleados;
    private final ConsultarEstadosGuardados consultarEstadosGuardados;

    ControladorRecepcionWeb(RepositorioDeBolsines repositorioDeBolsines, RepositorioDeEmpleados repositorioDeEmpleados) {
        this.repositorioDeBolsines = repositorioDeBolsines;
        this.repositorioDeEmpleados = repositorioDeEmpleados;
        this.consultarEstadosGuardados = new ConsultarEstadosGuardados(repositorioDeBolsines);
    }

    void registrarRutas(EnrutadorApi enrutador) {
        enrutador.agregar("GET /api/estado", (sesion, parametros) -> estado(sesion));
        enrutador.agregar("POST /api/sesion", this::iniciarSesion);
        enrutador.agregar("POST /api/recepcion/iniciar", this::iniciarRecepcion);          // paso 1 del CU
        enrutador.agregar("GET /api/recepcion/filtro", this::filtrar);                     // paso 4, A1 y A2
        enrutador.agregar("POST /api/recepcion/seleccion", this::seleccionarBolsin);       // paso 5
        enrutador.agregar("POST /api/recepcion/opcion", this::elegirOpcion);               // paso 8, A3, A4 y A5
        enrutador.agregar("POST /api/recepcion/confirmacion", this::confirmar);            // paso 10, A6
        enrutador.agregar("POST /api/recepcion/cancelar", this::cancelar);                 // observación 1
        enrutador.agregar("GET /api/consulta", (sesion, parametros) -> consultarEstadosGuardados.consultar());
    }

    // ---------------------------------------------------------------- pasos del caso de uso

    private Object iniciarSesion(SesionWeb sesion, ParametrosHttp parametros) {
        sesion.iniciar(repositorioDeBolsines, repositorioDeEmpleados, parametros.texto("usuario"));
        return estado(sesion);
    }

    private Object iniciarRecepcion(SesionWeb sesion, ParametrosHttp parametros) {
        sesion.exigirPaso("inicio", "exito");
        sesion.getPantalla().habilitarPantalla();
        sesion.getGestor().opcRegistrarRecepcion(sesion.getPantalla());
        return estado(sesion);
    }

    private Object seleccionarBolsin(SesionWeb sesion, ParametrosHttp parametros) {
        sesion.exigirPaso("seleccionBolsin");
        sesion.getPantalla().limpiarMensaje();
        sesion.getGestor().tomarSeleccionBolsin(parametros.entero("bolsin"));
        return estado(sesion);
    }

    private Object elegirOpcion(SesionWeb sesion, ParametrosHttp parametros) {
        sesion.exigirPaso("opcion");
        sesion.getPantalla().limpiarMensaje();
        sesion.getGestor().tomarOpcRecepcion(OpcionRecepcion.desdeNumero(parametros.entero("numero")),
                parametros.numeros("documentacion"));
        return estado(sesion);
    }

    private Object confirmar(SesionWeb sesion, ParametrosHttp parametros) {
        sesion.exigirPaso("confirmacion");
        sesion.getPantalla().limpiarMensaje();
        int emailsPrevios = sesion.getServidorCorreo().getEmailsEnviados().size();
        sesion.getGestor().tomarConfirmacion("true".equals(parametros.texto("confirma")));
        Map<String, Object> estado = estado(sesion);
        estado.put("emails", emailsDesde(sesion, emailsPrevios));
        return estado;
    }

    private Object cancelar(SesionWeb sesion, ParametrosHttp parametros) {
        sesion.exigirPaso("seleccionBolsin", "opcion", "confirmacion");
        sesion.getGestor().cancelarCasoDeUso();
        sesion.getPantalla().volverAlInicio();
        return estado(sesion);
    }

    /** Aplica los filtros del gestor. Sin resultados informa qué flujo alternativo ocurrió. */
    private Object filtrar(SesionWeb sesion, ParametrosHttp parametros) {
        sesion.exigirPaso("seleccionBolsin");
        List<Map<String, Object>> bolsines = null;
        String flujoAlternativo = null;

        if (parametros.tiene("precinto")) {
            bolsines = PantallaRecepcionWeb.bolsines(filtrarPorPrecinto(sesion, parametros.texto("precinto")));
            if (bolsines.isEmpty()) {
                flujoAlternativo = "A1";
            }
        }
        if (parametros.tiene("cmOrigen")) {
            List<Map<String, Object>> porOrigen = PantallaRecepcionWeb.bolsines(
                    sesion.getGestor().tomarFiltroCMOrigen(parametros.texto("cmOrigen")));
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
        respuesta.put("bolsines", bolsines == null ? sesion.getPantalla().getVista().get("bolsines") : bolsines);
        respuesta.put("flujoAlternativo", flujoAlternativo);
        return respuesta;
    }

    private List<ResumenDeBolsin> filtrarPorPrecinto(SesionWeb sesion, String precinto) {
        try {
            return sesion.getGestor().tomarFiltroPrecinto(Integer.parseInt(precinto.trim()));
        } catch (NumberFormatException e) {
            return List.of();   // un precinto que no es número no coincide con ninguno: A1
        }
    }

    // ---------------------------------------------------------------- respuestas

    /** Lo que la página necesita para dibujarse: usuarios, sesión actual y vista del CU. */
    private Map<String, Object> estado(SesionWeb sesion) {
        List<Map<String, Object>> empleados = new ArrayList<>();
        for (Empleado empleado : repositorioDeEmpleados.buscarEmpleados()) {
            empleados.add(datos(empleado));
        }
        Empleado logueado = sesion.getEmpleadoLogueado();
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("empleados", empleados);
        estado.put("sesion", logueado == null ? null : datos(logueado));
        estado.put("vista", sesion.getPantalla().getVista());
        return estado;
    }

    private Map<String, Object> datos(Empleado empleado) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("usuario", empleado.getUsuario().getNombre());
        datos.put("nombre", empleado.getNombre() + " " + empleado.getApellido());
        datos.put("cm", empleado.getCM().getNombre());
        return datos;
    }

    /** Los emails enviados a partir de la posición {@code desde}, con las claves que espera la página. */
    private List<Map<String, Object>> emailsDesde(SesionWeb sesion, int desde) {
        List<Map<String, Object>> emails = new ArrayList<>();
        List<Email> enviados = sesion.getServidorCorreo().getEmailsEnviados();
        for (Email email : enviados.subList(desde, enviados.size())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("para", email.destinatario());
            item.put("asunto", email.asunto());
            item.put("cuerpo", email.cuerpo());
            emails.add(item);
        }
        return emails;
    }
}
