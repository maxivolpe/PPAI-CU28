package bolsines.control;

import bolsines.entity.Bolsin;
import bolsines.entity.OpcionDeRecepcionInvalidaException;
import bolsines.entity.OpcionRecepcion;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static bolsines.soporte.Escenarios.bolsinEnviadoCon2Documentaciones;
import static bolsines.soporte.Escenarios.central;
import static bolsines.soporte.Escenarios.cordoba;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Las reglas de la opción de recepción elegida (paso 8), una por una. */
class ValidadorDeOpcionDeRecepcionTest {

    @Test
    void dadaLaOpcion1SinDocumentacionMarcada_cuandoSeValida_entoncesEsValida() {
        // Given
        Bolsin bolsin = bolsinEnviadoCon2Documentaciones(cordoba());
        var validador = new ValidadorDeOpcionDeRecepcion(cordoba(), bolsin);

        // When / Then
        assertDoesNotThrow(() -> validador.validar(OpcionRecepcion.CONTENIDO_IGUAL, Set.of()));
    }

    @Test
    void dadaLaOpcion1ConDocumentacionMarcada_cuandoSeValida_entoncesSeRechaza() {
        // Given
        Bolsin bolsin = bolsinEnviadoCon2Documentaciones(cordoba());
        var validador = new ValidadorDeOpcionDeRecepcion(cordoba(), bolsin);

        // When
        var error = assertThrows(OpcionDeRecepcionInvalidaException.class,
                () -> validador.validar(OpcionRecepcion.CONTENIDO_IGUAL, Set.of(1)));

        // Then
        assertEquals("La opción 1 no lleva documentación seleccionada.", error.getMessage());
    }

    @Test
    void dadaUnaOpcionAlternativaSinDocumentacionMarcada_cuandoSeValida_entoncesSeRechaza() {
        // Given
        Bolsin bolsin = bolsinEnviadoCon2Documentaciones(cordoba());
        var validador = new ValidadorDeOpcionDeRecepcion(cordoba(), bolsin);

        // When
        var error = assertThrows(OpcionDeRecepcionInvalidaException.class,
                () -> validador.validar(OpcionRecepcion.DOCUMENTACION_NO_RECIBIDA, Set.of()));

        // Then
        assertEquals("Debe indicar al menos una documentación para esta opción.", error.getMessage());
    }

    @Test
    void dadaDocumentacionQueNoPerteneceAlBolsin_cuandoSeValida_entoncesSeRechazaIndicandoCual() {
        // Given
        Bolsin bolsin = bolsinEnviadoCon2Documentaciones(cordoba());
        var validador = new ValidadorDeOpcionDeRecepcion(cordoba(), bolsin);

        // When
        var error = assertThrows(OpcionDeRecepcionInvalidaException.class,
                () -> validador.validar(OpcionRecepcion.DOCUMENTACION_NO_CORRESPONDE, Set.of(1, 999)));

        // Then
        assertEquals("La documentación 999 no pertenece al bolsín.", error.getMessage());
    }

    @Test
    void dadaLaOpcionRedirigirYUnaCMQueNoEsLaCentral_cuandoSeValida_entoncesSeRechaza() {
        // Given
        Bolsin bolsin = bolsinEnviadoCon2Documentaciones(cordoba());
        var validador = new ValidadorDeOpcionDeRecepcion(cordoba(), bolsin);

        // When
        var error = assertThrows(OpcionDeRecepcionInvalidaException.class,
                () -> validador.validar(OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR, Set.of(1)));

        // Then
        assertEquals("La opción 4 solo aplica cuando la CM destino es la Comisión Médica Central.", error.getMessage());
    }

    @Test
    void dadaLaOpcionRedirigirYLaCMCentral_cuandoSeValidaConDocumentacionDelBolsin_entoncesEsValida() {
        // Given
        Bolsin bolsin = bolsinEnviadoCon2Documentaciones(central());
        var validador = new ValidadorDeOpcionDeRecepcion(central(), bolsin);

        // When / Then
        assertDoesNotThrow(() -> validador.validar(OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR, Set.of(1, 2)));
    }
}
