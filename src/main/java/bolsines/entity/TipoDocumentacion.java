package bolsines.entity;

public class TipoDocumentacion {

    private final String nombre;
    private final String descripcion;

    public TipoDocumentacion(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
