package entities;

import java.time.LocalDateTime;

/**
 * Representa una notificacion interna relacionada
 * con la verificacion de un proveedor en el CU-028.
 */
public class NotificacionInterna {

    public static final String TIPO_APROBACION = "APROBACION";
    public static final String TIPO_RECHAZO = "RECHAZO";
    public static final String TIPO_CORRECCION = "CORRECCION";

    private final int idNotificacion;
    private final int idUsuarioDestinatario;
    private final int idProveedor;
    private final String tipo;
    private final String titulo;
    private final String mensaje;
    private final LocalDateTime fechaCreacion;
    private boolean leida;

    public NotificacionInterna(
            int idNotificacion,
            int idUsuarioDestinatario,
            int idProveedor,
            String tipo,
            String titulo,
            String mensaje,
            LocalDateTime fechaCreacion,
            boolean leida) {

        this.idNotificacion = idNotificacion;
        this.idUsuarioDestinatario = idUsuarioDestinatario;
        this.idProveedor = idProveedor;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.fechaCreacion = fechaCreacion;
        this.leida = leida;
    }

    public int getIdNotificacion() {
        return idNotificacion;
    }

    public int getIdUsuarioDestinatario() {
        return idUsuarioDestinatario;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public String getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }
}