package bolsines.soporte;

import bolsines.entity.Bolsin;
import bolsines.entity.CambioEstadoBolsin;
import bolsines.entity.CambioEstadoDocumentacion;
import bolsines.entity.ComisionMedica;
import bolsines.entity.DetalleRemito;
import bolsines.entity.Documentacion;
import bolsines.entity.Empleado;
import bolsines.entity.Remito;
import bolsines.entity.TipoDocumentacion;
import bolsines.entity.Usuario;
import bolsines.entity.estado.bolsin.EstadoBolsin;
import bolsines.entity.estado.documentacion.EstadoDocumentacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Armado de objetos de dominio para los tests: no dependen de la base de datos ni de los
 * números del seed, así que cambiar los datos de demo no los rompe.
 */
public final class Escenarios {

    /** Cuando el bolsín y su documentación pasaron al estado que tienen al empezar el test. */
    public static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 12, 8, 30);
    /** El momento en que el encargado registra la recepción. */
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 10, 15, 30);
    public static final Clock RELOJ_FIJO = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private Escenarios() {
    }

    public static ComisionMedica cordoba() {
        return new ComisionMedica("CBA", "Comisión Médica Córdoba", "Av. Colón 150", "cba@organismo.gob.ar", "0351", false);
    }

    public static ComisionMedica rosario() {
        return new ComisionMedica("ROS", "Comisión Médica Rosario", "San Martín 980", "ros@organismo.gob.ar", "0341", false);
    }

    public static ComisionMedica central() {
        return new ComisionMedica("CMC", "Comisión Médica Central", "Av. Corrientes 800", "cmc@organismo.gob.ar", "011", true);
    }

    public static Empleado encargadoDe(ComisionMedica comisionMedica) {
        return empleado("cmendez", comisionMedica);
    }

    /** Un empleado con ese nombre de usuario, que trabaja en esa CM. */
    public static Empleado empleado(String nombreUsuario, ComisionMedica comisionMedica) {
        return new Empleado("Nombre " + nombreUsuario, "Apellido " + nombreUsuario,
                nombreUsuario + "@organismo.gob.ar", comisionMedica, new Usuario(nombreUsuario, "clave"));
    }

    /** Una documentación en el estado dado, con su cambio de estado vigente (como queda al cargarla de la base). */
    public static Documentacion documentacion(int numero, EstadoDocumentacion estado, Empleado responsable) {
        Documentacion documentacion = new Documentacion(numero, "Asunto " + numero, LocalDate.of(2026, 11, 9),
                new TipoDocumentacion("Expediente", "Expediente administrativo"), estado);
        documentacion.agregarCambioEstado(new CambioEstadoDocumentacion(INICIO, responsable, estado));
        return documentacion;
    }

    /** Un remito «En Bolsín Enviado» con esa documentación. */
    public static Remito remitoEnBolsinEnviado(int numero, ComisionMedica origen, ComisionMedica destino,
                                               Documentacion... documentaciones) {
        Remito remito = new Remito(numero, LocalDate.of(2026, 11, 9), origen, destino,
                new bolsines.entity.estado.remito.EnBolsinEnviado());
        for (Documentacion documentacion : documentaciones) {
            remito.agregarDetalle(new DetalleRemito(null, documentacion));
        }
        return remito;
    }

    /** Un bolsín en el estado dado, con su cambio de estado vigente y sus remitos. */
    public static Bolsin bolsin(int numero, EstadoBolsin estado, ComisionMedica origen, ComisionMedica destino,
                                Empleado responsable, Remito... remitos) {
        Bolsin bolsin = new Bolsin(numero, 700000 + numero, LocalDate.of(2026, 11, 10), 2000, origen, destino, estado);
        bolsin.agregarCambioEstado(new CambioEstadoBolsin(INICIO, responsable, estado));
        for (Remito remito : remitos) {
            bolsin.agregarRemito(remito);
        }
        return bolsin;
    }

    /** Un bolsín «Enviado» de Rosario a {@code destino}, con un remito que lleva las documentaciones 1 y 2. */
    public static Bolsin bolsinEnviadoCon2Documentaciones(ComisionMedica destino) {
        Empleado emisor = encargadoDe(rosario());
        Documentacion primera = documentacion(1, new bolsines.entity.estado.documentacion.EnBolsinEnviado(), emisor);
        Documentacion segunda = documentacion(2, new bolsines.entity.estado.documentacion.EnBolsinEnviado(), emisor);
        Remito remito = remitoEnBolsinEnviado(10, rosario(), destino, primera, segunda);
        return bolsin(100, new bolsines.entity.estado.bolsin.Enviado(), rosario(), destino, emisor, remito);
    }

    /** La documentación número {@code numero} del bolsín. */
    public static Documentacion documentacionDe(Bolsin bolsin, int numero) {
        return bolsin.getRemitos().stream()
                .flatMap(remito -> remito.getDetallesRemito().stream())
                .map(DetalleRemito::getDocumentacion)
                .filter(documentacion -> documentacion.getNumero() == numero)
                .findFirst().orElseThrow();
    }
}
