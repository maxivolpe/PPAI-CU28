package bolsines.persistencia;

import bolsines.entity.Bolsin;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.CambioEstadoDocumentacion;
import bolsines.entity.ComisionMedica;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.entity.Empleado;
import bolsines.entity.Remito;
import bolsines.entity.TipoDocumentacion;
import bolsines.entity.Usuario;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee las tablas y arma los objetos del dominio. Cada tabla se lee una vez y las filas se
 * enganchan por su clave (mapas por código, email o número). El estado concreto de cada
 * objeto se reconstruye a partir del nombre guardado con {@link MapeoEstados}.
 */
final class LectorDeTablas {

    /** Qué hacer con cada fila de una consulta. */
    @FunctionalInterface
    private interface LectorDeFila {
        void leer(ResultSet fila) throws SQLException;
    }

    private LectorDeTablas() {
    }

    static List<Empleado> leerEmpleados(Connection conexion) throws SQLException {
        return new ArrayList<>(empleadosPorEmail(conexion, comisionesPorCodigo(conexion)).values());
    }

    /** Todos los bolsines con sus remitos, detalles, documentación e historiales. */
    static List<Bolsin> leerBolsines(Connection conexion) throws SQLException {
        Map<String, ComisionMedica> comisiones = comisionesPorCodigo(conexion);
        Map<String, Empleado> empleados = empleadosPorEmail(conexion, comisiones);
        Map<Integer, Documentacion> documentaciones = documentacionesPorNumero(conexion, empleados);
        Map<Integer, Bolsin> bolsines = bolsinesPorNumero(conexion, comisiones, empleados);
        agregarRemitos(conexion, comisiones, documentaciones, bolsines);
        return new ArrayList<>(bolsines.values());
    }

    // ---------------------------------------------------------------- organización

    private static Map<String, ComisionMedica> comisionesPorCodigo(Connection conexion) throws SQLException {
        Map<String, ComisionMedica> comisiones = new HashMap<>();
        recorrer(conexion, "SELECT * FROM comision_medica", fila ->
                comisiones.put(fila.getString("codigo"), new ComisionMedica(
                        fila.getString("codigo"), fila.getString("nombre"), fila.getString("direccion"),
                        fila.getString("email"), fila.getString("telefono"), fila.getInt("es_central") == 1)));
        return comisiones;
    }

    private static Map<String, Empleado> empleadosPorEmail(Connection conexion,
                                                           Map<String, ComisionMedica> comisiones) throws SQLException {
        Map<String, Empleado> empleados = new LinkedHashMap<>();
        String sql = "SELECT e.*, u.contrasenia FROM empleado e JOIN usuario u ON u.nombre = e.usuario_nombre "
                + "ORDER BY e.usuario_nombre";
        recorrer(conexion, sql, fila -> {
            Usuario usuario = new Usuario(fila.getString("usuario_nombre"), fila.getString("contrasenia"));
            empleados.put(fila.getString("email"), new Empleado(
                    fila.getString("nombre"), fila.getString("apellido"), fila.getString("email"),
                    comisiones.get(fila.getString("cm_codigo")), usuario));
        });
        return empleados;
    }

    // ---------------------------------------------------------------- documentación

    private static Map<Integer, Documentacion> documentacionesPorNumero(Connection conexion,
                                                                        Map<String, Empleado> empleados)
            throws SQLException {
        Map<String, TipoDocumentacion> tipos = new HashMap<>();
        recorrer(conexion, "SELECT * FROM tipo_documentacion", fila ->
                tipos.put(fila.getString("nombre"),
                        new TipoDocumentacion(fila.getString("nombre"), fila.getString("descripcion"))));

        Map<Integer, Documentacion> documentaciones = new HashMap<>();
        recorrer(conexion, "SELECT * FROM documentacion", fila ->
                documentaciones.put(fila.getInt("numero"), new Documentacion(
                        fila.getInt("numero"), fila.getString("asunto"),
                        LocalDate.parse(fila.getString("fecha_pase")), tipos.get(fila.getString("tipo")),
                        MapeoEstados.estadoDocumentacion(fila.getString("estado")))));

        recorrer(conexion, "SELECT * FROM cambio_estado_documentacion ORDER BY id", fila -> {
            CambioEstadoDocumentacion cambio = new CambioEstadoDocumentacion(
                    LocalDateTime.parse(fila.getString("fecha_hora_inicio")),
                    empleados.get(fila.getString("responsable_email")),
                    MapeoEstados.estadoDocumentacion(fila.getString("estado")));
            cambio.setFechaHoraFin(fechaHora(fila.getString("fecha_hora_fin")));
            documentaciones.get(fila.getInt("numero_documentacion")).agregarCambioEstado(cambio);
        });
        return documentaciones;
    }

    // ---------------------------------------------------------------- bolsines y remitos

    private static Map<Integer, Bolsin> bolsinesPorNumero(Connection conexion, Map<String, ComisionMedica> comisiones,
                                                          Map<String, Empleado> empleados) throws SQLException {
        Map<Integer, Bolsin> bolsines = new LinkedHashMap<>();
        recorrer(conexion, "SELECT * FROM bolsin ORDER BY numero_bolsin", fila ->
                bolsines.put(fila.getInt("numero_bolsin"), new Bolsin(
                        fila.getInt("numero_bolsin"), fila.getInt("numero_precinto"),
                        LocalDate.parse(fila.getString("fecha")), fila.getInt("peso"),
                        comisiones.get(fila.getString("cm_origen")), comisiones.get(fila.getString("cm_destino")),
                        MapeoEstados.estadoBolsin(fila.getString("estado")))));

        recorrer(conexion, "SELECT * FROM cambio_estado_bolsin ORDER BY id", fila -> {
            CambioEstadoBolsin cambio = new CambioEstadoBolsin(
                    LocalDateTime.parse(fila.getString("fecha_hora_inicio")),
                    empleados.get(fila.getString("responsable_email")),
                    MapeoEstados.estadoBolsin(fila.getString("estado")));
            cambio.setFechaHoraFin(fechaHora(fila.getString("fecha_hora_fin")));
            bolsines.get(fila.getInt("numero_bolsin")).agregarCambioEstado(cambio);
        });
        return bolsines;
    }

    private static void agregarRemitos(Connection conexion, Map<String, ComisionMedica> comisiones,
                                       Map<Integer, Documentacion> documentaciones, Map<Integer, Bolsin> bolsines)
            throws SQLException {
        Map<Integer, Remito> remitos = new HashMap<>();
        recorrer(conexion, "SELECT * FROM remito ORDER BY numero", fila -> {
            Remito remito = new Remito(
                    fila.getInt("numero"), LocalDate.parse(fila.getString("fecha")),
                    comisiones.get(fila.getString("cm_origen")), comisiones.get(fila.getString("cm_destino")),
                    MapeoEstados.estadoRemito(fila.getString("estado")));
            remitos.put(remito.getNumero(), remito);
            Bolsin bolsin = bolsines.get(fila.getInt("numero_bolsin"));
            if (bolsin != null) {
                bolsin.agregarRemito(remito);
            }
        });

        recorrer(conexion, "SELECT * FROM detalle_remito ORDER BY id", fila ->
                remitos.get(fila.getInt("numero_remito")).agregarDetalle(new DetalleRemito(
                        fila.getString("area_cmc_destino"),
                        documentaciones.get(fila.getInt("numero_documentacion")))));
    }

    // ---------------------------------------------------------------- utilidades JDBC

    private static void recorrer(Connection conexion, String sql, LectorDeFila lector) throws SQLException {
        try (Statement consulta = conexion.createStatement(); ResultSet fila = consulta.executeQuery(sql)) {
            while (fila.next()) {
                lector.leer(fila);
            }
        }
    }

    private static LocalDateTime fechaHora(String texto) {
        return texto == null ? null : LocalDateTime.parse(texto);
    }
}
