/**
 * ENTITY (rol «entidad» de Boundary-Control-Entity): las clases del modelo de dominio de la
 * cátedra que usa el CU 28 — Bolsin, Remito, DetalleRemito, Documentacion, Empleado,
 * ComisionMedica, Sesion, Usuario — y sus historiales de cambio de estado.
 *
 * Bolsin, Remito y Documentacion son los CONTEXTOS del patrón State; sus estados están en los
 * subpaquetes {@code estado.bolsin}, {@code estado.remito} y {@code estado.documentacion}.
 *
 * Regla: este paquete no depende de ningún otro paquete del sistema ni de tecnología (ni SQL ni
 * HTTP). Lo verifica ArquitecturaTest. En arquitectura hexagonal es el «dominio».
 */
package bolsines.entity;
