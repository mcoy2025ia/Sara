package entities;

import java.math.BigDecimal;

/**
 * Regalo de la mesa de regalos virtual (tabla Regalo, CU-20).
 * La mesa se asocia a una invitación (columna id_invitacion).
 * idInvitadoReserva y reservadoPor no son columnas: se llenan al consultar
 * con la reserva activa del regalo (tabla Reserva_regalo).
 */
public class Regalo {
    public static final String ESTADO_DISPONIBLE = "DISPONIBLE";
    public static final String ESTADO_RESERVADO = "RESERVADO";

    private int idRegalo;
    private int idInvitacion;
    private String nombre;
    private String descripcion;
    private String estado;
    private BigDecimal precioEstimado;
    private Integer idInvitadoReserva;
    private String reservadoPor;

    public Regalo() {
    }

    public Regalo(String nombre, String descripcion, BigDecimal precioEstimado) {
        this(0, 0, nombre, descripcion, ESTADO_DISPONIBLE, precioEstimado);
    }

    public Regalo(int idRegalo, int idInvitacion, String nombre, String descripcion, String estado,
                  BigDecimal precioEstimado) {
        this.idRegalo = idRegalo;
        this.idInvitacion = idInvitacion;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
        this.precioEstimado = precioEstimado;
    }

    /** R43: el regalo está reservado si tiene una reserva activa. */
    public boolean estaReservado() {
        return idInvitadoReserva != null;
    }

    public boolean estaReservadoPor(int idInvitado) {
        return idInvitadoReserva != null && idInvitadoReserva == idInvitado;
    }

    public int getIdRegalo() { return idRegalo; }
    public void setIdRegalo(int idRegalo) { this.idRegalo = idRegalo; }
    public int getIdInvitacion() { return idInvitacion; }
    public void setIdInvitacion(int idInvitacion) { this.idInvitacion = idInvitacion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getPrecioEstimado() { return precioEstimado; }
    public void setPrecioEstimado(BigDecimal precioEstimado) { this.precioEstimado = precioEstimado; }
    public Integer getIdInvitadoReserva() { return idInvitadoReserva; }
    public void setIdInvitadoReserva(Integer idInvitadoReserva) { this.idInvitadoReserva = idInvitadoReserva; }
    public String getReservadoPor() { return reservadoPor; }
    public void setReservadoPor(String reservadoPor) { this.reservadoPor = reservadoPor; }
}
