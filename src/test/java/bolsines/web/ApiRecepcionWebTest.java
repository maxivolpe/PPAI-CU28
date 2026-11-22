package bolsines.web;

import bolsines.entity.Bolsin;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.persistencia.BaseDeDatos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El CU 28 por HTTP, como lo usa la página: servidor real en un puerto libre y base SQLite
 * temporal. Cada {@link Navegador} guarda sus cookies, igual que un navegador de verdad.
 */
@DisplayName("API web del CU 28")
class ApiRecepcionWebTest {

    @TempDir
    Path carpeta;
    private BaseDeDatos baseDeDatos;
    private ServidorWeb servidor;

    @BeforeEach
    void levantarServidor() throws IOException {
        baseDeDatos = new BaseDeDatos(carpeta.resolve("bolsines.db").toString());
        baseDeDatos.inicializar();
        servidor = new ServidorWeb(baseDeDatos, baseDeDatos, 0);
        servidor.iniciar();
    }

    @AfterEach
    void detenerServidor() {
        servidor.detener();
    }

    @Test
    void dadaLaRaiz_cuandoElNavegadorLaPide_entoncesRecibeLaPagina() throws Exception {
        assertTrue(new Navegador().pedir("GET", "/").cuerpo().contains("id=\"raiz\""));
    }

    @Test
    void dadoUnEBDeLaCentral_cuandoRecorreElCasoDeUsoConLaOpcion4_entoncesLaDocumentacionQuedaParaRedirigirYSeNotifica()
            throws Exception {
        // Given
        Navegador navegador = new Navegador();
        assertTrue(navegador.pedir("POST", "/api/sesion?usuario=hrojas").cuerpo().contains("Comisión Médica Central"));
        String listado = navegador.pedir("POST", "/api/recepcion/iniciar").cuerpo();
        assertTrue(listado.contains("\"paso\":\"seleccionBolsin\"") && listado.contains("\"numero\":2004"));
        navegador.pedir("POST", "/api/recepcion/seleccion?bolsin=2004");
        navegador.pedir("POST", "/api/recepcion/opcion?numero=4&documentacion=6010");

        // When
        String resultado = navegador.pedir("POST", "/api/recepcion/confirmacion?confirma=true").cuerpo();

        // Then
        assertTrue(resultado.contains("\"paso\":\"exito\""));
        assertTrue(resultado.contains("\"emails\":[{\"para\":\"cmrosario@organismo.gob.ar\""));
        assertEquals("Para Redirigir", documentacion(bolsin(2004), 6010).getEstadoActual().getNombre());
        assertTrue(navegador.pedir("GET", "/api/consulta").cuerpo().contains("Recibido en CM Destino"));
    }

    @Test
    void dadoElListado_cuandoSeFiltraSinResultados_entoncesSeInformaElFlujoAlternativoA1oA2() throws Exception {
        // Given
        Navegador navegador = navegadorConElListadoDe("hrojas");

        // When / Then
        assertTrue(navegador.pedir("GET", "/api/recepcion/filtro?precinto=999999").cuerpo()
                .contains("\"flujoAlternativo\":\"A1\""));
        assertTrue(navegador.pedir("GET", "/api/recepcion/filtro?cmOrigen=Marte").cuerpo()
                .contains("\"flujoAlternativo\":\"A2\""));
    }

    @Test
    void dadaUnaOpcionAlternativaSinDocumentacion_cuandoSeElige_entoncesResponde400ConElMotivo() throws Exception {
        // Given
        Navegador navegador = navegadorConElListadoDe("hrojas");
        navegador.pedir("POST", "/api/recepcion/seleccion?bolsin=2004");

        // When
        Respuesta respuesta = navegador.pedir("POST", "/api/recepcion/opcion?numero=4");

        // Then
        assertEquals(400, respuesta.codigo());
        assertTrue(respuesta.cuerpo().contains("Debe indicar al menos una documentación para esta opción."));
    }

    @Test
    void dadaUnaOpcionElegida_cuandoNoSeConfirma_entoncesVuelveAlPasoDeOpcionYNoSeRegistraNada() throws Exception {
        // Given: flujo A6
        Navegador navegador = navegadorConElListadoDe("hrojas");
        navegador.pedir("POST", "/api/recepcion/seleccion?bolsin=2004");
        navegador.pedir("POST", "/api/recepcion/opcion?numero=4&documentacion=6010");

        // When
        String respuesta = navegador.pedir("POST", "/api/recepcion/confirmacion?confirma=false").cuerpo();

        // Then
        assertTrue(respuesta.contains("\"paso\":\"opcion\""));
        assertEquals("Enviado", bolsin(2004).getEstadoActual().getNombre());
    }

    @Test
    void dadoUnNavegadorSinSesion_cuandoIntentaIniciarElCasoDeUso_entoncesSeLePideIniciarSesion() throws Exception {
        // When
        Respuesta respuesta = new Navegador().pedir("POST", "/api/recepcion/iniciar");

        // Then
        assertEquals(400, respuesta.codigo());
        assertTrue(respuesta.cuerpo().contains("Primero hay que iniciar sesión."));
    }

    @Test
    void dadaUnaRutaQueNoExiste_cuandoSePide_entoncesResponde400() throws Exception {
        Respuesta respuesta = new Navegador().pedir("GET", "/api/nada");
        assertEquals(400, respuesta.codigo());
        assertTrue(respuesta.cuerpo().contains("Pedido inexistente"));
    }

    @Test
    void dadosDosNavegadoresConUsuariosDistintos_cuandoUsanElSistemaALaVez_entoncesCadaUnoVeSuPropioCasoDeUso()
            throws Exception {
        // Given
        Navegador deCordoba = navegadorConElListadoDe("cmendez");
        Navegador deLaCentral = navegadorConElListadoDe("hrojas");

        // When
        String vistaDeCordoba = deCordoba.pedir("GET", "/api/estado").cuerpo();

        // Then: el inicio de sesión de la Central no pisó la sesión de Córdoba
        assertTrue(vistaDeCordoba.contains("\"sesion\":{\"usuario\":\"cmendez\""));
        assertTrue(vistaDeCordoba.contains("\"numero\":2001"));
        assertFalse(vistaDeCordoba.contains("\"numero\":2004"));
        assertTrue(deLaCentral.pedir("GET", "/api/estado").cuerpo().contains("\"sesion\":{\"usuario\":\"hrojas\""));
    }

    @Test
    void dadosDosNavegadoresQueRecibenElMismoBolsin_cuandoElSegundoConfirma_entoncesSeRechazaYSeConservaElPrimero()
            throws Exception {
        // Given: dos personas de Córdoba con el bolsín 2001 seleccionado al mismo tiempo
        Navegador primero = navegadorConElListadoDe("cmendez");
        Navegador segundo = navegadorConElListadoDe("cmendez");
        for (Navegador navegador : new Navegador[]{primero, segundo}) {
            navegador.pedir("POST", "/api/recepcion/seleccion?bolsin=2001");
            navegador.pedir("POST", "/api/recepcion/opcion?numero=1");
        }
        primero.pedir("POST", "/api/recepcion/confirmacion?confirma=true");

        // When
        Respuesta respuesta = segundo.pedir("POST", "/api/recepcion/confirmacion?confirma=true");

        // Then
        assertEquals(400, respuesta.codigo());
        assertTrue(respuesta.cuerpo().contains("fue modificado por otro usuario"));
        assertEquals(2, bolsin(2001).getCambioEstados().size(), "queda un solo registro de recepción");
    }

    // ---------------------------------------------------------------- ayudas

    private record Respuesta(int codigo, String cuerpo) {
    }

    /** Un navegador: su propio cliente HTTP con sus propias cookies. */
    private class Navegador {

        private final HttpClient cliente = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

        Respuesta pedir(String metodo, String ruta) throws IOException, InterruptedException {
            HttpRequest pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + servidor.getPuerto() + ruta))
                    .method(metodo, HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> respuesta = cliente.send(pedido, HttpResponse.BodyHandlers.ofString());
            return new Respuesta(respuesta.statusCode(), respuesta.body());
        }
    }

    /** Un navegador que ya inició sesión con ese usuario y abrió el listado de bolsines. */
    private Navegador navegadorConElListadoDe(String usuario) throws IOException, InterruptedException {
        Navegador navegador = new Navegador();
        navegador.pedir("POST", "/api/sesion?usuario=" + usuario);
        navegador.pedir("POST", "/api/recepcion/iniciar");
        return navegador;
    }

    private Bolsin bolsin(int numero) {
        return baseDeDatos.buscarBolsines().stream()
                .filter(b -> b.getNumeroBolsin() == numero).findFirst().orElseThrow();
    }

    private Documentacion documentacion(Bolsin bolsin, int numero) {
        return bolsin.getRemitos().stream()
                .flatMap(r -> r.getDetallesRemito().stream())
                .map(DetalleRemito::getDocumentacion)
                .filter(d -> d.getNumero() == numero).findFirst().orElseThrow();
    }
}
