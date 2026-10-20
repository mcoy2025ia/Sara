package services;

import java.math.BigDecimal;
import java.util.List;

import entities.MensajeSolicitud;
import entities.PropuestaServicio;
import entities.SesionUsuario;
import entities.SolicitudEjecucion;
import repositories.RepositoryMensajesSolicitud;
import repositories.RepositoryPropuestaServicio;
import repositories.RepositorySolicitudEjecucion;

public class ServiceSolicitudes {

    private final RepositorySolicitudEjecucion repository;
    private final RepositoryPropuestaServicio repositoryPropuestas;
    private final RepositoryMensajesSolicitud repositoryMensajes
            = new RepositoryMensajesSolicitud();
    private final SesionUsuario sesion;

    public ServiceSolicitudes(
            RepositorySolicitudEjecucion repository,
            RepositoryPropuestaServicio repositoryPropuestas,
            SesionUsuario sesion) {

        if (sesion == null) {
            throw new IllegalArgumentException(
                    "Se requiere una sesion para habilitar CU12.");
        }

        this.repository = repository;
        this.repositoryPropuestas = repositoryPropuestas;
        this.sesion = sesion;
    }

    private void exigirSesion(String rolRequerido, int idActor) {
        exigirSesionAutenticada();

        if (!rolRequerido.equalsIgnoreCase(sesion.getTipoUsuario())) {
            throw new IllegalStateException(
                    "Tu rol no permite realizar esta operacion.");
        }

        if (sesion.getIdActor() != idActor) {
            throw new IllegalStateException(
                    "La operacion no pertenece a tu cuenta.");
        }
    }

    private void exigirSesionAutenticada() {
        if (sesion == null
                || sesion.getIdUsuario() <= 0
                || sesion.getIdActor() <= 0
                || !"ACTIVO".equalsIgnoreCase(
                        sesion.getEstadoUsuario())) {
            throw new IllegalStateException(
                    "Se requiere una sesion activa para esta operacion.");
        }

        String tipo = sesion.getTipoUsuario();
        boolean actorActivo
                = "PROVEEDOR".equalsIgnoreCase(tipo)
                ? ("APROBADO".equalsIgnoreCase(sesion.getEstadoActor()) || "VERIFICADO".equalsIgnoreCase(sesion.getEstadoActor()))
                : "ORGANIZADOR".equalsIgnoreCase(tipo)
                && "ACTIVO".equalsIgnoreCase(sesion.getEstadoActor());

        if (!actorActivo) {
            throw new IllegalStateException(
                    "La cuenta no tiene permisos activos para esta operacion.");
        }
    }

    public List<SolicitudEjecucion> listarSolicitudesProveedor(
            int idProveedor) {

        if (idProveedor <= 0) {
            throw new IllegalArgumentException(
                    "El proveedor no es valido.");
        }
        exigirSesion("PROVEEDOR", idProveedor);

        return repository.listarPorProveedor(idProveedor);
    }

    public void rechazarSolicitud(
            int idProveedor,
            int idSolicitud,
            String motivo) {

        if (idProveedor <= 0 || idSolicitud <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }
        exigirSesion("PROVEEDOR", idProveedor);

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes indicar el motivo del rechazo.");
        }

        String motivoLimpio = motivo.trim();

        if (motivoLimpio.length() > 500) {
            throw new IllegalArgumentException(
                    "El motivo no puede superar los 500 caracteres.");
        }

        boolean rechazada = repository.rechazarSolicitud(
                idProveedor,
                idSolicitud,
                motivoLimpio);

        if (!rechazada) {
            throw new IllegalStateException(
                    "La solicitud no existe, no pertenece al proveedor "
                    + "o ya fue respondida.");
        }
    }

    public int enviarPropuesta(
            int idProveedor,
            int idSolicitud,
            BigDecimal valor,
            String descripcion,
            String condiciones) {

        if (idProveedor <= 0 || idSolicitud <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }
        exigirSesion("PROVEEDOR", idProveedor);

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El precio final debe ser mayor que cero.");
        }

        if (valor.scale() > 2
                && valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "El precio no puede tener mas de dos decimales.");
        }

        if (valor.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "El precio supera el valor permitido.");
        }

        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes describir el alcance de la propuesta.");
        }

        String descripcionLimpia = descripcion.trim();

        if (descripcionLimpia.length() > 255) {
            throw new IllegalArgumentException(
                    "La descripcion no puede superar los 255 caracteres.");
        }

        if (condiciones == null || condiciones.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes indicar las condiciones de la propuesta.");
        }

        String condicionesLimpias = condiciones.trim();

        if (condicionesLimpias.length() > 1000) {
            throw new IllegalArgumentException(
                    "Las condiciones no pueden superar los 1000 caracteres.");
        }

        return repositoryPropuestas.guardarNuevaVersion(
                idProveedor,
                idSolicitud,
                valor,
                descripcionLimpia,
                condicionesLimpias);
    }

    public List<MensajeSolicitud> listarMensajes(
            int idSolicitud,
            int idUsuario) {

        if (idSolicitud <= 0 || idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }
        exigirSesionAutenticada();
        if (idUsuario != sesion.getIdUsuario()) {
            throw new IllegalStateException(
                    "No puedes consultar mensajes con otra cuenta.");
        }

        return repositoryMensajes.listarMensajes(
                idSolicitud,
                idUsuario);
    }

    public void enviarMensaje(
            int idSolicitud,
            int idUsuario,
            String contenido,
            String adjuntoUrl) {

        if (idSolicitud <= 0 || idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }
        exigirSesionAutenticada();
        if (idUsuario != sesion.getIdUsuario()) {
            throw new IllegalStateException(
                    "No puedes enviar mensajes con otra cuenta.");
        }

        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException(
                    "El mensaje no puede estar vacio.");
        }

        String texto = contenido.trim();

        if (texto.length() > 1000) {
            throw new IllegalArgumentException(
                    "El mensaje no puede superar los 1000 caracteres.");
        }

        String enlace = null;

        if (adjuntoUrl != null && !adjuntoUrl.isBlank()) {

            enlace = adjuntoUrl.trim();

            if (enlace.length() > 1000) {
                throw new IllegalArgumentException(
                        "El enlace no puede superar los 1000 caracteres.");
            }
        }

        repositoryMensajes.enviarMensaje(
                idSolicitud,
                idUsuario,
                texto,
                enlace);
    }

    public void rechazarPropuesta(
            int idOrganizador,
            int idPropuesta) {

        if (idOrganizador <= 0 || idPropuesta <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }
        exigirSesion("ORGANIZADOR", idOrganizador);

        repositoryPropuestas.rechazarPropuesta(
                idOrganizador,
                idPropuesta);
    }

    public void seleccionarPropuesta(
            int idOrganizador,
            int idPropuesta) {

        if (idOrganizador <= 0 || idPropuesta <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }
        exigirSesion("ORGANIZADOR", idOrganizador);

        repositoryPropuestas.seleccionarPropuesta(
                idOrganizador,
                idPropuesta);
    }

    public List<PropuestaServicio> listarPropuestasOrganizador(
            int idOrganizador) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del organizador no es valido.");
        }
        exigirSesion("ORGANIZADOR", idOrganizador);

        return repositoryPropuestas.listarPropuestasOrganizador(
                idOrganizador);
    }

    public List<String> listarNecesidadesOrganizador(
            int idOrganizador) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "El organizador no es valido.");
        }
        exigirSesion("ORGANIZADOR", idOrganizador);

        return repository.listarNecesidadesOrganizador(
                idOrganizador);
    }

    public int crearSolicitudParaNecesidad(
            int idOrganizador,
            int idEvento,
            int idServicio,
            int idNecesidadExistente) {

        if (idOrganizador <= 0
                || idEvento <= 0
                || idServicio <= 0
                || idNecesidadExistente <= 0) {

            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }
        exigirSesion("ORGANIZADOR", idOrganizador);

        return repository.crearSolicitudParaNecesidad(
                idOrganizador,
                idEvento,
                idServicio,
                idNecesidadExistente,
                null);
    }

    public int crearSolicitud(
            int idOrganizador,
            int idEvento,
            int idServicio,
            String descripcionNecesidad) {

        if (idOrganizador <= 0
                || idEvento <= 0
                || idServicio <= 0) {

            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }
        exigirSesion("ORGANIZADOR", idOrganizador);

        if (descripcionNecesidad == null
                || descripcionNecesidad.isBlank()) {

            throw new IllegalArgumentException(
                    "Debes describir la necesidad del evento.");
        }

        String descripcion = descripcionNecesidad.trim();

        if (descripcion.length() > 500) {
            throw new IllegalArgumentException(
                    "La descripcion no puede superar los 500 caracteres.");
        }

        return repository.crearSolicitud(
                idOrganizador,
                idEvento,
                idServicio,
                descripcion);
    }

    public List<PropuestaServicio> listarHistorialPropuestas(
            int idProveedor,
            int idSolicitud) {

        if (idProveedor <= 0 || idSolicitud <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }
        exigirSesion("PROVEEDOR", idProveedor);

        return repositoryPropuestas.listarHistorial(
                idProveedor,
                idSolicitud);
    }
}
