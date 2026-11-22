package bolsines.persistencia;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Abre las conexiones a la base SQLite y ejecuta trabajos dentro de una transacción. */
final class ConexionSqlite {

    /** Un trabajo de escritura sobre una conexión abierta. */
    @FunctionalInterface
    interface Accion {
        void ejecutar(Connection conexion) throws SQLException;
    }

    /** Un trabajo de lectura que devuelve un resultado. */
    @FunctionalInterface
    interface Consulta<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    private final String url;

    /** @param rutaArchivo archivo de la base, por ejemplo "data/bolsines.db". La carpeta se crea si no existe. */
    ConexionSqlite(String rutaArchivo) {
        File carpeta = new File(rutaArchivo).getAbsoluteFile().getParentFile();
        if (carpeta != null) {
            carpeta.mkdirs();
        }
        this.url = "jdbc:sqlite:" + rutaArchivo;
    }

    /** Ejecuta la acción en una transacción: si algo falla, no queda nada a medias. */
    void enTransaccion(Accion accion) throws SQLException {
        try (Connection conexion = abrir()) {
            conexion.setAutoCommit(false);
            try {
                accion.ejecutar(conexion);
                conexion.commit();
            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    <T> T consultar(Consulta<T> consulta) throws SQLException {
        try (Connection conexion = abrir()) {
            return consulta.ejecutar(conexion);
        }
    }

    private Connection abrir() throws SQLException {
        Connection conexion = DriverManager.getConnection(url);
        try (Statement sentencia = conexion.createStatement()) {
            // SQLite solo controla las REFERENCES del esquema si se lo pide en cada conexión.
            sentencia.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            conexion.close();
            throw e;
        }
        return conexion;
    }
}
