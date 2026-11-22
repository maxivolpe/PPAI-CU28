package bolsines.boundary;

import bolsines.boundary.ComandoDeSeleccion.Cancelar;
import bolsines.boundary.ComandoDeSeleccion.FiltrarPorOrigen;
import bolsines.boundary.ComandoDeSeleccion.FiltrarPorPrecinto;
import bolsines.boundary.ComandoDeSeleccion.Seleccionar;
import bolsines.boundary.ComandoDeSeleccion.VerTodos;
import bolsines.control.GestorRecepcion;
import bolsines.entity.ComisionMedica;
import bolsines.entity.DatosDeDocumentacion;
import bolsines.entity.OpcionRecepcion;
import bolsines.entity.ReglaDeNegocioException;

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

    private static final String CANCELAR = ComandoDeSeleccion.CANCELAR;

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
    public void solicitarSeleccionBolsin(List<ResumenDeBolsin> bolsinesEnviados) {
        List<ResumenDeBolsin> listado = bolsinesEnviados;
        while (true) {
            mostrarListado(listado);
            ComandoDeSeleccion comando = ComandoDeSeleccion.interpretar(leer());

            if (comando instanceof Cancelar) {
                gestor.cancelarCasoDeUso();
                return;
            }
            if (comando instanceof Seleccionar seleccion && contiene(bolsinesEnviados, seleccion.numeroBolsin())) {
                tomarSeleccionBolsin(seleccion.numeroBolsin());
                return;
            }
            listado = aplicarComando(comando, listado, bolsinesEnviados);
        }
    }

    private void mostrarListado(List<ResumenDeBolsin> listado) {
        salida.println();
        salida.println("Bolsines enviados a esta comisión médica:");
        for (ResumenDeBolsin bolsin : listado) {
            salida.println("  Bolsín " + bolsin.numero() + " | Origen: " + bolsin.cmOrigen()
                    + " | Precinto N° " + bolsin.precinto());
        }
        salida.println("Ingrese el número de bolsín, 'p <precinto>' o 'o <CM origen>' para filtrar, "
                + "'" + ComandoDeSeleccion.VER_TODOS + "' para ver todos, '" + CANCELAR + "' para cancelar:");
    }

    /** @return el listado a mostrar a continuación: el filtrado, o el mismo si el comando no filtra o no halló resultados. */
    private List<ResumenDeBolsin> aplicarComando(ComandoDeSeleccion comando, List<ResumenDeBolsin> actual,
                                                 List<ResumenDeBolsin> todos) {
        if (comando instanceof VerTodos) {
            return todos;
        }
        if (comando instanceof FiltrarPorPrecinto filtro) {
            List<ResumenDeBolsin> filtrado =
                    filtro.precinto() == null ? List.of() : gestor.tomarFiltroPrecinto(filtro.precinto());
            return filtradoOMensaje(filtrado, actual,
                    "[A1] No se encuentra bolsín con el número de precinto ingresado.");
        }
        if (comando instanceof FiltrarPorOrigen filtro) {
            return filtradoOMensaje(gestor.tomarFiltroCMOrigen(filtro.texto()), actual,
                    "[A2] No se encuentra bolsín con la CM de origen ingresada.");
        }
        mostrarMensaje("Ingreso no válido: elija un bolsín del listado.");
        return actual;
    }

    private List<ResumenDeBolsin> filtradoOMensaje(List<ResumenDeBolsin> filtrado, List<ResumenDeBolsin> actual,
                                                   String mensajeSinResultados) {
        if (filtrado.isEmpty()) {
            mostrarMensaje(mensajeSinResultados);
            return actual;
        }
        return filtrado;
    }

    private void tomarSeleccionBolsin(int numeroBolsin) {
        gestor.tomarSeleccionBolsin(numeroBolsin);
    }

    /** Paso 6: número de cada remito y, por cada documentación, asunto y tipo. */
    @Override
    public void mostrarInfoBolsin(ResumenDeBolsin bolsin, Map<Integer, List<DatosDeDocumentacion>> informacionBolsin) {
        salida.println();
        salida.println("Bolsín " + bolsin.numero() + " | Origen: " + bolsin.cmOrigen()
                + " | Precinto N° " + bolsin.precinto());
        for (Map.Entry<Integer, List<DatosDeDocumentacion>> remito : informacionBolsin.entrySet()) {
            salida.println("  Remito " + remito.getKey());
            for (DatosDeDocumentacion documentacion : remito.getValue()) {
                salida.println("    Doc. " + documentacion.numero() + " | " + documentacion.asunto()
                        + " | Tipo: " + documentacion.tipo());
            }
        }
    }

    // ---------------------------------------------------------------- opción de recepción

    /** Pasos 7 y 8. La opción 4 solo se ofrece si la CM del usuario es la CMC. */
    @Override
    public void solicitarOpcRecepcion(ComisionMedica comisionMedica) {
        while (true) {
            mostrarOpciones(comisionMedica);
            String texto = leer();
            if (texto.equalsIgnoreCase(CANCELAR)) {
                gestor.cancelarCasoDeUso();
                return;
            }

            Integer numero = ComandoDeSeleccion.aEntero(texto);
            if (numero == null || numero < 1 || numero > OpcionRecepcion.values().length) {
                mostrarMensaje("Opción no válida.");
                continue;
            }
            OpcionRecepcion opcion = OpcionRecepcion.desdeNumero(numero);

            if (tomarOpcRecepcion(opcion, pedirDocumentacionAfectada(opcion))) {
                return;
            }
        }
    }

    private void mostrarOpciones(ComisionMedica comisionMedica) {
        salida.println();
        salida.println("Opciones de recepción:");
        for (OpcionRecepcion opcion : OpcionRecepcion.values()) {
            salida.println("  " + opcion.getNumero() + ". " + opcion.getDescripcion()
                    + (opcion.estaDisponiblePara(comisionMedica)
                    ? "" : " (no disponible: solo para la Comisión Médica Central)"));
        }
        salida.println("Ingrese la opción ('" + CANCELAR + "' para cancelar):");
    }

    /** Las opciones 2, 3 y 4 piden los números de la documentación a la que se aplican. */
    private Set<Integer> pedirDocumentacionAfectada(OpcionRecepcion opcion) {
        Set<Integer> documentacionMarcada = new LinkedHashSet<>();
        if (opcion.requiereSeleccionDeDocumentacion()) {
            salida.println("Ingrese los números de la documentación afectada, separados por coma:");
            for (String parte : leer().split(",")) {
                Integer numeroDocumentacion = ComandoDeSeleccion.aEntero(parte);
                if (numeroDocumentacion != null) {
                    documentacionMarcada.add(numeroDocumentacion);
                }
            }
        }
        return documentacionMarcada;
    }

    /** @return falso si el gestor rechazó la opción (se vuelve a pedir). */
    private boolean tomarOpcRecepcion(OpcionRecepcion opcion, Set<Integer> documentacionMarcada) {
        try {
            gestor.tomarOpcRecepcion(opcion, documentacionMarcada);
            return true;
        } catch (ReglaDeNegocioException e) {
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
        try {
            gestor.tomarConfirmacion(confirma);
        } catch (ReglaDeNegocioException e) {
            // Por ejemplo, otro usuario recibió el mismo bolsín mientras tanto: no se guardó nada.
            mostrarMensaje(e.getMessage());
        }
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

    private boolean contiene(List<ResumenDeBolsin> bolsines, int numeroBolsin) {
        return bolsines.stream().anyMatch(bolsin -> bolsin.numero() == numeroBolsin);
    }
}
