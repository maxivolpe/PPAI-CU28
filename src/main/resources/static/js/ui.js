// Piezas de interfaz que se reutilizan en varias pantallas.

/** Estado visual "cargando". */
function Cargando({ texto = "Cargando…" }) {
    return <p className="cargando" role="status">{texto}</p>;
}

/** Estado visual "error": explica qué pasó y, si se puede, ofrece reintentar. */
function ErrorCarga({ mensaje, alReintentar }) {
    return (
        <div className="error-carga" role="alert">
            <p>{mensaje}</p>
            {alReintentar && <button onClick={alReintentar}>Reintentar</button>}
        </div>
    );
}

/** Etiqueta de color con el nombre de un estado (positivo, pendiente o negativo). */
function Estado({ nombre }) {
    return <span className={clases("estado", claseDeEstado(nombre))}>{nombre}</span>;
}

/** Historial de cambios de estado de un bolsín o de una documentación, con su responsable. */
function Historial({ cambios }) {
    return (
        <details className="historial">
            <summary>Historial ({cambios.length})</summary>
            <ul>
                {cambios.map((cambio, indice) => (
                    <li key={indice}>
                        {cambio.estado}: {formatearFechaHora(cambio.inicio)} a {formatearFechaHora(cambio.fin)} · {cambio.responsable}
                    </li>
                ))}
            </ul>
        </details>
    );
}

/** Encabezado con los datos del bolsín elegido. */
function DatosBolsin({ bolsin, cm }) {
    return (
        <div className="datos-bolsin">
            <h2>Bolsín {bolsin.numero}</h2>
            <p>Origen: {bolsin.cmOrigen} · Destino: {cm} · Precinto N° {bolsin.precinto}</p>
        </div>
    );
}

/**
 * Remito con su documentación. Qué se muestra de cada documentación lo decide quien lo usa:
 * `children` recibe la documentación y devuelve el contenido de su fila.
 */
function RemitoDetalle({ remito, conEstado = false, children }) {
    return (
        <div className="remito">
            <h4>Remito {remito.numero}{conEstado && <> <Estado nombre={remito.estado} /></>}</h4>
            <ul>
                {remito.documentacion.map((doc) => <li key={doc.numero}>{children(doc)}</li>)}
            </ul>
        </div>
    );
}

/**
 * Pop-up con cruz para cerrar; al cerrarlo el caso de uso sigue donde estaba.
 * Mueve el foco al cuadro al abrirse, lo retiene mientras está abierto y lo devuelve al cerrarse.
 */
function Aviso({ aviso, cerrar }) {
    const idTitulo = useId();
    const idTexto = useId();
    const botonCerrar = useRef(null);

    useTeclaEscape(cerrar);

    useEffect(() => {
        const anterior = document.activeElement;
        botonCerrar.current.focus();
        return () => {
            if (anterior && anterior.isConnected) {
                anterior.focus();
            }
        };
    }, []);

    // El cuadro tiene un solo control, así que Tab no debe sacar el foco de él.
    const retenerFoco = (evento) => {
        if (evento.key === "Tab") {
            evento.preventDefault();
            botonCerrar.current.focus();
        }
    };

    return (
        <div className="fondo-aviso" onClick={cerrar}>
            <div className={clases("aviso", aviso.tipo)} role="alertdialog" aria-modal="true"
                 aria-labelledby={idTitulo} aria-describedby={idTexto}
                 onClick={(e) => e.stopPropagation()} onKeyDown={retenerFoco}>
                <button ref={botonCerrar} className="cerrar" aria-label="Cerrar" onClick={cerrar}>×</button>
                <h3 id={idTitulo}>{aviso.titulo}</h3>
                <p id={idTexto}>{aviso.texto}</p>
            </div>
        </div>
    );
}
