package controllers;

import entities.*;
import services.ServicePublicacion;
import java.util.List;

public class ControllerPublicacion {
    private final ServicePublicacion service;
    private final SesionUsuario sesion;

    public ControllerPublicacion(ServicePublicacion service, SesionUsuario sesion) {
        this.service = service;
        this.sesion = sesion;
    }

    public boolean esAdministrador() { return service.esAdministrador(sesion); }
    public String nombreUsuario() { return service.nombreUsuario(sesion); }
    public List<Evento> listarEventos() { return service.listarEventos(sesion); }
    public void solicitar(int evento) { service.solicitar(sesion, evento); }
    public void revisar(int solicitud, boolean aprobar, String motivo) { service.revisar(sesion, solicitud, aprobar, motivo); }
    public List<SolicitudPublicacion> listarSolicitudes() { return service.listarSolicitudes(sesion); }
    public List<PlantillaEvento> listarPlantillas() { return service.listarPlantillas(sesion); }
    public List<String> notificaciones() { return service.notificaciones(sesion); }
}
