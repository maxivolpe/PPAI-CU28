package bolsines;

import bolsines.entity.Bolsin;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.CambioEstadoDocumentacion;
import bolsines.entity.ComisionMedica;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Remito;
import bolsines.entity.TipoDocumentacion;
import bolsines.entity.TransicionNoPermitidaException;
import bolsines.entity.Usuario;
import bolsines.entity.estado.bolsin.Cerrado;
import bolsines.entity.estado.bolsin.Creado;
import bolsines.entity.estado.bolsin.DeBaja;
import bolsines.entity.estado.bolsin.Enviado;
import bolsines.entity.estado.bolsin.EstadoBolsin;
import bolsines.entity.estado.bolsin.RecibidoEnCMDestino;
import bolsines.entity.estado.documentacion.EnBolsinEnviado;
import bolsines.entity.estado.documentacion.EnBolsinSaliente;
import bolsines.entity.estado.documentacion.EnRemito;
import bolsines.entity.estado.documentacion.EstadoDocumentacion;
import bolsines.entity.estado.documentacion.NoRecibida;
import bolsines.entity.estado.documentacion.ParaRedirigir;
import bolsines.entity.estado.documentacion.RecibidaYAceptada;
import bolsines.entity.estado.documentacion.RecibidaYRechazada;
import bolsines.entity.estado.documentacion.Registrada;
import bolsines.entity.estado.remito.RecibidoYAceptado;
import bolsines.entity.estado.remito.RecibidoYAceptadoParcial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PRUEBAS UNITARIAS: las clases del dominio solas, sin base de datos, sin pantallas y sin red.
 * Los objetos se arman en memoria (ver «Escenarios» al final) y todo se ejecuta en milisegundos.
 * Cada clase anidada agrupa las pruebas de un tema.
 */
@DisplayName("1. Pruebas unitarias del dominio")
class PruebasUnitariasTest {

    /** Cuando el bolsín y su documentación pasaron al estado que tienen al empezar cada prueba. */
    static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 12, 8, 30);
    /** El momento en que el encargado registra la recepción. */
    static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 10, 15, 30);

    static final Empleado EB_CORDOBA = empleado("cmendez", cordoba());
    static final Empleado EMISOR = empleado("sbenitez", rosario());

    @Nested
    @DisplayName("Máquina de estados de Documentación (patrón State)")
    class MaquinaDeEstadosDeDocumentacion {

        @ParameterizedTest(name = "{0}")
        @MethodSource("bolsines.PruebasUnitariasTest#eventosDeRecepcion")
        void dadaUnaDocumentacionEnBolsinEnviado_cuandoSeAplicaUnEventoDeRecepcion_entoncesPasaAlEstadoSiguienteYAbreUnNuevoCambio(
                Consumer<Documentacion> evento, Class<? extends EstadoDocumentacion> estadoEsperado) {
            // Given
            Documentacion documentacion = documentacion(1, new EnBolsinEnviado());

            // When
            evento.accept(documentacion);

            // Then
            List<CambioEstadoDocumentacion> historial = documentacion.getCambioEstadoDocumentacion();
            assertAll(
                    () -> assertInstanceOf(estadoEsperado, documentacion.getEstadoActual()),
                    () -> assertEquals(2, historial.size()),
                    () -> assertEquals(AHORA, historial.get(0).getFechaHoraFin(), "se cierra el estado anterior"),
                    () -> assertEquals(AHORA, historial.get(1).getFechaHoraInicio(), "el nuevo empieza en ese instante"),
                    () -> assertNull(historial.get(1).getFechaHoraFin(), "el nuevo queda vigente"),
                    () -> assertEquals(EB_CORDOBA, historial.get(1).getResponsable()));
        }

        @ParameterizedTest(name = "{0} / {1}")
        @MethodSource("bolsines.PruebasUnitariasTest#estadosDeDocumentacionQueNoSonEnBolsinEnviadoPorEvento")
        void dadaUnaDocumentacionQueNoEstaEnBolsinEnviado_cuandoSeAplicaUnEventoDeRecepcion_entoncesLaTransicionNoEstaPermitida(
                EstadoDocumentacion estado, Consumer<Documentacion> evento) {
            // Given
            Documentacion documentacion = documentacion(1, estado);

            // When / Then
            assertThrows(TransicionNoPermitidaException.class, () -> evento.accept(documentacion));
            assertEquals(estado, documentacion.getEstadoActual(), "un evento rechazado no cambia el estado");
            assertEquals(1, documentacion.getCambioEstadoDocumentacion().size(), "ni agrega historial");
        }

        @Test
        void dadosLosEstadosDeDocumentacion_cuandoSeConsultaSiDejanElRemitoIncompleto_entoncesSoloNoRecibidaYRechazadaLoDejan() {
            assertAll(
                    () -> assertTrue(new NoRecibida().dejaRemitoIncompleto()),
                    () -> assertTrue(new RecibidaYRechazada().dejaRemitoIncompleto()),
                    () -> assertFalse(new RecibidaYAceptada().dejaRemitoIncompleto()),
                    () -> assertFalse(new ParaRedirigir().dejaRemitoIncompleto()),
                    () -> assertFalse(new EnBolsinEnviado().dejaRemitoIncompleto()));
        }

        @Test
        void dadosDosEstadosDeDocumentacion_cuandoSeComparan_entoncesSonIgualesSoloSiSonDelMismoTipo() {
            assertAll(
                    () -> assertEquals(new NoRecibida(), new NoRecibida()),
                    () -> assertEquals(new NoRecibida().hashCode(), new NoRecibida().hashCode()),
                    () -> assertNotEquals(new NoRecibida(), new RecibidaYRechazada()),
                    () -> assertEquals("No Recibida", new NoRecibida().toString()));
        }
    }

    @Nested
    @DisplayName("Máquina de estados de Bolsín y cascada a Remito y Documentación")
    class MaquinaDeEstadosDeBolsin {

        @ParameterizedTest(name = "{0}")
        @MethodSource("bolsines.PruebasUnitariasTest#estadosDeBolsinQueNoSonEnviado")
        void dadoUnBolsinQueNoEstaEnviado_cuandoSeRegistraLaRecepcion_entoncesLaTransicionNoEstaPermitida(EstadoBolsin estado) {
            // Given
            Bolsin bolsin = bolsinEn(estado, cordoba());

            // When / Then
            assertThrows(TransicionNoPermitidaException.class, () -> recibir(bolsin, OpcionRecepcion.CONTENIDO_IGUAL, Set.of()));
            assertEquals(estado, bolsin.getEstadoActual());
        }

        @Test
        void dadoUnBolsinEnviado_cuandoSeRegistraLaRecepcionConLaOpcion1_entoncesBolsinRemitoYDocumentacionQuedanAceptados() {
            // Given
            Bolsin bolsin = bolsinEnviadoA(cordoba());

            // When
            recibir(bolsin, OpcionRecepcion.CONTENIDO_IGUAL, Set.of());

            // Then
            assertAll(
                    () -> assertInstanceOf(RecibidoEnCMDestino.class, bolsin.getEstadoActual()),
                    () -> assertInstanceOf(RecibidoYAceptado.class, bolsin.getRemitos().get(0).getEstadoActual()),
                    () -> assertInstanceOf(RecibidaYAceptada.class, documentacionDe(bolsin, 1).getEstadoActual()),
                    () -> assertInstanceOf(RecibidaYAceptada.class, documentacionDe(bolsin, 2).getEstadoActual()),
                    () -> assertEquals(AHORA, bolsin.getCambioEstados().get(0).getFechaHoraFin()),
                    () -> assertNull(bolsin.getCambioEstados().get(1).getFechaHoraFin()));
        }

        @Test
        void dadaUnaDocumentacionMarcadaComoNoRecibida_cuandoSeRegistraLaRecepcion_entoncesQuedaNoRecibidaYElRemitoParcial() {
            // Given: flujo A3
            Bolsin bolsin = bolsinEnviadoA(cordoba());

            // When
            recibir(bolsin, OpcionRecepcion.DOCUMENTACION_NO_RECIBIDA, Set.of(2));

            // Then
            assertAll(
                    () -> assertInstanceOf(NoRecibida.class, documentacionDe(bolsin, 2).getEstadoActual()),
                    () -> assertInstanceOf(RecibidaYAceptada.class, documentacionDe(bolsin, 1).getEstadoActual()),
                    () -> assertInstanceOf(RecibidoYAceptadoParcial.class, bolsin.getRemitos().get(0).getEstadoActual()));
        }

        @Test
        void dadaUnaDocumentacionQueNoCorresponde_cuandoSeRegistraLaRecepcion_entoncesQuedaRechazadaYElRemitoParcial() {
            // Given: flujo A4
            Bolsin bolsin = bolsinEnviadoA(cordoba());

            // When
            recibir(bolsin, OpcionRecepcion.DOCUMENTACION_NO_CORRESPONDE, Set.of(1));

            // Then
            assertInstanceOf(RecibidaYRechazada.class, documentacionDe(bolsin, 1).getEstadoActual());
            assertInstanceOf(RecibidoYAceptadoParcial.class, bolsin.getRemitos().get(0).getEstadoActual());
        }

        @Test
        void dadoUnBolsinEnviadoALaCentral_cuandoSeMarcaDocumentacionParaRedirigir_entoncesQuedaParaRedirigirYElRemitoAceptado() {
            // Given: flujo A5
            Bolsin bolsin = bolsinEnviadoA(central());

            // When
            recibir(bolsin, OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR, Set.of(1));

            // Then
            assertInstanceOf(ParaRedirigir.class, documentacionDe(bolsin, 1).getEstadoActual());
            assertInstanceOf(RecibidoYAceptado.class, bolsin.getRemitos().get(0).getEstadoActual());
        }

        @Test
        void dadoUnBolsinQueNoVaALaCentral_cuandoSePideRedirigirDocumentacion_entoncesSeRechazaAntesDeCambiarNada() {
            // Given: la guarda [comisión destino igual CMC] la evalúa el estado Enviado antes de tocar nada
            Bolsin bolsin = bolsinEnviadoA(cordoba());

            // When / Then
            assertThrows(IllegalStateException.class,
                    () -> recibir(bolsin, OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR, Set.of(1)));
            assertAll(
                    () -> assertInstanceOf(Enviado.class, bolsin.getEstadoActual()),
                    () -> assertInstanceOf(EnBolsinEnviado.class, documentacionDe(bolsin, 1).getEstadoActual()),
                    () -> assertEquals(1, bolsin.getCambioEstados().size()));
        }

        @Test
        void dadosDosEstadosDeBolsin_cuandoSeComparan_entoncesSonIgualesSoloSiSonDelMismoTipo() {
            assertEquals(new Enviado(), new Enviado());
            assertNotEquals(new Enviado(), new Cerrado());
        }
    }

    @Nested
    @DisplayName("Opciones de recepción")
    class OpcionesDeRecepcion {

        @ParameterizedTest
        @EnumSource(OpcionRecepcion.class)
        void dadaLaCMCentralComoDestino_cuandoSeConsultaCadaOpcion_entoncesTodasEstanDisponibles(OpcionRecepcion opcion) {
            assertTrue(opcion.estaDisponiblePara(central()));
        }

        @ParameterizedTest
        @EnumSource(value = OpcionRecepcion.class, names = "DOCUMENTACION_PARA_REDIRIGIR", mode = EnumSource.Mode.EXCLUDE)
        void dadaUnaCMQueNoEsLaCentral_cuandoSeConsultaUnaOpcionQueNoEsRedirigir_entoncesEstaDisponible(OpcionRecepcion opcion) {
            assertTrue(opcion.estaDisponiblePara(cordoba()));
        }

        @Test
        void dadaUnaCMQueNoEsLaCentral_cuandoSeConsultaLaOpcionRedirigir_entoncesNoEstaDisponible() {
            assertFalse(OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR.estaDisponiblePara(cordoba()));
        }

        @Test
        void dadasLasCuatroOpciones_cuandoSePreguntaCualesPidenMarcarDocumentacion_entoncesTodasMenosLaPrimera() {
            assertAll(
                    () -> assertFalse(OpcionRecepcion.CONTENIDO_IGUAL.requiereSeleccionDeDocumentacion()),
                    () -> assertTrue(OpcionRecepcion.DOCUMENTACION_NO_RECIBIDA.requiereSeleccionDeDocumentacion()),
                    () -> assertTrue(OpcionRecepcion.DOCUMENTACION_NO_CORRESPONDE.requiereSeleccionDeDocumentacion()),
                    () -> assertTrue(OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR.requiereSeleccionDeDocumentacion()));
        }

        @ParameterizedTest
        @EnumSource(OpcionRecepcion.class)
        void dadoElNumeroDeUnaOpcion_cuandoSeLaBusca_entoncesDevuelveEsaOpcion(OpcionRecepcion opcion) {
            assertEquals(opcion, OpcionRecepcion.desdeNumero(opcion.getNumero()));
        }

        @Test
        void dadoUnNumeroQueNoEsDeNingunaOpcion_cuandoSeLoBusca_entoncesSeRechaza() {
            assertThrows(IllegalArgumentException.class, () -> OpcionRecepcion.desdeNumero(9));
        }
    }

    @Nested
    @DisplayName("Consultas del bolsín")
    class ConsultasDelBolsin {

        @Test
        void dadoUnBolsin_cuandoSePreguntaSiEstaEnviado_entoncesRespondeSegunSuCambioDeEstadoVigente() {
            assertTrue(bolsinEn(new Enviado(), cordoba()).sosEnviado());
            assertFalse(bolsinEn(new RecibidoEnCMDestino(), cordoba()).sosEnviado());
        }

        @Test
        void dadoUnBolsin_cuandoSeLoFiltraPorPrecinto_entoncesSoloCoincideConSuNumeroDePrecinto() {
            Bolsin bolsin = bolsinEn(new Enviado(), cordoba());
            assertTrue(bolsin.esDePrecinto(700100));
            assertFalse(bolsin.esDePrecinto(999999));
        }

        @Test
        void dadoUnBolsin_cuandoSeLoFiltraPorCMDeOrigen_entoncesAlcanzaConParteDelNombreSinImportarMayusculasNiEspacios() {
            Bolsin bolsin = bolsinEn(new Enviado(), cordoba());
            assertTrue(bolsin.esDeCMOrigen("  ROSARIO "));
            assertFalse(bolsin.esDeCMOrigen("Mendoza"));
        }

        @Test
        void dadoUnBolsin_cuandoSeIntentaModificarSusListasDesdeAfuera_entoncesNoSePuede() {
            Bolsin bolsin = bolsinEnviadoA(cordoba());
            assertThrows(UnsupportedOperationException.class, () -> bolsin.getRemitos().clear());
            assertThrows(UnsupportedOperationException.class, () -> bolsin.getCambioEstados().clear());
            assertThrows(UnsupportedOperationException.class,
                    () -> bolsin.getRemitos().get(0).getDetallesRemito().clear());
        }
    }

    // ---------------------------------------------------------------- datos de las pruebas parametrizadas

    static Stream<Arguments> eventosDeRecepcion() {
        return Stream.of(
                Arguments.of(evento("registrarBolsinRecibido", d -> d.registrarBolsinRecibido(AHORA, EB_CORDOBA)),
                        RecibidaYAceptada.class),
                Arguments.of(evento("registrarDocumentacionNoRecibida",
                        d -> d.registrarDocumentacionNoRecibida(AHORA, EB_CORDOBA)), NoRecibida.class),
                Arguments.of(evento("registrarRechazoDeDocumentacion",
                        d -> d.registrarRechazoDeDocumentacion(AHORA, EB_CORDOBA)), RecibidaYRechazada.class),
                Arguments.of(evento("registrarDocumentacionParaRedirigir",
                        d -> d.registrarDocumentacionParaRedirigir(AHORA, EB_CORDOBA)), ParaRedirigir.class));
    }

    static Stream<Arguments> estadosDeDocumentacionQueNoSonEnBolsinEnviadoPorEvento() {
        List<EstadoDocumentacion> estados = List.of(new Registrada(), new EnRemito(), new EnBolsinSaliente(),
                new ParaRedirigir(), new RecibidaYAceptada(), new RecibidaYRechazada(), new NoRecibida(),
                new bolsines.entity.estado.documentacion.DeBaja());
        return estados.stream().flatMap(estado -> eventosDeRecepcion()
                .map(argumentos -> Arguments.of(estado, argumentos.get()[0])));
    }

    static Stream<EstadoBolsin> estadosDeBolsinQueNoSonEnviado() {
        return Stream.of(new Creado(), new Cerrado(), new RecibidoEnCMDestino(), new DeBaja());
    }

    private static Named<Consumer<Documentacion>> evento(String nombre, Consumer<Documentacion> evento) {
        return Named.of(nombre, evento);
    }

    // ---------------------------------------------------------------- escenarios (objetos armados en memoria)

    static ComisionMedica cordoba() {
        return new ComisionMedica("CBA", "Comisión Médica Córdoba", "Av. Colón 150", "cba@organismo.gob.ar", "0351", false);
    }

    static ComisionMedica rosario() {
        return new ComisionMedica("ROS", "Comisión Médica Rosario", "San Martín 980", "ros@organismo.gob.ar", "0341", false);
    }

    static ComisionMedica central() {
        return new ComisionMedica("CMC", "Comisión Médica Central", "Av. Corrientes 800", "cmc@organismo.gob.ar", "011", true);
    }

    static Empleado empleado(String nombreUsuario, ComisionMedica comisionMedica) {
        return new Empleado("Nombre " + nombreUsuario, "Apellido " + nombreUsuario,
                nombreUsuario + "@organismo.gob.ar", comisionMedica, new Usuario(nombreUsuario, "clave"));
    }

    /** Una documentación en ese estado, con su cambio de estado vigente (como queda al leerla de la base). */
    static Documentacion documentacion(int numero, EstadoDocumentacion estado) {
        Documentacion documentacion = new Documentacion(numero, "Asunto " + numero, LocalDate.of(2026, 11, 9),
                new TipoDocumentacion("Expediente", "Expediente administrativo"), estado);
        documentacion.agregarCambioEstado(new CambioEstadoDocumentacion(INICIO, EMISOR, estado));
        return documentacion;
    }

    /** El bolsín 100, de Rosario a {@code destino}, en ese estado y sin remitos. Su precinto es 700100. */
    static Bolsin bolsinEn(EstadoBolsin estado, ComisionMedica destino) {
        Bolsin bolsin = new Bolsin(100, 700100, LocalDate.of(2026, 11, 10), 2000, rosario(), destino, estado);
        bolsin.agregarCambioEstado(new CambioEstadoBolsin(INICIO, EMISOR, estado));
        return bolsin;
    }

    /** El bolsín 100 «Enviado» a {@code destino}, con el remito 10 que lleva las documentaciones 1 y 2. */
    static Bolsin bolsinEnviadoA(ComisionMedica destino) {
        Remito remito = new Remito(10, LocalDate.of(2026, 11, 9), rosario(), destino,
                new bolsines.entity.estado.remito.EnBolsinEnviado());
        remito.agregarDetalle(new DetalleRemito(null, documentacion(1, new EnBolsinEnviado())));
        remito.agregarDetalle(new DetalleRemito(null, documentacion(2, new EnBolsinEnviado())));
        Bolsin bolsin = bolsinEn(new Enviado(), destino);
        bolsin.agregarRemito(remito);
        return bolsin;
    }

    /** Registra la recepción del bolsín como lo hace el gestor: una sola llamada al contexto. */
    static void recibir(Bolsin bolsin, OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        Empleado responsable = bolsin.getDestino().sosCentral() ? empleado("hrojas", central()) : EB_CORDOBA;
        bolsin.registrarBolsinRecibido(AHORA, responsable, bolsin.buscarCambioEstadoActual(), opcion, documentacionMarcada);
    }

    static Documentacion documentacionDe(Bolsin bolsin, int numero) {
        return bolsin.getRemitos().stream()
                .flatMap(remito -> remito.getDetallesRemito().stream())
                .map(DetalleRemito::getDocumentacion)
                .filter(documentacion -> documentacion.getNumero() == numero)
                .findFirst().orElseThrow();
    }
}
