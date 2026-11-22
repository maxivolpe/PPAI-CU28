// Hooks propios. Este archivo declara los hooks de React que usan los demás archivos.

const { useState, useEffect, useCallback, useRef, useId } = React;

/**
 * Carga datos al montarse (o cuando cambian las dependencias) y expone los tres estados
 * visuales: cargando, error y datos. `recargar` repite el pedido.
 * Ignora la respuesta si el componente se desmontó o cambió de dependencias mientras tanto.
 */
function useDatos(cargar, dependencias) {
    const [resultado, setResultado] = useState({ datos: null, error: null, cargando: true });
    const [intento, setIntento] = useState(0);

    useEffect(() => {
        let vigente = true;
        setResultado({ datos: null, error: null, cargando: true });
        cargar().then(
            (datos) => vigente && setResultado({ datos, error: null, cargando: false }),
            (error) => vigente && setResultado({ datos: null, error, cargando: false }),
        );
        return () => {
            vigente = false;
        };
    }, [...dependencias, intento]);

    const recargar = useCallback(() => setIntento((n) => n + 1), []);
    return { ...resultado, recargar };
}

/** Ejecuta `alPresionar` cuando se aprieta Escape, siempre con la última versión de la función. */
function useTeclaEscape(alPresionar) {
    const ultima = useRef(alPresionar);
    ultima.current = alPresionar;

    useEffect(() => {
        const alTeclear = (evento) => evento.key === "Escape" && ultima.current();
        document.addEventListener("keydown", alTeclear);
        return () => document.removeEventListener("keydown", alTeclear);
    }, []);
}

/**
 * Cuando cambia `clave` (por ejemplo el paso del caso de uso) lleva el foco y el scroll al
 * principio del contenido, para que quien usa teclado o lector de pantalla empiece en lo nuevo.
 */
function useFocoAlCambiar(clave) {
    const primera = useRef(true);

    useEffect(() => {
        if (primera.current) {
            primera.current = false;
            return;
        }
        const contenido = document.getElementById("contenido");
        if (contenido) {
            contenido.focus({ preventScroll: true });
            window.scrollTo(0, 0);
        }
    }, [clave]);
}

/** Estado del pop-up: { tipo, titulo, texto } o null cuando no hay ninguno. */
function useAviso() {
    const [aviso, setAviso] = useState(null);
    const avisar = useCallback((tipo, titulo, texto) => setAviso({ tipo, titulo, texto }), []);
    const cerrarAviso = useCallback(() => setAviso(null), []);
    return { aviso, avisar, cerrarAviso };
}

/**
 * Estado que devuelve el servidor ({ empleados, sesion, vista }) y la forma de enviarle
 * acciones del Encargado de Bolsines. Cada acción redibuja con la vista nueva; los errores
 * y los mensajes del servidor salen en el pop-up.
 */
function useServidor(avisar) {
    const [estado, setEstado] = useState(null);
    const [emails, setEmails] = useState([]);     // emails del CU 29 de la última recepción
    const [error, setError] = useState(null);     // error de la carga inicial
    const [ocupado, setOcupado] = useState(false);
    const enCurso = useRef(false);                // evita enviar dos veces la misma acción

    const cargarEstado = useCallback(() => {
        setError(null);
        pedir("GET", RUTAS.estado).then(setEstado, setError);
    }, []);

    useEffect(cargarEstado, [cargarEstado]);

    const enviar = useCallback(async (ruta, parametros) => {
        if (enCurso.current) {
            return;
        }
        enCurso.current = true;
        setOcupado(true);
        try {
            const nuevo = await pedir("POST", ruta, parametros);
            setEstado(nuevo);
            setEmails(nuevo.emails || []);
            if (nuevo.vista.mensaje) {
                avisar(TIPO_AVISO.info, "Aviso", nuevo.vista.mensaje);
            }
        } catch (falla) {
            avisar(TIPO_AVISO.error, "No se pudo continuar", falla.message);
        } finally {
            enCurso.current = false;
            setOcupado(false);
        }
    }, [avisar]);

    return { estado, emails, error, ocupado, enviar, reintentar: cargarEstado };
}
