package bolsines.control;

import bolsines.entity.Bolsin;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.CambioEstadoDocumentacion;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.entity.Empleado;
import bolsines.entity.Remito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Consulta de apoyo (NO es parte del CU 28): lee todo de la base de datos y devuelve cómo
 * quedaron guardados los estados y sus historiales, con fecha y responsable. La consola y la
 * web solo dibujan este resultado.
 */
public class ConsultarEstadosGuardados {

    /** Un paso del historial de estados; {@code fin} es null mientras el estado está vigente. */
    public record CambioGuardado(LocalDateTime inicio, LocalDateTime fin, String estado, String responsable) {
    }

    public record DocumentacionGuardada(int numero, String asunto, String tipo, String estado,
                                        List<CambioGuardado> cambios) {
    }

    public record RemitoGuardado(int numero, String estado, List<DocumentacionGuardada> documentacion) {
    }

    public record BolsinGuardado(int numero, String cmOrigen, String cmDestino, int precinto, String estado,
                                 List<CambioGuardado> cambios, List<RemitoGuardado> remitos) {
    }

    private final RepositorioDeBolsines repositorioDeBolsines;

    public ConsultarEstadosGuardados(RepositorioDeBolsines repositorioDeBolsines) {
        this.repositorioDeBolsines = repositorioDeBolsines;
    }

    public List<BolsinGuardado> consultar() {
        List<BolsinGuardado> bolsines = new ArrayList<>();
        for (Bolsin bolsin : repositorioDeBolsines.buscarBolsines()) {
            bolsines.add(new BolsinGuardado(bolsin.getNumeroBolsin(), bolsin.getCMOrigen(),
                    bolsin.getDestino().getNombre(), bolsin.getNumPrecinto(), bolsin.getEstadoActual().getNombre(),
                    cambiosDelBolsin(bolsin), remitosDelBolsin(bolsin)));
        }
        return bolsines;
    }

    private List<CambioGuardado> cambiosDelBolsin(Bolsin bolsin) {
        List<CambioGuardado> cambios = new ArrayList<>();
        for (CambioEstadoBolsin cambio : bolsin.getCambioEstados()) {
            cambios.add(new CambioGuardado(cambio.getFechaHoraInicio(), cambio.getFechaHoraFin(),
                    cambio.getEstado().getNombre(), nombreCompleto(cambio.getResponsable())));
        }
        return cambios;
    }

    private List<RemitoGuardado> remitosDelBolsin(Bolsin bolsin) {
        List<RemitoGuardado> remitos = new ArrayList<>();
        for (Remito remito : bolsin.getRemitos()) {
            List<DocumentacionGuardada> documentaciones = new ArrayList<>();
            for (DetalleRemito detalle : remito.getDetallesRemito()) {
                documentaciones.add(documentacionGuardada(detalle.getDocumentacion()));
            }
            remitos.add(new RemitoGuardado(remito.getNumero(), remito.getEstadoActual().getNombre(), documentaciones));
        }
        return remitos;
    }

    private DocumentacionGuardada documentacionGuardada(Documentacion documentacion) {
        List<CambioGuardado> cambios = new ArrayList<>();
        for (CambioEstadoDocumentacion cambio : documentacion.getCambioEstadoDocumentacion()) {
            cambios.add(new CambioGuardado(cambio.getFechaHoraInicio(), cambio.getFechaHoraFin(),
                    cambio.getEstado().getNombre(), nombreCompleto(cambio.getResponsable())));
        }
        return new DocumentacionGuardada(documentacion.getNumero(), documentacion.getAsunto(),
                documentacion.getTipoDocumentacion(), documentacion.getEstadoActual().getNombre(), cambios);
    }

    private String nombreCompleto(Empleado empleado) {
        return empleado.getNombre() + " " + empleado.getApellido();
    }
}
