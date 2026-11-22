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
     * y documentación "En Bolsin Enviado". Los bolsines 2006 y 2007 son de control: no deben
     * aparecer en el listado de la CM Tucumán (otro destino y otro estado).
     */
    static final String DATOS_DE_PRUEBA = """
            INSERT INTO comision_medica VALUES
                ('TUC', 'Comisión Médica Tucumán', 'Av. Mate de Luna 320, San Miguel de Tucumán', 'cmtucuman@organismo.gob.ar', '0381-4310001', 0),
                ('SLA', 'Comisión Médica Salta', 'Av. Belgrano 710, Salta', 'cmsalta@organismo.gob.ar', '0387-4310002', 0),
                ('SFE', 'Comisión Médica Santa Fe', 'Bv. Gálvez 1520, Santa Fe', 'cmsantafe@organismo.gob.ar', '0342-4310003', 0),
                ('JUJ', 'Comisión Médica Jujuy', 'Av. Fascio 480, San Salvador de Jujuy', 'cmjujuy@organismo.gob.ar', '0388-4310004', 0),
                ('CMC', 'Comisión Médica Central', 'Av. Callao 1250, C.A.B.A.', 'cmcentral@organismo.gob.ar', '011-43810000', 1);
            INSERT INTO usuario VALUES
                ('cmendez', 'ppai2026'), ('hrojas', 'ppai2026'), ('sbenitez', 'ppai2026'), ('tcabrera', 'ppai2026'), ('vsuarez', 'ppai2026');
            INSERT INTO empleado VALUES
                ('carolina.mendez@organismo.gob.ar', 'Carolina', 'Méndez', 'TUC', 'cmendez'),
                ('hernan.rojas@organismo.gob.ar', 'Hernán', 'Rojas', 'CMC', 'hrojas'),
                ('sofia.benitez@organismo.gob.ar', 'Sofía', 'Benítez', 'SLA', 'sbenitez'),
                ('tomas.cabrera@organismo.gob.ar', 'Tomás', 'Cabrera', 'SFE', 'tcabrera'),
                ('valeria.suarez@organismo.gob.ar', 'Valeria', 'Suárez', 'JUJ', 'vsuarez');
            INSERT INTO tipo_documentacion VALUES
                ('Expediente', 'Expediente administrativo'),
                ('Dictamen', 'Dictamen médico'),
                ('Estudio Médico', 'Estudio médico complementario'),
                ('Carta Documento', 'Carta documento');
            INSERT INTO bolsin VALUES
                (2001, 731201, '2026-11-10', 2100, 'SLA', 'TUC', 'Enviado'),
                (2002, 731202, '2026-11-10', 1650, 'SFE', 'TUC', 'Enviado'),
                (2003, 731203, '2026-11-11', 2400, 'CMC', 'TUC', 'Enviado'),
                (2004, 731204, '2026-11-11', 1300, 'SLA', 'CMC', 'Enviado'),
                (2005, 731205, '2026-11-11', 1750, 'JUJ', 'CMC', 'Enviado'),
                (2006, 731206, '2026-11-11', 950, 'TUC', 'SLA', 'Enviado'),
                (2007, 731207, '2026-11-12', 2050, 'SFE', 'TUC', 'Cerrado');
            INSERT INTO remito VALUES
                (4001, '2026-11-09', 'SLA', 'TUC', 'En Bolsin Enviado', 2001),
                (4002, '2026-11-10', 'SLA', 'TUC', 'En Bolsin Enviado', 2001),
                (4003, '2026-11-10', 'SFE', 'TUC', 'En Bolsin Enviado', 2002),
                (4004, '2026-11-10', 'CMC', 'TUC', 'En Bolsin Enviado', 2003),
                (4005, '2026-11-11', 'CMC', 'TUC', 'En Bolsin Enviado', 2003),
                (4006, '2026-11-11', 'SLA', 'CMC', 'En Bolsin Enviado', 2004),
                (4007, '2026-11-11', 'JUJ', 'CMC', 'En Bolsin Enviado', 2005),
                (4008, '2026-11-11', 'TUC', 'SLA', 'En Bolsin Enviado', 2006),
                (4009, '2026-11-12', 'SFE', 'TUC', 'En Bolsin Saliente', 2007);
            INSERT INTO documentacion VALUES
                (6001, 'Expediente por accidente laboral - Aguirre, L.', '2026-11-09', 'Expediente', 'En Bolsin Enviado'),
                (6002, 'Dictamen de incapacidad - Navarro, G.', '2026-11-09', 'Dictamen', 'En Bolsin Enviado'),
                (6003, 'Estudio de resonancia - Ibarra, M.', '2026-11-10', 'Estudio Médico', 'En Bolsin Enviado'),
                (6004, 'Expediente por enfermedad profesional - Medina, V.', '2026-11-10', 'Expediente', 'En Bolsin Enviado'),
                (6005, 'Carta documento de citación - Medina, V.', '2026-11-10', 'Carta Documento', 'En Bolsin Enviado'),
                (6006, 'Dictamen de alta médica - Paz, H.', '2026-11-10', 'Dictamen', 'En Bolsin Enviado'),
                (6007, 'Dictamen de apelación - Quiroga, E.', '2026-11-10', 'Dictamen', 'En Bolsin Enviado'),
                (6008, 'Expediente devuelto con resolución - Bravo, D.', '2026-11-11', 'Expediente', 'En Bolsin Enviado'),
                (6009, 'Estudio de electromiograma - Bravo, D.', '2026-11-11', 'Estudio Médico', 'En Bolsin Enviado'),
                (6010, 'Expediente en apelación - Giménez, A.', '2026-11-11', 'Expediente', 'En Bolsin Enviado'),
                (6011, 'Dictamen jurisdiccional - Giménez, A.', '2026-11-11', 'Dictamen', 'En Bolsin Enviado'),
                (6012, 'Carta documento de reclamo - Roldán, S.', '2026-11-11', 'Carta Documento', 'En Bolsin Enviado'),
                (6013, 'Expediente para revisión - Sosa, J.', '2026-11-11', 'Expediente', 'En Bolsin Enviado'),
                (6014, 'Estudio de audiometría - Sosa, J.', '2026-11-11', 'Estudio Médico', 'En Bolsin Enviado'),
                (6015, 'Dictamen de incapacidad - Peralta, C.', '2026-11-11', 'Dictamen', 'En Bolsin Enviado'),
                (6016, 'Expediente por accidente in itinere - Domínguez, R.', '2026-11-12', 'Expediente', 'En Bolsin Saliente');
            INSERT INTO detalle_remito (numero_remito, area_cmc_destino, numero_documentacion) VALUES
                (4001, NULL, 6001), (4001, NULL, 6002), (4002, NULL, 6003),
                (4003, NULL, 6004), (4003, NULL, 6005), (4003, NULL, 6006),
                (4004, NULL, 6007), (4005, NULL, 6008), (4005, NULL, 6009),
                (4006, 'Asesoría Legal', 6010), (4006, 'Asesoría Legal', 6011), (4006, 'Mesa de Entradas', 6012),
                (4007, 'Auditoría Médica', 6013), (4007, 'Auditoría Médica', 6014),
                (4008, NULL, 6015), (4009, NULL, 6016);
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
