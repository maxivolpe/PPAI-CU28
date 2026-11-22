package bolsines.entity;

import bolsines.entity.estado.documentacion.DeBaja;
import bolsines.entity.estado.documentacion.EnBolsinEnviado;
import bolsines.entity.estado.documentacion.EnBolsinSaliente;
import bolsines.entity.estado.documentacion.EnRemito;
import bolsines.entity.estado.documentacion.EstadoDocumentacion;
import bolsines.entity.estado.documentacion.NoRecibida;
import bolsines.entity.estado.documentacion.ParaRedirigir;
import bolsines.entity.estado.documentacion.RecibidaYAceptada;
import bolsines.entity.estado.documentacion.RecibidaYRechazada;
import bolsines.entity.estado.documentacion.Registrada;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static bolsines.soporte.Escenarios.AHORA;
import static bolsines.soporte.Escenarios.cordoba;
import static bolsines.soporte.Escenarios.documentacion;
import static bolsines.soporte.Escenarios.encargadoDe;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Patrón State sobre la máquina de estados de Documentación: qué transiciones existen y cuáles se rechazan. */
class EstadosDeDocumentacionTest {

    private static final Empleado EB = encargadoDe(cordoba());

    private static Stream<Arguments> eventosDeRecepcion() {
        return Stream.of(
                Arguments.of(evento("registrarBolsinRecibido", d -> d.registrarBolsinRecibido(AHORA, EB)),
                        RecibidaYAceptada.class),
                Arguments.of(evento("registrarDocumentacionNoRecibida", d -> d.registrarDocumentacionNoRecibida(AHORA, EB)),
                        NoRecibida.class),
                Arguments.of(evento("registrarRechazoDeDocumentacion", d -> d.registrarRechazoDeDocumentacion(AHORA, EB)),
                        RecibidaYRechazada.class),
                Arguments.of(evento("registrarDocumentacionParaRedirigir",
                        d -> d.registrarDocumentacionParaRedirigir(AHORA, EB)), ParaRedirigir.class));
    }

    private static Stream<Arguments> estadosDistintosDeEnBolsinEnviadoPorEvento() {
        List<EstadoDocumentacion> estados = List.of(new Registrada(), new EnRemito(), new EnBolsinSaliente(),
                new ParaRedirigir(), new RecibidaYAceptada(), new RecibidaYRechazada(), new NoRecibida(),
                new DeBaja());
        return estados.stream().flatMap(estado -> eventosDeRecepcion()
                .map(argumentos -> Arguments.of(estado, argumentos.get()[0])));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("eventosDeRecepcion")
    void dadaUnaDocumentacionEnBolsinEnviado_cuandoSeAplicaUnEventoDeRecepcion_entoncesPasaAlEstadoSiguienteYAbreUnNuevoCambio(
            Consumer<Documentacion> evento, Class<? extends EstadoDocumentacion> estadoEsperado) {
        // Given
        Documentacion documentacion = documentacion(1, new EnBolsinEnviado(), EB);

        // When
        evento.accept(documentacion);

        // Then
        List<CambioEstadoDocumentacion> historial = documentacion.getCambioEstadoDocumentacion();
        assertInstanceOf(estadoEsperado, documentacion.getEstadoActual());
        assertEquals(2, historial.size());
        assertEquals(AHORA, historial.get(0).getFechaHoraFin(), "el estado anterior se cierra en el momento de la recepción");
        assertEquals(AHORA, historial.get(1).getFechaHoraInicio(), "el estado nuevo empieza en el mismo instante");
        assertNull(historial.get(1).getFechaHoraFin(), "el estado nuevo queda vigente");
        assertEquals(EB, historial.get(1).getResponsable());
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("estadosDistintosDeEnBolsinEnviadoPorEvento")
    void dadaUnaDocumentacionQueNoEstaEnBolsinEnviado_cuandoSeAplicaUnEventoDeRecepcion_entoncesLaTransicionNoEstaPermitida(
            EstadoDocumentacion estado, Consumer<Documentacion> evento) {
        // Given
        Documentacion documentacion = documentacion(1, estado, EB);

        // When / Then
        assertThrows(TransicionNoPermitidaException.class, () -> evento.accept(documentacion));
        assertEquals(estado, documentacion.getEstadoActual(), "un evento rechazado no cambia el estado");
        assertEquals(1, documentacion.getCambioEstadoDocumentacion().size(), "ni agrega historial");
    }

    @Test
    void dadosLosEstadosQueNoLlegaronOFueronRechazados_cuandoSeConsultaSiDejanElRemitoIncompleto_entoncesSoloEsosRespondenQueSi() {
        // Given / When / Then
        assertTrue(new NoRecibida().dejaRemitoIncompleto());
        assertTrue(new RecibidaYRechazada().dejaRemitoIncompleto());
        assertFalse(new RecibidaYAceptada().dejaRemitoIncompleto());
        assertFalse(new ParaRedirigir().dejaRemitoIncompleto());
        assertFalse(new EnBolsinEnviado().dejaRemitoIncompleto());
    }

    @Test
    void dadosDosEstadosDelMismoTipo_cuandoSeComparan_entoncesSonIguales() {
        // Given / When / Then
        assertEquals(new NoRecibida(), new NoRecibida());
        assertEquals(new NoRecibida().hashCode(), new NoRecibida().hashCode());
        assertNotEquals(new NoRecibida(), new RecibidaYRechazada());
    }

    private static Named<Consumer<Documentacion>> evento(String nombre, Consumer<Documentacion> evento) {
        return Named.of(nombre, evento);
    }
}
