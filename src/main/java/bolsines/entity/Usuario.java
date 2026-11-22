package bolsines.entity;

public class Usuario {

    private final String nombre;
    private final String contrasenia;

    public Usuario(String nombre, String contrasenia) {
        this.nombre = nombre;
        this.contrasenia = contrasenia;
    }

    public String getNombre() {
        return nombre;
    }

    public String getContrasenia() {
        return contrasenia;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Usuario u && nombre.equals(u.nombre);
    }

    @Override
    public int hashCode() {
        return nombre.hashCode();
    }
}
