package bolsines.persistencia;

import bolsines.entity.estado.bolsin.EstadoBolsin;
import bolsines.entity.estado.documentacion.EstadoDocumentacion;
import bolsines.entity.estado.remito.EstadoRemito;

import java.util.List;
import java.util.function.Function;

/**
 * PATRÓN STATE y persistencia: los estados concretos no tienen datos propios, así que en
 * la base se guarda solo su nombre (columna "estado"). Al leer, esta clase reconstruye el
 * objeto estado concreto que corresponde a ese nombre. Es el único lugar del sistema que
 * conoce la lista completa de estados concretos: agregar un estado es agregar su clase y
 * sumarla a la lista que corresponda.
 */
final class MapeoEstados {

    private static final List<EstadoBolsin> ESTADOS_BOLSIN = List.of(
            new bolsines.entity.estado.bolsin.Creado(),
            new bolsines.entity.estado.bolsin.Cerrado(),
            new bolsines.entity.estado.bolsin.Enviado(),
            new bolsines.entity.estado.bolsin.RecibidoEnCMDestino(),
            new bolsines.entity.estado.bolsin.DeBaja());

    private static final List<EstadoRemito> ESTADOS_REMITO = List.of(
            new bolsines.entity.estado.remito.Creado(),
            new bolsines.entity.estado.remito.EnBolsinSaliente(),
            new bolsines.entity.estado.remito.EnBolsinEnviado(),
            new bolsines.entity.estado.remito.RecibidoYAceptado(),
            new bolsines.entity.estado.remito.RecibidoYAceptadoParcial(),
            new bolsines.entity.estado.remito.Cancelado());

    private static final List<EstadoDocumentacion> ESTADOS_DOCUMENTACION = List.of(
            new bolsines.entity.estado.documentacion.Registrada(),
            new bolsines.entity.estado.documentacion.EnRemito(),
            new bolsines.entity.estado.documentacion.EnBolsinSaliente(),
            new bolsines.entity.estado.documentacion.EnBolsinEnviado(),
            new bolsines.entity.estado.documentacion.ParaRedirigir(),
            new bolsines.entity.estado.documentacion.RecibidaYAceptada(),
            new bolsines.entity.estado.documentacion.RecibidaYRechazada(),
            new bolsines.entity.estado.documentacion.NoRecibida(),
            new bolsines.entity.estado.documentacion.DeBaja());

    private MapeoEstados() {
    }

    static EstadoBolsin estadoBolsin(String nombre) {
        return buscarPorNombre(ESTADOS_BOLSIN, EstadoBolsin::getNombre, nombre, "bolsín");
    }

    static EstadoRemito estadoRemito(String nombre) {
        return buscarPorNombre(ESTADOS_REMITO, EstadoRemito::getNombre, nombre, "remito");
    }

    static EstadoDocumentacion estadoDocumentacion(String nombre) {
        return buscarPorNombre(ESTADOS_DOCUMENTACION, EstadoDocumentacion::getNombre, nombre, "documentación");
    }

    private static <E> E buscarPorNombre(List<E> estados, Function<E, String> nombreDe, String nombre,
                                         String entidad) {
        for (E estado : estados) {
            if (nombreDe.apply(estado).equals(nombre)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de " + entidad + " desconocido: " + nombre);
    }
}
