package bolsines.control;

import bolsines.boundary.Email;
import bolsines.entity.Bolsin;
import bolsines.entity.ComisionMedica;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.entity.Remito;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arma el contenido de los emails del CU 29: uno por cada CM de origen de los remitos del
 * bolsín, con la situación de cada remito y de su documentación.
 */
final class ArmadorDeEmailDeRecepcion {

    List<Email> armar(Bolsin bolsin, String nombreCMDestino) {
        String asunto = "Recepción del bolsín " + bolsin.getNumeroBolsin() + " en " + nombreCMDestino;
        List<Email> emails = new ArrayList<>();
        for (Map.Entry<ComisionMedica, StringBuilder> entrada : cuerpoPorCMOrigen(bolsin).entrySet()) {
            emails.add(new Email(entrada.getKey().getEmail(), asunto, entrada.getValue().toString()));
        }
        return emails;
    }

    private Map<ComisionMedica, StringBuilder> cuerpoPorCMOrigen(Bolsin bolsin) {
        Map<ComisionMedica, StringBuilder> cuerpoPorCMOrigen = new LinkedHashMap<>();
        for (Remito remito : bolsin.getRemitos()) {
            StringBuilder cuerpo = cuerpoPorCMOrigen.computeIfAbsent(remito.getOrigen(), cm -> new StringBuilder());
            agregarRemito(cuerpo, remito);
        }
        return cuerpoPorCMOrigen;
    }

    private void agregarRemito(StringBuilder cuerpo, Remito remito) {
        cuerpo.append("  Remito ").append(remito.getNumero())
                .append(": ").append(remito.getEstadoActual().getNombre()).append('\n');
        for (DetalleRemito detalle : remito.getDetallesRemito()) {
            Documentacion documentacion = detalle.getDocumentacion();
            cuerpo.append("    Doc. ").append(documentacion.getNumero())
                    .append(" - ").append(documentacion.getAsunto())
                    .append(": ").append(documentacion.getEstadoActual().getNombre()).append('\n');
        }
    }
}
