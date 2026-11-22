package bolsines.entity;

import java.time.LocalDateTime;

/**
 * Evento de la máquina de estados de Documentación que dispara cada opción de recepción.
 * Elegir el evento no es decidir por estado: qué hace (y si está permitido) lo resuelve el
 * estado actual de la Documentación (patrón State).
 */
@FunctionalInterface
interface EventoDeDocumentacion {

    void aplicar(Documentacion documentacion, LocalDateTime fechaHoraActual, Empleado responsable);
}
