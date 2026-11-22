package bolsines;

import bolsines.boundary.MenuConsola;
import bolsines.persistencia.BaseDeDatos;
import bolsines.web.ServidorWeb;

import java.io.IOException;
import java.util.Scanner;

/**
 * Arranque de la aplicación: crea los objetos y los conecta entre sí. Por defecto levanta la
 * interfaz web en http://localhost:8080 (se puede pasar otro puerto como argumento); con el
 * argumento "consola" usa la terminal. Ver {@link Configuracion} para las variables de entorno.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        Configuracion configuracion = Configuracion.desde(args, System.getenv());

        BaseDeDatos baseDeDatos = new BaseDeDatos(configuracion.rutaBaseDeDatos());
        baseDeDatos.inicializar(configuracion.cargarDatosDePrueba());

        // La misma BaseDeDatos implementa los dos repositorios que necesita el gestor.
        if (configuracion.modoConsola()) {
            new MenuConsola(baseDeDatos, baseDeDatos, new Scanner(System.in), System.out).ejecutar();
            return;
        }
        ServidorWeb servidor = new ServidorWeb(baseDeDatos, baseDeDatos, configuracion.puerto());
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));
        System.out.println("Bolsines - CU 28 en http://localhost:" + servidor.getPuerto() + " (Ctrl+C para detener)");
    }
}
