package bolsines.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivosEstaticosTest {

    private final ArchivosEstaticos archivos = new ArchivosEstaticos();

    @Test
    void dadaLaRaiz_cuandoSePideElArchivo_entoncesEntregaElIndiceHtml() throws IOException {
        // Given / When
        Optional<ArchivosEstaticos.Archivo> archivo = archivos.buscar("/");

        // Then
        assertTrue(archivo.isPresent());
        assertEquals("text/html; charset=utf-8", archivo.get().tipo());
        assertTrue(new String(archivo.get().contenido()).contains("id=\"raiz\""));
    }

    @Test
    void dadoUnArchivoJavaScriptExistente_cuandoSePide_entoncesSeEntregaConSuTipo() throws IOException {
        // Given / When
        Optional<ArchivosEstaticos.Archivo> archivo = archivos.buscar("/js/app.js");

        // Then
        assertTrue(archivo.isPresent());
        assertEquals("text/javascript; charset=utf-8", archivo.get().tipo());
    }

    @Test
    void dadoUnArchivoInexistente_cuandoSePide_entoncesNoHayArchivo() throws IOException {
        assertTrue(archivos.buscar("/no-existe.js").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/../pom.xml", "/js/../../pom.xml", "/..", "/js/../../../bolsines/Main.class"})
    void dadaUnaRutaQueIntentaSalirDeLaCarpetaDeLaPagina_cuandoSePide_entoncesNoSeEntrega(String ruta) throws IOException {
        assertTrue(archivos.buscar(ruta).isEmpty());
    }
}
