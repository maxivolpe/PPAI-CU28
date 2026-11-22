package bolsines.web;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte mapas, listas, {@code record}s, textos, números y booleanos a texto JSON (sin
 * librerías). Un record se escribe como objeto con un campo por componente.
 */
final class Json {

    private Json() {
    }

    static String escribir(Object valor) {
        StringBuilder json = new StringBuilder();
        escribir(valor, json);
        return json.toString();
    }

    private static void escribir(Object valor, StringBuilder json) {
        if (valor == null) {
            json.append("null");
        } else if (valor instanceof Number || valor instanceof Boolean) {
            json.append(valor);
        } else if (valor instanceof Map<?, ?> mapa) {
            objeto(mapa, json);
        } else if (valor instanceof Collection<?> lista) {
            arreglo(lista, json);
        } else if (valor.getClass().isRecord()) {
            objeto(componentes(valor), json);
        } else {
            texto(String.valueOf(valor), json);
        }
    }

    private static void objeto(Map<?, ?> mapa, StringBuilder json) {
        json.append('{');
        boolean primero = true;
        for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
            if (!primero) {
                json.append(',');
            }
            primero = false;
            texto(String.valueOf(entrada.getKey()), json);
            json.append(':');
            escribir(entrada.getValue(), json);
        }
        json.append('}');
    }

    private static void arreglo(Collection<?> lista, StringBuilder json) {
        json.append('[');
        boolean primero = true;
        for (Object elemento : lista) {
            if (!primero) {
                json.append(',');
            }
            primero = false;
            escribir(elemento, json);
        }
        json.append(']');
    }

    private static Map<String, Object> componentes(Object registro) {
        Map<String, Object> campos = new LinkedHashMap<>();
        for (RecordComponent componente : registro.getClass().getRecordComponents()) {
            try {
                var lector = componente.getAccessor();
                lector.setAccessible(true);
                campos.put(componente.getName(), lector.invoke(registro));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("No se pudo leer el campo " + componente.getName(), e);
            }
        }
        return campos;
    }

    private static void texto(String texto, StringBuilder json) {
        json.append('"');
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        json.append('"');
    }
}
