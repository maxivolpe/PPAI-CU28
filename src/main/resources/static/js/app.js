// Interfaz web del CU 28 "Registrar recepción de bolsín".
//
// La página no tiene lógica de negocio: le manda al servidor lo que hace el EB y dibuja la
// "vista" que el servidor devuelve (lo que el GestorRecepcion le pidió mostrar a
// PantallaRecepcionWeb). El campo vista.paso dice qué parte del caso de uso mostrar.
//
// Los archivos de js/ se cargan desde index.html en orden y comparten el ámbito global
// (no hay bundler ni Node): constantes -> utilidades -> api -> hooks -> ui -> vistas -> app.

function App() {
    const { aviso, avisar, cerrarAviso } = useAviso();
    const { estado, emails, error, ocupado, enviar, reintentar } = useServidor(avisar);
    const [pestania, setPestania] = useState(PESTANIAS.recepcion);

    if (error) {
        return (
            <main id="contenido" tabIndex={-1}>
                <ErrorCarga mensaje={error.message} alReintentar={reintentar} />
            </main>
        );
    }
    if (!estado) {
        return <Cargando />;
    }

    const { empleados, sesion, vista } = estado;

    return (
        <>
            <Cabecera empleados={empleados} sesion={sesion}
                      alElegirUsuario={(usuario) => enviar(RUTAS.sesion, { usuario })} />
            <Pestanias activa={pestania} alCambiar={setPestania} />

            <main id="contenido" tabIndex={-1}>
                {pestania === PESTANIAS.consulta && <ConsultaEstados />}
                {pestania === PESTANIAS.recepcion && !sesion && <SinSesion />}
                {pestania === PESTANIAS.recepcion && sesion && (
                    <Recepcion vista={vista} emails={emails} enviar={enviar} ocupado={ocupado} avisar={avisar} />
                )}
            </main>

            {aviso && <Aviso aviso={aviso} cerrar={cerrarAviso} />}
        </>
    );
}

function Cabecera({ empleados, sesion, alElegirUsuario }) {
    return (
        <header className="cabecera">
            <div>
                <h1>Bolsines</h1>
                <p>
                    {sesion
                        ? `${sesion.nombre} · Encargado de Bolsines · ${sesion.cm}`
                        : "Sin sesión iniciada"}
                </p>
            </div>
            <label className="usuario">
                Usuario
                <select value={sesion ? sesion.usuario : ""}
                        onChange={(e) => alElegirUsuario(e.target.value)}>
                    <option value="" disabled>Elegir…</option>
                    {empleados.map((empleado) => (
                        <option key={empleado.usuario} value={empleado.usuario}>
                            {empleado.nombre} ({empleado.cm})
                        </option>
                    ))}
                </select>
            </label>
        </header>
    );
}

function Pestanias({ activa, alCambiar }) {
    const pestanias = [
        { id: PESTANIAS.recepcion, titulo: "Registrar recepción" },
        { id: PESTANIAS.consulta, titulo: "Consultar estados" },
    ];
    return (
        <nav className="pestanias" aria-label="Secciones">
            {pestanias.map((pestania) => (
                <button key={pestania.id} className={clases(activa === pestania.id && "activa")}
                        aria-current={activa === pestania.id ? "page" : undefined}
                        onClick={() => alCambiar(pestania.id)}>
                    {pestania.titulo}
                </button>
            ))}
        </nav>
    );
}

function SinSesion() {
    return (
        <section className="tarjeta">
            <p>Elegí un usuario arriba para iniciar sesión.</p>
        </section>
    );
}

ReactDOM.createRoot(document.getElementById("raiz")).render(<App />);
