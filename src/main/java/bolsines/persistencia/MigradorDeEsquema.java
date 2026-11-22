package bolsines.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lleva la base al esquema actual: aplica, en orden y una sola vez, los scripts de la carpeta
 * resources/db/migraciones, y anota cada uno en la tabla esquema_version. Para cambiar el
 * esquema se agrega un script nuevo al final de la lista; los ya aplicados no se tocan.
 *
 * Una base creada por versiones anteriores del programa (sin esquema_version) también se
 * migra sin problemas, porque los scripts usan "IF NOT EXISTS".
 */
final class MigradorDeEsquema {

    static final List<String> MIGRACIONES = List.of(
            "V001__esquema_inicial.sql",
            "V002__indices.sql");

    private final ConexionSqlite conexion;

    MigradorDeEsquema(ConexionSqlite conexion) {
        this.conexion = conexion;
    }

    void migrar() throws SQLException {
        conexion.enTransaccion(this::crearTablaDeVersiones);
        Set<String> aplicadas = conexion.consultar(this::migracionesAplicadas);
        for (String migracion : MIGRACIONES) {
            if (!aplicadas.contains(migracion)) {
                conexion.enTransaccion(c -> aplicar(c, migracion));
            }
        }
    }

    private void crearTablaDeVersiones(Connection c) throws SQLException {
        try (Statement sentencia = c.createStatement()) {
            sentencia.executeUpdate("CREATE TABLE IF NOT EXISTS esquema_version ("
                    + "migracion TEXT PRIMARY KEY, aplicada_el TEXT NOT NULL)");
        }
    }

    private Set<String> migracionesAplicadas(Connection c) throws SQLException {
        Set<String> aplicadas = new HashSet<>();
        try (Statement consulta = c.createStatement();
             ResultSet fila = consulta.executeQuery("SELECT migracion FROM esquema_version")) {
            while (fila.next()) {
                aplicadas.add(fila.getString("migracion"));
            }
        }
        return aplicadas;
    }

    /** El script y su anotación van en la misma transacción: o se aplica entero y queda anotado, o nada. */
    private void aplicar(Connection c, String migracion) throws SQLException {
        ScriptSql.ejecutar(c, "/db/migraciones/" + migracion);
        try (PreparedStatement anotar = c.prepareStatement(
                "INSERT INTO esquema_version (migracion, aplicada_el) VALUES (?, ?)")) {
            anotar.setString(1, migracion);
            anotar.setString(2, LocalDateTime.now().toString());
            anotar.executeUpdate();
        }
    }
}
