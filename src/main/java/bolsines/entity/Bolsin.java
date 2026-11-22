package bolsines.entity;

import bolsines.entity.estado.bolsin.EstadoBolsin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PATRÓN STATE - rol CONTEXTO (Context).
 *
 * El Bolsín conoce su estado actual (EstadoBolsin) y le delega los eventos de su máquina
 * de estados. El gestor solo le dice "registrá que fuiste recibido"; qué significa eso
 * (y si se puede) lo resuelve el estado concreto en el que esté el bolsín.
 */
public class Bolsin {

    private final int numeroBolsin;
    private final int numeroPrecinto;
    private final LocalDate fecha;
    /** Peso expresado en gramos. */
    private final int peso;
    private final ComisionMedica origen;
    private final ComisionMedica destino;
    private final List<Remito> remitos = new ArrayList<>();

    /** Estado actual: el objeto State al que se delegan los eventos. */
    private EstadoBolsin estadoActual;
    /** Historial de cambios de estado, para la trazabilidad que pide la ERS. */
    private final List<CambioEstadoBolsin> cambioEstados = new ArrayList<>();

    public Bolsin(int numeroBolsin, int numeroPrecinto, LocalDate fecha, int peso,
                  ComisionMedica origen, ComisionMedica destino, EstadoBolsin estadoActual) {
        this.numeroBolsin = numeroBolsin;
        this.numeroPrecinto = numeroPrecinto;
        this.fecha = fecha;
        this.peso = peso;
        this.origen = origen;
        this.destino = destino;
        this.estadoActual = estadoActual;
    }

    public void agregarRemito(Remito remito) {
        remitos.add(remito);
    }

    // ---------------------------------------------------------------- evento (delegado al estado)

    /**
     * Punto de entrada del rediseño (el "enganche" con el diagrama de análisis): el gestor
     * invoca este método y el Bolsín lo delega, sin ningún if, en su estado actual.
     */
    public void registrarBolsinRecibido(LocalDateTime fechaHoraActual, Empleado responsable,
                                        CambioEstadoBolsin cambioEstadoActual,
                                        OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        estadoActual.registrarBolsinRecibido(this, fechaHoraActual, responsable, remitos,
                cambioEstadoActual, opcion, documentacionMarcada);
    }

    // ---------------------------------------------------------------- usados por los estados

    /** Lo invoca el estado concreto para dejar al Bolsín en su nuevo estado. */
    public void setEstado(EstadoBolsin estado) {
        this.estadoActual = estado;
    }

    public void agregarCambioEstado(CambioEstadoBolsin cambioEstado) {
        cambioEstados.add(cambioEstado);
    }

    // ---------------------------------------------------------------- consultas

    public boolean esTuCMDestino(ComisionMedica comisionMedica) {
        return destino.equals(comisionMedica);
    }

    /**
     * Igual que en el diagrama de secuencia de análisis: busca el cambio de estado vigente
     * (*sosActual()) y le pregunta si es Enviado; este delega en su estado.
     */
    public boolean sosEnviado() {
        for (CambioEstadoBolsin cambioEstado : cambioEstados) {
            if (cambioEstado.sosActual()) {
                return cambioEstado.sosEnviado();
            }
        }
        return false;
    }

    public CambioEstadoBolsin buscarCambioEstadoActual() {
        for (CambioEstadoBolsin cambioEstado : cambioEstados) {
            if (cambioEstado.sosActual()) {
                return cambioEstado;
            }
        }
        throw new IllegalStateException("El bolsín " + numeroBolsin + " no tiene un cambio de estado vigente.");
    }

    public String getCMOrigen() {
        return origen.getNombre();
    }

    /** Tell, don't ask: el bolsín responde si su precinto es el buscado. */
    public boolean esDePrecinto(int numeroPrecintoBuscado) {
        return numeroPrecinto == numeroPrecintoBuscado;
    }

    /** Tell, don't ask: el bolsín responde si el nombre de su CM de origen contiene el texto (sin distinguir mayúsculas). */
    public boolean esDeCMOrigen(String textoDelNombre) {
        return origen.getNombre().toLowerCase().contains(textoDelNombre.trim().toLowerCase());
    }

    public int getNumPrecinto() {
        return numeroPrecinto;
    }

    /** Por cada remito del bolsín (número) la documentación que contiene. */
    public Map<Integer, List<DatosDeDocumentacion>> obtenerInformacionRemito() {
        Map<Integer, List<DatosDeDocumentacion>> informacion = new LinkedHashMap<>();
        for (Remito remito : remitos) {
            informacion.put(remito.getNumero(), remito.tomarDocumentacion());
        }
        return informacion;
    }

    public int getNumeroBolsin() {
        return numeroBolsin;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public int getPeso() {
        return peso;
    }

    public ComisionMedica getOrigen() {
        return origen;
    }

    public ComisionMedica getDestino() {
        return destino;
    }

    /** Vista de solo lectura: los remitos se agregan con {@link #agregarRemito(Remito)}. */
    public List<Remito> getRemitos() {
        return Collections.unmodifiableList(remitos);
    }

    public EstadoBolsin getEstadoActual() {
        return estadoActual;
    }

    /** Vista de solo lectura: el historial crece con {@link #agregarCambioEstado(CambioEstadoBolsin)}. */
    public List<CambioEstadoBolsin> getCambioEstados() {
        return Collections.unmodifiableList(cambioEstados);
    }
}
