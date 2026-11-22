package bolsines.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static bolsines.soporte.Escenarios.central;
import static bolsines.soporte.Escenarios.cordoba;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpcionRecepcionTest {

    @Test
    void dadaLaCMCComoDestino_cuandoSeConsultaLaDisponibilidadDeCadaOpcion_entoncesTodasEstanDisponibles() {
        // Given
        ComisionMedica destino = central();

        // When / Then
        for (OpcionRecepcion opcion : OpcionRecepcion.values()) {
            assertTrue(opcion.estaDisponiblePara(destino), "opción " + opcion.getNumero());
        }
    }

    @ParameterizedTest
    @EnumSource(value = OpcionRecepcion.class, names = "DOCUMENTACION_PARA_REDIRIGIR", mode = EnumSource.Mode.EXCLUDE)
    void dadaUnaCMQueNoEsLaCentral_cuandoSeConsultaUnaOpcionQueNoEsRedirigir_entoncesEstaDisponible(OpcionRecepcion opcion) {
        // Given / When / Then
        assertTrue(opcion.estaDisponiblePara(cordoba()));
    }

    @Test
    void dadaUnaCMQueNoEsLaCentral_cuandoSeConsultaLaOpcionRedirigir_entoncesNoEstaDisponible() {
        // Given / When / Then
        assertFalse(OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR.estaDisponiblePara(cordoba()));
    }

    @Test
    void dadasLasOpciones_cuandoSePreguntaCualesRequierenSeleccionarDocumentacion_entoncesSoloLaPrimeraNoLaRequiere() {
        // Given / When / Then
        assertFalse(OpcionRecepcion.CONTENIDO_IGUAL.requiereSeleccionDeDocumentacion());
        assertTrue(OpcionRecepcion.DOCUMENTACION_NO_RECIBIDA.requiereSeleccionDeDocumentacion());
        assertTrue(OpcionRecepcion.DOCUMENTACION_NO_CORRESPONDE.requiereSeleccionDeDocumentacion());
        assertTrue(OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR.requiereSeleccionDeDocumentacion());
    }

    @Test
    void dadoUnNumeroValido_cuandoSeBuscaLaOpcion_entoncesDevuelveLaQueTieneEseNumero() {
        // Given / When / Then
        for (OpcionRecepcion opcion : OpcionRecepcion.values()) {
            assertEquals(opcion, OpcionRecepcion.desdeNumero(opcion.getNumero()));
        }
    }

    @Test
    void dadoUnNumeroInexistente_cuandoSeBuscaLaOpcion_entoncesSeRechazaConUnaReglaDeNegocio() {
        // Given / When / Then
        assertThrows(OpcionDeRecepcionInvalidaException.class, () -> OpcionRecepcion.desdeNumero(9));
    }
}
