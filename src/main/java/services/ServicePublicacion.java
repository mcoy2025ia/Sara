package services;

import entities.*;
import repositories.RepositoryEvento;
import repositories.RepositoryPublicacion;
import java.util.List;

public class ServicePublicacion {
    private final RepositoryPublicacion repository;
    private final RepositoryEvento eventos;

    public ServicePublicacion(RepositoryPublicacion repository, RepositoryEvento eventos) {
        this.repository = repository;
        this.eventos = eventos;
    }

    public boolean esAdministrador(SesionUsuario sesion) {
        validarSesion(sesion);
        return "ADMINISTRADOR".equalsIgnoreCase(sesion.getTipoUsuario());
    }

    public String nombreUsuario(SesionUsuario sesion) {
        validarSesion(sesion);
        return sesion.getNombre();
    }

    private void validarSesion(SesionUsuario sesion) {
        if (sesion == null || sesion.getIdUsuario() <= 0 || !"ACTIVO".equalsIgnoreCase(sesion.getEstadoUsuario()))
            throw new IllegalStateException("Inicia sesión para acceder a la publicación de eventos.");
        if ("ORGANIZADOR".equalsIgnoreCase(sesion.getTipoUsuario())
                && sesion.getIdActor() > 0 && "ACTIVO".equalsIgnoreCase(sesion.getEstadoActor())
                && eventos.organizadorActivo(sesion.getIdUsuario(), sesion.getIdActor())) return;
        if ("ADMINISTRADOR".equalsIgnoreCase(sesion.getTipoUsuario())
                && repository.listarAdministradores().stream().anyMatch(a -> a.id() == sesion.getIdUsuario())) return;
        throw new IllegalStateException("Tu cuenta no tiene un rol activo para acceder a la publicación de eventos.");
    }

    private int organizador(SesionUsuario sesion) {
        if (esAdministrador(sesion)) throw new IllegalStateException("Solo el organizador puede solicitar la publicación de sus eventos.");
        return sesion.getIdActor();
    }

    public List<Evento> listarEventos(SesionUsuario sesion) {
        return eventos.listarEventosPorOrganizador(organizador(sesion));
    }

    public void solicitar(SesionUsuario sesion, int evento) {
        int organizador = organizador(sesion);
        if (evento <= 0) throw new IllegalArgumentException("Selecciona un evento de tu cuenta.");
        repository.solicitar(organizador, evento);
    }

    public void revisar(SesionUsuario sesion, int solicitud, boolean aprobar, String motivo) {
        if (!esAdministrador(sesion)) throw new IllegalStateException("Solo un administrador puede revisar solicitudes.");
        if (solicitud <= 0) throw new IllegalArgumentException("Selecciona una solicitud pendiente.");
        if (!aprobar && (motivo == null || motivo.isBlank() || motivo.trim().length() > 250))
            throw new IllegalArgumentException("Escribe el motivo del rechazo, de hasta 250 caracteres.");
        repository.revisar(solicitud, sesion.getIdUsuario(), aprobar, aprobar ? null : motivo.trim());
    }

    public List<SolicitudPublicacion> listarSolicitudes(SesionUsuario sesion) {
        return repository.listarSolicitudes(esAdministrador(sesion) ? null : sesion.getIdActor());
    }

    public List<PlantillaEvento> listarPlantillas(SesionUsuario sesion) {
        validarSesion(sesion);
        return repository.listarPlantillas();
    }

    public List<String> notificaciones(SesionUsuario sesion) {
        validarSesion(sesion);
        return repository.listarNotificaciones(sesion.getIdUsuario());
    }
}
