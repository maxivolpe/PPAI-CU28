// Pasos 11 y 12 del CU 28: mensaje de éxito, estados finales y emails del CU 29.

function Resultado({ vista, emails, enviar, ocupado }) {
    return (
        <section className="tarjeta">
            <p className="exito" role="status">{vista.mensajeExito}</p>

            <EstadosRegistrados numeroBolsin={vista.bolsin.numero} />
            <NotificacionesEnviadas emails={emails} />

            <div className="acciones">
                <button className="principal" aria-disabled={ocupado} onClick={() => enviar(RUTAS.iniciar)}>
                    Registrar otra recepción
                </button>
            </div>
        </section>
    );
}

/** Lee de la base de datos cómo quedaron el bolsín recibido, sus remitos y su documentación. */
function EstadosRegistrados({ numeroBolsin }) {
    const { datos: bolsin, cargando, error, recargar } = useDatos(
        () => pedir("GET", RUTAS.consulta).then((bolsines) => bolsines.find((b) => b.numero === numeroBolsin)),
        [numeroBolsin],
    );

    if (cargando) {
        return <Cargando texto="Cargando los estados registrados…" />;
    }
    if (error) {
        return <ErrorCarga mensaje={`No se pudieron cargar los estados registrados. ${error.message}`}
                           alReintentar={recargar} />;
    }
    if (!bolsin) {
        return null;
    }
    return (
        <div>
            <h3>Estados registrados</h3>
            {bolsin.remitos.map((remito) => (
                <RemitoDetalle key={remito.numero} remito={remito} conEstado>
                    {(doc) => (
                        <>
                            <span>{doc.asunto}</span> <Estado nombre={doc.estado} />
                        </>
                    )}
                </RemitoDetalle>
            ))}
        </div>
    );
}

function NotificacionesEnviadas({ emails }) {
    return (
        <div>
            <h3>Notificaciones enviadas (CU 29)</h3>
            {emails.length === 0 && <p className="ayuda">No se enviaron notificaciones.</p>}
            {emails.map((email, indice) => (
                <details key={indice} className="email">
                    <summary>Para: {email.para}</summary>
                    <p><strong>{email.asunto}</strong></p>
                    <pre>{email.cuerpo}</pre>
                </details>
            ))}
        </div>
    );
}
