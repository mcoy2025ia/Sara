package entities;

import java.time.LocalDateTime;

/** Reserva de un regalo hecha por un invitado (tabla Reserva_regalo, CU-20). */
public class ReservaRegalo {
    public static final String ESTADO_RESERVADO = "RESERVADO";
    public static final String ESTADO_CANCELADO = "CANCELADO";

    private int idReserva;
    private int idEvento;
    private int idInvitado;
    private int idRegalo;
    private LocalDateTime fechaReserva;
    private String estado;

    public ReservaRegalo() {
    }

    public ReservaRegalo(int idReserva, int idEvento, int idInvitado, int idRegalo,
                         LocalDateTime fechaReserva, String estado) {
        this.idReserva = idReserva;
        this.idEvento = idEvento;
        this.idInvitado = idInvitado;
        this.idRegalo = idRegalo;
        this.fechaReserva = fechaReserva;
        this.estado = estado;
    }

    public int getIdReserva() { return idReserva; }
    public void setIdReserva(int idReserva) { this.idReserva = idReserva; }
    public int getIdEvento() { return idEvento; }
    public void setIdEvento(int idEvento) { this.idEvento = idEvento; }
    public int getIdInvitado() { return idInvitado; }
    public void setIdInvitado(int idInvitado) { this.idInvitado = idInvitado; }
    public int getIdRegalo() { return idRegalo; }
    public void setIdRegalo(int idRegalo) { this.idRegalo = idRegalo; }
    public LocalDateTime getFechaReserva() { return fechaReserva; }
    public void setFechaReserva(LocalDateTime fechaReserva) { this.fechaReserva = fechaReserva; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
