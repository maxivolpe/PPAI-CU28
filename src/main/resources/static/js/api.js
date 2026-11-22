// Comunicación con el servidor. La página no tiene lógica de negocio: manda lo que hace el
// Encargado de Bolsines y dibuja la "vista" que devuelve el servidor.

/**
 * Hace un pedido al servidor y devuelve el JSON de la respuesta.
 * Si el servidor rechaza el pedido lanza un Error con el motivo que informó; si no se puede
 * hablar con el servidor lanza un Error con un texto que explica cómo seguir.
 */
async function pedir(metodo, ruta, parametros) {
    const consulta = parametros ? "?" + new URLSearchParams(parametros) : "";

    let respuesta;
    try {
        respuesta = await fetch(ruta + consulta, { method: metodo });
    } catch {
        throw new Error("No se pudo conectar con el servidor. Verificá que la aplicación esté en ejecución.");
    }

    const datos = await respuesta.json().catch(() => null);
    if (!respuesta.ok) {
        throw new Error((datos && datos.error) || `El servidor respondió con un error (${respuesta.status}).`);
    }
    if (datos === null) {
        throw new Error("El servidor envió una respuesta que no se pudo interpretar.");
    }
    return datos;
}
