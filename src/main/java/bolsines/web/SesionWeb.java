package bolsines.web;

import bolsines.boundary.InterfazServidorCorreo;
import bolsines.boundary.PantallaRecepcionWeb;
import bolsines.control.GestorRecepcion;
import bolsines.control.RepositorioDeBolsines;
import bolsines.control.RepositorioDeEmpleados;
import bolsines.entity.Empleado;
import bolsines.entity.Sesion;

import java.time.LocalDateTime;

/**
 * Lo que el servidor recuerda de UN navegador: el empleado logueado, su gestor, su pantalla y
 * su servidor de correo simulado. Hay una por navegador (ver {@link SesionesWeb}).
 */
final class SesionWeb {

    private final PantallaRecepcionWeb pantalla = new PantallaRecepcionWeb();
    private Empleado empleadoLogueado;
    private InterfazServidorCorreo servidorCorreo;
    private GestorRecepcion gestor;

    /** Simula el login (CU 5): crea la Sesión del empleado elegido y un gestor para ella. */
    void iniciar(RepositorioDeBolsines repositorioDeBolsines, RepositorioDeEmpleados repositorioDeEmpleados,
                 String nombreUsuario) {
        for (Empleado empleado : repositorioDeEmpleados.buscarEmpleados()) {
            if (empleado.getUsuario().getNombre().equals(nombreUsuario)) {
                empleadoLogueado = empleado;
                Sesion sesion = new Sesion(LocalDateTime.now(), empleado.getUsuario());
                servidorCorreo = new InterfazServidorCorreo(System.out);
                gestor = new GestorRecepcion(repositorioDeBolsines, repositorioDeEmpleados, sesion, servidorCorreo);
                pantalla.habilitarPantalla();
                return;
            }
        }
        throw new PedidoInvalidoException("No existe el usuario " + nombreUsuario + ".");
    }

    /** El pedido solo es válido en alguno de estos pasos del caso de uso. */
    void exigirPaso(String... pasosPermitidos) {
        if (gestor == null) {
            throw new PedidoInvalidoException("Primero hay que iniciar sesión.");
        }
        for (String paso : pasosPermitidos) {
            if (paso.equals(pantalla.getPaso())) {
                return;
            }
        }
        throw new PedidoInvalidoException("La acción no corresponde a este momento del caso de uso.");
    }

    Empleado getEmpleadoLogueado() {
        return empleadoLogueado;
    }

    GestorRecepcion getGestor() {
        return gestor;
    }

    PantallaRecepcionWeb getPantalla() {
        return pantalla;
    }

    InterfazServidorCorreo getServidorCorreo() {
        return servidorCorreo;
    }
}
