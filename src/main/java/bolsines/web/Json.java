package bolsines.web;

import java.util.Collection;
import java.util.Map;

/** Convierte mapas, listas, textos, números y booleanos a texto JSON (sin librerías). */
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
        } else {
            texto(String.valueOf(valor), json);
        }
    }

    /** {"clave":valor,...} */
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

    /** [valor,...] */
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
