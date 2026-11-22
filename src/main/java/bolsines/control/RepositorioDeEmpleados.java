package bolsines.control;

import bolsines.entity.Empleado;

import java.util.List;

/**
 * Lo que el gestor necesita de la persistencia sobre los empleados: recorrerlos para encontrar
 * el del usuario logueado con esTuUsuario(), como en el diagrama de secuencia de análisis.
 */
public interface RepositorioDeEmpleados {

    List<Empleado> buscarEmpleados();
}
