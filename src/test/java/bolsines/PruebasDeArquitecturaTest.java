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
 * PRUEBAS DE ARQUITECTURA: revisan el código fuente, no lo ejecutan. Controlan que se
 * respeten las dependencias entre las capas Boundary - Control - Entity y que el sistema
 * conserve las clases de la versión entregada. Si alguien rompe una regla, el test dice dónde.
 */
@DisplayName("4. Pruebas de arquitectura")
class PruebasDeArquitecturaTest {

    private static final Path FUENTES = Path.of("src", "main", "java", "bolsines");

    /** Las clases de la versión entregada (las del informe y los diagramas). */
    private static final int CLASES_DE_LA_VERSION_ENTREGADA = 47;

    @Test
    void dadoElCodigoDelSistema_cuandoSeCuentanSusClases_entoncesSiguenSiendoLasDeLaVersionEntregada() throws IOException {
        try (Stream<Path> archivos = Files.walk(FUENTES)) {
            long clases = archivos.filter(ruta -> ruta.toString().endsWith(".java")).count();
            assertEquals(CLASES_DE_LA_VERSION_ENTREGADA, clases,
                    "las mejoras van dentro de las clases existentes: no se agregan ni se quitan clases");
        }
    }

    @Test
    void dadasLasEntidades_cuandoSeRevisanSusImports_entoncesNoDependenDeOtrasCapasNiDeTecnologia() throws IOException {
        assertSinImports("entity", "bolsines.boundary", "bolsines.control", "bolsines.persistencia",
                "bolsines.web", "java.sql", "com.sun.net.httpserver");
    }

    @Test
    void dadoElControl_cuandoSeRevisanSusImports_entoncesNoConoceLaWebNiElSql() throws IOException {
        assertSinImports("control", "bolsines.web", "java.sql", "com.sun.net.httpserver");
    }

    @Test
    void dadasLasPantallas_cuandoSeRevisanSusImports_entoncesNoConocenLaBaseDeDatos() throws IOException {
        assertSinImports("boundary", "bolsines.persistencia", "bolsines.web", "java.sql");
    }

    @Test
    void dadaLaPersistencia_cuandoSeRevisanSusImports_entoncesSoloConoceLasEntidades() throws IOException {
        assertSinImports("persistencia", "bolsines.boundary", "bolsines.control", "bolsines.web");
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
