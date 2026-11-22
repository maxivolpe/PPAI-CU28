package bolsines.entity;

/** Las cuatro opciones de recepción que el sistema ofrece en el paso 7 del CU 28. */
public enum OpcionRecepcion {

    CONTENIDO_IGUAL(1, "El contenido del bolsín es igual al registrado"),
    DOCUMENTACION_NO_RECIBIDA(2, "No se recibe toda la documentación asociada a los remitos que contiene el bolsín"),
    DOCUMENTACION_NO_CORRESPONDE(3, "Existe documentación que no corresponde al destino"),
    DOCUMENTACION_PARA_REDIRIGIR(4, "La documentación se debe redirigir a otra área");

    private final int numero;
    private final String descripcion;

    OpcionRecepcion(int numero, String descripcion) {
        this.numero = numero;
        this.descripcion = descripcion;
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
     * Guarda [comisión destino igual CMC] de la máquina de estados (alternativo A5): redirigir
     * documentación a otra área solo corresponde cuando la CM destino es la Comisión Médica
     * Central. Es el único lugar donde está escrita esta regla; la consultan el gestor, el
     * estado Enviado y las dos pantallas.
     */
    public boolean estaDisponiblePara(ComisionMedica destino) {
        return this != DOCUMENTACION_PARA_REDIRIGIR || destino.sosCentral();
    }

    public static OpcionRecepcion desdeNumero(int numero) {
        for (OpcionRecepcion opcion : values()) {
            if (opcion.numero == numero) {
                return opcion;
            }
        }
        throw new IllegalArgumentException("Opción de recepción inexistente: " + numero);
    }
}
