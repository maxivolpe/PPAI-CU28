package bolsines.boundary;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Frontera con el actor Servidor de Correo (CU 29 "Notificar recepción de bolsín").
 * Está simulada: en lugar de enviar el email lo escribe en la salida y lo deja anotado
 * en emailsEnviados, para que la pantalla web pueda mostrarlo.
 */
public class InterfazServidorCorreo {

    private final PrintStream salida;
    /** Cada email: {destinatario, asunto, cuerpo}. */
    private final List<String[]> emailsEnviados = new ArrayList<>();

    public InterfazServidorCorreo(PrintStream salida) {
        this.salida = salida;
    }

    public void enviarEmail(String destinatario, String asunto, String cuerpo) {
        salida.println();
        salida.println("[Email simulado] Para: " + destinatario);
        salida.println("[Email simulado] Asunto: " + asunto);
        salida.print(cuerpo);
        emailsEnviados.add(new String[]{destinatario, asunto, cuerpo});
    }

    public List<String[]> getEmailsEnviados() {
        return emailsEnviados;
    }
}
