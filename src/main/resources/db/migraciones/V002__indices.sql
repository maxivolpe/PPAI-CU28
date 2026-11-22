-- Índices para armar un bolsín con sus remitos y detalles, y para encontrar el cambio de
-- estado vigente de un bolsín o una documentación, sin recorrer las tablas completas.
CREATE INDEX IF NOT EXISTS ix_remito_bolsin ON remito (numero_bolsin);

CREATE INDEX IF NOT EXISTS ix_detalle_remito_remito ON detalle_remito (numero_remito);

CREATE INDEX IF NOT EXISTS ix_cambio_estado_bolsin ON cambio_estado_bolsin (numero_bolsin);

CREATE INDEX IF NOT EXISTS ix_cambio_estado_documentacion ON cambio_estado_documentacion (numero_documentacion);
