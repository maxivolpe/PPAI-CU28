package bolsines.boundary;

import bolsines.entity.ComisionMedica;
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
    public void solicitarSeleccionBolsin(List<String[]> bolsinesEnviados) {
        vista.put("bolsines", bolsines(bolsinesEnviados));
        vista.put("paso", "seleccionBolsin");
    }

    @Override
    public void mostrarInfoBolsin(int numeroBolsin, String cmOrigen, int numeroPrecinto,
                                  Map<Integer, List<String[]>> informacionBolsin) {
        List<Map<String, Object>> remitos = new ArrayList<>();
        for (Map.Entry<Integer, List<String[]>> remito : informacionBolsin.entrySet()) {
            List<Map<String, Object>> documentacion = new ArrayList<>();
            for (String[] datos : remito.getValue()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("numero", Integer.valueOf(datos[0]));
                item.put("asunto", datos[1]);
                item.put("tipo", datos[2]);
                documentacion.add(item);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("numero", remito.getKey());
            item.put("documentacion", documentacion);
            remitos.add(item);
        }
        Map<String, Object> bolsin = new LinkedHashMap<>();
        bolsin.put("numero", numeroBolsin);
        bolsin.put("cmOrigen", cmOrigen);
        bolsin.put("precinto", numeroPrecinto);
        bolsin.put("remitos", remitos);
        vista.put("bolsin", bolsin);
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

    /** Convierte el listado {número, CM origen, precinto} al formato que recibe la página. */
    public static List<Map<String, Object>> bolsines(List<String[]> listado) {
        List<Map<String, Object>> bolsines = new ArrayList<>();
        for (String[] datos : listado) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("numero", Integer.valueOf(datos[0]));
            item.put("cmOrigen", datos[1]);
            item.put("precinto", Integer.valueOf(datos[2]));
            bolsines.add(item);
        }
        return bolsines;
    }
}
