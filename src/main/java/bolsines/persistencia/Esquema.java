package bolsines.persistencia;

/** Sentencias SQL para crear las tablas y cargar los datos de prueba (separadas por ';'). */
final class Esquema {

    private Esquema() {
    }

    static final String TABLAS = """
            CREATE TABLE IF NOT EXISTS comision_medica (
                codigo TEXT PRIMARY KEY, nombre TEXT NOT NULL, direccion TEXT, email TEXT,
                telefono TEXT, es_central INTEGER NOT NULL);
            CREATE TABLE IF NOT EXISTS usuario (
                nombre TEXT PRIMARY KEY, contrasenia TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS empleado (
                email TEXT PRIMARY KEY, nombre TEXT NOT NULL, apellido TEXT NOT NULL,
                cm_codigo TEXT NOT NULL REFERENCES comision_medica(codigo),
                usuario_nombre TEXT NOT NULL REFERENCES usuario(nombre));
            CREATE TABLE IF NOT EXISTS tipo_documentacion (
                nombre TEXT PRIMARY KEY, descripcion TEXT);
            CREATE TABLE IF NOT EXISTS bolsin (
                numero_bolsin INTEGER PRIMARY KEY, numero_precinto INTEGER NOT NULL,
                fecha TEXT NOT NULL, peso INTEGER NOT NULL,
                cm_origen TEXT NOT NULL REFERENCES comision_medica(codigo),
                cm_destino TEXT NOT NULL REFERENCES comision_medica(codigo),
                estado TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS cambio_estado_bolsin (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numero_bolsin INTEGER NOT NULL REFERENCES bolsin(numero_bolsin),
                fecha_hora_inicio TEXT NOT NULL, fecha_hora_fin TEXT, estado TEXT NOT NULL,
                responsable_email TEXT NOT NULL REFERENCES empleado(email));
            CREATE TABLE IF NOT EXISTS remito (
                numero INTEGER PRIMARY KEY, fecha TEXT NOT NULL,
                cm_origen TEXT NOT NULL REFERENCES comision_medica(codigo),
                cm_destino TEXT NOT NULL REFERENCES comision_medica(codigo),
                estado TEXT NOT NULL,
                numero_bolsin INTEGER REFERENCES bolsin(numero_bolsin));
            CREATE TABLE IF NOT EXISTS documentacion (
                numero INTEGER PRIMARY KEY, asunto TEXT NOT NULL, fecha_pase TEXT NOT NULL,
                tipo TEXT NOT NULL REFERENCES tipo_documentacion(nombre), estado TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS detalle_remito (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numero_remito INTEGER NOT NULL REFERENCES remito(numero),
                area_cmc_destino TEXT,
                numero_documentacion INTEGER NOT NULL REFERENCES documentacion(numero));
            CREATE TABLE IF NOT EXISTS cambio_estado_documentacion (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numero_documentacion INTEGER NOT NULL REFERENCES documentacion(numero),
                fecha_hora_inicio TEXT NOT NULL, fecha_hora_fin TEXT, estado TEXT NOT NULL,
                responsable_email TEXT NOT NULL REFERENCES empleado(email))
            """;

    /**
     * Datos de prueba: el resultado de los casos de uso anteriores (7, 15, 19, 39 y 27).
     * Los bolsines ya fueron retirados por el correo, por eso están "Enviado" y sus remitos
     * y documentación "En Bolsin Enviado". Los bolsines 1006 y 1007 son de control: no deben
     * aparecer en el listado de la CM Córdoba (otro destino y otro estado).
     */
    static final String DATOS_DE_PRUEBA = """
            INSERT INTO comision_medica VALUES
                ('CBA', 'Comisión Médica Córdoba', 'Av. Colón 150, Córdoba', 'cmcordoba@organismo.gob.ar', '0351-4250001', 0),
                ('ROS', 'Comisión Médica Rosario', 'San Martín 980, Rosario', 'cmrosario@organismo.gob.ar', '0341-4250002', 0),
                ('VMA', 'Comisión Médica Villa María', 'Bv. España 45, Villa María', 'cmvillamaria@organismo.gob.ar', '0353-4250003', 0),
                ('MZA', 'Comisión Médica Mendoza', 'San Juan 1200, Mendoza', 'cmmendoza@organismo.gob.ar', '0261-4250004', 0),
                ('CMC', 'Comisión Médica Central', 'Av. Corrientes 800, C.A.B.A.', 'cmcentral@organismo.gob.ar', '011-43250000', 1);
            INSERT INTO usuario VALUES
                ('lfernandez', '1234'), ('dsosa', '1234'), ('mgomez', '1234'), ('jperez', '1234'), ('rdiaz', '1234');
            INSERT INTO empleado VALUES
                ('lucia.fernandez@organismo.gob.ar', 'Lucía', 'Fernández', 'CBA', 'lfernandez'),
                ('diego.sosa@organismo.gob.ar', 'Diego', 'Sosa', 'CMC', 'dsosa'),
                ('marta.gomez@organismo.gob.ar', 'Marta', 'Gómez', 'ROS', 'mgomez'),
                ('juan.perez@organismo.gob.ar', 'Juan', 'Pérez', 'VMA', 'jperez'),
                ('raul.diaz@organismo.gob.ar', 'Raúl', 'Díaz', 'MZA', 'rdiaz');
            INSERT INTO tipo_documentacion VALUES
                ('Expediente', 'Expediente administrativo'),
                ('Dictamen', 'Dictamen médico'),
                ('Estudio Médico', 'Estudio médico complementario'),
                ('Carta Documento', 'Carta documento');
            INSERT INTO bolsin VALUES
                (1001, 458101, '2026-11-10', 1850, 'ROS', 'CBA', 'Enviado'),
                (1002, 458102, '2026-11-10', 2300, 'VMA', 'CBA', 'Enviado'),
                (1003, 458103, '2026-11-11', 1200, 'CMC', 'CBA', 'Enviado'),
                (1004, 458104, '2026-11-11', 2750, 'ROS', 'CMC', 'Enviado'),
                (1005, 458105, '2026-11-11', 900, 'MZA', 'CMC', 'Enviado'),
                (1006, 458106, '2026-11-11', 1500, 'CBA', 'ROS', 'Enviado'),
                (1007, 458107, '2026-11-12', 1100, 'VMA', 'CBA', 'Cerrado');
            INSERT INTO remito VALUES
                (3001, '2026-11-09', 'ROS', 'CBA', 'En Bolsin Enviado', 1001),
                (3002, '2026-11-10', 'ROS', 'CBA', 'En Bolsin Enviado', 1001),
                (3003, '2026-11-10', 'VMA', 'CBA', 'En Bolsin Enviado', 1002),
                (3004, '2026-11-10', 'CMC', 'CBA', 'En Bolsin Enviado', 1003),
                (3005, '2026-11-11', 'CMC', 'CBA', 'En Bolsin Enviado', 1003),
                (3006, '2026-11-11', 'ROS', 'CMC', 'En Bolsin Enviado', 1004),
                (3007, '2026-11-11', 'MZA', 'CMC', 'En Bolsin Enviado', 1005),
                (3008, '2026-11-11', 'CBA', 'ROS', 'En Bolsin Enviado', 1006),
                (3009, '2026-11-12', 'VMA', 'CBA', 'En Bolsin Saliente', 1007);
            INSERT INTO documentacion VALUES
                (5001, 'Expediente por accidente laboral - Gómez, R.', '2026-11-09', 'Expediente', 'En Bolsin Enviado'),
                (5002, 'Dictamen de incapacidad - Pereyra, M.', '2026-11-09', 'Dictamen', 'En Bolsin Enviado'),
                (5003, 'Estudio de resonancia - López, A.', '2026-11-10', 'Estudio Médico', 'En Bolsin Enviado'),
                (5004, 'Expediente por enfermedad profesional - Ruiz, C.', '2026-11-10', 'Expediente', 'En Bolsin Enviado'),
                (5005, 'Carta documento de citación - Ruiz, C.', '2026-11-10', 'Carta Documento', 'En Bolsin Enviado'),
                (5006, 'Dictamen de alta médica - Torres, J.', '2026-11-10', 'Dictamen', 'En Bolsin Enviado'),
                (5007, 'Dictamen de apelación - Molina, S.', '2026-11-10', 'Dictamen', 'En Bolsin Enviado'),
                (5008, 'Expediente devuelto con resolución - Castro, P.', '2026-11-11', 'Expediente', 'En Bolsin Enviado'),
                (5009, 'Estudio de electromiograma - Castro, P.', '2026-11-11', 'Estudio Médico', 'En Bolsin Enviado'),
                (5010, 'Expediente en apelación - Vega, L.', '2026-11-11', 'Expediente', 'En Bolsin Enviado'),
                (5011, 'Dictamen jurisdiccional - Vega, L.', '2026-11-11', 'Dictamen', 'En Bolsin Enviado'),
                (5012, 'Carta documento de reclamo - Acosta, N.', '2026-11-11', 'Carta Documento', 'En Bolsin Enviado'),
                (5013, 'Expediente para revisión - Herrera, D.', '2026-11-11', 'Expediente', 'En Bolsin Enviado'),
                (5014, 'Estudio de audiometría - Herrera, D.', '2026-11-11', 'Estudio Médico', 'En Bolsin Enviado'),
                (5015, 'Dictamen de incapacidad - Luna, F.', '2026-11-11', 'Dictamen', 'En Bolsin Enviado'),
                (5016, 'Expediente por accidente in itinere - Ríos, E.', '2026-11-12', 'Expediente', 'En Bolsin Saliente');
            INSERT INTO detalle_remito (numero_remito, area_cmc_destino, numero_documentacion) VALUES
                (3001, NULL, 5001), (3001, NULL, 5002), (3002, NULL, 5003),
                (3003, NULL, 5004), (3003, NULL, 5005), (3003, NULL, 5006),
                (3004, NULL, 5007), (3005, NULL, 5008), (3005, NULL, 5009),
                (3006, 'Asesoría Legal', 5010), (3006, 'Asesoría Legal', 5011), (3006, 'Mesa de Entradas', 5012),
                (3007, 'Auditoría Médica', 5013), (3007, 'Auditoría Médica', 5014),
                (3008, NULL, 5015), (3009, NULL, 5016);
            INSERT INTO cambio_estado_bolsin (numero_bolsin, fecha_hora_inicio, estado, responsable_email)
                SELECT b.numero_bolsin, '2026-11-12T08:30:00', b.estado, e.email
                FROM bolsin b JOIN empleado e ON e.cm_codigo = b.cm_origen;
            INSERT INTO cambio_estado_documentacion (numero_documentacion, fecha_hora_inicio, estado, responsable_email)
                SELECT d.numero, '2026-11-12T08:30:00', d.estado, e.email
                FROM documentacion d
                JOIN detalle_remito dr ON dr.numero_documentacion = d.numero
                JOIN remito r ON r.numero = dr.numero_remito
                JOIN empleado e ON e.cm_codigo = r.cm_origen
            """;
}
