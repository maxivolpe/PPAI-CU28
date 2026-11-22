package bolsines.aceptacion;

import bolsines.boundary.InterfazServidorCorreo;
import bolsines.boundary.PantallaRecepcionConsola;
import bolsines.control.GestorRecepcion;
import bolsines.entity.Bolsin;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Remito;
import bolsines.entity.Sesion;
import bolsines.entity.TransicionNoPermitidaException;
import bolsines.persistencia.BaseDeDatos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de aceptación del CU 28: el Encargado de Bolsines lo recorre por consola (con entrada
 * simulada) de punta a punta — pantalla → gestor → entidades con State → SQLite — y el resultado
 * se verifica releyendo la base. Usan los datos de prueba de resources/db/datos.
 * Una clase anidada por flujo de la descripción del caso de uso.
 */
@DisplayName("CU 28 - Registrar recepción de bolsín")
class RegistrarRecepcionDeBolsinTest {

    /** Encargado de la Comisión Médica Córdoba: recibe los bolsines 2001, 2002 y 2003. */
    private static final String EB_CORDOBA = "cmendez";
    /** Encargado de la Comisión Médica Central: recibe los bolsines 2004 y 2005. */
    private static final String EB_CENTRAL = "hrojas";

    @TempDir
    Path carpeta;
    private BaseDeDatos baseDeDatos;

    @BeforeEach
    void crearBaseConDatosDePrueba() {
        baseDeDatos = new BaseDeDatos(carpeta.resolve("bolsines.db").toString());
        baseDeDatos.inicializar();
    }

    @Nested
    @DisplayName("Flujo principal")
    class FlujoPrincipal {

        @Test
        void dadoUnBolsinEnviadoASuCM_cuandoElEBConfirmaLaOpcion1_entoncesBolsinRemitosYDocumentacionQuedanAceptados() {
            // When
            String salida = ejecutar(EB_CORDOBA, "2001", "1", "s");

            // Then
            assertTrue(salida.contains("Recepción registrada con éxito"));
            Bolsin bolsin = bolsin(2001);
            assertEquals("Recibido en CM Destino", bolsin.getEstadoActual().getNombre());
            for (Remito remito : bolsin.getRemitos()) {
                assertEquals("Recibido y Aceptado", remito.getEstadoActual().getNombre());
                for (DetalleRemito detalle : remito.getDetallesRemito()) {
                    assertEquals("Recibida y Aceptada", detalle.getDocumentacion().getEstadoActual().getNombre());
                }
            }
        }

        @Test
        void dadoUnBolsinEnviadoASuCM_cuandoElEBConfirmaLaOpcion1_entoncesElHistorialCierraElEstadoAnteriorYAbreElNuevo() {
            // When
            ejecutar(EB_CORDOBA, "2001", "1", "s");

            // Then
            Bolsin bolsin = bolsin(2001);
            CambioEstadoBolsin anterior = bolsin.getCambioEstados().get(0);
            CambioEstadoBolsin vigente = bolsin.getCambioEstados().get(1);
            assertAll(
                    () -> assertEquals(2, bolsin.getCambioEstados().size()),
                    () -> assertEquals("Enviado", anterior.getEstado().getNombre()),
                    () -> assertNotNull(anterior.getFechaHoraFin()),
                    () -> assertEquals(anterior.getFechaHoraFin(), vigente.getFechaHoraInicio()),
                    () -> assertNull(vigente.getFechaHoraFin()),
                    () -> assertEquals("Méndez", vigente.getResponsable().getApellido()));
            Documentacion documentacion = documentacion(bolsin, 6001);
            assertAll(
                    () -> assertEquals(2, documentacion.getCambioEstadoDocumentacion().size()),
                    () -> assertNotNull(documentacion.getCambioEstadoDocumentacion().get(0).getFechaHoraFin()),
                    () -> assertEquals("Méndez",
                            documentacion.getCambioEstadoDocumentacion().get(1).getResponsable().getApellido()));
        }

        @Test
        void dadoUnBolsinEnviadoASuCM_cuandoElEBConfirmaLaOpcion1_entoncesSeNotificaALaCMDeOrigen() {
            // When
            String salida = ejecutar(EB_CORDOBA, "2001", "1", "s");

            // Then: CU 29 incluido
            assertTrue(salida.contains("Para: cmrosario@organismo.gob.ar"));
        }

        @Test
        void dadosBolsinesDeVariosDestinosYEstados_cuandoElEBIniciaElCasoDeUso_entoncesSoloVeLosEnviadosASuCM() {
            // When
            String salida = ejecutar(EB_CORDOBA, "c");

            // Then
            assertTrue(salida.contains("Comisión médica: Comisión Médica Córdoba"));
            assertTrue(salida.contains("Bolsín 2001") && salida.contains("Bolsín 2002") && salida.contains("Bolsín 2003"));
            assertFalse(salida.contains("Bolsín 2004"), "tiene destino Central");
            assertFalse(salida.contains("Bolsín 2006"), "tiene destino Rosario");
            assertFalse(salida.contains("Bolsín 2007"), "está Cerrado, no Enviado");
        }
    }

    @Nested
    @DisplayName("A1 - Filtro por precinto sin resultados")
    class A1 {

        @Test
        void dadoUnPrecintoInexistente_cuandoElEBFiltra_entoncesSeInformaYPuedeSeguirConElCasoDeUso() {
            // When
            String salida = ejecutar(EB_CORDOBA, "p 999999", "p 731202", "2002", "1", "s");

            // Then
            assertTrue(salida.contains("[A1] No se encuentra bolsín con el número de precinto ingresado."));
            assertEquals("Recibido en CM Destino", bolsin(2002).getEstadoActual().getNombre());
        }
    }

    @Nested
    @DisplayName("A2 - Filtro por CM de origen sin resultados")
    class A2 {

        @Test
        void dadaUnaCMDeOrigenSinBolsines_cuandoElEBFiltra_entoncesSeInformaYPuedeSeguirConElCasoDeUso() {
            // When
            String salida = ejecutar(EB_CORDOBA, "o Mendoza", "2002", "1", "s");

            // Then
            assertTrue(salida.contains("[A2] No se encuentra bolsín con la CM de origen ingresada."));
            assertEquals("Recibido en CM Destino", bolsin(2002).getEstadoActual().getNombre());
        }
    }

    @Nested
    @DisplayName("A3 - No se recibe toda la documentación")
    class A3 {

        @Test
        void dadaUnaDocumentacionQueNoLlego_cuandoElEBLaMarcaConLaOpcion2_entoncesQuedaNoRecibidaYSuRemitoParcial() {
            // When
            ejecutar(EB_CORDOBA, "2001", "2", "6002", "s");

            // Then
            Bolsin bolsin = bolsin(2001);
            assertAll(
                    () -> assertEquals("Recibido en CM Destino", bolsin.getEstadoActual().getNombre()),
                    () -> assertEquals("No Recibida", documentacion(bolsin, 6002).getEstadoActual().getNombre()),
                    () -> assertEquals("Recibida y Aceptada", documentacion(bolsin, 6001).getEstadoActual().getNombre()),
                    () -> assertEquals("Recibido y Aceptado Parcial", remito(bolsin, 4001).getEstadoActual().getNombre()),
                    () -> assertEquals("Recibido y Aceptado", remito(bolsin, 4002).getEstadoActual().getNombre()));
        }
    }

    @Nested
    @DisplayName("A4 - Documentación que no corresponde al destino")
    class A4 {

        @Test
        void dadaDocumentacionQueNoCorresponde_cuandoElEBLaMarcaConLaOpcion3_entoncesQuedaRechazadaYSuRemitoParcial() {
            // When
            ejecutar(EB_CORDOBA, "2002", "3", "6005, 6006", "s");

            // Then
            Bolsin bolsin = bolsin(2002);
            assertAll(
                    () -> assertEquals("Recibida y Rechazada", documentacion(bolsin, 6005).getEstadoActual().getNombre()),
                    () -> assertEquals("Recibida y Rechazada", documentacion(bolsin, 6006).getEstadoActual().getNombre()),
                    () -> assertEquals("Recibida y Aceptada", documentacion(bolsin, 6004).getEstadoActual().getNombre()),
                    () -> assertEquals("Recibido y Aceptado Parcial", remito(bolsin, 4003).getEstadoActual().getNombre()));
        }
    }

    @Nested
    @DisplayName("A5 - Documentación para redirigir (solo CM Central)")
    class A5 {

        @Test
        void dadaLaCMCentralComoDestino_cuandoElEBMarcaDocumentacionConLaOpcion4_entoncesQuedaParaRedirigir() {
            // When
            ejecutar(EB_CENTRAL, "2004", "4", "6010,6011", "s");

            // Then
            Bolsin bolsin = bolsin(2004);
            assertAll(
                    () -> assertEquals("Recibido en CM Destino", bolsin.getEstadoActual().getNombre()),
                    () -> assertEquals("Para Redirigir", documentacion(bolsin, 6010).getEstadoActual().getNombre()),
                    () -> assertEquals("Para Redirigir", documentacion(bolsin, 6011).getEstadoActual().getNombre()),
                    () -> assertEquals("Recibida y Aceptada", documentacion(bolsin, 6012).getEstadoActual().getNombre()),
                    () -> assertEquals("Recibido y Aceptado", remito(bolsin, 4006).getEstadoActual().getNombre()));
        }

        @Test
        void dadaUnaCMQueNoEsLaCentral_cuandoElEBEligeLaOpcion4_entoncesSeLeInformaQueNoAplicaYNoSeRegistraNada() {
            // When
            String salida = ejecutar(EB_CORDOBA, "2001", "4", "6001", "c");

            // Then
            assertTrue(salida.contains("La opción 4 solo aplica cuando la CM destino es la Comisión Médica Central."));
            assertEquals("Enviado", bolsin(2001).getEstadoActual().getNombre());
        }
    }

    @Nested
    @DisplayName("A6 - El EB no confirma")
    class A6 {

        @Test
        void dadaUnaOpcionElegida_cuandoElEBNoConfirma_entoncesSeVuelveAPedirLaOpcionYNoSeRegistraNada() {
            // When
            String salida = ejecutar(EB_CORDOBA, "2003", "2", "6008", "n", "c");

            // Then
            assertTrue(salida.contains("No se confirmó la selección"));
            Bolsin bolsin = bolsin(2003);
            assertAll(
                    () -> assertEquals("Enviado", bolsin.getEstadoActual().getNombre()),
                    () -> assertEquals("En Bolsin Enviado", documentacion(bolsin, 6008).getEstadoActual().getNombre()),
                    () -> assertEquals(1, bolsin.getCambioEstados().size()));
        }
    }

    @Nested
    @DisplayName("Validaciones de la opción elegida")
    class Validaciones {

        @Test
        void dadaUnaOpcionAlternativaSinDocumentacionMarcada_cuandoElEBLaElige_entoncesSeLePideIndicarAlgunaDocumentacion() {
            // When
            String salida = ejecutar(EB_CORDOBA, "2001", "2", "", "c");

            // Then
            assertTrue(salida.contains("Debe indicar al menos una documentación para esta opción."));
        }

        @Test
        void dadaDocumentacionDeOtroBolsin_cuandoElEBLaMarca_entoncesSeLeInformaQueNoPerteneceAlBolsin() {
            // When
            String salida = ejecutar(EB_CORDOBA, "2001", "2", "6004", "c");

            // Then
            assertTrue(salida.contains("La documentación 6004 no pertenece al bolsín."));
        }
    }

    @Nested
    @DisplayName("Patrón State sobre datos guardados")
    class PatronState {

        @Test
        void dadoUnBolsinYaRecibido_cuandoSeIntentaRegistrarOtraRecepcion_entoncesLaTransicionNoEstaPermitida() {
            // Given
            ejecutar(EB_CORDOBA, "2001", "1", "s");
            Bolsin recibido = bolsin(2001);

            // When / Then: RecibidoEnCMDestino no redefine el evento, así que lo rechaza
            assertThrows(TransicionNoPermitidaException.class, () -> recibido.registrarBolsinRecibido(
                    LocalDateTime.now(), empleado(EB_CORDOBA), recibido.buscarCambioEstadoActual(),
                    OpcionRecepcion.CONTENIDO_IGUAL, Set.of()));
        }

        @Test
        void dadaUnaDocumentacionYaAceptada_cuandoSeIntentaMarcarlaNoRecibida_entoncesLaTransicionNoEstaPermitida() {
            // Given
            ejecutar(EB_CORDOBA, "2001", "1", "s");
            Documentacion aceptada = documentacion(bolsin(2001), 6001);

            // When / Then
            assertThrows(TransicionNoPermitidaException.class,
                    () -> aceptada.registrarDocumentacionNoRecibida(LocalDateTime.now(), empleado(EB_CORDOBA)));
        }
    }

    // ---------------------------------------------------------------- ayudas

    /** Ejecuta el caso de uso por consola con las líneas dadas, como si las tipeara el EB. */
    private String ejecutar(String nombreUsuario, String... lineas) {
        Sesion sesion = new Sesion(LocalDateTime.now(), empleado(nombreUsuario).getUsuario());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream salida = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        GestorRecepcion gestor = new GestorRecepcion(baseDeDatos, baseDeDatos, sesion, new InterfazServidorCorreo(salida));
        Scanner entrada = new Scanner(String.join("\n", lineas) + "\n");

        new PantallaRecepcionConsola(gestor, entrada, salida).opcRegistrarRecepcion();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private Empleado empleado(String nombreUsuario) {
        return baseDeDatos.buscarEmpleados().stream()
                .filter(e -> e.getUsuario().getNombre().equals(nombreUsuario)).findFirst().orElseThrow();
    }

    private Bolsin bolsin(int numero) {
        return baseDeDatos.buscarBolsines().stream()
                .filter(b -> b.getNumeroBolsin() == numero).findFirst().orElseThrow();
    }

    private Remito remito(Bolsin bolsin, int numero) {
        return bolsin.getRemitos().stream().filter(r -> r.getNumero() == numero).findFirst().orElseThrow();
    }

    private Documentacion documentacion(Bolsin bolsin, int numero) {
        return bolsin.getRemitos().stream()
                .flatMap(r -> r.getDetallesRemito().stream())
                .map(DetalleRemito::getDocumentacion)
                .filter(d -> d.getNumero() == numero).findFirst().orElseThrow();
    }
}
