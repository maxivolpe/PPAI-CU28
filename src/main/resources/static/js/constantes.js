// Constantes de la interfaz: rutas del servidor, pasos del caso de uso, avisos y clasificación de estados.

/** Rutas del servidor (ServidorWeb). Es el único lugar donde aparecen escritas. */
const RUTAS = Object.freeze({
    estado: "/api/estado",
    sesion: "/api/sesion",
    consulta: "/api/consulta",
    filtro: "/api/recepcion/filtro",
    iniciar: "/api/recepcion/iniciar",
    seleccion: "/api/recepcion/seleccion",
    opcion: "/api/recepcion/opcion",
    confirmacion: "/api/recepcion/confirmacion",
    cancelar: "/api/recepcion/cancelar",
});

/** Pasos del caso de uso, en el orden en que los muestra el indicador de progreso. */
const PASOS = Object.freeze([
    { id: "seleccionBolsin", titulo: "Elegir bolsín" },
    { id: "opcion", titulo: "Controlar contenido" },
    { id: "confirmacion", titulo: "Confirmar" },
    { id: "exito", titulo: "Resultado" },
]);

const PESTANIAS = Object.freeze({
    recepcion: "recepcion",
    consulta: "consulta",
});

/** Tipos de aviso (pop-up). Coinciden con las clases CSS de .aviso. */
const TIPO_AVISO = Object.freeze({
    info: "info",
    alerta: "alerta",
    error: "error",
});

/** Textos de los flujos alternativos A1 y A2 del filtro de bolsines. */
const MENSAJES_FILTRO = Object.freeze({
    A1: "No se encuentra bolsín con el número de precinto ingresado.",
    A2: "No se encuentra bolsín con la CM de origen ingresada.",
    SIN_COINCIDENCIAS: "Ningún bolsín cumple los dos filtros a la vez.",
});

/** Estados que se muestran en rojo (algo faltó) y en amarillo (pendiente); el resto va en verde. */
const ESTADOS_NEGATIVOS = Object.freeze(["No Recibida", "Recibida y Rechazada", "Recibido y Aceptado Parcial"]);
const ESTADOS_PENDIENTES = Object.freeze(["Enviado", "En Bolsin Enviado", "Para Redirigir", "Cerrado"]);
