package entities;

import java.time.LocalDateTime;

/**
 * Invitación tal como la ve el invitado (CU-16): reúne datos de Invitado, Invitacion,
 * Evento y Lugar. Es solo de lectura; no corresponde a una tabla.
 */
public class InvitacionRecibida {
    private final int idInvitado;
    private final int idInvitacion;
    private final String nombreInvitado;
    private final String estadoInvitado;
    private final String estadoInvitacion;
    private final String mensaje;
    private final int idEvento;
    private final String evento;
    private final String tipoEvento;
    private final String estadoEvento;
    private final LocalDateTime inicio;
    private final LocalDateTime fin;
    private final int idLugar;
    private final String lugar;
    private final String direccion;
    private final int capacidad;
    private int ocupados;

    public InvitacionRecibida(int idInvitado, int idInvitacion, String nombreInvitado, String estadoInvitado,
                              String estadoInvitacion, String mensaje, int idEvento, String evento,
                              String tipoEvento, String estadoEvento, LocalDateTime inicio, LocalDateTime fin,
                              int idLugar, String lugar, String direccion, int capacidad) {
        this.idInvitado = idInvitado;
        this.idInvitacion = idInvitacion;
        this.nombreInvitado = nombreInvitado;
        this.estadoInvitado = estadoInvitado;
        this.estadoInvitacion = estadoInvitacion;
        this.mensaje = mensaje;
        this.idEvento = idEvento;
        this.evento = evento;
        this.tipoEvento = tipoEvento;
        this.estadoEvento = estadoEvento;
        this.inicio = inicio;
        this.fin = fin;
        this.idLugar = idLugar;
        this.lugar = lugar;
        this.direccion = direccion;
        this.capacidad = capacidad;
    }

    public int getIdInvitado() { return idInvitado; }
    public int getIdInvitacion() { return idInvitacion; }
    public String getNombreInvitado() { return nombreInvitado; }
    public String getEstadoInvitado() { return estadoInvitado; }
    public String getEstadoInvitacion() { return estadoInvitacion; }
    public String getMensaje() { return mensaje; }
    public int getIdEvento() { return idEvento; }
    public String getEvento() { return evento; }
    public String getTipoEvento() { return tipoEvento; }
    public String getEstadoEvento() { return estadoEvento; }
    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFin() { return fin; }
    public int getIdLugar() { return idLugar; }
    public String getLugar() { return lugar; }
    public String getDireccion() { return direccion; }
    public int getCapacidad() { return capacidad; }
    public int getOcupados() { return ocupados; }
    public void setOcupados(int ocupados) { this.ocupados = ocupados; }
}
