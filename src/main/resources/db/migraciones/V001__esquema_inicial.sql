-- Esquema inicial: una tabla por clase del modelo de dominio que usa el CU 28.
-- Del patrón State solo se guarda el NOMBRE del estado actual (columna "estado");
-- el historial de cambios de estado de Bolsín y de Documentación tiene su propia tabla.
-- "IF NOT EXISTS" permite aplicar este script sobre una base creada por versiones anteriores.

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
    responsable_email TEXT NOT NULL REFERENCES empleado(email));
