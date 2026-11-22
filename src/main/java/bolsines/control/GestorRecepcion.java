package bolsines.control;

import bolsines.boundary.InterfazServidorCorreo;
import bolsines.boundary.PantallaRecepcion;
import bolsines.entity.Bolsin;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.ComisionMedica;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Remito;
import bolsines.entity.Sesion;
import bolsines.entity.Usuario;
import bolsines.persistencia.BaseDeDatos;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gestor (controlador) del CU 28 "Registrar recepción de bolsín".
 *
 * PATRÓN STATE - rol CLIENTE: el gestor no sabe qué estados existen ni cuál sigue a cuál.
 * En el análisis buscaba los estados destino (buscarEstados()) y se los pasaba a las
 * entidades; con el rediseño ese método desaparece: le pide al Bolsín (contexto) que
 * registre su recepción y son los estados concretos los que crean el estado siguiente.
 */
public class GestorRecepcion {

    private final BaseDeDatos baseDeDatos;
    private final Sesion sesion;
    private final InterfazServidorCorreo servidorCorreo;
    private PantallaRecepcion pantalla;

    private Usuario usuarioActual;
    private Empleado responsable;
    private ComisionMedica cmEmpleado;
    private String nombreCMEmpleado;
    private List<Bolsin> bolsinesDelCMDestinoEnviados = new ArrayList<>();
    private Bolsin bolsinSeleccionado;
    private OpcionRecepcion opcionSeleccionada;
    private Set<Integer> documentacionMarcada = new LinkedHashSet<>();
    private LocalDateTime fechaHoraActual;
    private CambioEstadoBolsin cambioEstadoActual;

    public GestorRecepcion(BaseDeDatos baseDeDatos, Sesion sesion, InterfazServidorCorreo servidorCorreo) {
        this.baseDeDatos = baseDeDatos;
        this.sesion = sesion;
        this.servidorCorreo = servidorCorreo;
    }

    // ---------------------------------------------------------------- pasos 1 a 4

    public void opcRegistrarRecepcion(PantallaRecepcion pantalla) {
        this.pantalla = pantalla;
        buscarCMyMostrar();
        buscarBolsinesEnviados();
        if (bolsinesDelCMDestinoEnviados.isEmpty()) {
            pantalla.mostrarMensaje("No hay bolsines en estado Enviado para " + nombreCMEmpleado + ".");
            finCasoDeUso();
            return;
        }
        pantalla.solicitarSeleccionBolsin(listar(bolsinesDelCMDestinoEnviados));
    }

    /** Sesión -> Usuario -> Empleado -> Comisión Médica. */
    private void buscarCMyMostrar() {
        usuarioActual = sesion.getUsuario();
        for (Empleado empleado : baseDeDatos.buscarEmpleados()) {
            if (empleado.esTuUsuario(usuarioActual)) {
                responsable = empleado;
                cmEmpleado = empleado.getCM();
                nombreCMEmpleado = cmEmpleado.getNombre();
            }
        }
        if (responsable == null) {
            throw new IllegalStateException("El usuario logueado no tiene un empleado asociado.");
        }
        pantalla.mostrarCM(nombreCMEmpleado);
    }

    /** Bolsines cuya CM destino es la del empleado logueado y que están "Enviado". */
    private void buscarBolsinesEnviados() {
        bolsinesDelCMDestinoEnviados = new ArrayList<>();
        for (Bolsin bolsin : baseDeDatos.buscarBolsines()) {
            if (bolsin.esTuCMDestino(cmEmpleado) && bolsin.sosEnviado()) {
                bolsinesDelCMDestinoEnviados.add(bolsin);
            }
        }
    }

    /** Filtro por número de precinto. Lista vacía = flujo alternativo A1. */
    public List<String[]> tomarFiltroPrecinto(int numeroPrecinto) {
        List<Bolsin> encontrados = new ArrayList<>();
        for (Bolsin bolsin : bolsinesDelCMDestinoEnviados) {
            if (bolsin.getNumPrecinto() == numeroPrecinto) {
                encontrados.add(bolsin);
            }
        }
        return listar(encontrados);
    }

    /** Filtro por CM de origen (parte del nombre). Lista vacía = flujo alternativo A2. */
    public List<String[]> tomarFiltroCMOrigen(String nombreCMOrigen) {
        List<Bolsin> encontrados = new ArrayList<>();
        for (Bolsin bolsin : bolsinesDelCMDestinoEnviados) {
            if (bolsin.getCMOrigen().toLowerCase().contains(nombreCMOrigen.trim().toLowerCase())) {
                encontrados.add(bolsin);
            }
        }
        return listar(encontrados);
    }

    /** Por cada bolsín: {número de bolsín, CM origen, número de precinto}. */
    private List<String[]> listar(List<Bolsin> bolsines) {
        List<String[]> listado = new ArrayList<>();
        for (Bolsin bolsin : bolsines) {
            listado.add(new String[]{
                    String.valueOf(bolsin.getNumeroBolsin()),
                    bolsin.getCMOrigen(),
                    String.valueOf(bolsin.getNumPrecinto())});
        }
        return listado;
    }

    // ---------------------------------------------------------------- pasos 5 a 7

    public void tomarSeleccionBolsin(int numeroBolsin) {
        bolsinSeleccionado = null;
        for (Bolsin bolsin : bolsinesDelCMDestinoEnviados) {
            if (bolsin.getNumeroBolsin() == numeroBolsin) {
                bolsinSeleccionado = bolsin;
            }
        }
        if (bolsinSeleccionado == null) {
            throw new IllegalArgumentException("El bolsín " + numeroBolsin + " no está en el listado.");
        }
        buscarInformacionBolsin();
        pantalla.solicitarOpcRecepcion(cmEmpleado.sosCentral());
    }

    private void buscarInformacionBolsin() {
        Map<Integer, List<String[]>> informacionBolsin = bolsinSeleccionado.obtenerInformacionRemito();
        pantalla.mostrarInfoBolsin(bolsinSeleccionado.getNumeroBolsin(), bolsinSeleccionado.getCMOrigen(),
                bolsinSeleccionado.getNumPrecinto(), informacionBolsin);
    }

    // ---------------------------------------------------------------- pasos 8 a 10

    /**
     * @param documentacionMarcada números de la documentación no recibida (opción 2), que no
     *                             corresponde (opción 3) o a redirigir (opción 4). Vacío en la opción 1.
     * @throws IllegalArgumentException si la opción no es aplicable; no cambia nada.
     */
    public void tomarOpcRecepcion(OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        validarOpcRecepcion(opcion, documentacionMarcada);
        this.opcionSeleccionada = opcion;
        this.documentacionMarcada = new LinkedHashSet<>(documentacionMarcada);
        pantalla.solicitarConfirmacion(opcion, this.documentacionMarcada);
    }

    private void validarOpcRecepcion(OpcionRecepcion opcion, Set<Integer> marcada) {
        if (!opcion.requiereSeleccionDeDocumentacion()) {
            if (!marcada.isEmpty()) {
                throw new IllegalArgumentException("La opción 1 no lleva documentación seleccionada.");
            }
            return;
        }
        if (opcion == OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR && !cmEmpleado.sosCentral()) {
            throw new IllegalArgumentException(
                    "La opción 4 solo aplica cuando la CM destino es la Comisión Médica Central.");
        }
        if (marcada.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos una documentación para esta opción.");
        }
        Set<Integer> numerosDelBolsin = new LinkedHashSet<>();
        for (Remito remito : bolsinSeleccionado.getRemitos()) {
            for (DetalleRemito detalle : remito.getDetallesRemito()) {
                numerosDelBolsin.add(detalle.getDocumentacion().getNumero());
            }
        }
        for (Integer numero : marcada) {
            if (!numerosDelBolsin.contains(numero)) {
                throw new IllegalArgumentException("La documentación " + numero + " no pertenece al bolsín.");
            }
        }
    }

    public void tomarConfirmacion(boolean confirma) {
        if (!confirma) {
            // Flujo alternativo A6: no se registra nada y se vuelve a pedir la opción.
            pantalla.mostrarMensaje("No se confirmó la selección. Elija nuevamente la opción de recepción.");
            pantalla.solicitarOpcRecepcion(cmEmpleado.sosCentral());
            return;
        }
        fechaHoraActual = getFechaYHoraActual();
        registrarBolsinRecibido();
        llamarCasoDeUsoNotificarRecepcionDeBolsin();
        pantalla.mostrarMensajeExito(bolsinSeleccionado.getNumeroBolsin(),
                bolsinSeleccionado.getEstadoActual().getNombre());
        finCasoDeUso();
    }

    // ---------------------------------------------------------------- paso 11

    private LocalDateTime getFechaYHoraActual() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * PATRÓN STATE: acá empieza el rediseño. Una sola llamada al contexto (Bolsín); el
     * cambio de estado del bolsín, de sus remitos y de su documentación lo resuelven los
     * estados concretos. Recién cuando todo terminó bien se guarda en la base de datos.
     */
    private void registrarBolsinRecibido() {
        cambioEstadoActual = bolsinSeleccionado.buscarCambioEstadoActual();
        bolsinSeleccionado.registrarBolsinRecibido(fechaHoraActual, responsable, cambioEstadoActual,
                opcionSeleccionada, documentacionMarcada);
        baseDeDatos.guardarRecepcion(bolsinSeleccionado);
    }

    // ---------------------------------------------------------------- paso 12

    /** Inclusión del CU 29: un email a cada CM de origen con la situación de su documentación. */
    private void llamarCasoDeUsoNotificarRecepcionDeBolsin() {
        Map<ComisionMedica, StringBuilder> cuerpoPorCMOrigen = new LinkedHashMap<>();
        for (Remito remito : bolsinSeleccionado.getRemitos()) {
            StringBuilder cuerpo = cuerpoPorCMOrigen.computeIfAbsent(remito.getOrigen(), cm -> new StringBuilder());
            cuerpo.append("  Remito ").append(remito.getNumero())
                    .append(": ").append(remito.getEstadoActual().getNombre()).append('\n');
            for (DetalleRemito detalle : remito.getDetallesRemito()) {
                Documentacion documentacion = detalle.getDocumentacion();
                cuerpo.append("    Doc. ").append(documentacion.getNumero())
                        .append(" - ").append(documentacion.getAsunto())
                        .append(": ").append(documentacion.getEstadoActual().getNombre()).append('\n');
            }
        }
        for (Map.Entry<ComisionMedica, StringBuilder> entrada : cuerpoPorCMOrigen.entrySet()) {
            servidorCorreo.enviarEmail(entrada.getKey().getEmail(),
                    "Recepción del bolsín " + bolsinSeleccionado.getNumeroBolsin() + " en " + nombreCMEmpleado,
                    entrada.getValue().toString());
        }
    }

    // ---------------------------------------------------------------- fin y cancelación

    /** Observación 1: el EB puede cancelar en cualquier momento; no se guarda nada. */
    public void cancelarCasoDeUso() {
        pantalla.mostrarMensaje("Caso de uso cancelado. No se registró ningún cambio.");
        finCasoDeUso();
    }

    private void finCasoDeUso() {
        bolsinesDelCMDestinoEnviados = new ArrayList<>();
        bolsinSeleccionado = null;
        opcionSeleccionada = null;
        documentacionMarcada = new LinkedHashSet<>();
        fechaHoraActual = null;
        cambioEstadoActual = null;
    }
}
