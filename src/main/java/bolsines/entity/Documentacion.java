package bolsines.entity;

import bolsines.entity.estado.documentacion.EstadoDocumentacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PATRÓN STATE - rol CONTEXTO (Context).
 *
 * La Documentación no pregunta "¿en qué estado estoy?" con if/switch. Mantiene una
 * referencia a su estado actual (un objeto EstadoDocumentacion) y le delega cada evento
 * de la máquina de estados. El estado concreto decide si la transición es válida, crea
 * el estado siguiente y se lo asigna de vuelta a la Documentación con setEstado().
 */
public class Documentacion {

    private final int numero;
    private final String asunto;
    private final LocalDate fechaPase;
    private final TipoDocumentacion tipoDocumentacion;

    /** Estado actual: el objeto State al que se delegan los eventos. */
    private EstadoDocumentacion estadoActual;
    /** Historial de cambios de estado, para la trazabilidad que pide la ERS. */
    private final List<CambioEstadoDocumentacion> cambioEstadoDocumentacion = new ArrayList<>();

    public Documentacion(int numero, String asunto, LocalDate fechaPase,
                         TipoDocumentacion tipoDocumentacion, EstadoDocumentacion estadoActual) {
        this.numero = numero;
        this.asunto = asunto;
        this.fechaPase = fechaPase;
        this.tipoDocumentacion = tipoDocumentacion;
        this.estadoActual = estadoActual;
    }

    // ---------------------------------------------------------------- eventos (delegados al estado)

    /** Flujo principal del CU 28: la documentación llegó y se acepta. */
    public void registrarBolsinRecibido(LocalDateTime fechaHoraActual, Empleado responsable) {
        estadoActual.registrarBolsinRecibido(this, cambioEstadoDocumentacion, fechaHoraActual, responsable);
    }

    /** Alternativo A3: la documentación figura en el remito pero no llegó. */
    public void registrarDocumentacionNoRecibida(LocalDateTime fechaHoraActual, Empleado responsable) {
        estadoActual.registrarDocumentacionNoRecibida(this, cambioEstadoDocumentacion, fechaHoraActual, responsable);
    }

    /** Alternativo A4: la documentación llegó pero no corresponde a esta CM. */
    public void registrarRechazoDeDocumentacion(LocalDateTime fechaHoraActual, Empleado responsable) {
        estadoActual.registrarRechazoDeDocumentacion(this, cambioEstadoDocumentacion, fechaHoraActual, responsable);
    }

    /** Alternativo A5: la CMC debe redirigir la documentación a un área interna. */
    public void registrarDocumentacionParaRedirigir(LocalDateTime fechaHoraActual, Empleado responsable) {
        estadoActual.registrarDocumentacionParaRedirigir(this, cambioEstadoDocumentacion, fechaHoraActual, responsable);
    }

    // ---------------------------------------------------------------- usados por los estados

    /** Lo invoca el estado concreto para dejar a la Documentación en su nuevo estado. */
    public void setEstado(EstadoDocumentacion estado) {
        this.estadoActual = estado;
    }

    public void agregarCambioEstado(CambioEstadoDocumentacion nuevoCambioEstado) {
        cambioEstadoDocumentacion.add(nuevoCambioEstado);
    }

    // ---------------------------------------------------------------- consultas

    public int getNumero() {
        return numero;
    }

    public String getAsunto() {
        return asunto;
    }

    public LocalDate getFechaPase() {
        return fechaPase;
    }

    public String getTipoDocumentacion() {
        return tipoDocumentacion.getNombre();
    }

    public EstadoDocumentacion getEstadoActual() {
        return estadoActual;
    }

    /** Vista de solo lectura: el historial crece con {@link #agregarCambioEstado(CambioEstadoDocumentacion)}. */
    public List<CambioEstadoDocumentacion> getCambioEstadoDocumentacion() {
        return Collections.unmodifiableList(cambioEstadoDocumentacion);
    }
}
