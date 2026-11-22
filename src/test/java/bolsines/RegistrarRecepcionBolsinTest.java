package bolsines;

import bolsines.boundary.InterfazServidorCorreo;
import bolsines.boundary.PantallaRecepcionConsola;
import bolsines.control.GestorRecepcion;
import bolsines.entity.Bolsin;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.Documentacion;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Remito;
import bolsines.entity.Sesion;
import bolsines.entity.TransicionNoPermitidaException;
import bolsines.persistencia.BaseDeDatos;
import bolsines.web.ServidorWeb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Recorre el CU 28 de punta a punta (pantalla de consola con entrada simulada -> gestor ->
 * entidades con State -> SQLite) y verifica el resultado releyendo la base de datos.
 */
class RegistrarRecepcionBolsinTest {

    private static final String CORDOBA = "cmendez";
    private static final String CENTRAL = "hrojas";

    @TempDir
    Path carpeta;
    private BaseDeDatos baseDeDatos;

    @BeforeEach
    void crearBase() {
        baseDeDatos = new BaseDeDatos(carpeta.resolve("bolsines.db").toString());
        baseDeDatos.inicializar();
    }

    @Test
    void flujoPrincipal_todoQuedaRecibidoYAceptadoConTrazabilidad() {
        String salida = ejecutar(CORDOBA, "2001", "1", "s");

        assertTrue(salida.contains("Recepción registrada con éxito"));
        assertTrue(salida.contains("Para: cmrosario@organismo.gob.ar"), "notifica a la CM de origen (CU 29)");

        Bolsin bolsin = bolsin(2001);
        assertEquals("Recibido en CM Destino", bolsin.getEstadoActual().getNombre());
        assertEquals(2, bolsin.getCambioEstados().size());
        CambioEstadoBolsin anterior = bolsin.getCambioEstados().get(0);
        CambioEstadoBolsin vigente = bolsin.getCambioEstados().get(1);
        assertEquals("Enviado", anterior.getEstado().getNombre());
        assertNotNull(anterior.getFechaHoraFin());
        assertEquals(anterior.getFechaHoraFin(), vigente.getFechaHoraInicio());
        assertNull(vigente.getFechaHoraFin());
        assertEquals("Méndez", vigente.getResponsable().getApellido());

        for (Remito remito : bolsin.getRemitos()) {
            assertEquals("Recibido y Aceptado", remito.getEstadoActual().getNombre());
            for (DetalleRemito detalle : remito.getDetallesRemito()) {
                Documentacion documentacion = detalle.getDocumentacion();
                assertEquals("Recibida y Aceptada", documentacion.getEstadoActual().getNombre());
                assertEquals(2, documentacion.getCambioEstadoDocumentacion().size());
                assertNotNull(documentacion.getCambioEstadoDocumentacion().get(0).getFechaHoraFin());
                assertEquals("Méndez",
                        documentacion.getCambioEstadoDocumentacion().get(1).getResponsable().getApellido());
            }
        }
    }

    @Test
    void listado_soloBolsinesEnviadosALaCMDelUsuario() {
        String salida = ejecutar(CORDOBA, "c");

        assertTrue(salida.contains("Comisión médica: Comisión Médica Córdoba"));
        assertTrue(salida.contains("Bolsín 2001") && salida.contains("Bolsín 2002") && salida.contains("Bolsín 2003"));
        assertFalse(salida.contains("Bolsín 2004"), "destino CMC");
        assertFalse(salida.contains("Bolsín 2006"), "destino Rosario");
        assertFalse(salida.contains("Bolsín 2007"), "está Cerrado, no Enviado");
    }

    @Test
    void a1yA2_filtrosSinResultadoInformanYPermitenSeguir() {
        String salida = ejecutar(CORDOBA, "p 999999", "o Mendoza", "p 731202", "2002", "1", "s");

        assertTrue(salida.contains("[A1] No se encuentra bolsín con el número de precinto ingresado."));
        assertTrue(salida.contains("[A2] No se encuentra bolsín con la CM de origen ingresada."));
        assertEquals("Recibido en CM Destino", bolsin(2002).getEstadoActual().getNombre());
    }

    @Test
    void a3_documentacionNoRecibidaDejaElRemitoParcial() {
        ejecutar(CORDOBA, "2001", "2", "6002", "s");

        Bolsin bolsin = bolsin(2001);
        assertEquals("Recibido en CM Destino", bolsin.getEstadoActual().getNombre());
        assertEquals("No Recibida", documentacion(bolsin, 6002).getEstadoActual().getNombre());
        assertEquals("Recibida y Aceptada", documentacion(bolsin, 6001).getEstadoActual().getNombre());
        assertEquals("Recibido y Aceptado Parcial", remito(bolsin, 4001).getEstadoActual().getNombre());
        assertEquals("Recibido y Aceptado", remito(bolsin, 4002).getEstadoActual().getNombre());
    }

    @Test
    void a4_documentacionQueNoCorrespondeQuedaRechazada() {
        ejecutar(CORDOBA, "2002", "3", "6005, 6006", "s");

        Bolsin bolsin = bolsin(2002);
        assertEquals("Recibida y Rechazada", documentacion(bolsin, 6005).getEstadoActual().getNombre());
        assertEquals("Recibida y Rechazada", documentacion(bolsin, 6006).getEstadoActual().getNombre());
        assertEquals("Recibida y Aceptada", documentacion(bolsin, 6004).getEstadoActual().getNombre());
        assertEquals("Recibido y Aceptado Parcial", remito(bolsin, 4003).getEstadoActual().getNombre());
    }

    @Test
    void a5_laCMCRedirigeDocumentacion() {
        ejecutar(CENTRAL, "2004", "4", "6010,6011", "s");

        Bolsin bolsin = bolsin(2004);
        assertEquals("Recibido en CM Destino", bolsin.getEstadoActual().getNombre());
        assertEquals("Para Redirigir", documentacion(bolsin, 6010).getEstadoActual().getNombre());
        assertEquals("Para Redirigir", documentacion(bolsin, 6011).getEstadoActual().getNombre());
        assertEquals("Recibida y Aceptada", documentacion(bolsin, 6012).getEstadoActual().getNombre());
        assertEquals("Recibido y Aceptado", remito(bolsin, 4006).getEstadoActual().getNombre());
    }

    @Test
    void a5_unaCMQueNoEsLaCentralNoPuedeRedirigir() {
        String salida = ejecutar(CORDOBA, "2001", "4", "6001", "c");

        assertTrue(salida.contains("La opción 4 solo aplica cuando la CM destino es la Comisión Médica Central."));
        assertEquals("Enviado", bolsin(2001).getEstadoActual().getNombre());
    }

    @Test
    void a6_noConfirmarVuelveAPedirLaOpcionSinRegistrarNada() {
        String salida = ejecutar(CORDOBA, "2003", "2", "6008", "n", "c");

        assertTrue(salida.contains("No se confirmó la selección"));
        Bolsin bolsin = bolsin(2003);
        assertEquals("Enviado", bolsin.getEstadoActual().getNombre());
        assertEquals("En Bolsin Enviado", documentacion(bolsin, 6008).getEstadoActual().getNombre());
        assertEquals(1, bolsin.getCambioEstados().size());
    }

    @Test
    void validaciones_opcionAlternativaSinDocumentacionODeOtroBolsin() {
        String salida = ejecutar(CORDOBA, "2001", "2", "", "2", "6004", "c");

        assertTrue(salida.contains("Debe indicar al menos una documentación para esta opción."));
        assertTrue(salida.contains("La documentación 6004 no pertenece al bolsín."));
    }

    @Test
    void patronState_unBolsinYaRecibidoRechazaUnaNuevaRecepcion() {
        ejecutar(CORDOBA, "2001", "1", "s");
        Bolsin recibido = bolsin(2001);
        Empleado responsable = baseDeDatos.buscarEmpleados().get(0);

        // El estado concreto RecibidoEnCMDestino no redefine el evento: lo rechaza.
        assertThrows(TransicionNoPermitidaException.class, () -> recibido.registrarBolsinRecibido(
                LocalDateTime.now(), responsable, recibido.buscarCambioEstadoActual(),
                OpcionRecepcion.CONTENIDO_IGUAL, Set.of()));
        // Lo mismo la Documentación ya aceptada.
        assertThrows(TransicionNoPermitidaException.class, () -> documentacion(recibido, 6001)
                .registrarDocumentacionNoRecibida(LocalDateTime.now(), responsable));
    }

    @Test
    void web_mismoCasoDeUsoPorHttp() throws Exception {
        ServidorWeb servidor = new ServidorWeb(baseDeDatos, 0);
        servidor.iniciar();
        try {
            String base = "http://localhost:" + servidor.getPuerto();
            assertTrue(http("GET", base + "/").contains("id=\"raiz\""), "entrega la página");

            assertTrue(http("POST", base + "/api/sesion?usuario=" + CENTRAL).contains("Comisión Médica Central"));
            String listado = http("POST", base + "/api/recepcion/iniciar");
            assertTrue(listado.contains("\"paso\":\"seleccionBolsin\"") && listado.contains("\"numero\":2004"));

            assertTrue(http("GET", base + "/api/recepcion/filtro?precinto=999999").contains("\"flujoAlternativo\":\"A1\""));
            assertTrue(http("GET", base + "/api/recepcion/filtro?cmOrigen=Marte").contains("\"flujoAlternativo\":\"A2\""));

            assertTrue(http("POST", base + "/api/recepcion/seleccion?bolsin=2004").contains("\"paso\":\"opcion\""));
            // Opción alternativa sin documentación marcada: el gestor la rechaza.
            assertTrue(http("POST", base + "/api/recepcion/opcion?numero=4").contains("\"error\""));
            http("POST", base + "/api/recepcion/opcion?numero=4&documentacion=6010");
            // A6: no confirma y vuelve a las opciones.
            assertTrue(http("POST", base + "/api/recepcion/confirmacion?confirma=false").contains("\"paso\":\"opcion\""));
            http("POST", base + "/api/recepcion/opcion?numero=4&documentacion=6010");
            String resultado = http("POST", base + "/api/recepcion/confirmacion?confirma=true");
            assertTrue(resultado.contains("\"paso\":\"exito\"") && resultado.contains("\"emails\":[{"));

            assertEquals("Para Redirigir", documentacion(bolsin(2004), 6010).getEstadoActual().getNombre());
            assertTrue(http("GET", base + "/api/consulta").contains("Recibido en CM Destino"));
        } finally {
            servidor.detener();
        }
    }

    // ---------------------------------------------------------------- ayudas

    private String http(String metodo, String direccion) throws IOException, InterruptedException {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(direccion))
                .method(metodo, HttpRequest.BodyPublishers.noBody()).build();
        return HttpClient.newHttpClient().send(pedido, HttpResponse.BodyHandlers.ofString()).body();
    }

    /** Ejecuta el caso de uso con las líneas dadas como si las tipeara el EB. */
    private String ejecutar(String nombreUsuario, String... lineas) {
        Empleado empleado = baseDeDatos.buscarEmpleados().stream()
                .filter(e -> e.getUsuario().getNombre().equals(nombreUsuario)).findFirst().orElseThrow();
        Sesion sesion = new Sesion(LocalDateTime.now(), empleado.getUsuario());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream salida = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        GestorRecepcion gestor = new GestorRecepcion(baseDeDatos, sesion, new InterfazServidorCorreo(salida));
        Scanner entrada = new Scanner(String.join("\n", lineas) + "\n");
        new PantallaRecepcionConsola(gestor, entrada, salida).opcRegistrarRecepcion();
        return bytes.toString(StandardCharsets.UTF_8);
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
