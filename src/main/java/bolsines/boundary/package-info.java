/**
 * BOUNDARY (rol «frontera» de Boundary-Control-Entity): PantallaRecepcion es la interfaz que
 * ve el gestor; PantallaRecepcionWeb y PantallaRecepcionConsola son sus dos versiones.
 * InterfazServidorCorreo es la frontera con el actor Servidor de Correo (CU 29, simulado).
 *
 * Regla: no tiene lógica de negocio y no conoce la base de datos. En arquitectura hexagonal
 * son los «adaptadores» de entrada (pantallas) y de salida (correo).
 */
package bolsines.boundary;
