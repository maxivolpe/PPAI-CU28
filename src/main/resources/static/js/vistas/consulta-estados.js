// No es parte del CU 28: muestra lo guardado en la base de datos para verificarlo.

function ConsultaEstados() {
    const { datos: bolsines, cargando, error, recargar } = useDatos(() => pedir("GET", RUTAS.consulta), []);

    if (cargando) {
        return <Cargando />;
    }
    if (error) {
        return <ErrorCarga mensaje={`No se pudieron cargar los estados guardados. ${error.message}`}
                           alReintentar={recargar} />;
    }
    return (
        <div>
            <p className="ayuda">Estados guardados en la base de datos, con su historial y responsable.</p>
            {bolsines.length === 0 && <p className="vacio">Todavía no hay bolsines registrados.</p>}
            {bolsines.map((bolsin) => <BolsinGuardado key={bolsin.numero} bolsin={bolsin} />)}
        </div>
    );
}

function BolsinGuardado({ bolsin }) {
    return (
        <section className="tarjeta">
            <h2>Bolsín {bolsin.numero} <Estado nombre={bolsin.estado} /></h2>
            <p>{bolsin.cmOrigen} → {bolsin.cmDestino} · Precinto N° {bolsin.precinto}</p>
            <Historial cambios={bolsin.cambios} />

            {bolsin.remitos.map((remito) => (
                <RemitoDetalle key={remito.numero} remito={remito} conEstado>
                    {(doc) => (
                        <>
                            <span>{doc.asunto}</span>
                            <span className="etiqueta">{doc.tipo}</span>
                            <Estado nombre={doc.estado} />
                            <Historial cambios={doc.cambios} />
                        </>
                    )}
                </RemitoDetalle>
            ))}
        </section>
    );
}
