package controllers;

import java.math.BigDecimal;
import java.util.List;

import entities.MensajeSolicitud;
import entities.PropuestaServicio;
import entities.SolicitudEjecucion;
import services.ServiceSolicitudes;

public class ControllerSolicitudes {

    private final ServiceSolicitudes service;

    public ControllerSolicitudes(ServiceSolicitudes service) {
        this.service = service;
    }

    public List<SolicitudEjecucion> listarSolicitudesProveedor(
            int idProveedor) {

        return service.listarSolicitudesProveedor(idProveedor);
    }

    public void rechazarSolicitud(
            int idProveedor,
            int idSolicitud,
            String motivo) {

        service.rechazarSolicitud(
                idProveedor,
                idSolicitud,
                motivo);
    }

    public int enviarPropuesta(
            int idProveedor,
            int idSolicitud,
            BigDecimal valor,
            String descripcion,
            String condiciones) {

        return service.enviarPropuesta(
                idProveedor,
                idSolicitud,
                valor,
                descripcion,
                condiciones);
    }

    public List<MensajeSolicitud> listarMensajes(
            int idSolicitud,
            int idUsuario) {

        return service.listarMensajes(
                idSolicitud,
                idUsuario);
    }

    public void enviarMensaje(
            int idSolicitud,
            int idUsuario,
            String contenido,
            String adjuntoUrl) {

        service.enviarMensaje(
                idSolicitud,
                idUsuario,
                contenido,
                adjuntoUrl);
    }

    public void rechazarPropuesta(
            int idOrganizador,
            int idPropuesta) {

        service.rechazarPropuesta(
                idOrganizador,
                idPropuesta);
    }

    public void seleccionarPropuesta(
            int idOrganizador,
            int idPropuesta) {

        service.seleccionarPropuesta(
                idOrganizador,
                idPropuesta);
    }

    public List<PropuestaServicio> listarPropuestasOrganizador(
            int idOrganizador) {

        return service.listarPropuestasOrganizador(
                idOrganizador);
    }

    public List<String> listarNecesidadesOrganizador(
            int idOrganizador) {

        return service.listarNecesidadesOrganizador(
                idOrganizador);
    }

    public int crearSolicitudParaNecesidad(
            int idOrganizador,
            int idEvento,
            int idServicio,
            int idNecesidadExistente) {

        return service.crearSolicitudParaNecesidad(
                idOrganizador,
                idEvento,
                idServicio,
                idNecesidadExistente);
    }

    public int crearSolicitud(
            int idOrganizador,
            int idEvento,
            int idServicio,
            String descripcionNecesidad) {

        return service.crearSolicitud(
                idOrganizador,
                idEvento,
                idServicio,
                descripcionNecesidad);
    }

    public List<PropuestaServicio> listarHistorialPropuestas(
            int idProveedor,
            int idSolicitud) {

        return service.listarHistorialPropuestas(
                idProveedor,
                idSolicitud);
    }
}
