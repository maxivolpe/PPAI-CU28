// Pasos 4 y 5 del CU 28: listado de bolsines enviados, con filtros (alternativos A1 y A2).

function SeleccionBolsin({ vista, enviar, cancelar, avisar, ocupado }) {
    const [precinto, setPrecinto] = useState("");
    const [cmOrigen, setCmOrigen] = useState("");
    const [bolsines, setBolsines] = useState(vista.bolsines);
    const [filtrando, setFiltrando] = useState(false);

    async function filtrar(evento) {
        evento.preventDefault();
        if (filtrando) {
            return;
        }
        setFiltrando(true);
        try {
            const resultado = await pedir("GET", RUTAS.filtro, { precinto, cmOrigen });
            setBolsines(resultado.bolsines);
            const mensaje = mensajeDeFiltro(resultado);
            if (mensaje) {
                avisar(TIPO_AVISO.alerta, "Bolsín no encontrado", mensaje);
            }
        } catch (falla) {
            avisar(TIPO_AVISO.error, "No se pudo filtrar", falla.message);
        } finally {
            setFiltrando(false);
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

            <FiltrosBolsin precinto={precinto} cmOrigen={cmOrigen} filtrando={filtrando}
                           alCambiarPrecinto={setPrecinto} alCambiarCmOrigen={setCmOrigen}
                           alFiltrar={filtrar} alLimpiar={limpiar} />

            <TablaBolsines bolsines={bolsines} ocupado={ocupado}
                           alSeleccionar={(numero) => enviar(RUTAS.seleccion, { bolsin: numero })} />

            <div className="acciones">
                <button onClick={cancelar} aria-disabled={ocupado}>Cancelar</button>
            </div>
        </section>
    );
}

function FiltrosBolsin({ precinto, cmOrigen, filtrando, alCambiarPrecinto, alCambiarCmOrigen, alFiltrar, alLimpiar }) {
    return (
        <form className="filtros" onSubmit={alFiltrar}>
            <label>
                N° de precinto
                <input value={precinto} inputMode="numeric" autoComplete="off"
                       onChange={(e) => alCambiarPrecinto(e.target.value)} />
            </label>
            <label>
                CM de origen
                <input value={cmOrigen} autoComplete="off"
                       onChange={(e) => alCambiarCmOrigen(e.target.value)} />
            </label>
            <div className="filtros-botones">
                <button type="submit" aria-disabled={filtrando}>Filtrar</button>
                <button type="button" onClick={alLimpiar}>Ver todos</button>
            </div>
        </form>
    );
}

/**
 * Tabla de bolsines. En pantallas chicas cada fila se muestra como una ficha (ver styles.css),
 * por eso las celdas llevan data-label y los roles ARIA de tabla están escritos explícitamente.
 */
function TablaBolsines({ bolsines, ocupado, alSeleccionar }) {
    return (
        <table className="tabla" role="table" aria-label="Bolsines enviados">
            <thead role="rowgroup">
                <tr role="row">
                    <th role="columnheader" scope="col">Bolsín</th>
                    <th role="columnheader" scope="col">CM de origen</th>
                    <th role="columnheader" scope="col">N° de precinto</th>
                    <th role="columnheader" scope="col"></th>
                </tr>
            </thead>
            <tbody role="rowgroup">
                {bolsines.map((bolsin) => (
                    <tr key={bolsin.numero} role="row">
                        <td role="cell" data-label="Bolsín">{bolsin.numero}</td>
                        <td role="cell" data-label="CM de origen">{bolsin.cmOrigen}</td>
                        <td role="cell" data-label="N° de precinto">{bolsin.precinto}</td>
                        <td role="cell" className="derecha">
                            <button className="principal" aria-disabled={ocupado}
                                    aria-label={`Seleccionar bolsín ${bolsin.numero}`}
                                    onClick={() => alSeleccionar(bolsin.numero)}>
                                Seleccionar
                            </button>
                        </td>
                    </tr>
                ))}
                {bolsines.length === 0 && (
                    <tr role="row">
                        <td role="cell" colSpan="4" className="vacio">No hay bolsines para mostrar.</td>
                    </tr>
                )}
            </tbody>
        </table>
    );
}
