# Plan de refactorización — PPAI Bolsines (CU 28)

> **Cómo leer este documento:** las secciones 0 a 6 son la **propuesta original**. Lo que efectivamente se aplicó, lo que se cambió sobre la marcha y lo que se descartó (con el motivo) está en **§6 ter**. Ante una diferencia, vale §6 ter.
>
> **Base analizada:** rama `main`, commit `8d3396c`. ≈47 clases de producción (~3.100 líneas) y 1 clase de test (257 líneas, 11 tests).
> **Línea base de tests: NO verificada.** Esta máquina tiene `JAVA_HOME` en JDK 17 (y un JRE 8); el proyecto exige `release 21` y `mvn test` falla al cargar clases compiladas con 21. Ver Fase 0.
> **Semáforo de riesgo frente a las consignas:** 🟢 libre · 🟡 permitido pero cambia algo que figura en un diagrama (actualizar el informe) · 🔴 congelado, no tocar.

---

## 0. Lo que decide todo: las consignas congelan parte del código

Pediste hexagonal/DDD, renombrar métodos y bajar acoplamiento. Gran parte se puede hacer, pero **no todo**, porque la cátedra evalúa la *consistencia entre el código y el modelado* y el informe de la Entrega 2 ya fue presentado (17/11/2026).

| Qué dicen las consignas / el informe | Fuente | Consecuencia para el refactor |
|---|---|---|
| Se evalúa «consistencia con el modelado (clases gestor, pantalla/boundary, entidades, aplicación de los patrones)» | Consignas, p. 4, criterio 2.b | Los nombres de clase del diagrama no se tocan. |
| Hay que «defender la implementación y mostrar el patrón implementado» y «el programa debe funcionar a la hora de la presentación» | Consignas, p. 4, 2.a y 2.d | Cualquier refactor tiene que dejar el programa corriendo y el State legible en el código. |
| El diagrama de clases de diseño exige tipos, retornos, parámetros y **visibilidad** de métodos | Consignas, p. 4, punto 2 | Cambiar `public`→package-private en `setEstado`, etc., es 🟡. |
| Informe: Java 21, **sin frameworks, JDBC puro, SQLite**, web (React) + consola, carga automática de datos de prueba, flujo principal + A1–A6 | Informe E2, p. 10 | Nada de Spring/JPA/Hibernate. Mantener el arranque sin configuración. |
| El State se aplica en las **tres** jerarquías (`EstadoBolsin`, `EstadoRemito`, `EstadoDocumentacion`), una clase por estado | Informe E2, p. 7 y 10; diagrama de clases | Las 20 clases de estado concretas (5 + 6 + 9) se quedan; se refactoriza *alrededor*. |
| Cadena de delegación del diseño: Gestor → `Bolsin` → `Enviado` → `Remito` → `EnBolsinEnviado` → `DetalleRemito` → `Documentacion` → `EnBolsinEnviado` | Diagrama de secuencia de diseño | La cadena y los nombres `registrarBolsinRecibido`, `crearEstado`, `crearCambioEstado*`, `buscarCambioEstadoActual`, `setEstado`, `agregarCambioEstado` son 🔴. |
| Métodos del análisis: `opcRegistrarRecepcion`, `buscarCMyMostrar`, `buscarBolsinesEnviados`, `tomarSeleccionBolsin`, `buscarInformacionBolsin`, `tomarOpcRecepcion`, `tomarConfirmacion`, `getFechaYHoraActual`, `llamarCasoDeUsoNotificarRecepcionDeBolsin`, `finCasoDeUso`; en la pantalla `habilitarPantalla`, `mostrarCM`, `solicitar*`, `mostrar*`; en entidades `esTuCMDestino`, `esTuUsuario`, `sosActual`, `sosEnviado`, `getCM`, `getCMOrigen`, `getNumPrecinto`, `obtenerInformacionRemito`, `tomarDocumentacion` | Diagramas de análisis (`PPAI-28-REALIZACION.pdf`) | 🔴 Esos nombres «raros» (voseo, `tomar*`) **no son deuda técnica: son el vocabulario de la cátedra**. |
| ERS: notificación por mail vía API de Outlook (Graph) como *componente*; tecnología web | ERS §4.2 (RNF 1 y 5) | Encaja perfecto como **puerto** `InterfazServidorCorreo` con un adaptador simulado hoy y uno real mañana. |

**Qué sí es libre (🟢):** `BaseDeDatos`, `Esquema`, `MapeoEstados`, `ServidorWeb`, `Json`, `Main`, `OpcionRecepcion`, `TransicionNoPermitidaException`, el *interior* de todos los métodos, los tipos de datos que viajan entre Gestor y Pantalla, los tests, el build y la documentación.

### Principio rector: «hexagonal por dentro, BCE por fuera»

BCE (Boundary–Control–Entity) y la arquitectura hexagonal son la misma idea con otro vocabulario. Se adopta la estructura hexagonal **conservando los nombres BCE de clase** y dejando el mapeo explícito (sirve también como diapositiva para la defensa):

| BCE (consigna) | Hexagonal | Clase(s) |
|---|---|---|
| Boundary | Adaptador de entrada (+ *presenter*) | `PantallaRecepcionWeb`, `PantallaRecepcionConsola` |
| — | Puerto de salida (presenter) | `PantallaRecepcion` (la interfaz que el Gestor invoca) |
| Control | Servicio de aplicación / caso de uso | `GestorRecepcion` (CU 28), `NotificarRecepcionDeBolsin` (CU 29, `<<include>>`) |
| Entity + State | Dominio | `Bolsin`, `Remito`, `Documentacion`, estados… |
| Boundary con actor Servidor de Correo | Puerto de salida + adaptador | `InterfazServidorCorreo` → `ServidorCorreoSimulado` |
| (BCE no lo nombra) | Puertos y adaptadores de persistencia | `RepositorioDe*` → `*Sqlite` |

**No se parte `GestorRecepcion` en un `…UseCase` por método** (dogma de Clean Architecture): el Gestor de la cátedra es un controlador *conversacional* (varios eventos `tomar*` de la pantalla sobre un mismo estado de conversación). Trocearlo rompería el diagrama de secuencia. Lo que se hace es extraerle **colaboradores** (validador, notificador, búsqueda).

---

## 1. Lo que ya está bien (no se toca)

- State bien aplicado: el estado abstracto lanza `TransicionNoPermitidaException` por defecto, los contextos delegan sin `if`, cada estado crea a su sucesor.
- `guardarRecepcion` se ejecuta **una vez, al final y en una transacción** (`BaseDeDatos.java:221-253`); `PreparedStatement` en todo el acceso (sin SQL injection).
- La interfaz `PantallaRecepcion` ya desacopla el Gestor de web/consola.
- Tests contra SQLite real (`@TempDir`), que verifican releyendo la base.

---

## 2. Arquitectura objetivo

### 2.1 Jerarquía de paquetes

```
bolsines
├── Main.java                              ← solo parsea args y arma Aplicacion
├── dominio/                               ← «Entity» de BCE. Solo java.*; sin SQL ni HTTP
│   ├── bolsin/        Bolsin, CambioEstadoBolsin
│   │   └── estado/    EstadoBolsin, Creado, Cerrado, Enviado, RecibidoEnCMDestino, DeBaja
│   ├── remito/        Remito, DetalleRemito
│   │   └── estado/    EstadoRemito, Creado, EnBolsinSaliente, EnBolsinEnviado, RecibidoYAceptado, RecibidoYAceptadoParcial, Cancelado
│   ├── documentacion/ Documentacion, TipoDocumentacion, CambioEstadoDocumentacion
│   │   └── estado/    EstadoDocumentacion, Registrada, EnRemito, EnBolsinSaliente, EnBolsinEnviado, RecibidaYAceptada, …
│   ├── organizacion/  ComisionMedica, Empleado, Usuario, Sesion
│   ├── recepcion/     OpcionRecepcion, RegistroDeRecepcion (record)
│   └── error/         ReglaDeNegocioException, TransicionNoPermitidaException, …
├── aplicacion/                            ← «Control» de BCE
│   ├── recepcion/     GestorRecepcion, ValidadorDeOpcionDeRecepcion, PasoDeRecepcion
│   ├── notificacion/  NotificarRecepcionDeBolsin (CU 29), ArmadorDeEmailDeRecepcion
│   ├── consulta/      ConsultarEstadosGuardados (lo que hoy está duplicado en Main y ServidorWeb)
│   └── puertos/       RepositorioDeBolsines, RepositorioDeEmpleados,
│                      PantallaRecepcion, InterfazServidorCorreo   (+ java.time.Clock)
└── infraestructura/                       ← «Boundary» de BCE + adaptadores de salida
    ├── entrada/
    │   ├── web/       ServidorHttp, EnrutadorApi, ControladorRecepcionWeb, SesionesWeb,
    │   │              ArchivosEstaticos, PantallaRecepcionWeb, Json, vistas/ (records DTO)
    │   └── consola/   PantallaRecepcionConsola, MenuConsola, ComandoDeConsola (sealed)
    ├── persistencia/sqlite/
    │                  ConexionSqlite, MigradorDeEsquema, RepositorioDeBolsinesSqlite,
    │                  RepositorioDeEmpleadosSqlite, RegistroDeEstados, Convertidores
    ├── correo/        ServidorCorreoSimulado   (futuro: ServidorCorreoGraph, RNF 5)
    └── configuracion/ Aplicacion (raíz de composición), Configuracion (record)
src/main/resources/db/
    ├── migraciones/   V001__esquema.sql, V002__indices_y_restricciones.sql
    └── datos/         datos-de-prueba.sql
```

Cada paquete lleva un `package-info.java` que dice a qué rol BCE corresponde (para que quien lea el código vea la equivalencia con el diagrama).

**Regla de dependencia:** `infraestructura → aplicacion → dominio`. El dominio no importa nada de `java.sql`, `com.sun.net.httpserver` ni de las otras capas. Se verifica con ArchUnit (§6.6).

**Por qué el dominio va «por agregado»** y no por tipo: los estados quedan junto a su contexto y se elimina la colisión de nombres (hoy `EnBolsinEnviado`, `EnBolsinSaliente`, `Creado` y `DeBaja` se repiten en dos jerarquías cada uno, lo que obliga a `MapeoEstados` a usar nombres completos). Las dependencias entre paquetes quedan sin ciclos: `organizacion ← documentacion ← remito ← bolsin`.

### 2.2 Puertos

```java
// aplicacion/puertos
public interface RepositorioDeBolsines {
    /** Trae solo los bolsines de esa CM destino; el filtro por estado lo sigue haciendo sosEnviado() (diagrama de análisis). */
    List<Bolsin> buscarPorDestino(ComisionMedica destino);
    /** Persiste estado + historiales de bolsín, remitos y documentación en UNA transacción. */
    void guardarRecepcion(Bolsin bolsin);
}

public interface RepositorioDeEmpleados {
    List<Empleado> listar();   // el Gestor sigue iterando con esTuUsuario(), como en el diagrama de análisis
}

public interface InterfazServidorCorreo {                    // hoy es una clase concreta
    void enviarEmail(String destinatario, String asunto, String cuerpo);
}
```

`GestorRecepcion` pasa de depender de `BaseDeDatos` concreta (`GestorRecepcion.java:36`) a depender de estas interfaces + `Clock` (inversión de dependencias). El constructor queda:
`new GestorRecepcion(repoBolsines, repoEmpleados, sesion, servidorCorreo, reloj)`.

---

## 3. Qué dividir, renombrar o abstraer

### 3.1 Patrón State y entidades

| # | Hallazgo (ubicación) | Refactor | Riesgo |
|---|---|---|---|
| E1 | **Data clump**: `(fechaHora, responsable, opcion, documentacionMarcada)` viaja por 5 niveles; firmas de 6–7 parámetros (`EstadoBolsin.java:32-36`, `EstadoRemito.java:25-27`). Además `opcion`/`documentacionMarcada` ni figuran en el diagrama: crecieron con A3–A5. | `record RegistroDeRecepcion(LocalDateTime fechaHora, Empleado responsable, OpcionRecepcion opcion, Set<Integer> documentacionMarcada)` con `marcaA(int numeroDoc)`. Firmas → `(Bolsin, RegistroDeRecepcion)`. | 🟡 cambia firmas del diagrama de secuencia; hacerlo post-defensa y actualizar el informe |
| E2 | `DetalleRemito.registrarBolsinRecibido` hace `switch (opcion)` (`:31-40`) y tiene un `case CONTENIDO_IGUAL` duplicado del `if` de arriba. | Convertir `OpcionRecepcion` en un enum **con comportamiento** (cada constante conoce el evento que dispara). Ver sketch abajo. CC 5 → 2. | 🟢 |
| E3 | La regla A5 «solo la CM Central redirige» existe **3 veces**: `GestorRecepcion.java:175-178` (IAE), `Enviado.java:43-46` (ISE) y `PantallaRecepcionWeb.java:104`. | Una sola: `OpcionRecepcion.estaDisponiblePara(ComisionMedica destino)`. Gestor, `Enviado` y pantalla la consultan. | 🟢 |
| E4 | `EstadoDocumentacion.esNoRecibida()` / `esRecibidaYRechazada()` (`:29-35`) son *type-tests* con otro nombre; los usa `DetalleRemito.tenesDocumentacionAceptada` (`:44-47`). | Consulta semántica polimórfica `EstadoDocumentacion.dejaRemitoIncompleto()` (default `false`; `NoRecibida` y `RecibidaYRechazada` → `true`). Renombrar `tenesDocumentacionAceptada` → `dejaRemitoIncompleto()` (no está en ningún diagrama). | 🟢 |
| E5 | «Buscar el cambio de estado vigente» está escrito 3 veces (`Bolsin.java:85-101`, `EnBolsinEnviado.java:68-76` de Documentación). | Helper único `HistorialDeEstados.vigente(List<? extends CambioDeEstado<?>>)`. Los métodos privados `buscarCambioEstadoActual()` **se quedan** (están en el diagrama) y su cuerpo delega. | 🟢 |
| E6 | `CambioEstadoBolsin` y `CambioEstadoDocumentacion` son gemelas (mismos 4 campos, mismo `sosActual`/`setFechaHoraFin`). | `abstract class CambioDeEstado<E>`; las dos clases del diagrama la extienden. | 🟡 (los atributos pasan a la superclase) |
| E7 | `getNombre()` es a la vez etiqueta de UI **y** clave en la base (`MapeoEstados.java:46-71`): un cambio de texto rompe datos guardados, y los tests comparan strings visibles. | Agregar `codigo()` estable (`"EN_BOLSIN_ENVIADO"`) para persistencia y tests; `getNombre()` queda solo para mostrar. Es **aditivo**: nada existente se rompe. | 🟢 |
| E8 | Los estados no tienen datos pero se hace `new` en cada transición y `MapeoEstados` usa otras instancias → `estadoA == estadoB` da `false` entre «el mismo» estado. | `equals/hashCode` por `getClass()` y `toString() = getNombre()` en cada clase base. (El `new` se mantiene: es el del diagrama de secuencia.) | 🟢 |
| E9 | `EstadoDocumentacion` repite 12 veces `throw new TransicionNoPermitidaException("x", getNombre())`. | Helper `protected TransicionNoPermitidaException noPermitida(String evento)`. Los 8 stubs de otros CU se quedan (figuran en el diagrama). | 🟢 |
| E10 | Getters que devuelven la lista interna mutable (`Bolsin.getRemitos` `:140`, `getCambioEstados` `:148`, `Remito.getDetallesRemito` `:90`, `Documentacion.getCambioEstadoDocumentacion` `:94`). | `Collections.unmodifiableList(...)`. Los mutadores siguen siendo `agregarRemito`, `agregarDetalle`, `agregarCambioEstado`. | 🟢 |
| E11 | `setEstado` y `agregarCambioEstado` son públicos: cualquiera (p. ej. el Gestor) podría cambiar un estado salteándose la máquina. | Con el dominio por agregado, contexto y estados comparten paquete → package-private. | 🟡 el diagrama dice `+` (visibilidad evaluada) → solo post-defensa |
| E12 | Remito no guarda historial (`Remito.java:11-17`) pero el informe dice que sí (el informe afirma tablas de historial para bolsín, remito y documentación; en el código solo existen las de bolsín y documentación). | **Decidir una de dos**: corregir la frase del informe, o agregar `CambioEstadoRemito` + tabla `cambio_estado_remito`. No es refactor, es coherencia. | 🟡 |

**Sketch de E2 + E3 (enum con comportamiento):**

```java
@FunctionalInterface
interface EventoDeDocumentacion {
    void aplicar(Documentacion documentacion, LocalDateTime fechaHora, Empleado responsable);
}

public enum OpcionRecepcion {
    CONTENIDO_IGUAL(1, "El contenido del bolsín es igual al registrado", Documentacion::registrarBolsinRecibido),
    DOCUMENTACION_NO_RECIBIDA(2, "No se recibe toda la documentación…", Documentacion::registrarDocumentacionNoRecibida),
    DOCUMENTACION_NO_CORRESPONDE(3, "Existe documentación que no corresponde al destino", Documentacion::registrarRechazoDeDocumentacion),
    DOCUMENTACION_PARA_REDIRIGIR(4, "La documentación se debe redirigir a otra área", Documentacion::registrarDocumentacionParaRedirigir);

    // … numero, descripcion, evento …
    public void aplicarA(Documentacion d, LocalDateTime fechaHora, Empleado responsable) {
        evento.aplicar(d, fechaHora, responsable);
    }
    public boolean estaDisponiblePara(ComisionMedica destino) {          // E3: única fuente de la regla A5
        return this != DOCUMENTACION_PARA_REDIRIGIR || destino.sosCentral();
    }
}

// DetalleRemito: sin switch
public void registrarBolsinRecibido(LocalDateTime fechaHora, Empleado responsable,
                                    OpcionRecepcion opcion, Set<Integer> marcadas) {
    OpcionRecepcion aplicable = marcadas.contains(documentacion.getNumero())
            ? opcion : OpcionRecepcion.CONTENIDO_IGUAL;
    aplicable.aplicarA(documentacion, fechaHora, responsable);
}
```

### 3.2 `GestorRecepcion` y casos de uso

| # | Hallazgo (ubicación) | Refactor | Riesgo |
|---|---|---|---|
| G1 | `validarOpcRecepcion` (`:168-193`): 3 `return`/`throw` y 2 bucles, CC ≈ 9 (estimado a ojo; medir con PMD). | Extraer `ValidadorDeOpcionDeRecepcion` con tres reglas chicas: `exigirDocumentacionSegunOpcion`, `exigirOpcionDisponible` (usa E3), `exigirQuePerteneceAlBolsin`. Cada método CC ≤ 3. | 🟢 |
| G2 | CU 29 mezclado en el Gestor: arma el texto de los mails con `StringBuilder` (`:231-249`). | `NotificarRecepcionDeBolsin` (servicio de aplicación) + `ArmadorDeEmailDeRecepcion`. En el Gestor queda `llamarCasoDeUsoNotificarRecepcionDeBolsin()` como una línea (nombre del diagrama). | 🟢 |
| G3 | Controlador con 10 campos de «conversación» que se resetean a mano (`finCasoDeUso :259-266`) y que **no protege su propio protocolo**: llamar `tomarConfirmacion` sin selección da NPE. Hoy lo cubre el *adaptador web* (`ServidorWeb.exigirPaso :163`). | Enum `PasoDeRecepcion` (INICIO, SELECCION, OPCION, CONFIRMACION) dentro del Gestor + `exigirPaso(...)`; el protocolo es regla del caso de uso, no de HTTP. Los campos pasan a un `ContextoDeRecepcion` con `reiniciar()`. | 🟢 (si se quiere 1:1 con el diagrama de clases, los campos se dejan en el Gestor) |
| G4 | `buscarBolsinesEnviados` (`:89-96`) llama `baseDeDatos.buscarBolsines()`, que lee **toda la base** (9 consultas, todos los bolsines con remitos, documentación e historiales) y filtra en memoria. `buscarCMyMostrar` (`:73-86`) lee todos los empleados y recorre sin `break`. | Puertos: `repoBolsines.buscarPorDestino(cm)` (SQL con `WHERE cm_destino = ?`) y `repoEmpleados.listar()`. **Los dos bucles se mantienen en el Gestor**: el de `esTuCMDestino()`/`sosEnviado()` y el de `esTuUsuario()` son los del diagrama de secuencia de análisis (`*esTuUsuario()`, `esTuCmDestino()`, `*sosActual()`); solo cambia de dónde vienen los datos. (Una `buscarPorUsuario(u)` en el repositorio sería más eficiente pero elimina el `*esTuUsuario()` del diagrama: 🟡, no recomendada antes de la defensa.) | 🟢 |
| G5 | Los filtros hurgan en el Bolsín: `bolsin.getCMOrigen().toLowerCase().contains(...)` (`:113`), `bolsin.getNumPrecinto() == n` (`:102`). | *Tell, don't ask*: `Bolsin.esDePrecinto(int)` y `Bolsin.esDeCMOrigen(String)`. `tomarFiltro*` → una línea de `stream().filter(...)`. | 🟢 |
| G6 | `LocalDateTime.now()` fijo (`:213`): tests no determinísticos. | Inyectar `Clock`. `getFechaYHoraActual()` (nombre del diagrama) devuelve `LocalDateTime.now(reloj)`. | 🟢 |
| G7 | Datos que viajan como `String[]` posicional (`listar :121`, `PantallaRecepcion :21-24`, `datos[0]`, `datos[1]` en 3 clases). | `record ResumenDeBolsin(int numero, String cmOrigen, int precinto)` y `record InformacionDeBolsin(...)`. Cambian los tipos de la interfaz `PantallaRecepcion`, no sus nombres. | 🟢 |
| G8 | Todo error es `IllegalArgumentException`/`IllegalStateException`; el web atrapa **cualquier** `RuntimeException` y devuelve su mensaje como 400 (`ServidorWeb.java:82-87`): un NPE o un bug se le muestra al usuario como «error de validación». | Jerarquía `ReglaDeNegocioException` (base) → `OpcionDeRecepcionInvalidaException`, `BolsinFueraDelListadoException`, `TransicionNoPermitidaException`. El adaptador mapea **solo** esas a 400; el resto a 500 con mensaje genérico + log. | 🟢 |

### 3.3 Boundary, web y arranque

| # | Hallazgo (ubicación) | Refactor | Riesgo |
|---|---|---|---|
| B1 | `ServidorWeb` (368 líneas) hace routing, sesión, JSON, archivos estáticos, parseo de parámetros y consultas de lectura. `atenderApi` es un `switch` sobre strings `"POST /api/…"` (CC ≈ 11). | Dividir en `ServidorHttp` (ciclo de vida), `EnrutadorApi` (tabla `Map<Ruta, Manejador>`: agregar un endpoint no toca el router), `ControladorRecepcionWeb`, `ArchivosEstaticos`, DTO como `record`. | 🟢 |
| B2 | **Un solo gestor/pantalla/empleado para todos los clientes** (`ServidorWeb.java:44-48`; el propio javadoc admite «un único usuario a la vez»). Dos navegadores se pisan. | `SesionesWeb`: `Map<idSesion, ContextoDeUsuario(gestor, pantalla, correo)>` con cookie. Es el cambio que más separa «demo» de «producción». | 🟢 |
| B3 | La consulta de estados guardados está **copiada** en `Main.java:91-117` y `ServidorWeb.java:250-293`. | `ConsultarEstadosGuardados` (aplicación) devuelve records; consola y web solo lo dibujan. | 🟢 |
| B4 | `Main` mezcla arranque, menú de consola, login simulado y consulta. | `Main` → `Aplicacion.arrancar(Configuracion)` (raíz de composición manual, sin framework DI); `MenuConsola`; `InicioDeSesionSimulado`. `Configuracion` (record) lee args **y** variables de entorno (`BOLSINES_DB`, `BOLSINES_PUERTO`). | 🟢 |
| B5 | `PantallaRecepcionConsola.solicitarSeleccionBolsin` (`:57-99`): `while` con 7 ramas, CC ≈ 12. | Separar interpretar de ejecutar: `ComandoDeConsola` como `sealed interface` (`Cancelar`, `VerTodos`, `FiltrarPrecinto(n)`, `FiltrarOrigen(texto)`, `Seleccionar(n)`, `Invalido`) y un `switch` con pattern matching (Java 21). | 🟢 |
| B6 | HTTP: `HttpServer` con executor por defecto + `synchronized(this)` global; `estado()` relee todos los empleados en **cada** respuesta (`:178-188`); validación ad hoc `contains("..")` (`:347`). | `Executors.newVirtualThreadPerTaskExecutor()`; cachear empleados por sesión; normalizar la ruta contra la raíz estática; `Runtime.addShutdownHook` para `servidor.stop`. | 🟢 |

### 3.4 Estilo idiomático Java 21 (aplicar mientras se toca cada archivo)

`record` para DTO/value objects; `sealed` + pattern matching en comandos y resultados; `Stream.toList()`; `List.copyOf` en constructores; getters `numero()` para *records* y `getNumero()` en entidades (se deja por compatibilidad con el diagrama); `System.Logger` del JDK en lugar de `System.out` para diagnóstico (sin dependencias nuevas); `-Xlint:all` en el compilador.

---

## 4. Persistencia y datos de inicialización

### 4.1 Problemas actuales

| Problema | Ubicación |
|---|---|
| `BaseDeDatos` (302 líneas) tiene 4 razones para cambiar: conexión, DDL/seed, lectura-mapeo y escritura. | `BaseDeDatos.java` |
| Cada lectura hace ~9 `SELECT *` sin `WHERE`, y abre una conexión nueva por operación. | `:79-90`, `:272` |
| Se **borra y reinserta todo el historial** de cada bolsín y documentación al guardar (se pierden los `id`, no es idempotente). | `:227-231`, `:258-267` |
| `REFERENCES` declaradas pero **SQLite no las aplica** sin `PRAGMA foreign_keys=ON`. | `Esquema.java` + `abrirConexion` |
| Sin índices ni `CHECK` en `estado`; el estado es texto libre que coincide con la etiqueta de UI. | `Esquema.TABLAS` |
| Script SQL partido con `split(";")`: se rompe con un `;` dentro de un texto. | `BaseDeDatos.java:278` |
| DDL y datos de prueba viven en *text blocks* Java (`Esquema`), con contraseñas en texto plano y fechas fijas de noviembre-2026. | `Esquema.java:9-130` |
| Los tests dependen de números mágicos del seed (2001, 6002…): tocar los datos de demo rompe los tests. | `RegistrarRecepcionBolsinTest` |
| Sin control de concurrencia: dos EB pueden recibir el mismo bolsín (el chequeo de estado es solo en memoria). | `guardarRecepcion` |

### 4.2 Diseño propuesto (sigue siendo JDBC puro — compatible con el informe)

1. **`ConexionSqlite`**: única fuente de conexiones. Activa `foreign_keys` con `org.sqlite.SQLiteConfig`, y ofrece una unidad de trabajo `<T> T enTransaccion(Function<Connection, T> trabajo)` (commit/rollback centralizados; hoy están a mano en `:222-252`).
2. **`MigradorDeEsquema`**: aplica `db/migraciones/V00N__*.sql` en orden y registra cada una en la tabla `esquema_version`. Lista explícita de archivos (leer un directorio del classpath dentro de un jar es frágil). Un lector de scripts que respeta comentarios y `;` dentro de comillas reemplaza el `split(";")`.
3. **Datos de prueba fuera de Java**: `db/datos/datos-de-prueba.sql`. Se carga **en el primer arranque** (el informe afirma «al iniciar carga automáticamente datos de prueba»; no se pierde ese comportamiento) y se puede desactivar con `--sin-datos-de-prueba`. Contraseñas con hash (`PBKDF2` de `javax.crypto`, sin librerías) aunque el login sea simulado.
4. **Repositorios** (adaptadores del §2.2): `RepositorioDeBolsinesSqlite` y `RepositorioDeEmpleadosSqlite`, con `RowMapper` como lambdas chicas. `RegistroDeEstados<E>` reemplaza los tres métodos copiados de `MapeoEstados` por uno genérico `Map<String, Supplier<E>>` indexado por `codigo()` (E7).
5. **Historial append-only con *upsert***: en vez de `DELETE` + `INSERT`, clave natural única y actualización solo de `fecha_hora_fin`:

```sql
-- V002__indices_y_restricciones.sql
CREATE UNIQUE INDEX ux_ce_bolsin ON cambio_estado_bolsin(numero_bolsin, fecha_hora_inicio);
CREATE UNIQUE INDEX ux_ce_doc    ON cambio_estado_documentacion(numero_documentacion, fecha_hora_inicio);
CREATE INDEX ix_bolsin_destino   ON bolsin(cm_destino, estado);
CREATE INDEX ix_remito_bolsin    ON remito(numero_bolsin);
CREATE INDEX ix_detalle_remito   ON detalle_remito(numero_remito);

-- al guardar (por cada cambio del historial)
INSERT INTO cambio_estado_bolsin (numero_bolsin, fecha_hora_inicio, fecha_hora_fin, estado, responsable_email)
VALUES (?, ?, ?, ?, ?)
ON CONFLICT(numero_bolsin, fecha_hora_inicio) DO UPDATE SET fecha_hora_fin = excluded.fecha_hora_fin;
```
6. **Concurrencia optimista**: columna `version` en `bolsin`; `UPDATE … WHERE numero_bolsin = ? AND version = ?` y si afectó 0 filas → `ConflictoDeConcurrenciaException`. Protege el escenario «dos encargados reciben el mismo bolsín».
7. **`CHECK(estado IN (...))`** en las tres tablas con los `codigo()` válidos; falla en la base si aparece un estado inexistente.
8. **Fixtures de test en Java, no en SQL**: `BolsinBuilder`/`EscenariosDeRecepcion` (Object Mother) para tests de dominio y de aplicación; el SQL de demo queda solo para la app y para un par de tests de integración.

**Alternativas modernas y por qué no ahora:** Spring Data JDBC / JPA / jOOQ / Flyway rompen la declaración «sin frameworks, JDBC puro» del informe ya presentado. Si más adelante la cátedra lo permite, los puertos del §2.2 hacen que el cambio sea reemplazar los adaptadores `*Sqlite` sin tocar dominio ni aplicación (también es lo que habilitaría el RNF 2/3 de la ERS: base espejada en la nube).

---

## 5. Tests

### 5.1 Hallazgos del test actual

`RegistrarRecepcionBolsinTest` (257 líneas, 11 tests) es una buena **red de seguridad de caracterización**, pero:

- Todos los tests son de punta a punta (consola con entrada simulada + SQLite) → lentos, y un fallo no dice *dónde* se rompió.
- Aserciones sobre texto de pantalla (`salida.contains("Recepción registrada con éxito")`) y sobre etiquetas de estado (`getNombre()`).
- Un test hace varias cosas (`web_mismoCasoDeUsoPorHttp` ≈ 14 aserciones; `a1yA2…` mezcla dos flujos).
- **No hay ningún test unitario de un estado concreto**, ni de la matriz «qué estados rechazan qué evento».
- **Sin cobertura para** la atomicidad (`README`: «una falla a mitad de camino no deja nada a medias»), para la guarda A5 dentro de `Enviado` (el Gestor la intercepta antes), ni para historial/mail con más de una CM de origen.
- Tiempo real (`LocalDateTime.now()`), acoplamiento a números del seed.

### 5.2 Estructura propuesta

```
src/test/java/bolsines
├── dominio/
│   ├── bolsin/estado/     EnviadoTest, EstadosDeBolsinTest              (matriz de transiciones)
│   ├── remito/estado/     EnBolsinEnviadoRemitoTest
│   ├── documentacion/estado/ EnBolsinEnviadoDocumentacionTest, EstadosDeDocumentacionTest
│   └── recepcion/         OpcionRecepcionTest
├── aplicacion/recepcion/  GestorRecepcionFlujoPrincipalTest
│                          GestorRecepcionFlujosAlternativosTest   (@Nested A1…A6)
│                          ValidadorDeOpcionDeRecepcionTest
├── infraestructura/
│   ├── persistencia/      RepositorioDeBolsinesSqliteIT, MigradorDeEsquemaIT
│   ├── entrada/web/       ApiRecepcionWebIT
│   └── entrada/consola/   InterpreteDeComandosTest
├── aceptacion/            RegistrarRecepcionDeBolsinE2ETest          (los 11 actuales, migrados)
├── arquitectura/          ReglasDeArquitecturaTest                   (ArchUnit)
└── soporte/               BolsinBuilder, RepositorioDeBolsinesEnMemoria, PantallaRecepcionEspia,
                           ServidorCorreoEspia, RelojFijo
```

Pirámide: ~60 % dominio/aplicación (milisegundos, sin I/O), ~25 % integración (SQLite/HTTP), ~15 % E2E. Fakes escritos a mano (sin Mockito: coherente con «sin frameworks» y más legibles en una defensa).

### 5.3 Convención Given-When-Then

- **Método:** `dado<Contexto>_cuando<Acción>_entonces<Resultado>` en camelCase, en español como el resto del código.
- **`@DisplayName`** con la frase natural.
- **Cuerpo** con comentarios `// Given`, `// When`, `// Then`.
- **Un solo motivo de fallo por test**; varias aserciones sobre el mismo resultado se agrupan con `assertAll`.
- `@Nested` por escenario (`FlujoPrincipal`, `A3_DocumentacionNoRecibida`, …).
- Aserciones sobre el dominio (`assertInstanceOf(RecibidoEnCMDestino.class, …)` o `codigo()`), no sobre texto de pantalla.

**Mapa de migración de los tests actuales:**

| Hoy | Pasa a |
|---|---|
| `flujoPrincipal_todoQuedaRecibidoYAceptadoConTrazabilidad` | 3 tests: `dadoBolsinEnviadoParaMiCM_cuandoElEBConfirmaLaOpcion1_entoncesBolsinRemitosYDocumentacionQuedanAceptados` · `…_entoncesElHistorialCierraElEstadoAnteriorYAbreElNuevo` · `…_entoncesNotificaACadaCMDeOrigen` |
| `listado_soloBolsinesEnviadosALaCMDelUsuario` | `dadoBolsinesDeVariosDestinosYEstados_cuandoElEBInicia_entoncesListaSoloLosEnviadosASuCM` |
| `a1yA2_…` | `A1_dadoPrecintoInexistente_cuandoFiltra_entoncesInformaYPermiteSeguir` + `A2_…CMOrigenInexistente…` |
| `a3_…`, `a4_…` | `A3_dadaDocumentacionNoRecibida_…_entoncesElRemitoQuedaParcial`, `A4_dadaDocumentacionQueNoCorresponde_…_entoncesQuedaRechazada` |
| `a5_laCMCRedirige…`, `a5_unaCMQueNoEsLaCentral…` | `A5_dadaLaCMCComoDestino_…_entoncesQuedaParaRedirigir`, `A5_dadaUnaCMNoCentral_…_entoncesLaOpcionNoEstaDisponible` |
| `a6_noConfirmar…` | `A6_dadoQueElEBNoConfirma_cuandoSeVuelveAPedirLaOpcion_entoncesNoSeRegistraNada` |
| `validaciones_…` | 2 tests: `…SinDocumentacionMarcada…` y `…DocumentacionDeOtroBolsin…` |
| `patronState_unBolsinYaRecibido…` | `EstadosDeBolsinTest` (parametrizado) + `EstadosDeDocumentacionTest` |
| `web_mismoCasoDeUsoPorHttp` | `ApiRecepcionWebIT`: un test por endpoint/flujo |

### 5.4 Ejemplos

**Matriz del patrón State** (la mejor evidencia en la defensa de que «las demás transiciones están rechazadas por diseño»):

```java
class EstadosDeBolsinTest {

    static Stream<EstadoBolsin> estadosQueNoAdmitenRecepcion() {
        return Stream.of(new Creado(), new Cerrado(), new RecibidoEnCMDestino(), new DeBaja());
    }

    @ParameterizedTest(name = "estado {0}")
    @MethodSource("estadosQueNoAdmitenRecepcion")
    @DisplayName("Dado un bolsín que no está Enviado, cuando se registra la recepción, entonces la transición no está permitida")
    void dadoBolsinNoEnviado_cuandoSeRegistraLaRecepcion_entoncesLaTransicionNoEstaPermitida(EstadoBolsin estado) {
        // Given
        Bolsin bolsin = unBolsin().en(estado).build();
        // When / Then
        assertThrows(TransicionNoPermitidaException.class,
                () -> bolsin.registrarBolsinRecibido(AHORA, EB, bolsin.buscarCambioEstadoActual(), CONTENIDO_IGUAL, Set.of()));
    }
}
```

**Caso de uso con fakes, sin base ni consola:**

```java
@Test
void dadoBolsinEnviadoParaMiCM_cuandoElEBConfirmaLaOpcion1_entoncesQuedaRecibidoYSeGuarda() {
    // Given
    Bolsin bolsin = unBolsin().enviado().paraDestino(CORDOBA).conRemito(unRemito().conDocumentaciones(6001, 6002)).build();
    var repositorio = new RepositorioDeBolsinesEnMemoria(bolsin);
    var correo = new ServidorCorreoEspia();
    var gestor = new GestorRecepcion(repositorio, empleados(CAROLINA), sesionDe(CAROLINA), correo, RELOJ_FIJO);
    var pantalla = new PantallaRecepcionEspia();

    // When
    gestor.opcRegistrarRecepcion(pantalla);
    gestor.tomarSeleccionBolsin(bolsin.getNumeroBolsin());
    gestor.tomarOpcRecepcion(CONTENIDO_IGUAL, Set.of());
    gestor.tomarConfirmacion(true);

    // Then
    assertAll(
        () -> assertInstanceOf(RecibidoEnCMDestino.class, bolsin.getEstadoActual()),
        () -> assertEquals(List.of(bolsin), repositorio.guardados()),
        () -> assertEquals(1, correo.enviados().size()));
}
```

**Atomicidad** (hoy afirmada en el README pero sin test):

```java
@Test
void dadaUnaFallaAlGuardarUnaDocumentacion_cuandoSeGuardaLaRecepcion_entoncesNoQuedaNadaAMedias() {
    // Given: una restricción que fuerza el fallo al insertar el historial de la 2.ª documentación
    // When: guardarRecepcion(bolsin)
    // Then: assertThrows(...) y el bolsín, los remitos y la 1.ª documentación siguen como estaban al releer
}
```

### 5.5 Build de tests

- Tag `@Tag("integracion")` / `@Tag("e2e")` y perfiles Maven: `mvn test` (solo dominio+aplicación, segundos) y `mvn verify -Pcompleto`. Sin plugins nuevos de runtime.
- Dependencias **solo de test**: `archunit-junit5` (1.3.0 o superior) y, opcional, `jacoco-maven-plugin`. No alteran la declaración «sin frameworks» del informe (no entran al artefacto de ejecución).

### 5.6 Reglas de arquitectura (ArchUnit)

```java
@AnalyzeClasses(packages = "bolsines", importOptions = ImportOption.DoNotIncludeTests.class)
class ReglasDeArquitecturaTest {

    @ArchTest
    static final ArchRule dominioSinDependenciasExternas =
            noClasses().that().resideInAPackage("..dominio..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..aplicacion..", "..infraestructura..", "java.sql..", "com.sun.net.httpserver..");

    @ArchTest
    static final ArchRule aplicacionNoConoceLaInfraestructura =
            noClasses().that().resideInAPackage("..aplicacion..")
                    .should().dependOnClassesThat().resideInAPackage("..infraestructura..");
}
```

---

## 6. Hoja de ruta por fases

Cada fase deja `mvn test` en verde y el programa ejecutable (requisito de la defensa). Cada refactor **empieza escribiendo/migrando el test** que lo protege.

| Fase | Objetivo | Contenido | Criterio de salida | Riesgo | Cuándo |
|---|---|---|---|---|---|
| **0 — Red de seguridad** | Poder volver atrás y saber que todo estaba verde | Instalar/seleccionar **JDK 21** y correr `mvn test` (hoy no verificado). `maven-enforcer-plugin` con `requireJavaVersion [21,)` para fallar con mensaje claro. `git tag entrega-2-defensa` sobre el commit actual y rama `refactor/arquitectura`. Mover los PDFs sin trackear a `docs/` (o `.gitignore`). Medir CC con PMD/Checkstyle como línea base. | 11 tests verdes en JDK 21; tag creado | nulo | ahora |
| **1 — Higiene invisible al modelado** | Bajar complejidad sin tocar nada del diagrama | E2, E3, E4, E5, E7, E8, E9, E10 · G1, G2, G5, G6, G7, G8 · B1–B6 · `Clock` · `record`s para DTO | Todas las firmas y nombres de la tabla §0 intactos; tests de caracterización verdes; ningún método > CC 10 | bajo | **antes de la defensa** (si hay tiempo) |
| **2 — Persistencia** | Aislar y endurecer la persistencia | §4: puertos + adaptadores, `ConexionSqlite`, `MigradorDeEsquema`, seeds a `.sql`, FK activas, índices/`CHECK`, historial *upsert*, `version` optimista | Gestor solo conoce interfaces; test de atomicidad y de round-trip en verde; arranque sin configuración idéntico | medio (toca guardado) | antes de la defensa **solo si** Fase 1 quedó estable; si no, después |
| **3 — Pirámide de tests** | Tests rápidos, legibles y GWT | §5: fixtures, fakes, matriz de State, tests de caso de uso, IT de persistencia/web, E2E migrados, ArchUnit | `mvn test` < 10 s; los 11 originales migrados sin pérdida de cobertura; +atomicidad, +A5 en `Enviado` | bajo | en paralelo a 1 y 2 |
| **4 — Paquetes hexagonales** | Estructura §2.1 | Mover `entity`→`dominio.*`, `control`→`aplicacion`, `boundary`→`infraestructura.entrada`, `persistencia`→`infraestructura.persistencia`. Es mecánico (IDE «Move») y **no cambia ningún nombre de clase**. Actualizar README, `package-info.java`, `guia-exposicion.html` (sus números de línea se desfasan con cualquier cambio). | ArchUnit en verde; README con el mapa BCE ↔ hexagonal | bajo para el modelado, **alto para el material de defensa** | después de la defensa (o acordado con el tutor) |
| **5 — Cambios que tocan el diagrama** | Cerrar la deuda que sí está en el modelado | E1 (`RegistroDeRecepcion`), E6 (`CambioDeEstado<E>`), E11 (`setEstado` package-private), E12 (historial de Remito: corregir informe o implementarlo) | Informe y diagramas regenerados y consistentes con el código | medio (evaluado) | **después de la defensa**, en la misma entrega documental |
| **6 — Nivel producción** | Operabilidad | `Configuracion` por entorno, `System.Logger`, `SesionesWeb` multiusuario, executor de hilos virtuales, endpoint de salud, `Dockerfile`, CI (`mvn -B verify`), Checkstyle/Spotless con tope de CC, JaCoCo | CI verde; 2 sesiones simultáneas sin interferencia | bajo | después |

**Estimación relativa:** F0 S · F1 M · F2 M · F3 M · F4 S · F5 M · F6 M.

---

## 6 bis. Impacto sobre los diagramas entregados

**Fases 0 a 4 (todo lo 🟢): no cambia ningún diagrama.** Ni el de clases de diseño, ni el de secuencia de diseño, ni los de análisis, ni la máquina de estados. Las clases nuevas (repositorios, validador, notificador, DTO, `Clock`) son *detalle de implementación detrás* de lo modelado y no figuran en ninguno; los diagramas ya omiten la persistencia y la web. Los paquetes tampoco aparecen en los diagramas, así que mover carpetas (Fase 4) no los toca.

**Fase 5 (lo 🟡): sí hay que redibujar.**

| Cambio | Diagrama de clases de diseño | Diagrama de secuencia de diseño |
|---|---|---|
| E1 `RegistroDeRecepcion` | Clase nueva (`<<record>>`) y firmas nuevas en `Bolsin`, `Enviado`, `Remito`, `EnBolsinEnviado` (Remito) y `DetalleRemito` | Cambian los parámetros de 5 mensajes `registrarBolsinRecibido` (Gestor→Bolsin, Bolsin→Enviado, Enviado→Remito, Remito→EnBolsinEnviado, EnBolsinEnviado→DetalleRemito). La estructura de llamadas no cambia. |
| E6 `CambioDeEstado<E>` | Clase abstracta nueva + flechas de generalización; los 4 atributos pasan de `CambioEstadoBolsin` / `CambioEstadoDocumentacion` a la superclase | Sin cambios |
| E11 `setEstado` / `agregarCambioEstado` package-private | Visibilidad `+` → `~` en `Bolsin`, `Remito`, `Documentacion` | Sin cambios |
| E12 si se implementa `CambioEstadoRemito` | Clase nueva + asociación + `agregarCambioEstado` en `Remito` | `EnBolsinEnviado` (Remito) suma los mismos pasos que el de Documentación (`buscarCambioEstadoActual`, `setFechaHoraFin`, `crearCambioEstado…`, `new`, `agregarCambioEstado`) |
| E12 si solo se corrige el informe | Sin cambios | Sin cambios |

**Desfases que ya existen hoy, independientes del plan** (el código tiene más parámetros y métodos que el diagrama):

- `opcion` y `documentacionMarcada` no figuran en las firmas de `Bolsin`, `Remito` y `DetalleRemito`; el código las tiene desde que se implementaron A3–A5.
- `Documentacion` tiene en el código `registrarDocumentacionNoRecibida`, `registrarRechazoDeDocumentacion` y `registrarDocumentacionParaRedirigir`; en el diagrama esos métodos solo figuran en `EstadoDocumentacion`.
- El diagrama de secuencia de diseño modela únicamente el flujo principal; A3–A5 no tienen secuencia propia.

Conviene resolverlos juntos con E1 al regenerar los diagramas, para que el informe quede consistente con el código de una sola vez.

---

## 6 ter. Estado de avance (rama `refactor/arquitectura`, sin commitear)

**Fase 0:** hecha. Tag `entrega-2-defensa` sobre `8d3396c` y rama `refactor/arquitectura`. La línea base se verificó con `mvn -Dmaven.compiler.release=17 clean test` (11/11 en verde) porque esta PC no tiene JDK 21; el código no usa nada exclusivo de 21. **Falta** correr `mvn test` con JDK 21 real, el `maven-enforcer` y medir complejidad ciclomática con PMD/Checkstyle.

**Fase 1, hecho:** E2, E3, E4, E8, E9, E10 (E5 se aplicó y después se deshizo en la revisión: ver más abajo) · G1, G2, G5, G6, G7, G8 · B1, B3, B4, B5 · nuevo `Configuracion` por variables de entorno · `ParametrosHttp` · logging con `System.Logger` · 95 tests nuevos (106 en total).
**Fase 1, diferido a propósito:**

| Ítem | Por qué |
|---|---|
| E7 `codigo()` de los estados | Solo tiene sentido junto con el migrador de la Fase 2: cambiar lo que se guarda en la columna `estado` rompe las bases `data/bolsines.db` ya creadas. |
| B2 una sesión web por usuario | Cambia el comportamiento (hoy un solo usuario a la vez) y el test web actual no maneja cookies. Se dejó preparado: todo el estado del servidor vive en `SesionWeb`. Pasa a la Fase 6. |
| B6 hilos virtuales / ajustes de executor | Requiere JDK 21. |
| G3 `PasoDeRecepcion` y G4 repositorios por puertos | Son la Fase 2 (G4) y tocan los atributos del Gestor del diagrama (G3). |
| `switch` con *pattern matching* en la consola | Es de Java 21; hoy `ComandoDeSeleccion` usa `sealed` + `instanceof` (Java 17). |

**Cambios visibles:** la consulta de estados por consola (opción 2 del menú) ahora muestra «Nombre Apellido» del responsable (antes solo el apellido), porque comparte el servicio `ConsultarEstadosGuardados` con la web. Errores inesperados del servidor web ahora responden 500 con mensaje genérico y quedan en el log; los de regla de negocio y pedidos mal formados siguen siendo 400.

**Material de defensa:** `guia-exposicion.html` cita números de línea de `GestorRecepcion`, `Bolsin`, `DetalleRemito`, `EnBolsinEnviado` y otras; con estos cambios quedan desfasados.

### Fases 2 a 6 (segunda tanda), con el criterio «pegarse a las consignas y que se entienda»

**Fase 2 — Persistencia, hecha:**
- `RepositorioDeBolsines` y `RepositorioDeEmpleados` (interfaces en `control`); `BaseDeDatos` las implementa y **conserva su nombre** (lo usan README y guía).
- `BaseDeDatos` se repartió en `ConexionSqlite` (FK activas, transacciones), `MigradorDeEsquema` (`esquema_version`), `LectorDeBolsines` y `GrabadorDeRecepcion`. `Esquema.java` pasó a `resources/db/migraciones/*.sql` y `resources/db/datos/datos-de-prueba.sql`.
- Historial: se cierra el cambio vigente y se agrega el nuevo (ya no se borra y reinserta).
- Control de concurrencia: `UPDATE … WHERE estado = <el leído>` → `BolsinModificadoPorOtroUsuarioException`.
- `BOLSINES_DATOS_DE_PRUEBA=no`.
- Verificado sobre una copia de la `data/bolsines.db` real creada por la versión anterior: migra sola, sin perder ni duplicar datos.

**Fase 2 — cambios respecto de lo planeado (para que se entienda mejor):**

| Planeado | Qué se hizo y por qué |
|---|---|
| `buscarPorDestino(cm)` filtrando en SQL | Se mantiene `buscarBolsines()` y el Gestor filtra con `esTuCMDestino()`/`sosEnviado()`: es el bucle del diagrama de secuencia de análisis. |
| *Upsert* con `ON CONFLICT` por (entidad, fecha de inicio) | Depende del formato del texto de la fecha (la base guarda `08:30:00`, Java escribe `08:30`) y duplicaría filas. Se reemplazó por «cerrar el vigente y agregar el nuevo», más simple y sin esa trampa. |
| Columna `version` para concurrencia | Alcanza con comparar el estado leído: no agrega columnas ni conceptos. |
| `CHECK (estado IN …)` | Obligaría a tocar SQL cada vez que se agrega un estado; `MapeoEstados` ya rechaza nombres desconocidos al leer. |
| E7 `codigo()` estable | Dos identificadores por estado para explicar en la defensa; el nombre de clase y `getNombre()` están congelados por el diagrama. |
| Contraseñas con hash | El login es simulado (CU 5 fuera de alcance) y la contraseña no se verifica en ningún lado. |
| G3 `PasoDeRecepcion` dentro del Gestor | Cambia los atributos del Gestor del diagrama; el paso lo sigue controlando `SesionWeb.exigirPaso`. |

**Revisión final antes de commitear:**
- Se comparó, con un script, cada método y atributo de las clases que figuran en los diagramas contra el tag `entrega-2-defensa`: no se perdió ni se renombró ninguno.
- Compilación con `-Xlint:all`: sin advertencias nuevas; quedan los avisos `serial` de las excepciones, que ya existían.
- No hay imports sin usar.
- **E5 se deshizo.** El helper `HistorialDeEstados` y la interfaz `CambioDeEstado` escondían detrás de `vigente(...).map(...).orElse(false)` el bucle `*sosActual()` que dibuja el diagrama de secuencia. Ahorrar seis líneas repetidas no compensa perder la correspondencia directa con el diagrama.
- `LectorDeBolsines` se renombró a `LectorDeTablas`, porque también lee empleados.
- Se corrigieron en el README la firma `solicitarOpcRecepcion(cmEmpleado)`, el fragmento de `EstadoBolsin` y la nota sobre `MapeoEstados` al agregar un estado.

**Fase 3 — Tests, hecha:** 145 tests en verde (con `release=17`).
- Los 11 originales se migraron a `aceptacion/RegistrarRecepcionDeBolsinTest` (Given-When-Then, un `@Nested` por flujo) y a `web/ApiRecepcionWebTest` (navegadores con cookies; incluye dos usuarios a la vez y el mismo bolsín recibido por dos).
- Tests nuevos:
  - `control/GestorRecepcionTest`, con repositorio en memoria, pantalla espía y reloj fijo.
  - `persistencia/BaseDeDatosTest`: migraciones, base de la versión anterior, ida y vuelta, atomicidad y concurrencia.
  - `persistencia/ScriptSqlTest`.
  - `ArquitecturaTest`, que revisa los `import` sin agregar ArchUnit.
- Prueba de mutación manual: anulando el control de concurrencia fallan exactamente los dos tests que lo cubren.

**Fase 4 — adaptada, hecha:** **no** se renombraron `boundary/control/entity` a `dominio/aplicacion/infraestructura`, porque es el vocabulario de las consignas, del informe y de los diagramas. En su lugar, cada paquete tiene un `package-info.java` que explica su rol BCE y su equivalente hexagonal, y `ArquitecturaTest` hace cumplir las dependencias.

**Fase 5 — no se aplica.** E1 (objeto parámetro), E6 (`CambioDeEstado<E>` como superclase), E11 (`setEstado` package-private) y E12 (historial de Remito) cambian firmas, jerarquías o visibilidades que figuran en el diagrama de clases y de secuencia ya entregados. La consigna evalúa la consistencia con ese modelado, así que el código sigue al diagrama. E12 se resuelve corrigiendo la frase del informe, no el código: el modelo de dominio de la cátedra no define historial de Remito.

**Fase 6 — Producción, hecha en parte:**
- Una sesión web por navegador (cookie `sesion`).
- Logging con `System.Logger`.
- Configuración por entorno.
- CI de GitHub Actions con JDK 21 (`.github/workflows/ci.yml`).

**No hecho, a propósito:**
- Hilos virtuales: requieren JDK 21 y, sin executor, los pedidos se atienden en orden, lo que evita carreras.
- Dockerfile y jar ejecutable: requieren plugins de empaquetado y no los pide la consigna.
- Checkstyle, JaCoCo y enforcer: agregan plugins y configuración al `pom` sin aportar a la defensa.
- Endpoint de salud: sin Docker ni orquestador no tiene quién lo consulte.

---

## 7. Riesgos y decisiones abiertas

1. **Fecha de la defensa:** no figura en los documentos (la fija el docente tutor). Define cuánto de Fase 1–2 es prudente hacer antes. Regla práctica: no mergear a `main` nada que no tenga el programa corriendo y los tests en verde 48 h antes.
2. **Nombre del evento principal de Documentación — inconsistencia heredada de los diagramas:** la máquina de estados entregada rotula la transición `En Bolsín Enviado → Recibida y Aceptada` como `registrarRecepcionDeDocumentacion()`, mientras el diagrama de clases/secuencia de diseño y el código usan `registrarBolsinRecibido()` (y en el código `registrarRecepcionDeDocumentacion()` figura como stub). No es un problema del código; conviene tener la respuesta preparada por si lo preguntan.
3. **Informe vs código (historial de Remito):** ver E12.
4. **Material de defensa:** `guia-exposicion.html` cita líneas del código actual; cualquier fase las desfasa. Regenerarla al final de cada fase que toque `control/`, `entity/` o `persistencia/`.
5. **Pruebas manuales:** tras la Fase 1 correr la web (`mvn -q compile exec:java`) y la consola con los usuarios `cmendez` y `hrojas` para los flujos principal, A3, A4, A5 y A6.

## 8. Qué NO hacer

- Spring/JPA/Hibernate/Lombok/Mockito: contradicen «sin frameworks» del informe y ocultan justo lo que hay que mostrar en la defensa.
- Un `…UseCase` por cada método del Gestor, o mover `GestorRecepcion` a otro nombre.
- Renombrar `sosEnviado`, `esTuCMDestino`, `tomarOpcRecepcion`, `registrarBolsinRecibido`, etc.
- Reemplazar el `new RecibidoYAceptada()` de las transiciones por singletons/enums: el diagrama de secuencia muestra el `new()` explícito.
- Reescribir los 11 tests existentes **antes** de terminar Fases 1–2: son la red que prueba que el refactor no cambió el comportamiento.
