/**
 * PERSISTENCIA en SQLite con JDBC puro (no figura en los diagramas de la realización del CU).
 * BaseDeDatos implementa las interfaces RepositorioDeBolsines y RepositorioDeEmpleados del
 * paquete control, y reparte el trabajo en ConexionSqlite, MigradorDeEsquema,
 * LectorDeTablas y GrabadorDeRecepcion. Los scripts SQL están en resources/db.
 *
 * Del patrón State solo se guarda el nombre del estado actual; MapeoEstados reconstruye el
 * objeto estado al leer. En arquitectura hexagonal es un «adaptador» de salida.
 */
package bolsines.persistencia;
