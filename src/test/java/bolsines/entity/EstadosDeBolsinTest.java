package bolsines.entity;

import bolsines.entity.estado.bolsin.Cerrado;
import bolsines.entity.estado.bolsin.Creado;
import bolsines.entity.estado.bolsin.DeBaja;
import bolsines.entity.estado.bolsin.Enviado;
import bolsines.entity.estado.bolsin.EstadoBolsin;
import bolsines.entity.estado.bolsin.RecibidoEnCMDestino;
import bolsines.entity.estado.documentacion.EnBolsinEnviado;
import bolsines.entity.estado.documentacion.NoRecibida;
import bolsines.entity.estado.documentacion.RecibidaYAceptada;
import bolsines.entity.estado.remito.RecibidoYAceptado;
import bolsines.entity.estado.remito.RecibidoYAceptadoParcial;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static bolsines.soporte.Escenarios.AHORA;
import static bolsines.soporte.Escenarios.bolsin;
import static bolsines.soporte.Escenarios.bolsinEnviadoCon2Documentaciones;
import static bolsines.soporte.Escenarios.cordoba;
import static bolsines.soporte.Escenarios.central;
import static bolsines.soporte.Escenarios.documentacionDe;
import static bolsines.soporte.Escenarios.encargadoDe;
import static bolsines.soporte.Escenarios.rosario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Patrón State sobre Bolsín, y cascada hacia Remito y Documentación. */
class EstadosDeBolsinTest {

    private static final Empleado EB = encargadoDe(cordoba());

    private static Stream<EstadoBolsin> estadosDistintosDeEnviado() {
        return Stream.of(new Creado(), new Cerrado(), new RecibidoEnCMDestino(), new DeBaja());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("estadosDistintosDeEnviado")
    void dadoUnBolsinQueNoEstaEnviado_cuandoSeRegistraLaRecepcion_entoncesLaTransicionNoEstaPermitida(EstadoBolsin estado) {
        // Given
        Bolsin bolsin = bolsin(100, estado, rosario(), cordoba(), EB);

        // When / Then
        assertThrows(TransicionNoPermitidaException.class, () -> bolsin.registrarBolsinRecibido(
                AHORA, EB, bolsin.buscarCambioEstadoActual(), OpcionRecepcion.CONTENIDO_IGUAL, Set.of()));
        assertEquals(estado, bolsin.getEstadoActual());
    }

    @Test
    void dadoUnBolsinEnviado_cuandoSeRegistraLaRecepcionConContenidoIgual_entoncesTodoQuedaRecibidoYAceptado() {
        // Given
        Bolsin bolsin = bolsinEnviadoCon2Documentaciones(cordoba());

        // When
        bolsin.registrarBolsinRecibido(AHORA, EB, bolsin.buscarCambioEstadoActual(),
                OpcionRecepcion.CONTENIDO_IGUAL, Set.of());

        // Then
        assertInstanceOf(RecibidoEnCMDestino.class, bolsin.getEstadoActual());
        assertInstanceOf(RecibidoYAceptado.class, bolsin.getRemitos().get(0).getEstadoActual());
        assertInstanceOf(RecibidaYAceptada.class, documentacionDe(bolsin, 1).getEstadoActual());
        assertInstanceOf(RecibidaYAceptada.class, documentacionDe(bolsin, 2).getEstadoActual());
        assertEquals(AHORA, bolsin.getCambioEstados().get(0).getFechaHoraFin());
        assertNull(bolsin.getCambioEstados().get(1).getFechaHoraFin());
    }

    @Test
    void dadoUnBolsinEnviado_cuandoSeMarcaUnaDocumentacionComoNoRecibida_entoncesElRemitoQuedaParcialYLaOtraAceptada() {
        // Given
        Bolsin bolsin = bolsinEnviadoCon2Documentaciones(cordoba());

        // When
        bolsin.registrarBolsinRecibido(AHORA, EB, bolsin.buscarCambioEstadoActual(),
                OpcionRecepcion.DOCUMENTACION_NO_RECIBIDA, Set.of(2));

        // Then
        assertInstanceOf(NoRecibida.class, documentacionDe(bolsin, 2).getEstadoActual());
        assertInstanceOf(RecibidaYAceptada.class, documentacionDe(bolsin, 1).getEstadoActual());
        assertInstanceOf(RecibidoYAceptadoParcial.class, bolsin.getRemitos().get(0).getEstadoActual());
    }

    @Test
    void dadoUnBolsinEnviadoACentral_cuandoSeMarcaDocumentacionParaRedirigir_entoncesQuedaParaRedirigir() {
        // Given
        Bolsin bolsin = bolsinEnviadoCon2Documentaciones(central());

        // When
        bolsin.registrarBolsinRecibido(AHORA, encargadoDe(central()), bolsin.buscarCambioEstadoActual(),
                OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR, Set.of(1));

        // Then
        assertInstanceOf(bolsines.entity.estado.documentacion.ParaRedirigir.class,
                documentacionDe(bolsin, 1).getEstadoActual());
    }

    @Test
    void dadoUnBolsinEnviadoQueNoVaALaCentral_cuandoSePideRedirigirDocumentacion_entoncesSeRechazaAntesDeCambiarNada() {
        // Given: la guarda [comisión destino igual CMC] la evalúa el estado Enviado antes de tocar remitos y documentación
        Bolsin bolsin = bolsinEnviadoCon2Documentaciones(cordoba());

        // When / Then
        assertThrows(OpcionDeRecepcionInvalidaException.class, () -> bolsin.registrarBolsinRecibido(
                AHORA, EB, bolsin.buscarCambioEstadoActual(), OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR, Set.of(1)));
        assertInstanceOf(Enviado.class, bolsin.getEstadoActual());
        assertInstanceOf(EnBolsinEnviado.class, documentacionDe(bolsin, 1).getEstadoActual());
        assertInstanceOf(bolsines.entity.estado.remito.EnBolsinEnviado.class, bolsin.getRemitos().get(0).getEstadoActual());
        assertEquals(1, bolsin.getCambioEstados().size());
    }

    @Test
    void dadoUnBolsin_cuandoSeConsultaSiEstaEnviado_entoncesRespondeSegunElEstadoDelCambioVigente() {
        // Given / When / Then
        assertTrue(bolsin(1, new Enviado(), rosario(), cordoba(), EB).sosEnviado());
        assertFalse(bolsin(2, new RecibidoEnCMDestino(), rosario(), cordoba(), EB).sosEnviado());
    }

    @Test
    void dadosDosEstadosDeBolsin_cuandoSeComparan_entoncesSonIgualesSoloSiSonDelMismoTipo() {
        // Given / When / Then
        assertEquals(new Enviado(), new Enviado());
        assertNotEquals(new Enviado(), new Cerrado());
    }
}
