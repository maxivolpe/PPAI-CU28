// Interfaz web del CU 28 "Registrar recepción de bolsín".
//
// La página no tiene lógica de negocio: le manda al servidor lo que hace el EB y dibuja
// la "vista" que el servidor devuelve (lo que el GestorRecepcion le pidió mostrar a
// PantallaRecepcionWeb). El campo vista.paso dice qué parte del caso de uso mostrar.

const { useState, useEffect } = React;

const PASOS = [
    { id: "seleccionBolsin", titulo: "Elegir bolsín" },
    { id: "opcion", titulo: "Controlar contenido" },
    { id: "confirmacion", titulo: "Confirmar" },
    { id: "exito", titulo: "Resultado" },
];

/** Hace un pedido al servidor y devuelve la respuesta. Si el servidor rechaza, lanza el motivo. */
async function pedir(metodo, ruta, parametros) {
    const consulta = parametros ? "?" + new URLSearchParams(parametros) : "";
    const respuesta = await fetch(ruta + consulta, { method: metodo });
    const datos = await respuesta.json();
    if (!respuesta.ok) {
        throw new Error(datos.error);
    }
    return datos;
}

function App() {
    const [estado, setEstado] = useState(null);       // { empleados, sesion, vista }
    const [emails, setEmails] = useState([]);         // emails del CU 29 de la última recepción
    const [pestania, setPestania] = useState("recepcion");
    const [aviso, setAviso] = useState(null);         // pop-up: { tipo, titulo, texto }

    useEffect(() => {
        pedir("GET", "/api/estado").then(setEstado);
    }, []);

    /** Envía una acción del EB y redibuja con la vista nueva. Los errores van al pop-up. */
    async function enviar(ruta, parametros) {
        try {
            const nuevo = await pedir("POST", ruta, parametros);
            setEstado(nuevo);
            setEmails(nuevo.emails || []);
            if (nuevo.vista.mensaje) {
                setAviso({ tipo: "info", titulo: "Aviso", texto: nuevo.vista.mensaje });
            }
        } catch (error) {
            setAviso({ tipo: "error", titulo: "No se pudo continuar", texto: error.message });
        }
    }

    if (!estado) {
        return <p className="cargando">Cargando…</p>;
    }
    const { empleados, sesion, vista } = estado;

    return (
        <div>
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
                            onChange={(e) => enviar("/api/sesion", { usuario: e.target.value })}>
                        <option value="" disabled>Elegir…</option>
                        {empleados.map((empleado) => (
                            <option key={empleado.usuario} value={empleado.usuario}>
                                {empleado.nombre} ({empleado.cm})
                            </option>
                        ))}
                    </select>
                </label>
            </header>

            <nav className="pestanias">
                <button className={pestania === "recepcion" ? "activa" : ""}
                        onClick={() => setPestania("recepcion")}>Registrar recepción</button>
                <button className={pestania === "consulta" ? "activa" : ""}
                        onClick={() => setPestania("consulta")}>Consultar estados</button>
            </nav>

            <main>
                {pestania === "consulta" && <ConsultaEstados />}
                {pestania === "recepcion" && !sesion && (
                    <section className="tarjeta">
                        <p>Elegí un usuario arriba a la derecha para iniciar sesión.</p>
                    </section>
                )}
                {pestania === "recepcion" && sesion && (
                    <Recepcion vista={vista} emails={emails} enviar={enviar} setAviso={setAviso} />
                )}
            </main>

            {aviso && <Aviso aviso={aviso} cerrar={() => setAviso(null)} />}
        </div>
    );
}

// ------------------------------------------------------------------ caso de uso 28

function Recepcion({ vista, emails, enviar, setAviso }) {
    const cancelar = () => enviar("/api/recepcion/cancelar");

    if (vista.paso === "inicio") {
        return (
            <section className="tarjeta centrada">
                <h2>Recepción de bolsines</h2>
                <p>Registrá los bolsines que el correo entregó en tu comisión médica.</p>
                <button className="principal" onClick={() => enviar("/api/recepcion/iniciar")}>
                    Registrar recepción de bolsín
                </button>
            </section>
        );
    }

    return (
        <div>
            <Pasos actual={vista.paso} />
            {vista.paso === "seleccionBolsin" &&
                <SeleccionBolsin vista={vista} enviar={enviar} cancelar={cancelar} setAviso={setAviso} />}
            {vista.paso === "opcion" &&
                <OpcionRecepcion vista={vista} enviar={enviar} cancelar={cancelar} />}
            {vista.paso === "confirmacion" &&
                <Confirmacion vista={vista} enviar={enviar} cancelar={cancelar} />}
            {vista.paso === "exito" &&
                <Resultado vista={vista} emails={emails} enviar={enviar} />}
        </div>
    );
}

function Pasos({ actual }) {
    const indiceActual = PASOS.findIndex((paso) => paso.id === actual);
    return (
        <ol className="pasos">
            {PASOS.map((paso, indice) => (
                <li key={paso.id}
                    className={indice === indiceActual ? "actual" : indice < indiceActual ? "hecho" : ""}>
                    {indice + 1}. {paso.titulo}
                </li>
            ))}
        </ol>
    );
}

/** Pasos 4 y 5: listado de bolsines enviados, con filtros (alternativos A1 y A2). */
function SeleccionBolsin({ vista, enviar, cancelar, setAviso }) {
    const [precinto, setPrecinto] = useState("");
    const [cmOrigen, setCmOrigen] = useState("");
    const [bolsines, setBolsines] = useState(vista.bolsines);

    async function filtrar(evento) {
        evento.preventDefault();
        const resultado = await pedir("GET", "/api/recepcion/filtro", { precinto, cmOrigen });
        setBolsines(resultado.bolsines);
        if (resultado.flujoAlternativo === "A1") {
            setAviso({ tipo: "alerta", titulo: "Bolsín no encontrado",
                       texto: "No se encuentra bolsín con el número de precinto ingresado." });
        } else if (resultado.flujoAlternativo === "A2") {
            setAviso({ tipo: "alerta", titulo: "Bolsín no encontrado",
                       texto: "No se encuentra bolsín con la CM de origen ingresada." });
        } else if (resultado.bolsines.length === 0) {
            setAviso({ tipo: "alerta", titulo: "Bolsín no encontrado",
                       texto: "Ningún bolsín cumple los dos filtros a la vez." });
        }
    }

    function limpiar() {
        setPrecinto("");
        setCmOrigen("");
        setBolsines(vista.bolsines);
    }

    return (
        <section className="tarjeta">
            <h2>Bolsines enviados a {vista.cm}</h2>

            <form className="filtros" onSubmit={filtrar}>
                <label>
                    N° de precinto
                    <input value={precinto} inputMode="numeric"
                           onChange={(e) => setPrecinto(e.target.value)} />
                </label>
                <label>
                    CM de origen
                    <input value={cmOrigen} onChange={(e) => setCmOrigen(e.target.value)} />
                </label>
                <button type="submit">Filtrar</button>
                <button type="button" onClick={limpiar}>Ver todos</button>
            </form>

            <table>
                <thead>
                    <tr><th>Bolsín</th><th>CM de origen</th><th>N° de precinto</th><th></th></tr>
                </thead>
                <tbody>
                    {bolsines.map((bolsin) => (
                        <tr key={bolsin.numero}>
                            <td>{bolsin.numero}</td>
                            <td>{bolsin.cmOrigen}</td>
                            <td>{bolsin.precinto}</td>
                            <td className="derecha">
                                <button className="principal"
                                        onClick={() => enviar("/api/recepcion/seleccion", { bolsin: bolsin.numero })}>
                                    Seleccionar
                                </button>
                            </td>
                        </tr>
                    ))}
                    {bolsines.length === 0 && (
                        <tr><td colSpan="4" className="vacio">No hay bolsines para mostrar.</td></tr>
                    )}
                </tbody>
            </table>

            <div className="acciones">
                <button onClick={cancelar}>Cancelar</button>
            </div>
        </section>
    );
}

/** Pasos 6 a 8: contenido del bolsín y las cuatro opciones de recepción (A3, A4 y A5). */
function OpcionRecepcion({ vista, enviar, cancelar }) {
    const [numeroOpcion, setNumeroOpcion] = useState(null);
    const [marcadas, setMarcadas] = useState([]);    // números de documentación afectada

    const opcion = vista.opciones.find((o) => o.numero === numeroOpcion);
    const pideDocumentacion = opcion ? opcion.requiereSeleccion : false;
    const puedeContinuar = opcion && (!pideDocumentacion || marcadas.length > 0);

    function elegirOpcion(numero) {
        setNumeroOpcion(numero);
        setMarcadas([]);
    }

    function marcar(numero) {
        setMarcadas(marcadas.includes(numero)
            ? marcadas.filter((n) => n !== numero)
            : [...marcadas, numero]);
    }

    return (
        <section className="tarjeta">
            <DatosBolsin bolsin={vista.bolsin} cm={vista.cm} />

            <h3>Opción de recepción</h3>
            <div className="opciones">
                {vista.opciones.map((o) => (
                    <label key={o.numero} className={o.disponible ? "" : "deshabilitada"}>
                        <input type="radio" name="opcion" disabled={!o.disponible}
                               checked={numeroOpcion === o.numero}
                               onChange={() => elegirOpcion(o.numero)} />
                        {o.numero}. {o.descripcion}
                        {!o.disponible && <small> (solo para la Comisión Médica Central)</small>}
                    </label>
                ))}
            </div>

            <h3>Contenido registrado</h3>
            {pideDocumentacion && <p className="ayuda">Marcá la documentación a la que se aplica la opción elegida.</p>}
            {vista.bolsin.remitos.map((remito) => (
                <div key={remito.numero} className="remito">
                    <h4>Remito {remito.numero}</h4>
                    <ul>
                        {remito.documentacion.map((doc) => (
                            <li key={doc.numero}>
                                <label>
                                    {pideDocumentacion && (
                                        <input type="checkbox" checked={marcadas.includes(doc.numero)}
                                               onChange={() => marcar(doc.numero)} />
                                    )}
                                    <span>{doc.asunto}</span>
                                    <span className="etiqueta">{doc.tipo}</span>
                                    <small>Doc. {doc.numero}</small>
                                </label>
                            </li>
                        ))}
                    </ul>
                </div>
            ))}

            <div className="acciones">
                <button onClick={cancelar}>Cancelar</button>
                <button className="principal" disabled={!puedeContinuar}
                        onClick={() => enviar("/api/recepcion/opcion",
                            { numero: numeroOpcion, documentacion: marcadas.join(",") })}>
                    Continuar
                </button>
            </div>
        </section>
    );
}

/** Pasos 9 y 10: confirmación. "No confirmar" es el alternativo A6. */
function Confirmacion({ vista, enviar, cancelar }) {
    const marcadas = vista.documentacionMarcada;
    const afectada = vista.bolsin.remitos
        .flatMap((remito) => remito.documentacion)
        .filter((doc) => marcadas.includes(doc.numero));

    return (
        <section className="tarjeta">
            <DatosBolsin bolsin={vista.bolsin} cm={vista.cm} />

            <h3>Opción elegida</h3>
            <p>{vista.opcionElegida.numero}. {vista.opcionElegida.descripcion}</p>

            {afectada.length > 0 && (
                <div>
                    <h3>Documentación afectada</h3>
                    <ul>
                        {afectada.map((doc) => <li key={doc.numero}>{doc.asunto} (Doc. {doc.numero})</li>)}
                    </ul>
                </div>
            )}

            <p className="pregunta">¿Confirmás el registro de la recepción?</p>
            <div className="acciones">
                <button onClick={cancelar}>Cancelar</button>
                <button onClick={() => enviar("/api/recepcion/confirmacion", { confirma: false })}>
                    No confirmar
                </button>
                <button className="principal"
                        onClick={() => enviar("/api/recepcion/confirmacion", { confirma: true })}>
                    Confirmar
                </button>
            </div>
        </section>
    );
}

/** Pasos 11 y 12: mensaje de éxito, estados finales y emails del CU 29. */
function Resultado({ vista, emails, enviar }) {
    const [bolsin, setBolsin] = useState(null);

    useEffect(() => {
        pedir("GET", "/api/consulta").then((bolsines) =>
            setBolsin(bolsines.find((b) => b.numero === vista.bolsin.numero)));
    }, []);

    return (
        <section className="tarjeta">
            <p className="exito">{vista.mensajeExito}</p>

            {bolsin && (
                <div>
                    <h3>Estados registrados</h3>
                    {bolsin.remitos.map((remito) => (
                        <div key={remito.numero} className="remito">
                            <h4>Remito {remito.numero} <Estado nombre={remito.estado} /></h4>
                            <ul>
                                {remito.documentacion.map((doc) => (
                                    <li key={doc.numero}>
                                        <span>{doc.asunto}</span> <Estado nombre={doc.estado} />
                                    </li>
                                ))}
                            </ul>
                        </div>
                    ))}
                </div>
            )}

            <h3>Notificaciones enviadas (CU 29)</h3>
            {emails.map((email, indice) => (
                <details key={indice} className="email">
                    <summary>Para: {email.para}</summary>
                    <p><strong>{email.asunto}</strong></p>
                    <pre>{email.cuerpo}</pre>
                </details>
            ))}

            <div className="acciones">
                <button className="principal" onClick={() => enviar("/api/recepcion/iniciar")}>
                    Registrar otra recepción
                </button>
            </div>
        </section>
    );
}

function DatosBolsin({ bolsin, cm }) {
    return (
        <div className="datos-bolsin">
            <h2>Bolsín {bolsin.numero}</h2>
            <p>Origen: {bolsin.cmOrigen} · Destino: {cm} · Precinto N° {bolsin.precinto}</p>
        </div>
    );
}

// ------------------------------------------------------------------ consulta de estados

/** No es parte del CU 28: muestra lo guardado en la base de datos para verificarlo. */
function ConsultaEstados() {
    const [bolsines, setBolsines] = useState(null);

    useEffect(() => {
        pedir("GET", "/api/consulta").then(setBolsines);
    }, []);

    if (!bolsines) {
        return <p className="cargando">Cargando…</p>;
    }
    return (
        <div>
            <p className="ayuda">Estados guardados en la base de datos, con su historial y responsable.</p>
            {bolsines.map((bolsin) => (
                <section key={bolsin.numero} className="tarjeta">
                    <h2>Bolsín {bolsin.numero} <Estado nombre={bolsin.estado} /></h2>
                    <p>{bolsin.cmOrigen} → {bolsin.cmDestino} · Precinto N° {bolsin.precinto}</p>
                    <Historial cambios={bolsin.cambios} />

                    {bolsin.remitos.map((remito) => (
                        <div key={remito.numero} className="remito">
                            <h4>Remito {remito.numero} <Estado nombre={remito.estado} /></h4>
                            <ul>
                                {remito.documentacion.map((doc) => (
                                    <li key={doc.numero}>
                                        <span>{doc.asunto}</span>
                                        <span className="etiqueta">{doc.tipo}</span>
                                        <Estado nombre={doc.estado} />
                                        <Historial cambios={doc.cambios} />
                                    </li>
                                ))}
                            </ul>
                        </div>
                    ))}
                </section>
            ))}
        </div>
    );
}

function Historial({ cambios }) {
    const fecha = (texto) => (texto ? texto.replace("T", " ") : "vigente");
    return (
        <details className="historial">
            <summary>Historial ({cambios.length})</summary>
            <ul>
                {cambios.map((cambio, indice) => (
                    <li key={indice}>
                        {cambio.estado}: {fecha(cambio.inicio)} a {fecha(cambio.fin)} · {cambio.responsable}
                    </li>
                ))}
            </ul>
        </details>
    );
}

// ------------------------------------------------------------------ piezas comunes

function Estado({ nombre }) {
    const negativo = ["No Recibida", "Recibida y Rechazada", "Recibido y Aceptado Parcial"].includes(nombre);
    const pendiente = ["Enviado", "En Bolsin Enviado", "Para Redirigir", "Cerrado"].includes(nombre);
    return <span className={"estado " + (negativo ? "negativo" : pendiente ? "pendiente" : "positivo")}>{nombre}</span>;
}

/** Pop-up con cruz para cerrar; al cerrarlo el caso de uso sigue donde estaba. */
function Aviso({ aviso, cerrar }) {
    useEffect(() => {
        const alTeclear = (evento) => evento.key === "Escape" && cerrar();
        document.addEventListener("keydown", alTeclear);
        return () => document.removeEventListener("keydown", alTeclear);
    }, []);

    return (
        <div className="fondo-aviso" onClick={cerrar}>
            <div className={"aviso " + aviso.tipo} role="alertdialog" onClick={(e) => e.stopPropagation()}>
                <button className="cerrar" aria-label="Cerrar" onClick={cerrar}>×</button>
                <h3>{aviso.titulo}</h3>
                <p>{aviso.texto}</p>
            </div>
        </div>
    );
}

ReactDOM.createRoot(document.getElementById("raiz")).render(<App />);
