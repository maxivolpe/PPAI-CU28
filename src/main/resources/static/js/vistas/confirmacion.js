// Pasos 9 y 10 del CU 28: confirmación. "No confirmar" es el alternativo A6.

function Confirmacion({ vista, enviar, cancelar, ocupado }) {
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
                    <ul className="lista-simple">
                        {afectada.map((doc) => <li key={doc.numero}>{doc.asunto} (Doc. {doc.numero})</li>)}
                    </ul>
                </div>
            )}

            <p className="pregunta">¿Confirmás el registro de la recepción?</p>
            <div className="acciones">
                <button onClick={cancelar} aria-disabled={ocupado}>Cancelar</button>
                <button aria-disabled={ocupado}
                        onClick={() => enviar(RUTAS.confirmacion, { confirma: false })}>
                    No confirmar
                </button>
                <button className="principal" aria-disabled={ocupado}
                        onClick={() => enviar(RUTAS.confirmacion, { confirma: true })}>
                    Confirmar
                </button>
            </div>
        </section>
    );
}
