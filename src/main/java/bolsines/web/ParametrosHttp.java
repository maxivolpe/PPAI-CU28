package bolsines.web;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Lectura de los parámetros que llegan en la dirección: /api/algo?nombre=valor&otro=valor. */
final class ParametrosHttp {

    private final Map<String, String> valores;

    private ParametrosHttp(Map<String, String> valores) {
        this.valores = valores;
    }

    static ParametrosHttp desde(String consultaSinDecodificar) {
        Map<String, String> valores = new LinkedHashMap<>();
        if (consultaSinDecodificar != null) {
            for (String par : consultaSinDecodificar.split("&")) {
                int igual = par.indexOf('=');
                if (igual > 0) {
                    valores.put(URLDecoder.decode(par.substring(0, igual), StandardCharsets.UTF_8),
                            URLDecoder.decode(par.substring(igual + 1), StandardCharsets.UTF_8));
                }
            }
        }
        return new ParametrosHttp(valores);
    }

    /** El valor del parámetro, o null si no vino. */
    String texto(String nombre) {
        return valores.get(nombre);
    }

    boolean tiene(String nombre) {
        String valor = valores.get(nombre);
        return valor != null && !valor.isBlank();
    }

    int entero(String nombre) {
        try {
            return Integer.parseInt(valores.get(nombre).trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new PedidoInvalidoException("Falta el número '" + nombre + "'.");
        }
    }

    /** "5001,5002" -> {5001, 5002}. */
    Set<Integer> numeros(String nombre) {
        Set<Integer> numeros = new LinkedHashSet<>();
        String texto = valores.get(nombre);
        if (texto != null && !texto.isBlank()) {
            for (String parte : texto.split(",")) {
                try {
                    numeros.add(Integer.valueOf(parte.trim()));
                } catch (NumberFormatException e) {
                    throw new PedidoInvalidoException("Número de documentación no válido: " + parte);
                }
            }
        }
        return numeros;
    }
}
