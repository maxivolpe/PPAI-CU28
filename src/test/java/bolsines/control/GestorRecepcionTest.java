package bolsines.control;

import bolsines.boundary.InterfazServidorCorreo;
import bolsines.boundary.ResumenDeBolsin;
import bolsines.entity.Bolsin;
import bolsines.entity.BolsinFueraDelListadoException;
import bolsines.entity.ComisionMedica;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionDeRecepcionInvalidaException;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Sesion;
import bolsines.entity.estado.bolsin.Cerrado;
import bolsines.entity.estado.bolsin.Enviado;
import bolsines.entity.estado.bolsin.RecibidoEnCMDestino;
import bolsines.entity.estado.documentacion.NoRecibida;
import bolsines.entity.estado.remito.RecibidoYAceptadoParcial;
import bolsines.soporte.PantallaRecepcionEspia;
import bolsines.soporte.RepositorioEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import static bolsines.soporte.Escenarios.AHORA;
import static bolsines.soporte.Escenarios.RELOJ_FIJO;
import static bolsines.soporte.Escenarios.bolsin;
import static bolsines.soporte.Escenarios.bolsinEnviadoCon2Documentaciones;
import static bolsines.soporte.Escenarios.central;
import static bolsines.soporte.Escenarios.cordoba;
import static bolsines.soporte.Escenarios.documentacionDe;
import static bolsines.soporte.Escenarios.empleado;
import static bolsines.soporte.Escenarios.rosario;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El GestorRecepcion sin base de datos ni consola: repositorio en memoria, pantalla espía y
 * reloj fijo. Cada flujo del caso de uso se prueba en milisegundos.
 */
@DisplayName("GestorRecepcion")
class GestorRecepcionTest {

    private final ComisionMedica cordoba = cordoba();
    private final Empleado ebCordoba = empleado("cmendez", cordoba);
    private final Empleado emisor = empleado("sbenitez", rosario());

    /** Enviado a Córdoba: es el que el EB puede recibir. Documentaciones 1 y 2 en el remito 10. */
    private Bolsin bolsinParaCordoba;
    private RepositorioEnMemoria repositorio;
    private PantallaRecepcionEspia pantalla;
    private InterfazServidorCorreo correo;
    private GestorRecepcion gestor;

    @BeforeEach
    void armarEscenario() {
        bolsinParaCordoba = bolsinEnviadoCon2Documentaciones(cordoba);
        Bolsin cerradoParaCordoba = bolsin(200, new Cerrado(), rosario(), cordoba, emisor);
        Bolsin enviadoParaCentral = bolsin(300, new Enviado(), rosario(), central(), emisor);
        repositorio = new RepositorioEnMemoria()
                .conEmpleados(ebCordoba, emisor)
                .conBolsines(bolsinParaCordoba, cerradoParaCordoba, enviadoParaCentral);
        pantalla = new PantallaRecepcionEspia();
        correo = new InterfazServidorCorreo(new PrintStream(new ByteArrayOutputStream()));
        gestor = gestorPara(ebCordoba);
    }

    @Nested
    @DisplayName("Pasos 1 a 6: listado y selección")
    class ListadoYSeleccion {

        @Test
        void dadosBolsinesDeVariosDestinosYEstados_cuandoElEBIniciaElCasoDeUso_entoncesSeLeOfrecenSoloLosEnviadosASuCM() {
            // When
            gestor.opcRegistrarRecepcion(pantalla);

            // Then
            assertTrue(pantalla.mensajes.contains("CM: Comisión Médica Córdoba"));
            assertEquals(List.of(new ResumenDeBolsin(100, "Comisión Médica Rosario", 700100)), pantalla.bolsinesOfrecidos);
        }

        @Test
        void dadoUnEBSinBolsinesPendientes_cuandoIniciaElCasoDeUso_entoncesSeLeInformaYTermina() {
            // Given
            Empleado ebRosario = empleado("tcabrera", rosario());
            repositorio.conEmpleados(ebRosario);

            // When
            gestorPara(ebRosario).opcRegistrarRecepcion(pantalla);

            // Then
            assertTrue(pantalla.mensajes.contains("No hay bolsines en estado Enviado para Comisión Médica Rosario."));
            assertTrue(pantalla.bolsinesOfrecidos.isEmpty());
        }

        @Test
        void dadoElListado_cuandoElEBSeleccionaUnBolsin_entoncesSeMuestraSuInformacionYSePideLaOpcion() {
            // Given
            gestor.opcRegistrarRecepcion(pantalla);

            // When
            gestor.tomarSeleccionBolsin(100);

            // Then
            assertEquals(100, pantalla.bolsinMostrado.numero());
            assertEquals(Set.of(10), pantalla.informacionMostrada.keySet());
            assertEquals(1, pantalla.vecesQueSePidioLaOpcion);
            assertEquals(cordoba, pantalla.cmParaLaOpcion);
        }

        @Test
        void dadoUnBolsinQueNoFueOfrecido_cuandoElEBIntentaSeleccionarlo_entoncesSeRechaza() {
            // Given
            gestor.opcRegistrarRecepcion(pantalla);

            // When / Then: el 300 va a la Central, no a Córdoba
            assertThrows(BolsinFueraDelListadoException.class, () -> gestor.tomarSeleccionBolsin(300));
        }
    }

    @Nested
    @DisplayName("A1 y A2: filtros")
    class Filtros {

        @Test
        void dadoUnPrecintoDelListado_cuandoElEBFiltra_entoncesDevuelveEseBolsin() {
            gestor.opcRegistrarRecepcion(pantalla);
            assertEquals(List.of(100), numeros(gestor.tomarFiltroPrecinto(700100)));
        }

        @Test
        void dadoUnPrecintoInexistente_cuandoElEBFiltra_entoncesNoDevuelveNingunoYEsElFlujoA1() {
            gestor.opcRegistrarRecepcion(pantalla);
            assertTrue(gestor.tomarFiltroPrecinto(999999).isEmpty());
        }

        @Test
        void dadaParteDelNombreDeLaCMDeOrigen_cuandoElEBFiltra_entoncesNoDistingueMayusculasNiEspacios() {
            gestor.opcRegistrarRecepcion(pantalla);
            assertEquals(List.of(100), numeros(gestor.tomarFiltroCMOrigen("  ROSARIO ")));
        }

        @Test
        void dadaUnaCMDeOrigenSinBolsines_cuandoElEBFiltra_entoncesNoDevuelveNingunoYEsElFlujoA2() {
            gestor.opcRegistrarRecepcion(pantalla);
            assertTrue(gestor.tomarFiltroCMOrigen("Mendoza").isEmpty());
        }
    }

    @Nested
    @DisplayName("Pasos 8 a 13: opción, confirmación y registro")
    class Registro {

        @Test
        void dadoUnBolsinSeleccionado_cuandoElEBConfirmaLaOpcion1_entoncesQuedaRecibidoSeGuardaYSeNotifica() {
            // Given
            seleccionarBolsinParaCordoba();
            gestor.tomarOpcRecepcion(OpcionRecepcion.CONTENIDO_IGUAL, Set.of());

            // When
            gestor.tomarConfirmacion(true);

            // Then
            assertAll(
                    () -> assertInstanceOf(RecibidoEnCMDestino.class, bolsinParaCordoba.getEstadoActual()),
                    () -> assertEquals(List.of(bolsinParaCordoba), repositorio.recepcionesGuardadas),
                    () -> assertEquals("ros@organismo.gob.ar", correo.getEmailsEnviados().get(0).destinatario()),
                    () -> assertEquals("100: Recibido en CM Destino", pantalla.exito));
        }

        @Test
        void dadoUnRelojFijo_cuandoElEBConfirma_entoncesElHistorialUsaEsaFechaYHoraExactas() {
            // Given
            seleccionarBolsinParaCordoba();
            gestor.tomarOpcRecepcion(OpcionRecepcion.CONTENIDO_IGUAL, Set.of());

            // When
            gestor.tomarConfirmacion(true);

            // Then
            assertEquals(AHORA, bolsinParaCordoba.getCambioEstados().get(0).getFechaHoraFin());
            assertEquals(AHORA, bolsinParaCordoba.getCambioEstados().get(1).getFechaHoraInicio());
            assertEquals(ebCordoba, bolsinParaCordoba.getCambioEstados().get(1).getResponsable());
        }

        @Test
        void dadaUnaDocumentacionMarcadaComoNoRecibida_cuandoElEBConfirma_entoncesEsaQuedaNoRecibidaYElRemitoParcial() {
            // Given: flujo A3
            seleccionarBolsinParaCordoba();
            gestor.tomarOpcRecepcion(OpcionRecepcion.DOCUMENTACION_NO_RECIBIDA, Set.of(2));

            // When
            gestor.tomarConfirmacion(true);

            // Then
            assertInstanceOf(NoRecibida.class, documentacionDe(bolsinParaCordoba, 2).getEstadoActual());
            assertInstanceOf(RecibidoYAceptadoParcial.class, bolsinParaCordoba.getRemitos().get(0).getEstadoActual());
        }

        @Test
        void dadaUnaCMQueNoEsLaCentral_cuandoElEBEligeRedirigir_entoncesSeRechazaSinPedirConfirmacion() {
            // Given
            seleccionarBolsinParaCordoba();

            // When / Then: flujo A5 fuera de la CMC
            assertThrows(OpcionDeRecepcionInvalidaException.class,
                    () -> gestor.tomarOpcRecepcion(OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR, Set.of(1)));
            assertFalse(pantalla.confirmacionSolicitada);
        }

        @Test
        void dadaUnaOpcionElegida_cuandoElEBNoConfirma_entoncesSeVuelveAPedirLaOpcionYNoSeGuardaNada() {
            // Given: flujo A6
            seleccionarBolsinParaCordoba();
            gestor.tomarOpcRecepcion(OpcionRecepcion.CONTENIDO_IGUAL, Set.of());

            // When
            gestor.tomarConfirmacion(false);

            // Then
            assertTrue(pantalla.mensajes.contains(
                    "No se confirmó la selección. Elija nuevamente la opción de recepción."));
            assertEquals(2, pantalla.vecesQueSePidioLaOpcion);
            assertTrue(repositorio.recepcionesGuardadas.isEmpty());
            assertInstanceOf(Enviado.class, bolsinParaCordoba.getEstadoActual());
        }

        @Test
        void dadoUnBolsinSeleccionado_cuandoElEBCancela_entoncesNoSeGuardaNada() {
            // Given: observación 1
            seleccionarBolsinParaCordoba();

            // When
            gestor.cancelarCasoDeUso();

            // Then
            assertTrue(pantalla.mensajes.contains("Caso de uso cancelado. No se registró ningún cambio."));
            assertTrue(repositorio.recepcionesGuardadas.isEmpty());
        }
    }

    // ---------------------------------------------------------------- ayudas

    private GestorRecepcion gestorPara(Empleado empleado) {
        return new GestorRecepcion(repositorio, repositorio, new Sesion(AHORA, empleado.getUsuario()), correo,
                RELOJ_FIJO);
    }

    private void seleccionarBolsinParaCordoba() {
        gestor.opcRegistrarRecepcion(pantalla);
        gestor.tomarSeleccionBolsin(bolsinParaCordoba.getNumeroBolsin());
    }

    private static List<Integer> numeros(List<ResumenDeBolsin> bolsines) {
        return bolsines.stream().map(ResumenDeBolsin::numero).toList();
    }
}
