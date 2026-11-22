package bolsines;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reglas de dependencia entre las capas Boundary - Control - Entity (y la persistencia),
 * revisando los import de cada archivo fuente. Si alguien agrega una dependencia prohibida,
 * este test falla y dice dónde.
 */
@DisplayName("Arquitectura Boundary - Control - Entity")
class ArquitecturaTest {

    private static final Path FUENTES = Path.of("src", "main", "java", "bolsines");

    @Test
    void dadasLasEntidades_cuandoSeRevisanSusImports_entoncesNoDependenDeOtrasCapasNiDeTecnologia() throws IOException {
        assertSinImports("entity", "bolsines.boundary", "bolsines.control", "bolsines.persistencia",
                "bolsines.web", "java.sql", "com.sun.net.httpserver");
    }

    @Test
    void dadoElControl_cuandoSeRevisanSusImports_entoncesNoConoceLaBaseDeDatosNiLaWeb() throws IOException {
        assertSinImports("control", "bolsines.persistencia", "bolsines.web", "java.sql", "com.sun.net.httpserver");
    }

    @Test
    void dadasLasPantallas_cuandoSeRevisanSusImports_entoncesNoConocenLaBaseDeDatos() throws IOException {
        assertSinImports("boundary", "bolsines.persistencia", "bolsines.web", "java.sql");
    }

    @Test
    void dadaLaPersistencia_cuandoSeRevisanSusImports_entoncesNoConoceLasPantallas() throws IOException {
        assertSinImports("persistencia", "bolsines.boundary", "bolsines.web");
    }

    private static void assertSinImports(String paquete, String... paquetesProhibidos) throws IOException {
        List<String> violaciones = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(FUENTES.resolve(paquete))) {
            for (Path archivo : archivos.filter(ruta -> ruta.toString().endsWith(".java")).toList()) {
                for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                    for (String prohibido : paquetesProhibidos) {
                        if (linea.startsWith("import " + prohibido + ".")) {
                            violaciones.add(archivo.getFileName() + ": " + linea);
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), violaciones, "dependencias prohibidas en el paquete " + paquete);
    }
}
