package bolsines.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

/** Entrega los archivos de la página, que viajan dentro del programa (carpeta resources/static). */
final class ArchivosEstaticos {

    private static final String RAIZ = "/static";

    /** Un archivo listo para enviar: su tipo de contenido y sus bytes. */
    record Archivo(String tipo, byte[] contenido) {
    }

    Optional<Archivo> buscar(String ruta) throws IOException {
        String archivo = ruta.equals("/") ? "/index.html" : ruta;
        String recurso = recursoSeguro(archivo);
        if (recurso == null) {
            return Optional.empty();
        }
        try (InputStream contenido = ArchivosEstaticos.class.getResourceAsStream(recurso)) {
            return contenido == null
                    ? Optional.empty()
                    : Optional.of(new Archivo(tipoDe(archivo), contenido.readAllBytes()));
        }
    }

    /** La ruta del recurso, o null si intenta salir de la carpeta de la página. */
    private String recursoSeguro(String archivo) {
        try {
            String normalizada = URI.create(RAIZ + archivo).normalize().getPath();
            return normalizada != null && normalizada.startsWith(RAIZ + "/") ? normalizada : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String tipoDe(String archivo) {
        if (archivo.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (archivo.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (archivo.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        return "application/octet-stream";
    }
}
