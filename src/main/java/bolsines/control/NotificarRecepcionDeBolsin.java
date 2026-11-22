package bolsines.control;

import bolsines.boundary.Email;
import bolsines.boundary.InterfazServidorCorreo;
import bolsines.entity.Bolsin;

/**
 * CU 29 "Notificar recepción de bolsín", incluido por el CU 28: un email a cada CM de
 * origen con la situación de su documentación. Se envía a través de la frontera con el
 * Servidor de Correo.
 */
final class NotificarRecepcionDeBolsin {

    private final InterfazServidorCorreo servidorCorreo;
    private final ArmadorDeEmailDeRecepcion armador = new ArmadorDeEmailDeRecepcion();

    NotificarRecepcionDeBolsin(InterfazServidorCorreo servidorCorreo) {
        this.servidorCorreo = servidorCorreo;
    }

    void notificar(Bolsin bolsinRecibido, String nombreCMDestino) {
        for (Email email : armador.armar(bolsinRecibido, nombreCMDestino)) {
            servidorCorreo.enviarEmail(email.destinatario(), email.asunto(), email.cuerpo());
        }
    }
}
