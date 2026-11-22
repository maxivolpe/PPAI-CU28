package bolsines.persistencia;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptSqlTest {

    @Test
    void dadoUnScriptConVariasSentencias_cuandoSeParte_entoncesDevuelveUnaPorCadaPuntoYComa() {
        assertEquals(List.of("CREATE TABLE a (x INTEGER)", "INSERT INTO a VALUES (1)"),
                ScriptSql.sentencias("CREATE TABLE a (x INTEGER);\nINSERT INTO a VALUES (1);\n"));
    }

    @Test
    void dadoUnPuntoYComaDentroDeUnTexto_cuandoSeParte_entoncesNoCortaLaSentencia() {
        assertEquals(List.of("INSERT INTO a VALUES ('uno; dos')"),
                ScriptSql.sentencias("INSERT INTO a VALUES ('uno; dos');"));
    }

    @Test
    void dadosComentariosDeLinea_cuandoSeParte_entoncesSeDescartan() {
        assertEquals(List.of("SELECT 1", "SELECT 2"),
                ScriptSql.sentencias("-- primero; con punto y coma\nSELECT 1;\nSELECT 2; -- al final\n-- sin sentencia"));
    }

    @Test
    void dadoUnGuionDobleDentroDeUnTexto_cuandoSeParte_entoncesNoSeTomaComoComentario() {
        assertEquals(List.of("INSERT INTO a VALUES ('a -- b')"),
                ScriptSql.sentencias("INSERT INTO a VALUES ('a -- b');"));
    }
}
