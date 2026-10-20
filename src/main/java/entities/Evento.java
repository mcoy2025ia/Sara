package entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Evento {
    public static final String ESTADO_INICIAL = "PLANEADO";

    private Integer idEvento;
    private final int idOrganizador;
    private final String nombre;
    private final String tipo;
    private final String descripcion;
    private final LocalDateTime fechaInicio;
    private final LocalDateTime fechaFin;
    private final String ubicacion;
    private final BigDecimal presupuesto;
    private final Integer invitadosIniciales;
    private final LocalDateTime fechaCreacion;
    private String estado;

    public Evento(int idOrganizador, String nombre, String tipo, String descripcion,
                  LocalDateTime fechaInicio, LocalDateTime fechaFin, String ubicacion,
                  BigDecimal presupuesto, int invitadosIniciales) {
        this(null, idOrganizador, nombre, tipo, descripcion, fechaInicio, fechaFin,
                ubicacion, presupuesto, invitadosIniciales, ESTADO_INICIAL, LocalDateTime.now());
    }

    public Evento(Integer idEvento, int idOrganizador, String nombre, String tipo, String descripcion,
                  LocalDateTime fechaInicio, LocalDateTime fechaFin, String ubicacion,
                  BigDecimal presupuesto, Integer invitadosIniciales, String estado, LocalDateTime fechaCreacion) {
        this.idEvento = idEvento;
        this.idOrganizador = idOrganizador;
        this.nombre = nombre;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.ubicacion = ubicacion;
        this.presupuesto = presupuesto;
        this.invitadosIniciales = invitadosIniciales;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
    }

    public Integer getIdEvento() { return idEvento; }
    public void setIdEvento(Integer idEvento) { this.idEvento = idEvento; }
    public int getIdOrganizador() { return idOrganizador; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public String getUbicacion() { return ubicacion; }
    public BigDecimal getPresupuesto() { return presupuesto; }
    public Integer getInvitadosIniciales() { return invitadosIniciales; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public String toString() {
        return "Evento{" +
                "idEvento=" + idEvento +
                ", idOrganizador=" + idOrganizador +
                ", nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", fechaInicio=" + fechaInicio +
                ", fechaFin=" + fechaFin +
                ", ubicacion='" + ubicacion + '\'' +
                ", presupuesto=" + presupuesto +
                ", invitadosIniciales=" + invitadosIniciales +
                ", estado='" + estado + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                '}';
    }
}
