package entities;

import java.time.LocalDateTime;

/** Invitación de un evento (tabla Invitacion). */
public class Invitacion {
    public static final String ESTADO_ENVIADA = "ENVIADA";
    public static final String ESTADO_ACEPTADA = "ACEPTADA";
    public static final String ESTADO_RECHAZADA = "RECHAZADA";
    public static final String ESTADO_REVOCADA = "REVOCADA";

    private int idInvitacion;
    private int idEvento;
    private int idLugar;
    private LocalDateTime fechaEnvio;
    private String mensaje;
    private String estadoRespuesta;

    public Invitacion() {
    }

    public Invitacion(int idEvento, int idLugar, LocalDateTime fechaEnvio, String mensaje, String estadoRespuesta) {
        this(0, idEvento, idLugar, fechaEnvio, mensaje, estadoRespuesta);
    }

    public Invitacion(int idInvitacion, int idEvento, int idLugar, LocalDateTime fechaEnvio,
                      String mensaje, String estadoRespuesta) {
        this.idInvitacion = idInvitacion;
        this.idEvento = idEvento;
        this.idLugar = idLugar;
        this.fechaEnvio = fechaEnvio;
        this.mensaje = mensaje;
        this.estadoRespuesta = estadoRespuesta;
    }

    /** Una invitación revocada ya no cuenta para el aforo ni para invitados duplicados. */
    public boolean estaActiva() {
        return !ESTADO_REVOCADA.equalsIgnoreCase(estadoRespuesta);
    }

    public int getIdInvitacion() { return idInvitacion; }
    public void setIdInvitacion(int idInvitacion) { this.idInvitacion = idInvitacion; }
    public int getIdEvento() { return idEvento; }
    public void setIdEvento(int idEvento) { this.idEvento = idEvento; }
    public int getIdLugar() { return idLugar; }
    public void setIdLugar(int idLugar) { this.idLugar = idLugar; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getEstadoRespuesta() { return estadoRespuesta; }
    public void setEstadoRespuesta(String estadoRespuesta) { this.estadoRespuesta = estadoRespuesta; }
}
