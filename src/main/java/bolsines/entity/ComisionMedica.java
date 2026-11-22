package bolsines.entity;

public class ComisionMedica {

    private final String codigo;
    private final String nombre;
    private final String direccion;
    private final String email;
    private final String telefono;
    /** Verdadero solo para la Comisión Médica Central (CMC). */
    private final boolean esCentral;

    public ComisionMedica(String codigo, String nombre, String direccion, String email,
                          String telefono, boolean esCentral) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.direccion = direccion;
        this.email = email;
        this.telefono = telefono;
        this.esCentral = esCentral;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    /** Usado por la guarda [comisión destino igual CMC] de la máquina de estados. */
    public boolean sosCentral() {
        return esCentral;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof ComisionMedica cm && codigo.equals(cm.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }
}
