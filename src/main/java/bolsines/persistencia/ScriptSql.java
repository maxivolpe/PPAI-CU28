package bolsines.persistencia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Ejecuta un archivo .sql que viaja dentro del programa (carpeta resources/db). Lo parte en
 * sentencias por ';', sin cortar los ';' que estén dentro de un texto entre comillas y
 * descartando los comentarios que empiezan con '--'.
 */
final class ScriptSql {

    private ScriptSql() {
    }

    static void ejecutar(Connection conexion, String recurso) throws SQLException {
        try (Statement sentencia = conexion.createStatement()) {
            for (String sql : sentencias(leer(recurso))) {
                sentencia.executeUpdate(sql);
            }
        }
    }

    static List<String> sentencias(String script) {
        List<String> sentencias = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean dentroDeTexto = false;
        for (int i = 0; i < script.length(); i++) {
            char caracter = script.charAt(i);
            if (!dentroDeTexto && script.startsWith("--", i)) {
                int finDeLinea = script.indexOf('\n', i);
                i = (finDeLinea < 0 ? script.length() : finDeLinea) - 1;   // salta el comentario
                continue;
            }
            if (caracter == '\'') {
                dentroDeTexto = !dentroDeTexto;
            }
            if (caracter == ';' && !dentroDeTexto) {
                agregarSiNoEstaVacia(sentencias, actual);
                actual.setLength(0);
            } else {
                actual.append(caracter);
            }
        }
        agregarSiNoEstaVacia(sentencias, actual);
        return sentencias;
    }

    private static void agregarSiNoEstaVacia(List<String> sentencias, StringBuilder sentencia) {
        String sql = sentencia.toString().trim();
        if (!sql.isEmpty()) {
            sentencias.add(sql);
        }
    }

    private static String leer(String recurso) {
        try (InputStream contenido = ScriptSql.class.getResourceAsStream(recurso)) {
            if (contenido == null) {
                throw new IllegalStateException("No se encontró el script " + recurso);
            }
            return new String(contenido.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el script " + recurso, e);
        }
    }
}
