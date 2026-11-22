package bolsines.entity;

public class Empleado {

    private final String nombre;
    private final String apellido;
    /** El email identifica al empleado en la base de datos. */
    private final String email;
    private final ComisionMedica comisionMedica;
    private final Usuario usuario;

    public Empleado(String nombre, String apellido, String email,
                    ComisionMedica comisionMedica, Usuario usuario) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.comisionMedica = comisionMedica;
        this.usuario = usuario;
    }

    public boolean esTuUsuario(Usuario unUsuario) {
        return usuario.equals(unUsuario);
    }

    public ComisionMedica getCM() {
        return comisionMedica;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
