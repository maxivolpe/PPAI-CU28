// Pasos 6 a 8 del CU 28: contenido del bolsín y las cuatro opciones de recepción (A3, A4 y A5).

function OpcionRecepcion({ vista, enviar, cancelar, ocupado }) {
    const [numeroOpcion, setNumeroOpcion] = useState(null);
    const [marcadas, setMarcadas] = useState([]);    // números de la documentación afectada

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

            <OpcionesDeRecepcion opciones={vista.opciones} elegida={numeroOpcion} alElegir={elegirOpcion} />

            <h3>Contenido registrado</h3>
            {pideDocumentacion && <p className="ayuda">Marcá la documentación a la que se aplica la opción elegida.</p>}
            {vista.bolsin.remitos.map((remito) => (
                <RemitoDetalle key={remito.numero} remito={remito}>
                    {(doc) => (
                        <label>
                            {pideDocumentacion && (
                                <input type="checkbox" checked={marcadas.includes(doc.numero)}
                                       onChange={() => marcar(doc.numero)} />
                            )}
                            <span>{doc.asunto}</span>
                            <span className="etiqueta">{doc.tipo}</span>
                            <small>Doc. {doc.numero}</small>
                        </label>
                    )}
                </RemitoDetalle>
            ))}

            <div className="acciones">
                <button onClick={cancelar} aria-disabled={ocupado}>Cancelar</button>
                <button className="principal" disabled={!puedeContinuar} aria-disabled={ocupado}
                        onClick={() => enviar(RUTAS.opcion,
                            { numero: numeroOpcion, documentacion: marcadas.join(",") })}>
                    Continuar
                </button>
            </div>
        </section>
    );
}

/** Las opciones de recepción como grupo de radios; la 4 solo está disponible para la CM Central. */
function OpcionesDeRecepcion({ opciones, elegida, alElegir }) {
    const idTitulo = useId();
    return (
        <div>
            <h3 id={idTitulo}>Opción de recepción</h3>
            <div className="opciones" role="radiogroup" aria-labelledby={idTitulo}>
                {opciones.map((o) => (
                    <label key={o.numero}
                           className={clases(!o.disponible && "deshabilitada", elegida === o.numero && "elegida")}>
                        <input type="radio" name="opcion" disabled={!o.disponible}
                               checked={elegida === o.numero}
                               onChange={() => alElegir(o.numero)} />
                        <span>
                            {o.numero}. {o.descripcion}
                            {!o.disponible && <small> (solo para la Comisión Médica Central)</small>}
                        </span>
                    </label>
                ))}
            </div>
        </div>
    );
}
