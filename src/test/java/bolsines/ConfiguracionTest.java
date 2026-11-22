package bolsines;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionTest {

    @Test
    void dadoSinArgumentosNiVariables_cuandoSeArmaLaConfiguracion_entoncesUsaLaWebEnEl8080ConLaBaseYLosDatosDeSiempre() {
        // Given / When
        Configuracion configuracion = Configuracion.desde(new String[0], Map.of());

        // Then
        assertFalse(configuracion.modoConsola());
        assertEquals(8080, configuracion.puerto());
        assertEquals("data/bolsines.db", configuracion.rutaBaseDeDatos());
        assertTrue(configuracion.cargarDatosDePrueba());
    }

    @Test
    void dadoElArgumentoConsola_cuandoSeArmaLaConfiguracion_entoncesUsaLaTerminal() {
        assertTrue(Configuracion.desde(new String[]{"Consola"}, Map.of()).modoConsola());
    }

    @Test
    void dadoUnNumeroDePuertoComoArgumento_cuandoHayTambienUnaVariableDeEntorno_entoncesGanaElArgumento() {
        // Given
        Map<String, String> entorno = Map.of("BOLSINES_PUERTO", "7000");

        // When / Then
        assertEquals(9090, Configuracion.desde(new String[]{"9090"}, entorno).puerto());
        assertEquals(7000, Configuracion.desde(new String[0], entorno).puerto());
    }

    @Test
    void dadaUnaVariableDeEntornoParaLaBase_cuandoSeArmaLaConfiguracion_entoncesUsaEsaRuta() {
        assertEquals("otra/ruta.db",
                Configuracion.desde(new String[0], Map.of("BOLSINES_DB", "otra/ruta.db")).rutaBaseDeDatos());
    }

    @Test
    void dadoQueSePidenSinDatosDePrueba_cuandoSeArmaLaConfiguracion_entoncesNoLosCarga() {
        assertFalse(Configuracion.desde(new String[0], Map.of("BOLSINES_DATOS_DE_PRUEBA", "no")).cargarDatosDePrueba());
    }
}
