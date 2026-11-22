package bolsines.boundary;

/**
 * Lo que el EB puede escribir en la consola al elegir un bolsín (pasos 4 y 5). Interpretar el
 * texto es una función pura: no lee ni escribe nada, por eso se prueba sin consola.
 */
sealed interface ComandoDeSeleccion {

    /** Cancela el caso de uso (observación 1). */
    record Cancelar() implements ComandoDeSeleccion {
    }

    /** Vuelve a mostrar todos los bolsines, sin filtro. */
    record VerTodos() implements ComandoDeSeleccion {
    }

    /** Filtro por número de precinto (A1 si no hay resultados). {@code precinto} es null si el texto no era un número. */
    record FiltrarPorPrecinto(Integer precinto) implements ComandoDeSeleccion {
    }

    /** Filtro por parte del nombre de la CM de origen (A2 si no hay resultados). */
    record FiltrarPorOrigen(String texto) implements ComandoDeSeleccion {
    }

    /** Elige un bolsín por su número. */
    record Seleccionar(int numeroBolsin) implements ComandoDeSeleccion {
    }

    /** Texto que no corresponde a ningún comando. */
    record Invalido() implements ComandoDeSeleccion {
    }

    String CANCELAR = "c";
    String VER_TODOS = "t";

    static ComandoDeSeleccion interpretar(String texto) {
        String minusculas = texto.toLowerCase();
        if (minusculas.equals(CANCELAR)) {
            return new Cancelar();
        }
        if (minusculas.equals(VER_TODOS)) {
            return new VerTodos();
        }
        if (minusculas.startsWith("p ")) {
            return new FiltrarPorPrecinto(aEntero(texto.substring(2)));
        }
        if (minusculas.startsWith("o ")) {
            return new FiltrarPorOrigen(texto.substring(2));
        }
        Integer numero = aEntero(texto);
        return numero == null ? new Invalido() : new Seleccionar(numero);
    }

    /** El entero que expresa el texto, o null si no es un número. */
    static Integer aEntero(String texto) {
        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
