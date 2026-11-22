package bolsines.entity;

import java.time.LocalDateTime;

/**
 * Las cuatro opciones de recepción que el sistema ofrece en el paso 7 del CU 28.
 * Cada opción conoce el evento de la máquina de estados de Documentación que dispara,
 * por eso quien la aplica no necesita un {@code switch}.
 */
public enum OpcionRecepcion {

    CONTENIDO_IGUAL(1, "El contenido del bolsín es igual al registrado",
            Documentacion::registrarBolsinRecibido),
    DOCUMENTACION_NO_RECIBIDA(2, "No se recibe toda la documentación asociada a los remitos que contiene el bolsín",
            Documentacion::registrarDocumentacionNoRecibida),
    DOCUMENTACION_NO_CORRESPONDE(3, "Existe documentación que no corresponde al destino",
            Documentacion::registrarRechazoDeDocumentacion),
    DOCUMENTACION_PARA_REDIRIGIR(4, "La documentación se debe redirigir a otra área",
            Documentacion::registrarDocumentacionParaRedirigir);

    private final int numero;
    private final String descripcion;
    private final EventoDeDocumentacion evento;

    OpcionRecepcion(int numero, String descripcion, EventoDeDocumentacion evento) {
        this.numero = numero;
        this.descripcion = descripcion;
        this.evento = evento;
    }

    public int getNumero() {
        return numero;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Las opciones 2, 3 y 4 exigen indicar a qué documentación se aplican. */
    public boolean requiereSeleccionDeDocumentacion() {
        return this != CONTENIDO_IGUAL;
    }

    /**
     * Única fuente de la guarda [comisión destino igual CMC] (alternativo A5): redirigir
     * documentación a otra área solo corresponde cuando el destino es la Comisión Médica Central.
     */
    public boolean estaDisponiblePara(ComisionMedica destino) {
        return this != DOCUMENTACION_PARA_REDIRIGIR || destino.sosCentral();
    }

    /** Dispara sobre la documentación el evento que corresponde a esta opción. */
    public void aplicarA(Documentacion documentacion, LocalDateTime fechaHoraActual, Empleado responsable) {
        evento.aplicar(documentacion, fechaHoraActual, responsable);
    }

    public static OpcionRecepcion desdeNumero(int numero) {
        for (OpcionRecepcion opcion : values()) {
            if (opcion.numero == numero) {
                return opcion;
            }
        }
        throw new OpcionDeRecepcionInvalidaException("Opción de recepción inexistente: " + numero);
    }
}
