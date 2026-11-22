package bolsines.web;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTest {

    private record Persona(String nombre, int edad, List<String> alias) {
    }

    @Test
    void dadoUnMapaConValoresDeTodoTipo_cuandoSeEscribe_entoncesSaleJsonEnElMismoOrden() {
        // Given
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("texto", "hola");
        mapa.put("numero", 7);
        mapa.put("verdadero", true);
        mapa.put("nulo", null);
        mapa.put("lista", List.of(1, 2));

        // When / Then
        assertEquals("{\"texto\":\"hola\",\"numero\":7,\"verdadero\":true,\"nulo\":null,\"lista\":[1,2]}",
                Json.escribir(mapa));
    }

    @Test
    void dadoUnRecord_cuandoSeEscribe_entoncesSaleComoObjetoConUnCampoPorComponente() {
        // Given
        Persona persona = new Persona("Ana", 30, List.of("a", "b"));

        // When / Then
        assertEquals("{\"nombre\":\"Ana\",\"edad\":30,\"alias\":[\"a\",\"b\"]}", Json.escribir(persona));
    }

    @Test
    void dadoUnTextoConComillasBarrasSaltosYControl_cuandoSeEscribe_entoncesSeEscapa() {
        // Given
        String texto = "dice \"hola\"\\\n\t\u0001";

        // When / Then
        assertEquals("\"dice \\\"hola\\\"\\\\\\n\\t\\u0001\"", Json.escribir(texto));
    }

    @Test
    void dadoUnTextoConTildes_cuandoSeEscribe_entoncesSeConservanSinEscapar() {
        assertEquals("\"Comisión Médica Córdoba\"", Json.escribir("Comisión Médica Córdoba"));
    }

    @Test
    void dadoUnValorNulo_cuandoSeEscribe_entoncesSaleNull() {
        assertEquals("null", Json.escribir(null));
    }
}
