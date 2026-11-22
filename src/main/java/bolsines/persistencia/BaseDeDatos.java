package bolsines.persistencia;

import bolsines.control.RepositorioDeBolsines;
import bolsines.control.RepositorioDeEmpleados;
import bolsines.entity.Bolsin;
import bolsines.entity.Empleado;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Persistencia en SQLite con JDBC puro (sin framework). Implementa los dos repositorios que
 * usa el gestor: el gestor conoce solo las interfaces RepositorioDeBolsines y
 * RepositorioDeEmpleados, no sabe que detrás hay SQLite.
 *
 * Del patrón State solo se persiste el NOMBRE del estado actual; el objeto estado concreto
 * se reconstruye al leer con MapeoEstados.
 *
 * El trabajo está repartido en clases chicas de este paquete:
 *  - ConexionSqlite: abre conexiones y ejecuta transacciones;
 *  - MigradorDeEsquema: crea o actualiza las tablas (scripts de resources/db/migraciones);
 *  - LectorDeTablas: lee las tablas y arma los objetos del dominio;
 *  - GrabadorDeRecepcion: guarda el resultado de la recepción.
 */
public class BaseDeDatos implements RepositorioDeBolsines, RepositorioDeEmpleados {

    private static final String DATOS_DE_PRUEBA = "/db/datos/datos-de-prueba.sql";

    private final ConexionSqlite conexion;

    /** @param rutaArchivo archivo de la base, por ejemplo "data/bolsines.db". Se crea si no existe. */
    public BaseDeDatos(String rutaArchivo) {
        this.conexion = new ConexionSqlite(rutaArchivo);
    }

    /** Crea o actualiza las tablas y, si la base está vacía, carga los datos de prueba. */
    public void inicializar() {
        inicializar(true);
    }

    /** @param cargarDatosDePrueba si es verdadero y la base está vacía, carga los datos de prueba. */
    public void inicializar(boolean cargarDatosDePrueba) {
        try {
            new MigradorDeEsquema(conexion).migrar();
            if (cargarDatosDePrueba && conexion.consultar(this::estaVacia)) {
                conexion.enTransaccion(c -> ScriptSql.ejecutar(c, DATOS_DE_PRUEBA));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo inicializar la base de datos.", e);
        }
    }

    // ---------------------------------------------------------------- RepositorioDeEmpleados

    @Override
    public List<Empleado> buscarEmpleados() {
        try {
            return conexion.consultar(LectorDeTablas::leerEmpleados);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron leer los empleados.", e);
        }
    }

    // ---------------------------------------------------------------- RepositorioDeBolsines

    @Override
    public List<Bolsin> buscarBolsines() {
        try {
            return conexion.consultar(LectorDeTablas::leerBolsines);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron leer los bolsines.", e);
        }
    }

    @Override
    public void guardarRecepcion(Bolsin bolsin) {
        try {
            conexion.enTransaccion(c -> GrabadorDeRecepcion.grabar(c, bolsin));
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la recepción del bolsín.", e);
        }
    }

    // ---------------------------------------------------------------- utilidades

    private boolean estaVacia(Connection c) throws SQLException {
        try (Statement consulta = c.createStatement();
             ResultSet filas = consulta.executeQuery("SELECT COUNT(*) FROM comision_medica")) {
            filas.next();
            return filas.getInt(1) == 0;
        }
    }
}
