// CU 28 "Registrar recepción de bolsín": elige qué paso mostrar según vista.paso, que es lo que
// el GestorRecepcion le pidió mostrar a PantallaRecepcionWeb.

function Recepcion({ vista, emails, enviar, ocupado, avisar }) {
    const cancelar = () => enviar(RUTAS.cancelar);

    useFocoAlCambiar(vista.paso);

    if (vista.paso === "inicio") {
        return <InicioRecepcion enviar={enviar} ocupado={ocupado} />;
    }

    return (
        <div>
            <Pasos actual={vista.paso} />
            {vista.paso === "seleccionBolsin" &&
                <SeleccionBolsin vista={vista} enviar={enviar} cancelar={cancelar} avisar={avisar} ocupado={ocupado} />}
            {vista.paso === "opcion" &&
                <OpcionRecepcion vista={vista} enviar={enviar} cancelar={cancelar} ocupado={ocupado} />}
            {vista.paso === "confirmacion" &&
                <Confirmacion vista={vista} enviar={enviar} cancelar={cancelar} ocupado={ocupado} />}
            {vista.paso === "exito" &&
                <Resultado vista={vista} emails={emails} enviar={enviar} ocupado={ocupado} />}
        </div>
    );
}

function InicioRecepcion({ enviar, ocupado }) {
    return (
        <section className="tarjeta centrada">
            <h2>Recepción de bolsines</h2>
            <p>Registrá los bolsines que el correo entregó en tu comisión médica.</p>
            <button className="principal" aria-disabled={ocupado} onClick={() => enviar(RUTAS.iniciar)}>
                Registrar recepción de bolsín
            </button>
        </section>
    );
}

/** Indicador de progreso: los pasos hechos, el actual y los que faltan. */
function Pasos({ actual }) {
    const indiceActual = PASOS.findIndex((paso) => paso.id === actual);
    return (
        <ol className="pasos" aria-label="Pasos del caso de uso">
            {PASOS.map((paso, indice) => (
                <li key={paso.id}
                    className={indice === indiceActual ? "actual" : indice < indiceActual ? "hecho" : ""}
                    aria-current={indice === indiceActual ? "step" : undefined}>
                    {indice + 1}. {paso.titulo}
                </li>
            ))}
        </ol>
    );
}
