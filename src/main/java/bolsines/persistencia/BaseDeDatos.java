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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
 * Persistencia en SQLite con JDBC puro (sin framework): lee las tablas y arma los objetos
 * del dominio, y guarda el resultado de registrar la recepción de un bolsín.
 *
 * Del patrón State solo se persiste el NOMBRE del estado actual; el objeto estado concreto
 * se reconstruye al leer con MapeoEstados.
 */
public class BaseDeDatos {

    private final String url;

    /** @param rutaArchivo archivo de la base, por ejemplo "data/bolsines.db". Se crea si no existe. */
    public BaseDeDatos(String rutaArchivo) {
        File carpeta = new File(rutaArchivo).getAbsoluteFile().getParentFile();
        if (carpeta != null) {
            carpeta.mkdirs();
        }
        this.url = "jdbc:sqlite:" + rutaArchivo;
    }

    /** Crea las tablas si no existen y carga los datos de prueba si la base está vacía. */
    public void inicializar() {
        try (Connection conexion = abrirConexion()) {
            ejecutarSentencias(conexion, Esquema.TABLAS);
            try (Statement consulta = conexion.createStatement();
                 ResultSet filas = consulta.executeQuery("SELECT COUNT(*) FROM comision_medica")) {
                filas.next();
                if (filas.getInt(1) > 0) {
                    return;
                }
            }
            conexion.setAutoCommit(false);
            ejecutarSentencias(conexion, Esquema.DATOS_DE_PRUEBA);
            conexion.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo inicializar la base de datos.", e);
        }
    }

    // ---------------------------------------------------------------- lectura

    public List<Empleado> buscarEmpleados() {
        try (Connection conexion = abrirConexion()) {
            return new ArrayList<>(leerEmpleados(conexion, leerComisionesMedicas(conexion)).values());
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron leer los empleados.", e);
        }
    }

    /** Devuelve todos los bolsines con sus remitos, detalles, documentación e historiales. */
    public List<Bolsin> buscarBolsines() {
        try (Connection conexion = abrirConexion()) {
            Map<String, ComisionMedica> comisiones = leerComisionesMedicas(conexion);
            Map<String, Empleado> empleados = leerEmpleados(conexion, comisiones);
            Map<Integer, Documentacion> documentaciones = leerDocumentaciones(conexion, empleados);
            Map<Integer, Bolsin> bolsines = leerBolsines(conexion, comisiones, empleados);
            leerRemitos(conexion, comisiones, documentaciones, bolsines);
            return new ArrayList<>(bolsines.values());
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron leer los bolsines.", e);
        }
    }

    private Map<String, ComisionMedica> leerComisionesMedicas(Connection conexion) throws SQLException {
        Map<String, ComisionMedica> comisiones = new HashMap<>();
        try (Statement consulta = conexion.createStatement();
             ResultSet fila = consulta.executeQuery("SELECT * FROM comision_medica")) {
            while (fila.next()) {
                comisiones.put(fila.getString("codigo"), new ComisionMedica(
                        fila.getString("codigo"), fila.getString("nombre"), fila.getString("direccion"),
                        fila.getString("email"), fila.getString("telefono"), fila.getInt("es_central") == 1));
            }
        }
        return comisiones;
    }

    private Map<String, Empleado> leerEmpleados(Connection conexion, Map<String, ComisionMedica> comisiones)
            throws SQLException {
        Map<String, Empleado> empleados = new LinkedHashMap<>();
        String sql = "SELECT e.*, u.contrasenia FROM empleado e JOIN usuario u ON u.nombre = e.usuario_nombre "
                + "ORDER BY e.usuario_nombre";
        try (Statement consulta = conexion.createStatement(); ResultSet fila = consulta.executeQuery(sql)) {
            while (fila.next()) {
                Usuario usuario = new Usuario(fila.getString("usuario_nombre"), fila.getString("contrasenia"));
                empleados.put(fila.getString("email"), new Empleado(
                        fila.getString("nombre"), fila.getString("apellido"), fila.getString("email"),
                        comisiones.get(fila.getString("cm_codigo")), usuario));
            }
        }
        return empleados;
    }

    private Map<Integer, Documentacion> leerDocumentaciones(Connection conexion, Map<String, Empleado> empleados)
            throws SQLException {
        Map<String, TipoDocumentacion> tipos = new HashMap<>();
        try (Statement consulta = conexion.createStatement();
             ResultSet fila = consulta.executeQuery("SELECT * FROM tipo_documentacion")) {
            while (fila.next()) {
                tipos.put(fila.getString("nombre"),
                        new TipoDocumentacion(fila.getString("nombre"), fila.getString("descripcion")));
            }
        }

        Map<Integer, Documentacion> documentaciones = new HashMap<>();
        try (Statement consulta = conexion.createStatement();
             ResultSet fila = consulta.executeQuery("SELECT * FROM documentacion")) {
            while (fila.next()) {
                documentaciones.put(fila.getInt("numero"), new Documentacion(
                        fila.getInt("numero"), fila.getString("asunto"),
                        LocalDate.parse(fila.getString("fecha_pase")), tipos.get(fila.getString("tipo")),
                        MapeoEstados.estadoDocumentacion(fila.getString("estado"))));
            }
        }

        try (Statement consulta = conexion.createStatement();
             ResultSet fila = consulta.executeQuery("SELECT * FROM cambio_estado_documentacion ORDER BY id")) {
            while (fila.next()) {
                CambioEstadoDocumentacion cambio = new CambioEstadoDocumentacion(
                        LocalDateTime.parse(fila.getString("fecha_hora_inicio")),
                        empleados.get(fila.getString("responsable_email")),
                        MapeoEstados.estadoDocumentacion(fila.getString("estado")));
                cambio.setFechaHoraFin(leerFechaHora(fila.getString("fecha_hora_fin")));
                documentaciones.get(fila.getInt("numero_documentacion")).agregarCambioEstado(cambio);
            }
        }
        return documentaciones;
    }

    private Map<Integer, Bolsin> leerBolsines(Connection conexion, Map<String, ComisionMedica> comisiones,
                                              Map<String, Empleado> empleados) throws SQLException {
        Map<Integer, Bolsin> bolsines = new LinkedHashMap<>();
        try (Statement consulta = conexion.createStatement();
             ResultSet fila = consulta.executeQuery("SELECT * FROM bolsin ORDER BY numero_bolsin")) {
            while (fila.next()) {
                bolsines.put(fila.getInt("numero_bolsin"), new Bolsin(
                        fila.getInt("numero_bolsin"), fila.getInt("numero_precinto"),
                        LocalDate.parse(fila.getString("fecha")), fila.getInt("peso"),
                        comisiones.get(fila.getString("cm_origen")), comisiones.get(fila.getString("cm_destino")),
                        MapeoEstados.estadoBolsin(fila.getString("estado"))));
            }
        }

        try (Statement consulta = conexion.createStatement();
             ResultSet fila = consulta.executeQuery("SELECT * FROM cambio_estado_bolsin ORDER BY id")) {
            while (fila.next()) {
                CambioEstadoBolsin cambio = new CambioEstadoBolsin(
                        LocalDateTime.parse(fila.getString("fecha_hora_inicio")),
                        empleados.get(fila.getString("responsable_email")),
                        MapeoEstados.estadoBolsin(fila.getString("estado")));
                cambio.setFechaHoraFin(leerFechaHora(fila.getString("fecha_hora_fin")));
                bolsines.get(fila.getInt("numero_bolsin")).agregarCambioEstado(cambio);
            }
        }
        return bolsines;
    }

    private void leerRemitos(Connection conexion, Map<String, ComisionMedica> comisiones,
                             Map<Integer, Documentacion> documentaciones, Map<Integer, Bolsin> bolsines)
            throws SQLException {
        Map<Integer, Remito> remitos = new HashMap<>();
        try (Statement consulta = conexion.createStatement();
             ResultSet fila = consulta.executeQuery("SELECT * FROM remito ORDER BY numero")) {
            while (fila.next()) {
                Remito remito = new Remito(
                        fila.getInt("numero"), LocalDate.parse(fila.getString("fecha")),
                        comisiones.get(fila.getString("cm_origen")), comisiones.get(fila.getString("cm_destino")),
                        MapeoEstados.estadoRemito(fila.getString("estado")));
                remitos.put(remito.getNumero(), remito);
                Bolsin bolsin = bolsines.get(fila.getInt("numero_bolsin"));
                if (bolsin != null) {
                    bolsin.agregarRemito(remito);
                }
            }
        }

        try (Statement consulta = conexion.createStatement();
             ResultSet fila = consulta.executeQuery("SELECT * FROM detalle_remito ORDER BY id")) {
            while (fila.next()) {
                remitos.get(fila.getInt("numero_remito")).agregarDetalle(new DetalleRemito(
                        fila.getString("area_cmc_destino"),
                        documentaciones.get(fila.getInt("numero_documentacion"))));
            }
        }
    }

    // ---------------------------------------------------------------- escritura

    /**
     * Guarda en UNA transacción el resultado de la recepción: estado e historial del bolsín,
     * estado de cada remito, y estado e historial de cada documentación. Si algo falla no
     * queda nada a medias.
     *
     * El historial no se reescribe: en la base se hace lo mismo que hizo el patrón State en
     * memoria, se cierra el cambio de estado vigente y se agrega el nuevo.
     */
    public void guardarRecepcion(Bolsin bolsin) {
        try (Connection conexion = abrirConexion()) {
            conexion.setAutoCommit(false);
            try {
                actualizarEstadoDelBolsin(conexion, bolsin);
                registrarNuevoCambioDelBolsin(conexion, bolsin);
                for (Remito remito : bolsin.getRemitos()) {
                    actualizar(conexion, "UPDATE remito SET estado = ? WHERE numero = ?",
                            remito.getEstadoActual().getNombre(), remito.getNumero());
                    for (DetalleRemito detalle : remito.getDetallesRemito()) {
                        guardarDocumentacion(conexion, detalle.getDocumentacion());
                    }
                }
                conexion.commit();
            } catch (SQLException | RuntimeException e) {
                conexion.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la recepción del bolsín.", e);
        }
    }

    /**
     * Cambia el estado del bolsín solo si en la base sigue en el estado en que se leyó. Si otro
     * usuario lo recibió mientras tanto, la recepción se rechaza en lugar de pisar la suya.
     */
    private void actualizarEstadoDelBolsin(Connection conexion, Bolsin bolsin) throws SQLException {
        int filasActualizadas = actualizar(conexion,
                "UPDATE bolsin SET estado = ? WHERE numero_bolsin = ? AND estado = ?",
                bolsin.getEstadoActual().getNombre(), bolsin.getNumeroBolsin(), estadoAlLeerlo(bolsin));
        if (filasActualizadas != 1) {
            throw new IllegalStateException("El bolsín " + bolsin.getNumeroBolsin()
                    + " fue modificado por otro usuario mientras se registraba la recepción. "
                    + "Vuelva a iniciar el caso de uso.");
        }
    }

    /** El estado en que estaba el bolsín al leerlo de la base: el del último cambio de estado ya cerrado. */
    private String estadoAlLeerlo(Bolsin bolsin) {
        List<CambioEstadoBolsin> historial = bolsin.getCambioEstados();
        for (int i = historial.size() - 1; i >= 0; i--) {
            if (!historial.get(i).sosActual()) {
                return historial.get(i).getEstado().getNombre();
            }
        }
        return bolsin.getEstadoActual().getNombre();
    }

    /** Cierra en la base el cambio de estado vigente del bolsín y agrega el que creó el estado Enviado. */
    private void registrarNuevoCambioDelBolsin(Connection conexion, Bolsin bolsin) throws SQLException {
        CambioEstadoBolsin nuevo = bolsin.buscarCambioEstadoActual();
        actualizar(conexion, "UPDATE cambio_estado_bolsin SET fecha_hora_fin = ? "
                        + "WHERE numero_bolsin = ? AND fecha_hora_fin IS NULL",
                texto(nuevo.getFechaHoraInicio()), bolsin.getNumeroBolsin());
        actualizar(conexion, "INSERT INTO cambio_estado_bolsin "
                        + "(numero_bolsin, fecha_hora_inicio, fecha_hora_fin, estado, responsable_email) "
                        + "VALUES (?, ?, NULL, ?, ?)",
                bolsin.getNumeroBolsin(), texto(nuevo.getFechaHoraInicio()), nuevo.getEstado().getNombre(),
                nuevo.getResponsable().getEmail());
    }

    /** Estado de la documentación; en su historial, se cierra el vigente y se agrega el nuevo. */
    private void guardarDocumentacion(Connection conexion, Documentacion documentacion) throws SQLException {
        actualizar(conexion, "UPDATE documentacion SET estado = ? WHERE numero = ?",
                documentacion.getEstadoActual().getNombre(), documentacion.getNumero());
        CambioEstadoDocumentacion nuevo = cambioVigente(documentacion);
        actualizar(conexion, "UPDATE cambio_estado_documentacion SET fecha_hora_fin = ? "
                        + "WHERE numero_documentacion = ? AND fecha_hora_fin IS NULL",
                texto(nuevo.getFechaHoraInicio()), documentacion.getNumero());
        actualizar(conexion, "INSERT INTO cambio_estado_documentacion "
                        + "(numero_documentacion, fecha_hora_inicio, fecha_hora_fin, estado, responsable_email) "
                        + "VALUES (?, ?, NULL, ?, ?)",
                documentacion.getNumero(), texto(nuevo.getFechaHoraInicio()), nuevo.getEstado().getNombre(),
                nuevo.getResponsable().getEmail());
    }

    /** El cambio de estado que el patrón State acaba de agregar: el único sin fecha y hora de fin. */
    private CambioEstadoDocumentacion cambioVigente(Documentacion documentacion) {
        for (CambioEstadoDocumentacion cambio : documentacion.getCambioEstadoDocumentacion()) {
            if (cambio.sosActual()) {
                return cambio;
            }
        }
        throw new IllegalStateException(
                "La documentación " + documentacion.getNumero() + " no tiene un cambio de estado vigente.");
    }

    // ---------------------------------------------------------------- utilidades JDBC

    private Connection abrirConexion() throws SQLException {
        Connection conexion = DriverManager.getConnection(url);
        try (Statement sentencia = conexion.createStatement()) {
            // SQLite solo controla las REFERENCES del esquema si se lo pide en cada conexión.
            sentencia.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            conexion.close();
            throw e;
        }
        return conexion;
    }

    private void ejecutarSentencias(Connection conexion, String sentencias) throws SQLException {
        try (Statement sentencia = conexion.createStatement()) {
            for (String sql : sentencias.split(";")) {
                if (!sql.isBlank()) {
                    sentencia.executeUpdate(sql);
                }
            }
        }
    }

    /** @return la cantidad de filas modificadas. */
    private int actualizar(Connection conexion, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                sentencia.setObject(i + 1, parametros[i]);
            }
            return sentencia.executeUpdate();
        }
    }

    private String texto(LocalDateTime fechaHora) {
        return fechaHora == null ? null : fechaHora.toString();
    }

    private LocalDateTime leerFechaHora(String texto) {
        return texto == null ? null : LocalDateTime.parse(texto);
    }
}
