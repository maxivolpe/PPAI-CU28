package bolsines.soporte;

import bolsines.boundary.PantallaRecepcion;
import bolsines.boundary.ResumenDeBolsin;
import bolsines.entity.ComisionMedica;
import bolsines.entity.DatosDeDocumentacion;
import bolsines.entity.OpcionRecepcion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pantalla de prueba: no muestra nada, solo anota lo que el gestor le pidió. */
public class PantallaRecepcionEspia implements PantallaRecepcion {

    public final List<String> mensajes = new ArrayList<>();
    public List<ResumenDeBolsin> bolsinesOfrecidos = List.of();
    public ComisionMedica cmMostrada;
    public ResumenDeBolsin bolsinMostrado;
    public Map<Integer, List<DatosDeDocumentacion>> informacionMostrada = Map.of();
    public int vecesQueSePidioLaOpcion;
    public ComisionMedica cmParaLaOpcion;
    public boolean confirmacionSolicitada;
    public String exito;

    @Override
    public void mostrarCM(String nombreCMEmpleado) {
        mensajes.add("CM: " + nombreCMEmpleado);
    }

    @Override
    public void mostrarMensaje(String mensaje) {
        mensajes.add(mensaje);
    }

    @Override
    public void solicitarSeleccionBolsin(List<ResumenDeBolsin> bolsinesEnviados) {
        bolsinesOfrecidos = bolsinesEnviados;
    }

    @Override
    public void mostrarInfoBolsin(ResumenDeBolsin bolsin, Map<Integer, List<DatosDeDocumentacion>> informacionBolsin) {
        bolsinMostrado = bolsin;
        informacionMostrada = informacionBolsin;
    }

    @Override
    public void solicitarOpcRecepcion(ComisionMedica comisionMedica) {
        vecesQueSePidioLaOpcion++;
        cmParaLaOpcion = comisionMedica;
    }

    @Override
    public void solicitarConfirmacion(OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        confirmacionSolicitada = true;
    }

    @Override
    public void mostrarMensajeExito(int numeroBolsin, String estadoBolsin) {
        exito = numeroBolsin + ": " + estadoBolsin;
    }
}
