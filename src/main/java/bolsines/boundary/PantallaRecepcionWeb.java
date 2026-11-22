package bolsines.boundary;

import bolsines.entity.ComisionMedica;
import bolsines.entity.DatosDeDocumentacion;
import bolsines.entity.OpcionRecepcion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Clase frontera del CU 28, en versión web. Cada mensaje del gestor guarda en "vista" lo
 * que hay que mostrar; el ServidorWeb le envía esa vista al navegador (como JSON) y la
 * página hecha con React (static/app.js) la dibuja. No tiene lógica de negocio.
 */
public class PantallaRecepcionWeb implements PantallaRecepcion {

    /** Lo que la página debe mostrar en este momento. "paso" indica qué parte del CU. */
    private Map<String, Object> vista = new LinkedHashMap<>();

    public PantallaRecepcionWeb() {
        habilitarPantalla();
    }

    /** Deja la pantalla en el inicio, antes de que el EB elija registrar una recepción. */
    public void habilitarPantalla() {
        vista = new LinkedHashMap<>();
        vista.put("paso", "inicio");
    }

    /** Vuelve al inicio conservando el último mensaje (cancelación o caso de uso sin bolsines). */
    public void volverAlInicio() {
        Object mensaje = vista.get("mensaje");
        habilitarPantalla();
        if (mensaje != null) {
            vista.put("mensaje", mensaje);
        }
    }

    public void limpiarMensaje() {
        vista.remove("mensaje");
    }

    public String getPaso() {
        return (String) vista.get("paso");
    }

    public Map<String, Object> getVista() {
        return vista;
    }

    // ---------------------------------------------------------------- mensajes del gestor

    @Override
    public void mostrarCM(String nombreCMEmpleado) {
        vista.put("cm", nombreCMEmpleado);
    }

    @Override
    public void mostrarMensaje(String mensaje) {
        vista.put("mensaje", mensaje);
    }

    @Override
    public void solicitarSeleccionBolsin(List<ResumenDeBolsin> bolsinesEnviados) {
        vista.put("bolsines", bolsines(bolsinesEnviados));
        vista.put("paso", "seleccionBolsin");
    }

    @Override
    public void mostrarInfoBolsin(ResumenDeBolsin bolsin, Map<Integer, List<DatosDeDocumentacion>> informacionBolsin) {
        List<Map<String, Object>> remitos = new ArrayList<>();
        for (Map.Entry<Integer, List<DatosDeDocumentacion>> remito : informacionBolsin.entrySet()) {
            List<Map<String, Object>> documentacion = new ArrayList<>();
            for (DatosDeDocumentacion datos : remito.getValue()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("numero", datos.numero());
                item.put("asunto", datos.asunto());
                item.put("tipo", datos.tipo());
                documentacion.add(item);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("numero", remito.getKey());
            item.put("documentacion", documentacion);
            remitos.add(item);
        }
        Map<String, Object> vistaBolsin = new LinkedHashMap<>();
        vistaBolsin.put("numero", bolsin.numero());
        vistaBolsin.put("cmOrigen", bolsin.cmOrigen());
        vistaBolsin.put("precinto", bolsin.precinto());
        vistaBolsin.put("remitos", remitos);
        vista.put("bolsin", vistaBolsin);
    }

    @Override
    public void solicitarOpcRecepcion(ComisionMedica comisionMedica) {
        List<Map<String, Object>> opciones = new ArrayList<>();
        for (OpcionRecepcion opcion : OpcionRecepcion.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("numero", opcion.getNumero());
            item.put("descripcion", opcion.getDescripcion());
            item.put("requiereSeleccion", opcion.requiereSeleccionDeDocumentacion());
            item.put("disponible", opcion.estaDisponiblePara(comisionMedica));
            opciones.add(item);
        }
        vista.put("opciones", opciones);
        vista.put("paso", "opcion");
    }

    @Override
    public void solicitarConfirmacion(OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        Map<String, Object> elegida = new LinkedHashMap<>();
        elegida.put("numero", opcion.getNumero());
        elegida.put("descripcion", opcion.getDescripcion());
        vista.put("opcionElegida", elegida);
        vista.put("documentacionMarcada", new ArrayList<>(documentacionMarcada));
        vista.put("paso", "confirmacion");
    }

    @Override
    public void mostrarMensajeExito(int numeroBolsin, String estadoBolsin) {
        vista.put("mensajeExito", "Recepción registrada con éxito. Bolsín " + numeroBolsin + ": " + estadoBolsin + ".");
        vista.put("paso", "exito");
    }

    /** Convierte el listado de bolsines al formato que recibe la página. */
    public static List<Map<String, Object>> bolsines(List<ResumenDeBolsin> listado) {
        List<Map<String, Object>> bolsines = new ArrayList<>();
        for (ResumenDeBolsin resumen : listado) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("numero", resumen.numero());
            item.put("cmOrigen", resumen.cmOrigen());
            item.put("precinto", resumen.precinto());
            bolsines.add(item);
        }
        return bolsines;
    }
}
