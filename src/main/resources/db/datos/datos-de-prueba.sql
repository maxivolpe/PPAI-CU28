-- Datos de prueba: el resultado de los casos de uso anteriores (7, 15, 19, 39 y 27).
-- Los bolsines ya fueron retirados por el correo, por eso están "Enviado" y sus remitos y
-- documentación "En Bolsin Enviado". El bolsín 2007 es de control: está "Cerrado" y no debe
-- aparecer en ningún listado. Se cargan solo si la base está vacía.
-- Todos los usuarios tienen la contraseña ppai2026 (el inicio de sesión es simulado: CU 5 fuera de alcance).

INSERT INTO comision_medica VALUES
    ('CBA', 'Comisión Médica Córdoba', 'Av. Colón 150, Córdoba', 'cmcordoba@organismo.gob.ar', '0351-4250001', 0),
    ('ROS', 'Comisión Médica Rosario', 'San Martín 980, Rosario', 'cmrosario@organismo.gob.ar', '0341-4250002', 0),
    ('VMA', 'Comisión Médica Villa María', 'Bv. España 45, Villa María', 'cmvillamaria@organismo.gob.ar', '0353-4250003', 0),
    ('MZA', 'Comisión Médica Mendoza', 'San Juan 1200, Mendoza', 'cmmendoza@organismo.gob.ar', '0261-4250004', 0),
    ('CMC', 'Comisión Médica Central', 'Av. Corrientes 800, C.A.B.A.', 'cmcentral@organismo.gob.ar', '011-43250000', 1);

INSERT INTO usuario VALUES
    ('cmendez', 'ppai2026'), ('hrojas', 'ppai2026'), ('sbenitez', 'ppai2026'), ('tcabrera', 'ppai2026'), ('vsuarez', 'ppai2026');

INSERT INTO empleado VALUES
    ('carolina.mendez@organismo.gob.ar', 'Carolina', 'Méndez', 'CBA', 'cmendez'),
    ('hernan.rojas@organismo.gob.ar', 'Hernán', 'Rojas', 'CMC', 'hrojas'),
    ('sofia.benitez@organismo.gob.ar', 'Sofía', 'Benítez', 'ROS', 'sbenitez'),
    ('tomas.cabrera@organismo.gob.ar', 'Tomás', 'Cabrera', 'VMA', 'tcabrera'),
    ('valeria.suarez@organismo.gob.ar', 'Valeria', 'Suárez', 'MZA', 'vsuarez');

INSERT INTO tipo_documentacion VALUES
    ('Expediente', 'Expediente administrativo'),
    ('Dictamen', 'Dictamen médico'),
    ('Estudio Médico', 'Estudio médico complementario'),
    ('Carta Documento', 'Carta documento');

INSERT INTO bolsin VALUES
    (2001, 731201, '2026-11-10', 2100, 'ROS', 'CBA', 'Enviado'),
    (2002, 731202, '2026-11-10', 1650, 'VMA', 'CBA', 'Enviado'),
    (2003, 731203, '2026-11-11', 2400, 'CMC', 'CBA', 'Enviado'),
    (2004, 731204, '2026-11-11', 1300, 'ROS', 'CMC', 'Enviado'),
    (2005, 731205, '2026-11-11', 1750, 'MZA', 'CMC', 'Enviado'),
    (2006, 731206, '2026-11-11', 950, 'CBA', 'ROS', 'Enviado'),
    (2007, 731207, '2026-11-12', 2050, 'VMA', 'CBA', 'Cerrado');

INSERT INTO remito VALUES
    (4001, '2026-11-09', 'ROS', 'CBA', 'En Bolsin Enviado', 2001),
    (4002, '2026-11-10', 'ROS', 'CBA', 'En Bolsin Enviado', 2001),
    (4003, '2026-11-10', 'VMA', 'CBA', 'En Bolsin Enviado', 2002),
    (4004, '2026-11-10', 'CMC', 'CBA', 'En Bolsin Enviado', 2003),
    (4005, '2026-11-11', 'CMC', 'CBA', 'En Bolsin Enviado', 2003),
    (4006, '2026-11-11', 'ROS', 'CMC', 'En Bolsin Enviado', 2004),
    (4007, '2026-11-11', 'MZA', 'CMC', 'En Bolsin Enviado', 2005),
    (4008, '2026-11-11', 'CBA', 'ROS', 'En Bolsin Enviado', 2006),
    (4009, '2026-11-12', 'VMA', 'CBA', 'En Bolsin Saliente', 2007);

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

-- El cambio de estado vigente de cada bolsín y de cada documentación, con un responsable de la CM de origen.
INSERT INTO cambio_estado_bolsin (numero_bolsin, fecha_hora_inicio, estado, responsable_email)
    SELECT b.numero_bolsin, '2026-11-12T08:30:00', b.estado, e.email
    FROM bolsin b JOIN empleado e ON e.cm_codigo = b.cm_origen;

INSERT INTO cambio_estado_documentacion (numero_documentacion, fecha_hora_inicio, estado, responsable_email)
    SELECT d.numero, '2026-11-12T08:30:00', d.estado, e.email
    FROM documentacion d
    JOIN detalle_remito dr ON dr.numero_documentacion = d.numero
    JOIN remito r ON r.numero = dr.numero_remito
    JOIN empleado e ON e.cm_codigo = r.cm_origen;
