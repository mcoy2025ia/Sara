
package entities;

import java.time.LocalDateTime;

/**
 * Representa un mensaje del canal interno de comunicacion
 * entre el administrador y el organizador para CU-030.
 */
public class MensajeOrganizador {

    public static final String TIPO_MENSAJE = "MENSAJE";
    public static final String TIPO_SOLICITUD_CORRECCION =
            "SOLICITUD_CORRECCION";
    public static final String TIPO_RESPUESTA = "RESPUESTA";

    public static final String ESTADO_ENVIADO = "ENVIADO";
    public static final String ESTADO_ERROR = "ERROR";

    private int idMensaje;
    private int idOrganizador;
    private int idRemitente;
    private int idDestinatario;
    private String contenido;
    private String tipoMensaje;
    private String estado;
    private LocalDateTime fechaHora;

    public MensajeOrganizador() {
        this.tipoMensaje = TIPO_MENSAJE;
        this.estado = ESTADO_ENVIADO;
    }

    public MensajeOrganizador(
            int idOrganizador,
            int idRemitente,
            int idDestinatario,
            String contenido,
            String tipoMensaje) {

        this.idOrganizador = idOrganizador;
        this.idRemitente = idRemitente;
        this.idDestinatario = idDestinatario;
        this.contenido = contenido;
        this.tipoMensaje = tipoMensaje;
        this.estado = ESTADO_ENVIADO;
    }

    public int getIdMensaje() {
        return idMensaje;
    }

    public void setIdMensaje(int idMensaje) {
        this.idMensaje = idMensaje;
    }

    public int getIdOrganizador() {
        return idOrganizador;
    }

    public void setIdOrganizador(int idOrganizador) {
        this.idOrganizador = idOrganizador;
    }

    public int getIdRemitente() {
        return idRemitente;
    }

    public void setIdRemitente(int idRemitente) {
        this.idRemitente = idRemitente;
    }

    public int getIdDestinatario() {
        return idDestinatario;
    }

    public void setIdDestinatario(int idDestinatario) {
        this.idDestinatario = idDestinatario;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getTipoMensaje() {
        return tipoMensaje;
    }

    public void setTipoMensaje(String tipoMensaje) {
        this.tipoMensaje = tipoMensaje;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    /**
     * Valida los datos necesarios antes de intentar guardar el mensaje.
     */
    public boolean esValido() {
        return idOrganizador > 0
                && idRemitente > 0
                && idDestinatario > 0
                && contenido != null
                && !contenido.isBlank()
                && contenido.trim().length() <= 1000
                && tipoValido(tipoMensaje);
    }

    public static boolean tipoValido(String tipo) {
        return TIPO_MENSAJE.equalsIgnoreCase(tipo)
                || TIPO_SOLICITUD_CORRECCION.equalsIgnoreCase(tipo)
                || TIPO_RESPUESTA.equalsIgnoreCase(tipo);
    }

    /**
     * Solo los mensajes guardados correctamente deben mostrarse
     * como enviados en el historial.
     */
    public boolean fueEnviado() {
        return ESTADO_ENVIADO.equalsIgnoreCase(estado)
                && idMensaje > 0;
    }
}
