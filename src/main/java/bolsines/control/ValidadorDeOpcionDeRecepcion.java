package bolsines.control;

import bolsines.entity.Bolsin;
import bolsines.entity.ComisionMedica;
import bolsines.entity.DetalleRemito;
import bolsines.entity.OpcionDeRecepcionInvalidaException;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.Remito;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reglas que debe cumplir la opción de recepción elegida por el EB (paso 8) antes de pedir
 * la confirmación. Si alguna falla lanza {@link OpcionDeRecepcionInvalidaException} y no se
 * cambia nada.
 */
final class ValidadorDeOpcionDeRecepcion {

    private final ComisionMedica comisionDestino;
    private final Bolsin bolsin;

    ValidadorDeOpcionDeRecepcion(ComisionMedica comisionDestino, Bolsin bolsin) {
        this.comisionDestino = comisionDestino;
        this.bolsin = bolsin;
    }

    /**
     * @param documentacionMarcada números de la documentación no recibida (opción 2), que no
     *                             corresponde (opción 3) o a redirigir (opción 4). Vacío en la opción 1.
     */
    void validar(OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        if (!opcion.requiereSeleccionDeDocumentacion()) {
            exigirSinDocumentacionMarcada(documentacionMarcada);
            return;
        }
        exigirOpcionDisponible(opcion);
        exigirAlMenosUnaDocumentacion(documentacionMarcada);
        exigirQuePertenezcanAlBolsin(documentacionMarcada);
    }

    private void exigirSinDocumentacionMarcada(Set<Integer> documentacionMarcada) {
        if (!documentacionMarcada.isEmpty()) {
            throw new OpcionDeRecepcionInvalidaException("La opción 1 no lleva documentación seleccionada.");
        }
    }

    private void exigirOpcionDisponible(OpcionRecepcion opcion) {
        if (!opcion.estaDisponiblePara(comisionDestino)) {
            throw new OpcionDeRecepcionInvalidaException(
                    "La opción " + opcion.getNumero()
                            + " solo aplica cuando la CM destino es la Comisión Médica Central.");
        }
    }

    private void exigirAlMenosUnaDocumentacion(Set<Integer> documentacionMarcada) {
        if (documentacionMarcada.isEmpty()) {
            throw new OpcionDeRecepcionInvalidaException("Debe indicar al menos una documentación para esta opción.");
        }
    }

    private void exigirQuePertenezcanAlBolsin(Set<Integer> documentacionMarcada) {
        Set<Integer> numerosDelBolsin = numerosDeDocumentacionDelBolsin();
        for (Integer numero : documentacionMarcada) {
            if (!numerosDelBolsin.contains(numero)) {
                throw new OpcionDeRecepcionInvalidaException("La documentación " + numero + " no pertenece al bolsín.");
            }
        }
    }

    private Set<Integer> numerosDeDocumentacionDelBolsin() {
        Set<Integer> numeros = new LinkedHashSet<>();
        for (Remito remito : bolsin.getRemitos()) {
            for (DetalleRemito detalle : remito.getDetallesRemito()) {
                numeros.add(detalle.getDocumentacion().getNumero());
            }
        }
        return numeros;
    }
}
