package bolsines.persistencia;

import bolsines.entity.Bolsin;
import bolsines.entity.BolsinModificadoPorOtroUsuarioException;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.CambioEstadoDocumentacion;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.entity.Empleado;
import bolsines.entity.OpcionRecepcion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import static bolsines.soporte.Escenarios.AHORA;
import static bolsines.soporte.Escenarios.cordoba;
import static bolsines.soporte.Escenarios.empleado;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La persistencia SQLite contra un archivo de base real, en una carpeta temporal. */
@DisplayName("BaseDeDatos (SQLite)")
class BaseDeDatosTest {

    @TempDir
    Path carpeta;

    @Nested
    @DisplayName("Inicialización y migraciones")
    class Inicializacion {

        @Test
        void dadaUnaBaseNueva_cuandoSeInicializa_entoncesCreaLasTablasYCargaLosDatosDePrueba() {
            // Given / When
            BaseDeDatos baseDeDatos = baseInicializada();

            // Then
            assertEquals(7, baseDeDatos.buscarBolsines().size());
            assertEquals(5, baseDeDatos.buscarEmpleados().size());
        }

        @Test
        void dadaUnaBaseYaInicializada_cuandoSeInicializaOtraVez_entoncesNoDuplicaNada() throws Exception {
            // Given
            BaseDeDatos baseDeDatos = baseInicializada();

            // When
            baseDeDatos.inicializar();

            // Then
            assertEquals(7, baseDeDatos.buscarBolsines().size());
            assertEquals(MigradorDeEsquema.MIGRACIONES.size(), contar("SELECT COUNT(*) FROM esquema_version"));
        }

        @Test
        void dadoQueNoSeQuierenDatosDePrueba_cuandoSeInicializaUnaBaseNueva_entoncesQuedaVacia() {
            // Given
            BaseDeDatos baseDeDatos = new BaseDeDatos(rutaDeLaBase());

            // When
            baseDeDatos.inicializar(false);

            // Then
            assertTrue(baseDeDatos.buscarBolsines().isEmpty());
        }

        @Test
        void dadaUnaBaseCreadaPorLaVersionAnteriorDelPrograma_cuandoSeInicializa_entoncesSeMigraSinPerderDatos()
                throws Exception {
            // Given: tablas y datos, pero sin la tabla esquema_version (así quedaban las bases antes)
            ConexionSqlite conexion = new ConexionSqlite(rutaDeLaBase());
            conexion.enTransaccion(c -> {
                ScriptSql.ejecutar(c, "/db/migraciones/V001__esquema_inicial.sql");
                ScriptSql.ejecutar(c, "/db/datos/datos-de-prueba.sql");
            });

            // When
            BaseDeDatos baseDeDatos = new BaseDeDatos(rutaDeLaBase());
            baseDeDatos.inicializar();

            // Then
            assertEquals(7, baseDeDatos.buscarBolsines().size());
            assertEquals(MigradorDeEsquema.MIGRACIONES.size(), contar("SELECT COUNT(*) FROM esquema_version"));
        }
    }

    @Nested
    @DisplayName("Guardar la recepción")
    class GuardarRecepcion {

        @Test
        void dadaUnaRecepcionRegistrada_cuandoSeGuardaYSeVuelveALeer_entoncesEstadosEHistorialesQuedanIguales() {
            // Given
            BaseDeDatos baseDeDatos = baseInicializada();
            Empleado responsable = empleadoDeLaBase(baseDeDatos, "cmendez");
            Bolsin bolsin = recibirEnMemoria(baseDeDatos, 2001, responsable);

            // When
            baseDeDatos.guardarRecepcion(bolsin);

            // Then
            Bolsin releido = bolsin(baseDeDatos, 2001);
            List<CambioEstadoBolsin> historial = releido.getCambioEstados();
            assertAll(
                    () -> assertEquals("Recibido en CM Destino", releido.getEstadoActual().getNombre()),
                    () -> assertEquals(2, historial.size()),
                    () -> assertEquals(AHORA, historial.get(0).getFechaHoraFin()),
                    () -> assertEquals(AHORA, historial.get(1).getFechaHoraInicio()),
                    () -> assertNull(historial.get(1).getFechaHoraFin()),
                    () -> assertEquals(responsable.getEmail(), historial.get(1).getResponsable().getEmail()),
                    () -> assertEquals("Recibido y Aceptado", releido.getRemitos().get(0).getEstadoActual().getNombre()));
            List<CambioEstadoDocumentacion> historialDocumentacion =
                    documentacion(releido, 6001).getCambioEstadoDocumentacion();
            assertAll(
                    () -> assertEquals(2, historialDocumentacion.size()),
                    () -> assertEquals(AHORA, historialDocumentacion.get(0).getFechaHoraFin()),
                    () -> assertEquals("Recibida y Aceptada", historialDocumentacion.get(1).getEstado().getNombre()));
        }

        @Test
        void dadaUnaFallaAMitadDelGuardado_cuandoSeGuardaLaRecepcion_entoncesNoQuedaNadaAMedias() {
            // Given: un responsable que no existe en la tabla empleado hace fallar la clave foránea
            // del historial, DESPUÉS de haber actualizado el estado del bolsín
            BaseDeDatos baseDeDatos = baseInicializada();
            Bolsin bolsin = recibirEnMemoria(baseDeDatos, 2001, empleado("fantasma", cordoba()));

            // When
            assertThrows(IllegalStateException.class, () -> baseDeDatos.guardarRecepcion(bolsin));

            // Then
            Bolsin releido = bolsin(baseDeDatos, 2001);
            assertAll(
                    () -> assertEquals("Enviado", releido.getEstadoActual().getNombre()),
                    () -> assertEquals(1, releido.getCambioEstados().size()),
                    () -> assertNull(releido.getCambioEstados().get(0).getFechaHoraFin()),
                    () -> assertEquals("En Bolsin Enviado", documentacion(releido, 6001).getEstadoActual().getNombre()));
        }

        @Test
        void dadoUnBolsinQueOtroUsuarioYaRecibio_cuandoSeGuardaOtraRecepcionDelMismo_entoncesSeRechazaYSeConservaLaPrimera() {
            // Given: dos usuarios leyeron el bolsín 2001 cuando estaba "Enviado"
            BaseDeDatos baseDeDatos = baseInicializada();
            Empleado responsable = empleadoDeLaBase(baseDeDatos, "cmendez");
            Bolsin delPrimero = recibirEnMemoria(baseDeDatos, 2001, responsable);
            Bolsin delSegundo = recibirEnMemoria(baseDeDatos, 2001, responsable);
            baseDeDatos.guardarRecepcion(delPrimero);

            // When / Then
            assertThrows(BolsinModificadoPorOtroUsuarioException.class, () -> baseDeDatos.guardarRecepcion(delSegundo));
            assertEquals(2, bolsin(baseDeDatos, 2001).getCambioEstados().size());
        }
    }

    // ---------------------------------------------------------------- ayudas

    private String rutaDeLaBase() {
        return carpeta.resolve("bolsines.db").toString();
    }

    private BaseDeDatos baseInicializada() {
        BaseDeDatos baseDeDatos = new BaseDeDatos(rutaDeLaBase());
        baseDeDatos.inicializar();
        return baseDeDatos;
    }

    /** Lee el bolsín de la base y registra su recepción (opción 1) solo en memoria, sin guardarla. */
    private Bolsin recibirEnMemoria(BaseDeDatos baseDeDatos, int numero, Empleado responsable) {
        Bolsin bolsin = bolsin(baseDeDatos, numero);
        bolsin.registrarBolsinRecibido(AHORA, responsable, bolsin.buscarCambioEstadoActual(),
                OpcionRecepcion.CONTENIDO_IGUAL, Set.of());
        return bolsin;
    }

    private Empleado empleadoDeLaBase(BaseDeDatos baseDeDatos, String nombreUsuario) {
        return baseDeDatos.buscarEmpleados().stream()
                .filter(e -> e.getUsuario().getNombre().equals(nombreUsuario)).findFirst().orElseThrow();
    }

    private Bolsin bolsin(BaseDeDatos baseDeDatos, int numero) {
        return baseDeDatos.buscarBolsines().stream()
                .filter(b -> b.getNumeroBolsin() == numero).findFirst().orElseThrow();
    }

    private Documentacion documentacion(Bolsin bolsin, int numero) {
        return bolsin.getRemitos().stream()
                .flatMap(r -> r.getDetallesRemito().stream())
                .map(DetalleRemito::getDocumentacion)
                .filter(d -> d.getNumero() == numero).findFirst().orElseThrow();
    }

    private int contar(String sql) throws Exception {
        return new ConexionSqlite(rutaDeLaBase()).consultar(c -> {
            try (Statement consulta = c.createStatement(); ResultSet fila = consulta.executeQuery(sql)) {
                fila.next();
                return fila.getInt(1);
            }
        });
    }
}
