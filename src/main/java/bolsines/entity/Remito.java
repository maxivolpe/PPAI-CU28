package bolsines.entity;

import bolsines.entity.estado.remito.EstadoRemito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * PATRÓN STATE - rol CONTEXTO (Context).
 *
 * El Remito delega en su estado actual (EstadoRemito) el evento de recepción. A diferencia
 * de Bolsín y Documentación no lleva historial de cambios de estado, porque el modelo de
 * dominio no lo define: solo guarda el estado actual.
 */
public class Remito {

    private final int numero;
    private final LocalDate fecha;
    private final ComisionMedica origen;
    private final ComisionMedica destino;
    private final List<DetalleRemito> detallesRemito = new ArrayList<>();

    /** Estado actual: el objeto State al que se delegan los eventos. */
    private EstadoRemito estadoActual;

    public Remito(int numero, LocalDate fecha, ComisionMedica origen, ComisionMedica destino,
                  EstadoRemito estadoActual) {
        this.numero = numero;
        this.fecha = fecha;
        this.origen = origen;
        this.destino = destino;
        this.estadoActual = estadoActual;
    }

    public void agregarDetalle(DetalleRemito detalle) {
        detallesRemito.add(detalle);
    }

    // ---------------------------------------------------------------- evento (delegado al estado)

    public void registrarBolsinRecibido(LocalDateTime fechaHoraActual, Empleado responsable,
                                        OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        estadoActual.registrarBolsinRecibido(this, detallesRemito, fechaHoraActual, responsable,
                opcion, documentacionMarcada);
    }

    /** Lo invoca el estado concreto para dejar al Remito en su nuevo estado. */
    public void setEstado(EstadoRemito estado) {
        this.estadoActual = estado;
    }

    // ---------------------------------------------------------------- consultas

    /** Por cada documentación del remito: {número, asunto, tipo de documentación}. */
    public List<String[]> tomarDocumentacion() {
        List<String[]> datos = new ArrayList<>();
        for (DetalleRemito detalle : detallesRemito) {
            Documentacion documentacion = detalle.getDocumentacion();
            datos.add(new String[]{
                    String.valueOf(documentacion.getNumero()),
                    documentacion.getAsunto(),
                    documentacion.getTipoDocumentacion()});
        }
        return datos;
    }

    public int getNumero() {
        return numero;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public ComisionMedica getOrigen() {
        return origen;
    }

    public ComisionMedica getDestino() {
        return destino;
    }

    public EstadoRemito getEstadoActual() {
        return estadoActual;
    }

    /** Vista de solo lectura: los detalles se agregan con {@link #agregarDetalle(DetalleRemito)}. */
    public List<DetalleRemito> getDetallesRemito() {
        return Collections.unmodifiableList(detallesRemito);
    }
}
