package bolsines;

import bolsines.boundary.InterfazServidorCorreo;
import bolsines.boundary.PantallaRecepcionConsola;
import bolsines.control.GestorRecepcion;
import bolsines.entity.Bolsin;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.CambioEstadoDocumentacion;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.entity.Empleado;
import bolsines.entity.Remito;
import bolsines.entity.Sesion;
import bolsines.persistencia.BaseDeDatos;
import bolsines.web.ServidorWeb;

import java.io.IOException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

/**
 * Arranque de la aplicación. Por defecto levanta la interfaz web en http://localhost:8080
 * (se puede pasar otro puerto como argumento); con el argumento "consola" usa la terminal.
 * El inicio de sesión (CU 5) y la consulta de estados NO son parte del CU 28: están para
 * poder probarlo y verificar la persistencia.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        BaseDeDatos baseDeDatos = new BaseDeDatos("data/bolsines.db");
        baseDeDatos.inicializar();

        if (args.length > 0 && args[0].equalsIgnoreCase("consola")) {
            ejecutarEnConsola(baseDeDatos);
            return;
        }
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        ServidorWeb servidor = new ServidorWeb(baseDeDatos, puerto);
        servidor.iniciar();
        System.out.println("Bolsines - CU 28 en http://localhost:" + servidor.getPuerto() + " (Ctrl+C para detener)");
    }

    private static void ejecutarEnConsola(BaseDeDatos baseDeDatos) {
        Scanner entrada = new Scanner(System.in);

        Sesion sesion = iniciarSesion(baseDeDatos, entrada);
        GestorRecepcion gestor = new GestorRecepcion(baseDeDatos, sesion, new InterfazServidorCorreo(System.out));
        PantallaRecepcionConsola pantalla = new PantallaRecepcionConsola(gestor, entrada, System.out);

        while (true) {
            System.out.println();
            System.out.println("1. Registrar recepción de bolsín");
            System.out.println("2. Consultar estados guardados en la base de datos");
            System.out.println("0. Salir");
            System.out.print("> ");
            String opcion = entrada.nextLine().trim();
            if (opcion.equals("1")) {
                pantalla.opcRegistrarRecepcion();
            } else if (opcion.equals("2")) {
                consultarEstados(baseDeDatos);
            } else if (opcion.equals("0")) {
                return;
            }
        }
    }

    /** Simula el login: crea la Sesión del empleado elegido. */
    private static Sesion iniciarSesion(BaseDeDatos baseDeDatos, Scanner entrada) {
        List<Empleado> empleados = baseDeDatos.buscarEmpleados();
        while (true) {
            System.out.println("Iniciar sesión como Encargado de Bolsines:");
            for (int i = 0; i < empleados.size(); i++) {
                Empleado empleado = empleados.get(i);
                System.out.println("  " + (i + 1) + ". " + empleado.getUsuario().getNombre() + " - "
                        + empleado.getNombre() + " " + empleado.getApellido()
                        + " (" + empleado.getCM().getNombre() + ")");
            }
            System.out.print("> ");
            try {
                int elegido = Integer.parseInt(entrada.nextLine().trim());
                return new Sesion(LocalDateTime.now(), empleados.get(elegido - 1).getUsuario());
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                System.out.println("Opción no válida.");
            }
        }
    }

    /** Lee todo de la base y muestra estados e historiales con su responsable. */
    private static void consultarEstados(BaseDeDatos baseDeDatos) {
        for (Bolsin bolsin : baseDeDatos.buscarBolsines()) {
            System.out.println();
            System.out.println("Bolsín " + bolsin.getNumeroBolsin() + " | " + bolsin.getCMOrigen() + " -> "
                    + bolsin.getDestino().getNombre() + " | Estado: " + bolsin.getEstadoActual().getNombre());
            for (CambioEstadoBolsin cambio : bolsin.getCambioEstados()) {
                System.out.println("    " + lineaDeHistorial(cambio.getFechaHoraInicio(), cambio.getFechaHoraFin(),
                        cambio.getEstado().getNombre(), cambio.getResponsable()));
            }
            for (Remito remito : bolsin.getRemitos()) {
                mostrarRemito(remito);
            }
        }
    }

    private static void mostrarRemito(Remito remito) {
        System.out.println("  Remito " + remito.getNumero() + " | Estado: " + remito.getEstadoActual().getNombre());
        for (DetalleRemito detalle : remito.getDetallesRemito()) {
            Documentacion documentacion = detalle.getDocumentacion();
            System.out.println("    Doc. " + documentacion.getNumero() + " | " + documentacion.getAsunto()
                    + " | Estado: " + documentacion.getEstadoActual().getNombre());
            for (CambioEstadoDocumentacion cambio : documentacion.getCambioEstadoDocumentacion()) {
                System.out.println("        " + lineaDeHistorial(cambio.getFechaHoraInicio(),
                        cambio.getFechaHoraFin(), cambio.getEstado().getNombre(), cambio.getResponsable()));
            }
        }
    }

    /** "[inicio a fin] Estado - Apellido"; si el estado sigue vigente, en lugar de la fecha de fin dice "vigente". */
    private static String lineaDeHistorial(LocalDateTime inicio, LocalDateTime fin, String estado, Empleado responsable) {
        return "[" + inicio + " a " + (fin == null ? "vigente" : fin) + "] " + estado + " - " + responsable.getApellido();
    }
}
