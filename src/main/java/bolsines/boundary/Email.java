package bolsines.boundary;

/** Un email de notificación: a quién, con qué asunto y qué texto. */
public record Email(String destinatario, String asunto, String cuerpo) {
}
