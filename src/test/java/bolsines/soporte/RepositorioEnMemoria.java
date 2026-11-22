package bolsines.soporte;

import bolsines.control.RepositorioDeBolsines;
import bolsines.control.RepositorioDeEmpleados;
import bolsines.entity.Bolsin;
import bolsines.entity.Empleado;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia de prueba: guarda todo en listas. Permite probar el gestor sin SQLite y ver
 * qué le pidió guardar.
 */
public class RepositorioEnMemoria implements RepositorioDeBolsines, RepositorioDeEmpleados {

    private final List<Empleado> empleados = new ArrayList<>();
    private final List<Bolsin> bolsines = new ArrayList<>();
    /** Los bolsines que el gestor mandó a guardar, en orden. */
    public final List<Bolsin> recepcionesGuardadas = new ArrayList<>();

    public RepositorioEnMemoria conEmpleados(Empleado... empleados) {
        this.empleados.addAll(List.of(empleados));
        return this;
    }

    public RepositorioEnMemoria conBolsines(Bolsin... bolsines) {
        this.bolsines.addAll(List.of(bolsines));
        return this;
    }

    @Override
    public List<Empleado> buscarEmpleados() {
        return List.copyOf(empleados);
    }

    @Override
    public List<Bolsin> buscarBolsines() {
        return List.copyOf(bolsines);
    }

    @Override
    public void guardarRecepcion(Bolsin bolsin) {
        recepcionesGuardadas.add(bolsin);
    }
}
