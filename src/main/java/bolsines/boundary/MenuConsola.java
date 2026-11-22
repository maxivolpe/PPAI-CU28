package bolsines.boundary;

import bolsines.control.ConsultarEstadosGuardados;
import bolsines.control.ConsultarEstadosGuardados.BolsinGuardado;
import bolsines.control.ConsultarEstadosGuardados.CambioGuardado;
import bolsines.control.ConsultarEstadosGuardados.DocumentacionGuardada;
import bolsines.control.ConsultarEstadosGuardados.RemitoGuardado;
import bolsines.control.GestorRecepcion;
import bolsines.control.RepositorioDeBolsines;
import bolsines.control.RepositorioDeEmpleados;
import bolsines.entity.Empleado;
import bolsines.entity.Sesion;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

/**
 * Interfaz de consola de la aplicación: inicio de sesión simulado (CU 5), el CU 28 y la
 * consulta de estados guardados. El inicio de sesión y la consulta NO son parte del CU 28:
 * están para poder probarlo y verificar la persistencia.
 */
public class MenuConsola {

    private final RepositorioDeBolsines repositorioDeBolsines;
    private final RepositorioDeEmpleados repositorioDeEmpleados;
    private final Scanner entrada;
    private final PrintStream salida;

    public MenuConsola(RepositorioDeBolsines repositorioDeBolsines, RepositorioDeEmpleados repositorioDeEmpleados,
                       Scanner entrada, PrintStream salida) {
        this.repositorioDeBolsines = repositorioDeBolsines;
        this.repositorioDeEmpleados = repositorioDeEmpleados;
        this.entrada = entrada;
        this.salida = salida;
    }

    public void ejecutar() {
        Sesion sesion = iniciarSesion();
        GestorRecepcion gestor = new GestorRecepcion(repositorioDeBolsines, repositorioDeEmpleados, sesion,
                new InterfazServidorCorreo(salida));
        PantallaRecepcionConsola pantalla = new PantallaRecepcionConsola(gestor, entrada, salida);

        while (true) {
            salida.println();
            salida.println("1. Registrar recepción de bolsín");
            salida.println("2. Consultar estados guardados en la base de datos");
            salida.println("0. Salir");
            salida.print("> ");
            String opcion = entrada.nextLine().trim();
            if (opcion.equals("1")) {
                pantalla.opcRegistrarRecepcion();
            } else if (opcion.equals("2")) {
                consultarEstados();
            } else if (opcion.equals("0")) {
                return;
            }
        }
    }

    /** Simula el login: crea la Sesión del empleado elegido. */
    private Sesion iniciarSesion() {
        List<Empleado> empleados = repositorioDeEmpleados.buscarEmpleados();
        while (true) {
            salida.println("Iniciar sesión como Encargado de Bolsines:");
            for (int i = 0; i < empleados.size(); i++) {
                Empleado empleado = empleados.get(i);
                salida.println("  " + (i + 1) + ". " + empleado.getUsuario().getNombre() + " - "
                        + empleado.getNombre() + " " + empleado.getApellido()
                        + " (" + empleado.getCM().getNombre() + ")");
            }
            salida.print("> ");
            try {
                int elegido = Integer.parseInt(entrada.nextLine().trim());
                return new Sesion(LocalDateTime.now(), empleados.get(elegido - 1).getUsuario());
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                salida.println("Opción no válida.");
            }
        }
    }

    /** Muestra estados e historiales con su responsable, tal como quedaron en la base. */
    private void consultarEstados() {
        for (BolsinGuardado bolsin : new ConsultarEstadosGuardados(repositorioDeBolsines).consultar()) {
            salida.println();
            salida.println("Bolsín " + bolsin.numero() + " | " + bolsin.cmOrigen() + " -> "
                    + bolsin.cmDestino() + " | Estado: " + bolsin.estado());
            mostrarCambios(bolsin.cambios(), "    ");
            for (RemitoGuardado remito : bolsin.remitos()) {
                salida.println("  Remito " + remito.numero() + " | Estado: " + remito.estado());
                for (DocumentacionGuardada documentacion : remito.documentacion()) {
                    salida.println("    Doc. " + documentacion.numero() + " | " + documentacion.asunto()
                            + " | Estado: " + documentacion.estado());
                    mostrarCambios(documentacion.cambios(), "        ");
                }
            }
        }
    }

    private void mostrarCambios(List<CambioGuardado> cambios, String sangria) {
        for (CambioGuardado cambio : cambios) {
            salida.println(sangria + "[" + cambio.inicio() + " a "
                    + (cambio.fin() == null ? "vigente" : cambio.fin()) + "] "
                    + cambio.estado() + " - " + cambio.responsable());
        }
    }
}
