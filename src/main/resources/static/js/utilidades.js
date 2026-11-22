// Funciones puras de presentación, sin React ni acceso a la red.

/** Une nombres de clase CSS ignorando los valores falsos: clases("a", false, "b") -> "a b". */
function clases(...nombres) {
    return nombres.filter(Boolean).join(" ");
}

/** Clase CSS que le corresponde a un estado: "negativo", "pendiente" o "positivo". */
function claseDeEstado(nombre) {
    if (ESTADOS_NEGATIVOS.includes(nombre)) {
        return "negativo";
    }
    if (ESTADOS_PENDIENTES.includes(nombre)) {
        return "pendiente";
    }
    return "positivo";
}

/** "2026-11-12T08:30:00" -> "2026-11-12 08:30:00"; sin fecha de fin el estado sigue "vigente". */
function formatearFechaHora(texto) {
    return texto ? texto.replace("T", " ") : "vigente";
}

/** Texto del aviso que corresponde al resultado de un filtro, o null si no hay nada que avisar. */
function mensajeDeFiltro(resultado) {
    if (resultado.flujoAlternativo === "A1") {
        return MENSAJES_FILTRO.A1;
    }
    if (resultado.flujoAlternativo === "A2") {
        return MENSAJES_FILTRO.A2;
    }
    if (resultado.bolsines.length === 0) {
        return MENSAJES_FILTRO.SIN_COINCIDENCIAS;
    }
    return null;
}
