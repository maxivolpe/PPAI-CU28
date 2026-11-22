package bolsines;

import java.util.Map;

/**
 * Cómo se arranca la aplicación. Valores por defecto: web en el puerto 8080, base en
 * {@code data/bolsines.db} y datos de prueba cargados si la base está vacía.
 *
 * Se pueden cambiar con variables de entorno ({@code BOLSINES_PUERTO}, {@code BOLSINES_DB},
 * {@code BOLSINES_DATOS_DE_PRUEBA=no}) y con el argumento: {@code consola} para usar la
 * terminal, o un número de puerto para la web (el argumento gana sobre la variable de entorno).
 */
public record Configuracion(boolean modoConsola, int puerto, String rutaBaseDeDatos, boolean cargarDatosDePrueba) {

    static final int PUERTO_POR_DEFECTO = 8080;
    static final String BASE_DE_DATOS_POR_DEFECTO = "data/bolsines.db";

    public static Configuracion desde(String[] argumentos, Map<String, String> entorno) {
        String rutaBaseDeDatos = entorno.getOrDefault("BOLSINES_DB", BASE_DE_DATOS_POR_DEFECTO);
        boolean cargarDatosDePrueba = !entorno.getOrDefault("BOLSINES_DATOS_DE_PRUEBA", "si").equalsIgnoreCase("no");
        int puerto = Integer.parseInt(entorno.getOrDefault("BOLSINES_PUERTO", String.valueOf(PUERTO_POR_DEFECTO)));

        boolean modoConsola = argumentos.length > 0 && argumentos[0].equalsIgnoreCase("consola");
        if (argumentos.length > 0 && !modoConsola) {
            puerto = Integer.parseInt(argumentos[0]);
        }
        return new Configuracion(modoConsola, puerto, rutaBaseDeDatos, cargarDatosDePrueba);
    }
}
