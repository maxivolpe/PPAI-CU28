/**
 * CONTROL (rol «control» de Boundary-Control-Entity): GestorRecepcion coordina el CU 28 paso
 * por paso, tal como en los diagramas de secuencia. Lo ayudan ValidadorDeOpcionDeRecepcion
 * (reglas del paso 8) y NotificarRecepcionDeBolsin (CU 29 incluido).
 *
 * El gestor no conoce SQLite: declara lo que necesita de la persistencia con dos interfaces,
 * RepositorioDeBolsines y RepositorioDeEmpleados, que implementa {@code persistencia.BaseDeDatos}.
 *
 * Regla: no depende de la persistencia ni de la web. En arquitectura hexagonal es la capa de
 * «aplicación», y las dos interfaces son sus «puertos» de salida.
 */
package bolsines.control;
