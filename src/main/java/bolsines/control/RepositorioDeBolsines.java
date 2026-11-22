package bolsines.control;

import bolsines.entity.Bolsin;

import java.util.List;

/**
 * Lo que el gestor necesita de la persistencia sobre los bolsines. El gestor depende de esta
 * interfaz y no de SQLite; la implementación es {@code persistencia.BaseDeDatos}. Los tests
 * pueden usar una versión en memoria.
 */
public interface RepositorioDeBolsines {

    /**
     * Todos los bolsines con sus remitos, documentación e historiales de estado. Es el gestor
     * el que elige los que corresponden con esTuCMDestino() y sosEnviado(), como en el
     * diagrama de secuencia de análisis.
     */
    List<Bolsin> buscarBolsines();

    /**
     * Guarda, todo junto o nada, el estado y el historial del bolsín, el estado de sus remitos
     * y el estado y el historial de su documentación.
     *
     * @throws bolsines.entity.BolsinModificadoPorOtroUsuarioException si otro usuario cambió el
     *         estado del bolsín desde que se leyó.
     */
    void guardarRecepcion(Bolsin bolsin);
}
