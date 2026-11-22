package bolsines.boundary;

import bolsines.control.GestorRecepcion;
import bolsines.entity.OpcionRecepcion;

import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Clase frontera del CU 28, en versión de consola (se usa en los tests y con "consola").
 * Muestra lo que le pide el gestor y le pasa lo que ingresa el EB. No tiene lógica de negocio.
 */
public class PantallaRecepcionConsola implements PantallaRecepcion {

    private static final String CANCELAR = "c";

    private final GestorRecepcion gestor;
    private final Scanner entrada;
    private final PrintStream salida;

    public PantallaRecepcionConsola(GestorRecepcion gestor, Scanner entrada, PrintStream salida) {
        this.gestor = gestor;
        this.entrada = entrada;
        this.salida = salida;
    }

    /** Paso 1: el EB selecciona la opción para registrar la recepción de un bolsín. */
    public void opcRegistrarRecepcion() {
        habilitarPantalla();
        gestor.opcRegistrarRecepcion(this);
    }

    private void habilitarPantalla() {
        salida.println();
        salida.println("==== Registrar recepción de bolsín ====");
    }

    @Override
    public void mostrarCM(String nombreCMEmpleado) {
        salida.println("Comisión médica: " + nombreCMEmpleado);
    }

    @Override
    public void mostrarMensaje(String mensaje) {
        salida.println();
        salida.println(">> " + mensaje);
    }

    // ---------------------------------------------------------------- selección de bolsín

    /** Pasos 4 y 5, con los filtros de los flujos alternativos A1 y A2. */
    @Override
    public void solicitarSeleccionBolsin(List<String[]> bolsinesEnviados) {
        List<String[]> listado = bolsinesEnviados;
        while (true) {
            salida.println();
            salida.println("Bolsines enviados a esta comisión médica:");
            for (String[] bolsin : listado) {
                salida.println("  Bolsín " + bolsin[0] + " | Origen: " + bolsin[1] + " | Precinto N° " + bolsin[2]);
            }
            salida.println("Ingrese el número de bolsín, 'p <precinto>' o 'o <CM origen>' para filtrar, "
                    + "'t' para ver todos, '" + CANCELAR + "' para cancelar:");
            String texto = leer();

            if (texto.equalsIgnoreCase(CANCELAR)) {
                gestor.cancelarCasoDeUso();
                return;
            }
            if (texto.equalsIgnoreCase("t")) {
                listado = bolsinesEnviados;
            } else if (texto.toLowerCase().startsWith("p ")) {
                Integer precinto = aEntero(texto.substring(2));
                List<String[]> filtrado = precinto == null ? List.of() : gestor.tomarFiltroPrecinto(precinto);
                if (filtrado.isEmpty()) {
                    mostrarMensaje("[A1] No se encuentra bolsín con el número de precinto ingresado.");
                } else {
                    listado = filtrado;
                }
            } else if (texto.toLowerCase().startsWith("o ")) {
                List<String[]> filtrado = gestor.tomarFiltroCMOrigen(texto.substring(2));
                if (filtrado.isEmpty()) {
                    mostrarMensaje("[A2] No se encuentra bolsín con la CM de origen ingresada.");
                } else {
                    listado = filtrado;
                }
            } else {
                Integer numeroBolsin = aEntero(texto);
                if (numeroBolsin != null && contiene(bolsinesEnviados, numeroBolsin)) {
                    tomarSeleccionBolsin(numeroBolsin);
                    return;
                }
                mostrarMensaje("Ingreso no válido: elija un bolsín del listado.");
            }
        }
    }

    private void tomarSeleccionBolsin(int numeroBolsin) {
        gestor.tomarSeleccionBolsin(numeroBolsin);
    }

    /** Paso 6: número de cada remito y, por cada documentación, asunto y tipo. */
    @Override
    public void mostrarInfoBolsin(int numeroBolsin, String cmOrigen, int numeroPrecinto,
                                  Map<Integer, List<String[]>> informacionBolsin) {
        salida.println();
        salida.println("Bolsín " + numeroBolsin + " | Origen: " + cmOrigen + " | Precinto N° " + numeroPrecinto);
        for (Map.Entry<Integer, List<String[]>> remito : informacionBolsin.entrySet()) {
            salida.println("  Remito " + remito.getKey());
            for (String[] documentacion : remito.getValue()) {
                salida.println("    Doc. " + documentacion[0] + " | " + documentacion[1]
                        + " | Tipo: " + documentacion[2]);
            }
        }
    }

    // ---------------------------------------------------------------- opción de recepción

    /** Pasos 7 y 8. La opción 4 solo se ofrece si la CM del usuario es la CMC. */
    @Override
    public void solicitarOpcRecepcion(boolean cmEsCentral) {
        while (true) {
            salida.println();
            salida.println("Opciones de recepción:");
            for (OpcionRecepcion opcion : OpcionRecepcion.values()) {
                boolean noDisponible = opcion == OpcionRecepcion.DOCUMENTACION_PARA_REDIRIGIR && !cmEsCentral;
                salida.println("  " + opcion.getNumero() + ". " + opcion.getDescripcion()
                        + (noDisponible ? " (no disponible: solo para la Comisión Médica Central)" : ""));
            }
            salida.println("Ingrese la opción ('" + CANCELAR + "' para cancelar):");
            String texto = leer();
            if (texto.equalsIgnoreCase(CANCELAR)) {
                gestor.cancelarCasoDeUso();
                return;
            }

            Integer numero = aEntero(texto);
            if (numero == null || numero < 1 || numero > OpcionRecepcion.values().length) {
                mostrarMensaje("Opción no válida.");
                continue;
            }
            OpcionRecepcion opcion = OpcionRecepcion.desdeNumero(numero);

            Set<Integer> documentacionMarcada = new LinkedHashSet<>();
            if (opcion.requiereSeleccionDeDocumentacion()) {
                salida.println("Ingrese los números de la documentación afectada, separados por coma:");
                for (String parte : leer().split(",")) {
                    Integer numeroDocumentacion = aEntero(parte);
                    if (numeroDocumentacion != null) {
                        documentacionMarcada.add(numeroDocumentacion);
                    }
                }
            }

            if (tomarOpcRecepcion(opcion, documentacionMarcada)) {
                return;
            }
        }
    }

    /** @return falso si el gestor rechazó la opción (se vuelve a pedir). */
    private boolean tomarOpcRecepcion(OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        try {
            gestor.tomarOpcRecepcion(opcion, documentacionMarcada);
            return true;
        } catch (IllegalArgumentException e) {
            mostrarMensaje(e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------- confirmación

    /** Pasos 9 y 10. Responder 'n' es el flujo alternativo A6. */
    @Override
    public void solicitarConfirmacion(OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        salida.println();
        salida.println("Opción elegida: " + opcion.getNumero() + ". " + opcion.getDescripcion());
        if (!documentacionMarcada.isEmpty()) {
            salida.println("Documentación afectada: " + documentacionMarcada);
        }
        while (true) {
            salida.println("¿Confirma el registro de la recepción? (s/n, '" + CANCELAR + "' para cancelar):");
            String texto = leer();
            if (texto.equalsIgnoreCase(CANCELAR)) {
                gestor.cancelarCasoDeUso();
                return;
            }
            if (texto.equalsIgnoreCase("s") || texto.equalsIgnoreCase("n")) {
                tomarConfirmacion(texto.equalsIgnoreCase("s"));
                return;
            }
        }
    }

    private void tomarConfirmacion(boolean confirma) {
        gestor.tomarConfirmacion(confirma);
    }

    @Override
    public void mostrarMensajeExito(int numeroBolsin, String estadoBolsin) {
        mostrarMensaje("Recepción registrada con éxito. Bolsín " + numeroBolsin + ": " + estadoBolsin + ".");
    }

    // ---------------------------------------------------------------- utilidades de entrada

    private String leer() {
        salida.print("> ");
        return entrada.nextLine().trim();
    }

    private Integer aEntero(String texto) {
        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean contiene(List<String[]> bolsines, int numeroBolsin) {
        for (String[] bolsin : bolsines) {
            if (bolsin[0].equals(String.valueOf(numeroBolsin))) {
                return true;
            }
        }
        return false;
    }
}
