package controllers;

import entities.Evento;
import entities.Invitacion;
import entities.InvitacionRecibida;
import entities.Invitado;
import entities.Lugar;
import entities.Regalo;
import entities.SesionUsuario;
import java.util.List;
import java.util.Map;
import services.ServiceInvitados;

/** Intermediario entre las vistas del módulo de invitados y ServiceInvitados (CU-16, CU-17 y CU-20). */
public class ControllerInvitados {

    private final ServiceInvitados serviceInvitados;
    /** Sesión del organizador; es null en el acceso del invitado, que no tiene cuenta. */
    private final SesionUsuario sesion;

    public ControllerInvitados(ServiceInvitados serviceInvitados) {
        this(serviceInvitados, null);
    }

    public ControllerInvitados(ServiceInvitados serviceInvitados, SesionUsuario sesion) {
        this.serviceInvitados = serviceInvitados;
        this.sesion = sesion;
    }

    public List<String> consultarAvisosPorCorreo(String correo) {
        return serviceInvitados.consultarAvisosPorCorreo(correo);
    }

    public List<entities.Fotografia> consultarGaleria(String correo, String telefono) {
        return serviceInvitados.consultarGaleria(correo,telefono);
    }

    /** Con sesión de organizador solo aparecen sus eventos. */
    public List<Evento> listarEventos() {
        Integer organizador = organizadorEnSesion();
        return organizador == null ? serviceInvitados.listarEventos() : serviceInvitados.listarEventos(organizador);
    }

    public List<Lugar> listarLugares() {
        return serviceInvitados.listarLugares();
    }

    public List<Invitacion> listarInvitaciones(int eventoId) {
        validarEvento(eventoId);
        return serviceInvitados.listarInvitaciones(eventoId);
    }

    public List<Invitado> listarInvitados(int idInvitacion) {
        validarInvitacion(idInvitacion);
        return serviceInvitados.listarInvitados(idInvitacion);
    }

    public Map<Integer, Integer> contarAsistentesPorInvitacion(int eventoId) {
        validarEvento(eventoId);
        return serviceInvitados.contarAsistentesPorInvitacion(eventoId);
    }

    public int contarAsistentes(int eventoId, int idLugar) {
        validarEvento(eventoId);
        return serviceInvitados.contarAsistentes(eventoId, idLugar);
    }

    public void crearInvitacion(int eventoId, Invitacion datos) {
        validarEvento(eventoId);
        serviceInvitados.crearInvitacion(eventoId, datos);
    }

    public void actualizarInvitacion(int idInvitacion, Invitacion datos) {
        validarInvitacion(idInvitacion);
        serviceInvitados.actualizarInvitacion(idInvitacion, datos);
    }

    public void revocarInvitacion(int idInvitacion) {
        validarInvitacion(idInvitacion);
        serviceInvitados.revocarInvitacion(idInvitacion);
    }

    public void agregarInvitado(int idInvitacion, Invitado datos) {
        validarInvitacion(idInvitacion);
        serviceInvitados.agregarInvitado(idInvitacion, datos);
    }

    public void quitarInvitado(int idInvitado) {
        if (organizadorEnSesion() != null) validarEvento(serviceInvitados.eventoDeInvitado(idInvitado));
        serviceInvitados.quitarInvitado(idInvitado);
    }

    public List<InvitacionRecibida> consultarInvitacionesPorCorreo(String correo, String telefono) {
        return serviceInvitados.consultarInvitacionesPorCorreo(correo, telefono);
    }

    public void gestionarRespuestaInvitacion(int idInvitado, String correo, String telefono, String respuesta) {
        serviceInvitados.gestionarRespuestaInvitacion(idInvitado, correo, telefono, respuesta);
    }

    // La mesa de regalos solo la arma el organizador con sesión, y solo en sus eventos.
    public List<Regalo> listarRegalos(int idInvitacion) {
        return serviceInvitados.listarRegalos(organizadorRequerido(), idInvitacion);
    }

    public void agregarRegalo(int idInvitacion, Regalo datos) {
        serviceInvitados.agregarRegalo(organizadorRequerido(), idInvitacion, datos);
    }

    public void quitarRegalo(int idRegalo) {
        serviceInvitados.quitarRegalo(organizadorRequerido(), idRegalo);
    }

    public List<Regalo> consultarRegalosDisponibles(int idInvitado, String correo, String telefono) {
        return serviceInvitados.consultarRegalosDisponibles(idInvitado, correo, telefono);
    }

    public boolean validarDisponibilidadRegalo(int idRegalo) {
        return serviceInvitados.validarDisponibilidadRegalo(idRegalo);
    }

    public void reservarRegalo(int idInvitado, String correo, String telefono, int idRegalo) {
        serviceInvitados.reservarRegalo(idInvitado, correo, telefono, idRegalo);
    }

    public void cancelarReservaRegalo(int idInvitado, String correo, String telefono, int idRegalo) {
        serviceInvitados.cancelarReservaRegalo(idInvitado, correo, telefono, idRegalo);
    }

    // ------------------------------------------------------------------
    // Cada organizador solo gestiona las invitaciones de sus propios eventos.
    // ------------------------------------------------------------------
    private Integer organizadorEnSesion() {
        if (sesion == null || !"ORGANIZADOR".equalsIgnoreCase(sesion.getTipoUsuario())) return null;
        return sesion.getIdActor();
    }

    private int organizadorRequerido() {
        Integer organizador = organizadorEnSesion();
        if (organizador == null)
            throw new IllegalArgumentException("Inicia sesión como organizador para gestionar la mesa de regalos.");
        return organizador;
    }

    private void validarEvento(int eventoId) {
        Integer organizador = organizadorEnSesion();
        if (organizador != null) serviceInvitados.validarEventoDelOrganizador(organizador, eventoId);
    }

    private void validarInvitacion(int idInvitacion) {
        if (organizadorEnSesion() != null) validarEvento(serviceInvitados.eventoDeInvitacion(idInvitacion));
    }
}
