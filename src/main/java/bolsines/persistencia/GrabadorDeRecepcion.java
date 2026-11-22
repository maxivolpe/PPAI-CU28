package bolsines.persistencia;

import bolsines.entity.Bolsin;
import bolsines.entity.BolsinModificadoPorOtroUsuarioException;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.CambioEstadoDocumentacion;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.entity.Remito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Escribe el resultado de registrar la recepción de un bolsín. BaseDeDatos lo llama dentro de
 * una transacción, así que si algo falla no queda nada a medias.
 *
 * Por cada objeto que cambió de estado se hace lo mismo que hizo el patrón State en memoria:
 * se actualiza el estado actual, se cierra en la base el cambio de estado vigente y se agrega
 * el nuevo. El historial anterior no se toca.
 */
final class GrabadorDeRecepcion {

    private GrabadorDeRecepcion() {
    }

    static void grabar(Connection conexion, Bolsin bolsin) throws SQLException {
        actualizarEstadoDelBolsin(conexion, bolsin);
        registrarNuevoCambioDelBolsin(conexion, bolsin);
        for (Remito remito : bolsin.getRemitos()) {
            ejecutar(conexion, "UPDATE remito SET estado = ? WHERE numero = ?",
                    remito.getEstadoActual().getNombre(), remito.getNumero());
            for (DetalleRemito detalle : remito.getDetallesRemito()) {
                grabarDocumentacion(conexion, detalle.getDocumentacion());
            }
        }
    }

    // ---------------------------------------------------------------- bolsín

    /**
     * Cambia el estado del bolsín solo si en la base sigue en el estado en que se leyó. Si otro
     * usuario lo recibió mientras tanto, la recepción se rechaza en lugar de pisar la suya.
     */
    private static void actualizarEstadoDelBolsin(Connection conexion, Bolsin bolsin) throws SQLException {
        int filasActualizadas = ejecutar(conexion,
                "UPDATE bolsin SET estado = ? WHERE numero_bolsin = ? AND estado = ?",
                bolsin.getEstadoActual().getNombre(), bolsin.getNumeroBolsin(), estadoAlLeerlo(bolsin));
        if (filasActualizadas != 1) {
            throw new BolsinModificadoPorOtroUsuarioException(bolsin.getNumeroBolsin());
        }
    }

    /** El estado en que estaba el bolsín al leerlo de la base: el del último cambio de estado ya cerrado. */
    private static String estadoAlLeerlo(Bolsin bolsin) {
        List<CambioEstadoBolsin> historial = bolsin.getCambioEstados();
        for (int i = historial.size() - 1; i >= 0; i--) {
            if (!historial.get(i).sosActual()) {
                return historial.get(i).getEstado().getNombre();
            }
        }
        return bolsin.getEstadoActual().getNombre();
    }

    private static void registrarNuevoCambioDelBolsin(Connection conexion, Bolsin bolsin) throws SQLException {
        CambioEstadoBolsin nuevo = bolsin.buscarCambioEstadoActual();
        ejecutar(conexion, "UPDATE cambio_estado_bolsin SET fecha_hora_fin = ? "
                        + "WHERE numero_bolsin = ? AND fecha_hora_fin IS NULL",
                texto(nuevo.getFechaHoraInicio()), bolsin.getNumeroBolsin());
        ejecutar(conexion, "INSERT INTO cambio_estado_bolsin "
                        + "(numero_bolsin, fecha_hora_inicio, fecha_hora_fin, estado, responsable_email) "
                        + "VALUES (?, ?, NULL, ?, ?)",
                bolsin.getNumeroBolsin(), texto(nuevo.getFechaHoraInicio()), nuevo.getEstado().getNombre(),
                nuevo.getResponsable().getEmail());
    }

    // ---------------------------------------------------------------- documentación

    private static void grabarDocumentacion(Connection conexion, Documentacion documentacion) throws SQLException {
        ejecutar(conexion, "UPDATE documentacion SET estado = ? WHERE numero = ?",
                documentacion.getEstadoActual().getNombre(), documentacion.getNumero());

        CambioEstadoDocumentacion nuevo = cambioVigente(documentacion);
        ejecutar(conexion, "UPDATE cambio_estado_documentacion SET fecha_hora_fin = ? "
                        + "WHERE numero_documentacion = ? AND fecha_hora_fin IS NULL",
                texto(nuevo.getFechaHoraInicio()), documentacion.getNumero());
        ejecutar(conexion, "INSERT INTO cambio_estado_documentacion "
                        + "(numero_documentacion, fecha_hora_inicio, fecha_hora_fin, estado, responsable_email) "
                        + "VALUES (?, ?, NULL, ?, ?)",
                documentacion.getNumero(), texto(nuevo.getFechaHoraInicio()), nuevo.getEstado().getNombre(),
                nuevo.getResponsable().getEmail());
    }

    /** El cambio de estado que el patrón State acaba de agregar: el único sin fecha y hora de fin. */
    private static CambioEstadoDocumentacion cambioVigente(Documentacion documentacion) {
        for (CambioEstadoDocumentacion cambio : documentacion.getCambioEstadoDocumentacion()) {
            if (cambio.sosActual()) {
                return cambio;
            }
        }
        throw new IllegalStateException(
                "La documentación " + documentacion.getNumero() + " no tiene un cambio de estado vigente.");
    }

    // ---------------------------------------------------------------- utilidades JDBC

    /** @return la cantidad de filas modificadas. */
    private static int ejecutar(Connection conexion, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                sentencia.setObject(i + 1, parametros[i]);
            }
            return sentencia.executeUpdate();
        }
    }

    private static String texto(LocalDateTime fechaHora) {
        return fechaHora.toString();
    }
}
