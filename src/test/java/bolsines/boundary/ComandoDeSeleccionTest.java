package bolsines.boundary;

import bolsines.boundary.ComandoDeSeleccion.Cancelar;
import bolsines.boundary.ComandoDeSeleccion.FiltrarPorOrigen;
import bolsines.boundary.ComandoDeSeleccion.FiltrarPorPrecinto;
import bolsines.boundary.ComandoDeSeleccion.Invalido;
import bolsines.boundary.ComandoDeSeleccion.Seleccionar;
import bolsines.boundary.ComandoDeSeleccion.VerTodos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Interpretar lo que escribe el EB es una función pura: se prueba sin consola ni gestor. */
class ComandoDeSeleccionTest {

    @ParameterizedTest
    @ValueSource(strings = {"c", "C"})
    void dadaLaLetraDeCancelar_cuandoSeInterpreta_entoncesEsCancelar(String texto) {
        assertEquals(new Cancelar(), ComandoDeSeleccion.interpretar(texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"t", "T"})
    void dadaLaLetraDeVerTodos_cuandoSeInterpreta_entoncesEsVerTodos(String texto) {
        assertEquals(new VerTodos(), ComandoDeSeleccion.interpretar(texto));
    }

    @Test
    void dadoUnNumero_cuandoSeInterpreta_entoncesEsLaSeleccionDeEseBolsin() {
        assertEquals(new Seleccionar(2001), ComandoDeSeleccion.interpretar("2001"));
    }

    @Test
    void dadoUnPrefijoDePrecintoYUnNumero_cuandoSeInterpreta_entoncesEsUnFiltroPorPrecinto() {
        assertEquals(new FiltrarPorPrecinto(731202), ComandoDeSeleccion.interpretar("p 731202"));
        assertEquals(new FiltrarPorPrecinto(731202), ComandoDeSeleccion.interpretar("P 731202"));
    }

    @Test
    void dadoUnPrefijoDePrecintoYTextoQueNoEsNumero_cuandoSeInterpreta_entoncesElPrecintoVaNuloYSeTrataComoA1() {
        FiltrarPorPrecinto comando = (FiltrarPorPrecinto) ComandoDeSeleccion.interpretar("p abc");
        assertNull(comando.precinto());
    }

    @Test
    void dadoUnPrefijoDeOrigenYUnTexto_cuandoSeInterpreta_entoncesEsUnFiltroPorCMDeOrigen() {
        assertEquals(new FiltrarPorOrigen("Rosario"), ComandoDeSeleccion.interpretar("o Rosario"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "hola", "p", "o", "12x"})
    void dadoUnTextoQueNoEsNingunComando_cuandoSeInterpreta_entoncesEsInvalido(String texto) {
        assertEquals(new Invalido(), ComandoDeSeleccion.interpretar(texto));
    }
}
