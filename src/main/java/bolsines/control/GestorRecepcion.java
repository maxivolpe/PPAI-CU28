package bolsines.control;

import bolsines.boundary.InterfazServidorCorreo;
import bolsines.boundary.PantallaRecepcion;
import bolsines.boundary.ResumenDeBolsin;
import bolsines.entity.Bolsin;
import bolsines.entity.BolsinFueraDelListadoException;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.ComisionMedica;
import bolsines.entity.DatosDeDocumentacion;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Sesion;
import bolsines.entity.Usuario;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
 *
 * Las reglas de la opción elegida y el armado de los emails del CU 29 viven en colaboradores
 * ({@link ValidadorDeOpcionDeRecepcion}, {@link NotificarRecepcionDeBolsin}); el gestor
 * conserva la secuencia del caso de uso tal como figura en los diagramas.
 *
 * La persistencia le llega como dos interfaces ({@link RepositorioDeBolsines} y
 * {@link RepositorioDeEmpleados}): el gestor no sabe que detrás hay SQLite.
 */
public class GestorRecepcion {

    private final RepositorioDeBolsines repositorioDeBolsines;
    private final RepositorioDeEmpleados repositorioDeEmpleados;
    private final Sesion sesion;
    private final NotificarRecepcionDeBolsin notificarRecepcion;
    private final Clock reloj;
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

    public GestorRecepcion(RepositorioDeBolsines repositorioDeBolsines, RepositorioDeEmpleados repositorioDeEmpleados,
                           Sesion sesion, InterfazServidorCorreo servidorCorreo) {
        this(repositorioDeBolsines, repositorioDeEmpleados, sesion, servidorCorreo, Clock.systemDefaultZone());
    }

    /** @param reloj fuente de la fecha y hora de la recepción; los tests inyectan uno fijo. */
    public GestorRecepcion(RepositorioDeBolsines repositorioDeBolsines, RepositorioDeEmpleados repositorioDeEmpleados,
                           Sesion sesion, InterfazServidorCorreo servidorCorreo, Clock reloj) {
        this.repositorioDeBolsines = repositorioDeBolsines;
        this.repositorioDeEmpleados = repositorioDeEmpleados;
        this.sesion = sesion;
        this.notificarRecepcion = new NotificarRecepcionDeBolsin(servidorCorreo);
        this.reloj = reloj;
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
        pantalla.solicitarSeleccionBolsin(resumirListado(bolsinesDelCMDestinoEnviados));
    }

    /** Sesión -> Usuario -> Empleado -> Comisión Médica. */
    private void buscarCMyMostrar() {
        usuarioActual = sesion.getUsuario();
        for (Empleado empleado : repositorioDeEmpleados.buscarEmpleados()) {
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
        for (Bolsin bolsin : repositorioDeBolsines.buscarBolsines()) {
            if (bolsin.esTuCMDestino(cmEmpleado) && bolsin.sosEnviado()) {
                bolsinesDelCMDestinoEnviados.add(bolsin);
            }
        }
    }

    /** Filtro por número de precinto. Lista vacía = flujo alternativo A1. */
    public List<ResumenDeBolsin> tomarFiltroPrecinto(int numeroPrecinto) {
        return resumirListado(bolsinesDelCMDestinoEnviados.stream()
                .filter(bolsin -> bolsin.esDePrecinto(numeroPrecinto))
                .toList());
    }

    /** Filtro por CM de origen (parte del nombre). Lista vacía = flujo alternativo A2. */
    public List<ResumenDeBolsin> tomarFiltroCMOrigen(String nombreCMOrigen) {
        return resumirListado(bolsinesDelCMDestinoEnviados.stream()
                .filter(bolsin -> bolsin.esDeCMOrigen(nombreCMOrigen))
                .toList());
    }

    private List<ResumenDeBolsin> resumirListado(List<Bolsin> bolsines) {
        return bolsines.stream().map(this::resumir).toList();
    }

    private ResumenDeBolsin resumir(Bolsin bolsin) {
        return new ResumenDeBolsin(bolsin.getNumeroBolsin(), bolsin.getCMOrigen(), bolsin.getNumPrecinto());
    }

    // ---------------------------------------------------------------- pasos 5 a 7

    public void tomarSeleccionBolsin(int numeroBolsin) {
        bolsinSeleccionado = bolsinesDelCMDestinoEnviados.stream()
                .filter(bolsin -> bolsin.getNumeroBolsin() == numeroBolsin)
                .findFirst()
                .orElseThrow(() -> new BolsinFueraDelListadoException(numeroBolsin));
        buscarInformacionBolsin();
        pantalla.solicitarOpcRecepcion(cmEmpleado);
    }

    private void buscarInformacionBolsin() {
        Map<Integer, List<DatosDeDocumentacion>> informacionBolsin = bolsinSeleccionado.obtenerInformacionRemito();
        pantalla.mostrarInfoBolsin(resumir(bolsinSeleccionado), informacionBolsin);
    }

    // ---------------------------------------------------------------- pasos 8 a 10

    /**
     * @param documentacionMarcada números de la documentación no recibida (opción 2), que no
     *                             corresponde (opción 3) o a redirigir (opción 4). Vacío en la opción 1.
     * @throws bolsines.entity.OpcionDeRecepcionInvalidaException si la opción no es aplicable; no cambia nada.
     */
    public void tomarOpcRecepcion(OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        new ValidadorDeOpcionDeRecepcion(cmEmpleado, bolsinSeleccionado).validar(opcion, documentacionMarcada);
        this.opcionSeleccionada = opcion;
        this.documentacionMarcada = new LinkedHashSet<>(documentacionMarcada);
        pantalla.solicitarConfirmacion(opcion, this.documentacionMarcada);
    }

    public void tomarConfirmacion(boolean confirma) {
        if (!confirma) {
            // Flujo alternativo A6: no se registra nada y se vuelve a pedir la opción.
            pantalla.mostrarMensaje("No se confirmó la selección. Elija nuevamente la opción de recepción.");
            pantalla.solicitarOpcRecepcion(cmEmpleado);
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
        return LocalDateTime.now(reloj).truncatedTo(ChronoUnit.SECONDS);
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
        repositorioDeBolsines.guardarRecepcion(bolsinSeleccionado);
    }

    // ---------------------------------------------------------------- paso 12

    /** Inclusión del CU 29: un email a cada CM de origen con la situación de su documentación. */
    private void llamarCasoDeUsoNotificarRecepcionDeBolsin() {
        notificarRecepcion.notificar(bolsinSeleccionado, nombreCMEmpleado);
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
