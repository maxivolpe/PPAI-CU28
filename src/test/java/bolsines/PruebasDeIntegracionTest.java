package bolsines;

import bolsines.boundary.InterfazServidorCorreo;
import bolsines.boundary.PantallaRecepcion;
import bolsines.control.GestorRecepcion;
import bolsines.entity.Bolsin;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.CambioEstadoDocumentacion;
import bolsines.entity.ComisionMedica;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Sesion;
import bolsines.entity.Usuario;
import bolsines.persistencia.BaseDeDatos;
import bolsines.web.ServidorWeb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PRUEBAS DE INTEGRACIÓN: varias piezas reales trabajando juntas sobre una base SQLite de
 * verdad, creada de cero con los datos de prueba en una carpeta temporal para cada test.
 * Usan los números de los datos de prueba (por ejemplo, el bolsín 2001 va de Rosario a Córdoba).
 */
@DisplayName("2. Pruebas de integración")
class PruebasDeIntegracionTest {

    static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 10, 15, 30);
    static final Clock RELOJ_FIJO = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    Path carpeta;
    BaseDeDatos baseDeDatos;

    @BeforeEach
    void crearBaseConDatosDePrueba() {
        baseDeDatos = new BaseDeDatos(carpeta.resolve("bolsines.db").toString());
        baseDeDatos.inicializar();
    }

    @Nested
    @DisplayName("Gestor + base de datos, paso por paso")
    class GestorConBaseDeDatos {

        private final PantallaEspia pantalla = new PantallaEspia();
        private final InterfazServidorCorreo correo = new InterfazServidorCorreo(new PrintStream(new ByteArrayOutputStream()));
        private GestorRecepcion gestor;

        @BeforeEach
        void armarGestorDeCordoba() {
            gestor = gestorPara("cmendez");
        }

        @Test
        void dadoUnEBDeCordoba_cuandoIniciaElCasoDeUso_entoncesVeSuCMYSoloLosBolsinesEnviadosASuCM() {
            // When
            gestor.opcRegistrarRecepcion(pantalla);

            // Then
            assertTrue(pantalla.mensajes.contains("CM: Comisión Médica Córdoba"));
            assertEquals(List.of("2001", "2002", "2003"), pantalla.numerosOfrecidos());
        }

        @Test
        void dadoUnEBSinBolsinesPendientes_cuandoIniciaElCasoDeUso_entoncesSeLeInformaYTermina() {
            // When
            gestorPara("tcabrera").opcRegistrarRecepcion(pantalla);

            // Then
            assertTrue(pantalla.mensajes.contains("No hay bolsines en estado Enviado para Comisión Médica Villa María."));
            assertTrue(pantalla.numerosOfrecidos().isEmpty());
        }

        @Test
        void dadoElListado_cuandoElEBSeleccionaUnBolsin_entoncesSeMuestranSusRemitosYSePideLaOpcion() {
            // Given
            gestor.opcRegistrarRecepcion(pantalla);

            // When
            gestor.tomarSeleccionBolsin(2001);

            // Then
            assertEquals(Set.of(4001, 4002), pantalla.informacionMostrada.keySet());
            assertEquals(1, pantalla.vecesQueSePidioLaOpcion);
            assertEquals("Comisión Médica Córdoba", pantalla.cmParaLaOpcion.getNombre());
        }

        @Test
        void dadoUnBolsinQueNoFueOfrecido_cuandoElEBIntentaSeleccionarlo_entoncesSeRechaza() {
            // Given: el 2004 va a la Comisión Médica Central, no a Córdoba
            gestor.opcRegistrarRecepcion(pantalla);

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> gestor.tomarSeleccionBolsin(2004));
        }

        @Test
        void dadoElListado_cuandoElEBFiltraPorPrecintoOPorCMDeOrigen_entoncesSoloQuedanLosQueCoinciden() {
            // Given
            gestor.opcRegistrarRecepcion(pantalla);

            // When / Then: un filtro sin resultados es el flujo A1 (precinto) o A2 (CM de origen)
            assertAll(
                    () -> assertEquals(List.of("2002"), numeros(gestor.tomarFiltroPrecinto(731202))),
                    () -> assertTrue(gestor.tomarFiltroPrecinto(999999).isEmpty(), "A1"),
                    () -> assertEquals(List.of("2002"), numeros(gestor.tomarFiltroCMOrigen("  villa mar"))),
                    () -> assertTrue(gestor.tomarFiltroCMOrigen("Mendoza").isEmpty(), "A2"));
        }

        @Test
        void dadoUnRelojFijo_cuandoElEBConfirmaLaOpcion1_entoncesLaRecepcionSeGuardaConEsaFechaYHora() {
            // Given
            seleccionar(2001, OpcionRecepcion.CONTENIDO_IGUAL, Set.of());

            // When
            gestor.tomarConfirmacion(true);

            // Then
            List<CambioEstadoBolsin> historial = bolsin(2001).getCambioEstados();
            assertAll(
                    () -> assertEquals("Recibido en CM Destino", bolsin(2001).getEstadoActual().getNombre()),
                    () -> assertEquals(AHORA, historial.get(0).getFechaHoraFin()),
                    () -> assertEquals(AHORA, historial.get(1).getFechaHoraInicio()),
                    () -> assertEquals("Méndez", historial.get(1).getResponsable().getApellido()),
                    () -> assertEquals("2001: Recibido en CM Destino", pantalla.exito));
        }

        @Test
        void dadaUnaRecepcionConfirmada_cuandoTermina_entoncesSeNotificaALaCMDeOrigenConLaSituacionDeSuDocumentacion() {
            // Given: CU 29 incluido
            seleccionar(2001, OpcionRecepcion.DOCUMENTACION_NO_RECIBIDA, Set.of(6002));

            // When
            gestor.tomarConfirmacion(true);

            // Then
            String[] email = correo.getEmailsEnviados().get(0);
            assertAll(
                    () -> assertEquals(1, correo.getEmailsEnviados().size()),
                    () -> assertEquals("cmrosario@organismo.gob.ar", email[0]),
                    () -> assertEquals("Recepción del bolsín 2001 en Comisión Médica Córdoba", email[1]),
                    () -> assertTrue(email[2].contains("Remito 4001: Recibido y Aceptado Parcial")),
                    () -> assertTrue(email[2].contains("Doc. 6002 - Dictamen de incapacidad - Navarro, G.: No Recibida")));
        }

        @Test
        void dadaLaOpcion1ConDocumentacionMarcada_cuandoElEBLaElige_entoncesSeRechaza() {
            seleccionar(2001);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> gestor.tomarOpcRecepcion(OpcionRecepcion.CONTENIDO_IGUAL, Set.of(6001)));
            assertEquals("La opción 1 no lleva documentación seleccionada.", error.getMessage());
        }

        @Test
        void dadaUnaOpcionAlternativaSinDocumentacionMarcada_cuandoElEBLaElige_entoncesSeLePideMarcarAlguna() {
            seleccionar(2001);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> gestor.tomarOpcRecepcion(OpcionRecepcion.DOCUMENTACION_NO_RECIBIDA, Set.of()));
            assertEquals("Debe indicar al menos una documentación para esta opción.", error.getMessage());
        }

        @Test
        void dadaDocumentacionDeOtroBolsin_cuandoElEBLaMarca_entoncesSeLeIndicaCualNoPertenece() {
            seleccionar(2001);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> gestor.tomarOpcRecepcion(OpcionRecepcion.DOCUMENTACION_NO_RECIBIDA, Set.of(6001, 6004)));
            assertEquals("La documentación 6004 no pertenece al bolsín.", error.getMessage());
        }

        @Test
        void dadaUnaCMQueNoEsLaCentral_cuandoElEBEligeRedirigir_entoncesSeRechazaSinPedirConfirmacion() {
            // Given: flujo A5 fuera de la Comisión Médica Central
            seleccionar(2001);

            // When / Then
            assertThrows(IllegalArgumentException.class,
                    () -> gestor.tomarOpcRecepcion(OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR, Set.of(6001)));
            assertFalse(pantalla.confirmacionSolicitada);
        }

        @Test
        void dadaUnaOpcionElegida_cuandoElEBNoConfirma_entoncesSeVuelveAPedirLaOpcionYNoSeGuardaNada() {
            // Given: flujo A6
            seleccionar(2001, OpcionRecepcion.CONTENIDO_IGUAL, Set.of());

            // When
            gestor.tomarConfirmacion(false);

            // Then
            assertTrue(pantalla.mensajes.contains("No se confirmó la selección. Elija nuevamente la opción de recepción."));
            assertEquals(2, pantalla.vecesQueSePidioLaOpcion);
            assertEquals("Enviado", bolsin(2001).getEstadoActual().getNombre());
        }

        @Test
        void dadoUnBolsinSeleccionado_cuandoElEBCancela_entoncesNoSeGuardaNada() {
            // Given: observación 1
            seleccionar(2001);

            // When
            gestor.cancelarCasoDeUso();

            // Then
            assertTrue(pantalla.mensajes.contains("Caso de uso cancelado. No se registró ningún cambio."));
            assertEquals("Enviado", bolsin(2001).getEstadoActual().getNombre());
        }

        private GestorRecepcion gestorPara(String nombreUsuario) {
            Sesion sesion = new Sesion(AHORA, empleado(nombreUsuario).getUsuario());
            return new GestorRecepcion(baseDeDatos, sesion, correo, RELOJ_FIJO);
        }

        private void seleccionar(int numeroBolsin) {
            gestor.opcRegistrarRecepcion(pantalla);
            gestor.tomarSeleccionBolsin(numeroBolsin);
        }

        private void seleccionar(int numeroBolsin, OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
            seleccionar(numeroBolsin);
            gestor.tomarOpcRecepcion(opcion, documentacionMarcada);
        }
    }

    @Nested
    @DisplayName("Persistencia en SQLite")
    class PersistenciaSqlite {

        @Test
        void dadaUnaBaseNueva_cuandoSeInicializa_entoncesTieneLosDatosDePrueba() {
            assertEquals(7, baseDeDatos.buscarBolsines().size());
            assertEquals(5, baseDeDatos.buscarEmpleados().size());
        }

        @Test
        void dadaUnaBaseYaInicializada_cuandoSeInicializaOtraVez_entoncesNoDuplicaNada() {
            // When
            baseDeDatos.inicializar();

            // Then
            assertEquals(7, baseDeDatos.buscarBolsines().size());
            assertEquals(1, bolsin(2001).getCambioEstados().size());
        }

        @Test
        void dadaUnaRecepcionRegistrada_cuandoSeGuardaYSeVuelveALeer_entoncesEstadosEHistorialesQuedanIguales() {
            // Given
            Bolsin bolsin = recibirEnMemoria(2001, empleado("cmendez"));

            // When
            baseDeDatos.guardarRecepcion(bolsin);

            // Then
            Bolsin releido = bolsin(2001);
            List<CambioEstadoDocumentacion> historialDocumentacion = documentacion(releido, 6001).getCambioEstadoDocumentacion();
            assertAll(
                    () -> assertEquals("Recibido en CM Destino", releido.getEstadoActual().getNombre()),
                    () -> assertEquals(2, releido.getCambioEstados().size()),
                    () -> assertEquals(AHORA, releido.getCambioEstados().get(0).getFechaHoraFin()),
                    () -> assertNull(releido.getCambioEstados().get(1).getFechaHoraFin()),
                    () -> assertEquals("Recibido y Aceptado", releido.getRemitos().get(0).getEstadoActual().getNombre()),
                    () -> assertEquals(2, historialDocumentacion.size()),
                    () -> assertEquals("Recibida y Aceptada", historialDocumentacion.get(1).getEstado().getNombre()));
        }

        @Test
        void dadaUnaFallaAMitadDelGuardado_cuandoSeGuardaLaRecepcion_entoncesNoQuedaNadaAMedias() {
            // Given: un responsable que no existe en la tabla empleado hace fallar la clave foránea del
            // historial DESPUÉS de haber actualizado el estado del bolsín
            Empleado inexistente = new Empleado("Nadie", "Fantasma", "fantasma@organismo.gob.ar",
                    empleado("cmendez").getCM(), new Usuario("fantasma", "clave"));
            Bolsin bolsin = recibirEnMemoria(2001, inexistente);

            // When
            assertThrows(IllegalStateException.class, () -> baseDeDatos.guardarRecepcion(bolsin));

            // Then
            Bolsin releido = bolsin(2001);
            assertAll(
                    () -> assertEquals("Enviado", releido.getEstadoActual().getNombre()),
                    () -> assertEquals(1, releido.getCambioEstados().size()),
                    () -> assertNull(releido.getCambioEstados().get(0).getFechaHoraFin()),
                    () -> assertEquals("En Bolsin Enviado", documentacion(releido, 6001).getEstadoActual().getNombre()));
        }

        @Test
        void dadoUnBolsinQueOtroUsuarioYaRecibio_cuandoSeGuardaOtraRecepcionDelMismo_entoncesSeRechazaYSeConservaLaPrimera() {
            // Given: dos usuarios leyeron el bolsín 2001 cuando estaba "Enviado"; el primero ya guardó
            Bolsin delPrimero = recibirEnMemoria(2001, empleado("cmendez"));
            Bolsin delSegundo = recibirEnMemoria(2001, empleado("cmendez"));
            baseDeDatos.guardarRecepcion(delPrimero);

            // When
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> baseDeDatos.guardarRecepcion(delSegundo));

            // Then
            assertTrue(error.getMessage().contains("fue modificado por otro usuario"));
            assertEquals(2, bolsin(2001).getCambioEstados().size(), "queda un solo registro de recepción");
        }

        /** Lee el bolsín de la base y registra su recepción (opción 1) solo en memoria, sin guardarla. */
        private Bolsin recibirEnMemoria(int numero, Empleado responsable) {
            Bolsin bolsin = bolsin(numero);
            bolsin.registrarBolsinRecibido(AHORA, responsable, bolsin.buscarCambioEstadoActual(),
                    OpcionRecepcion.CONTENIDO_IGUAL, Set.of());
            return bolsin;
        }
    }

    @Nested
    @DisplayName("API web (HTTP), como la usa la página")
    class ApiWeb {

        private ServidorWeb servidor;
        private final HttpClient cliente = HttpClient.newHttpClient();

        @BeforeEach
        void levantarServidor() throws IOException {
            servidor = new ServidorWeb(baseDeDatos, 0);
            servidor.iniciar();
        }

        @AfterEach
        void detenerServidor() {
            servidor.detener();
        }

        @Test
        void dadaLaRaiz_cuandoElNavegadorLaPide_entoncesRecibeLaPagina() throws Exception {
            assertTrue(pedir("GET", "/").cuerpo().contains("id=\"raiz\""));
        }

        @Test
        void dadoUnEBDeLaCentral_cuandoRecorreElCasoDeUsoConLaOpcion4_entoncesLaDocumentacionQuedaParaRedirigirYSeNotifica()
                throws Exception {
            // Given
            assertTrue(pedir("POST", "/api/sesion?usuario=hrojas").cuerpo().contains("Comisión Médica Central"));
            String listado = pedir("POST", "/api/recepcion/iniciar").cuerpo();
            assertTrue(listado.contains("\"paso\":\"seleccionBolsin\"") && listado.contains("\"numero\":2004"));
            pedir("POST", "/api/recepcion/seleccion?bolsin=2004");
            pedir("POST", "/api/recepcion/opcion?numero=4&documentacion=6010");

            // When
            String resultado = pedir("POST", "/api/recepcion/confirmacion?confirma=true").cuerpo();

            // Then
            assertTrue(resultado.contains("\"paso\":\"exito\""));
            assertTrue(resultado.contains("\"emails\":[{\"para\":\"cmrosario@organismo.gob.ar\""));
            assertEquals("Para Redirigir", documentacion(bolsin(2004), 6010).getEstadoActual().getNombre());
        }

        @Test
        void dadoElListado_cuandoSeFiltraSinResultados_entoncesSeInformaElFlujoAlternativoA1oA2() throws Exception {
            // Given
            iniciarComo("hrojas");

            // When / Then
            assertTrue(pedir("GET", "/api/recepcion/filtro?precinto=731204").cuerpo().contains("\"flujoAlternativo\":null"));
            assertTrue(pedir("GET", "/api/recepcion/filtro?precinto=999999").cuerpo().contains("\"flujoAlternativo\":\"A1\""));
            assertTrue(pedir("GET", "/api/recepcion/filtro?cmOrigen=Marte").cuerpo().contains("\"flujoAlternativo\":\"A2\""));
        }

        @Test
        void dadaUnaOpcionAlternativaSinDocumentacion_cuandoSeElige_entoncesResponde400ConElMotivo() throws Exception {
            // Given
            iniciarComo("hrojas");
            pedir("POST", "/api/recepcion/seleccion?bolsin=2004");

            // When
            Respuesta respuesta = pedir("POST", "/api/recepcion/opcion?numero=4");

            // Then
            assertEquals(400, respuesta.codigo());
            assertTrue(respuesta.cuerpo().contains("Debe indicar al menos una documentación para esta opción."));
        }

        @Test
        void dadaUnaOpcionElegida_cuandoNoSeConfirma_entoncesVuelveAlPasoDeOpcionYNoSeRegistraNada() throws Exception {
            // Given: flujo A6
            iniciarComo("hrojas");
            pedir("POST", "/api/recepcion/seleccion?bolsin=2004");
            pedir("POST", "/api/recepcion/opcion?numero=4&documentacion=6010");

            // When
            String respuesta = pedir("POST", "/api/recepcion/confirmacion?confirma=false").cuerpo();

            // Then
            assertTrue(respuesta.contains("\"paso\":\"opcion\""));
            assertEquals("Enviado", bolsin(2004).getEstadoActual().getNombre());
        }

        @Test
        void dadoQueNadieInicioSesion_cuandoSeQuiereIniciarElCasoDeUso_entoncesSePideIniciarSesion() throws Exception {
            Respuesta respuesta = pedir("POST", "/api/recepcion/iniciar");
            assertEquals(400, respuesta.codigo());
            assertTrue(respuesta.cuerpo().contains("Primero hay que iniciar sesión."));
        }

        @Test
        void dadaUnaRutaDeLaApiQueNoExiste_cuandoSePide_entoncesResponde400() throws Exception {
            Respuesta respuesta = pedir("GET", "/api/nada");
            assertEquals(400, respuesta.codigo());
            assertTrue(respuesta.cuerpo().contains("Pedido inexistente"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/%2e%2e/pom.xml", "/js/%2e%2e/%2e%2e/pom.xml", "/no-existe.js"})
        void dadaUnaRutaFueraDeLaPaginaOInexistente_cuandoSePide_entoncesResponde404(String ruta) throws Exception {
            assertEquals(404, pedir("GET", ruta).codigo());
        }

        @Test
        void dadaUnaRecepcionRegistrada_cuandoSeConsultanLosEstados_entoncesDevuelveCadaBolsinConSuHistorial() throws Exception {
            // Given
            iniciarComo("hrojas");
            pedir("POST", "/api/recepcion/seleccion?bolsin=2004");
            pedir("POST", "/api/recepcion/opcion?numero=1");
            pedir("POST", "/api/recepcion/confirmacion?confirma=true");

            // When
            String consulta = pedir("GET", "/api/consulta").cuerpo();

            // Then
            assertTrue(consulta.contains("\"numero\":2004,\"cmOrigen\":\"Comisión Médica Rosario\""));
            assertTrue(consulta.contains("\"estado\":\"Recibido en CM Destino\",\"responsable\":\"Hernán Rojas\""));
        }

        private void iniciarComo(String usuario) throws IOException, InterruptedException {
            pedir("POST", "/api/sesion?usuario=" + usuario);
            pedir("POST", "/api/recepcion/iniciar");
        }

        private Respuesta pedir(String metodo, String ruta) throws IOException, InterruptedException {
            HttpRequest pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + servidor.getPuerto() + ruta))
                    .method(metodo, HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> respuesta = cliente.send(pedido, HttpResponse.BodyHandlers.ofString());
            return new Respuesta(respuesta.statusCode(), respuesta.body());
        }
    }

    // ---------------------------------------------------------------- ayudas

    record Respuesta(int codigo, String cuerpo) {
    }

    Empleado empleado(String nombreUsuario) {
        return baseDeDatos.buscarEmpleados().stream()
                .filter(e -> e.getUsuario().getNombre().equals(nombreUsuario)).findFirst().orElseThrow();
    }

    Bolsin bolsin(int numero) {
        return baseDeDatos.buscarBolsines().stream()
                .filter(b -> b.getNumeroBolsin() == numero).findFirst().orElseThrow();
    }

    static Documentacion documentacion(Bolsin bolsin, int numero) {
        return bolsin.getRemitos().stream()
                .flatMap(r -> r.getDetallesRemito().stream())
                .map(DetalleRemito::getDocumentacion)
                .filter(d -> d.getNumero() == numero).findFirst().orElseThrow();
    }

    static List<String> numeros(List<String[]> listado) {
        return listado.stream().map(bolsin -> bolsin[0]).toList();
    }

    /** Pantalla de prueba: no dibuja nada, solo anota lo que el gestor le pidió. */
    static class PantallaEspia implements PantallaRecepcion {

        final List<String> mensajes = new ArrayList<>();
        List<String[]> bolsinesOfrecidos = List.of();
        Map<Integer, List<String[]>> informacionMostrada = Map.of();
        int vecesQueSePidioLaOpcion;
        ComisionMedica cmParaLaOpcion;
        boolean confirmacionSolicitada;
        String exito;

        List<String> numerosOfrecidos() {
            return numeros(bolsinesOfrecidos);
        }

        @Override
        public void mostrarCM(String nombreCMEmpleado) {
            mensajes.add("CM: " + nombreCMEmpleado);
        }

        @Override
        public void mostrarMensaje(String mensaje) {
            mensajes.add(mensaje);
        }

        @Override
        public void solicitarSeleccionBolsin(List<String[]> bolsinesEnviados) {
            bolsinesOfrecidos = bolsinesEnviados;
        }

        @Override
        public void mostrarInfoBolsin(int numeroBolsin, String cmOrigen, int numeroPrecinto,
                                      Map<Integer, List<String[]>> informacionBolsin) {
            informacionMostrada = informacionBolsin;
        }

        @Override
        public void solicitarOpcRecepcion(ComisionMedica comisionMedica) {
            vecesQueSePidioLaOpcion++;
            cmParaLaOpcion = comisionMedica;
        }

        @Override
        public void solicitarConfirmacion(OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
            confirmacionSolicitada = true;
        }

        @Override
        public void mostrarMensajeExito(int numeroBolsin, String estadoBolsin) {
            exito = numeroBolsin + ": " + estadoBolsin;
        }
    }
}
