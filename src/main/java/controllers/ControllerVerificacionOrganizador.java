
package controllers;

import entities.BitacoraOrganizador;
import entities.DocumentoOrganizador;
import entities.EvaluacionOrganizador;
import entities.MensajeOrganizador;
import entities.Organizador;
import entities.SesionUsuario;
import services.ServiceVerificacionOrganizador;

import java.util.List;
import java.util.Map;

/**
 * Controlador de administracion para CU-030.
 *
 * Valida la sesion del administrador y coordina las solicitudes
 * de la vista con el servicio de administracion.
 */
public class ControllerVerificacionOrganizador {

    private final ServiceVerificacionOrganizador serviceAdministracion;
    private final SesionUsuario sesion;

    public ControllerVerificacionOrganizador(
            ServiceVerificacionOrganizador serviceAdministracion,
            SesionUsuario sesion) {

        if (serviceAdministracion == null) {
            throw new IllegalArgumentException(
                    "El servicio de administracion es obligatorio.");
        }

        if (sesion == null) {
            throw new IllegalArgumentException(
                    "Se necesita una sesion administrativa.");
        }

        this.serviceAdministracion = serviceAdministracion;
        this.sesion = sesion;
    }

    // ---------------------------------------------------------
    // CONSULTAS DE ORGANIZADORES
    // ---------------------------------------------------------

    /**
     * Consulta la lista de organizadores pendientes de verificacion.
     */
    public List<Organizador> consultarOrganizadoresPendientes() {
        validarAdministrador();

        return serviceAdministracion
                .consultarOrganizadoresPendientes();
    }

    /**
     * Consulta los datos completos del organizador seleccionado.
     */
    public Map<String, Object> consultarDetalleOrganizador(
            int idOrganizador) {

        validarAdministrador();

        return serviceAdministracion
                .consultarDetalleOrganizador(idOrganizador);
    }

    /**
     * Consulta los documentos asociados al perfil.
     */
    public List<DocumentoOrganizador> consultarDocumentosOrganizador(
            int idOrganizador) {

        validarAdministrador();

        return serviceAdministracion
                .consultarDocumentosOrganizador(idOrganizador);
    }

    // ---------------------------------------------------------
    // VERIFICACIONES
    // ---------------------------------------------------------

    /**
     * Comprueba que la informacion profesional este completa.
     */
    public boolean verificarInformacionOrganizador(
            int idOrganizador) {

        validarAdministrador();

        return serviceAdministracion
                .verificarInformacionOrganizador(idOrganizador);
    }

    /**
     * Comprueba los documentos presentados, su validacion
     * y su vigencia.
     */
    public boolean verificarDocumentacionOrganizador(
            int idOrganizador,
            Map<String, Object> archivos) {

        validarAdministrador();

        return serviceAdministracion
                .verificarDocumentacionOrganizador(
                        idOrganizador, archivos);
    }

    /**
     * Comprueba si el documento de identidad esta duplicado
     * en cuentas de clientes o proveedores.
     */
    public boolean verificarIdentidadDuplicada(
            int idOrganizador) {

        validarAdministrador();

        return serviceAdministracion
                .verificarIdentidadDuplicada(idOrganizador);
    }

    /**
     * Verifica que todos los requisitos basicos para aprobar
     * el perfil se cumplan.
     */
    public boolean verificarPerfilParaAprobacion(
            int idOrganizador) {

        validarAdministrador();

        return serviceAdministracion
                .verificarPerfilParaAprobacion(idOrganizador);
    }

    // ---------------------------------------------------------
    // EVALUACION Y CATEGORIZACION
    // ---------------------------------------------------------

    /**
     * Calcula el puntaje, la categoria sugerida y la necesidad
     * de una revision adicional de presupuesto.
     */
    public EvaluacionOrganizador evaluarOrganizador(
            int idOrganizador) {

        validarAdministrador();

        return serviceAdministracion
                .evaluarOrganizador(idOrganizador);
    }

    /**
     * Permite confirmar o modificar la categoria propuesta.
     * Si cambia manualmente, el servicio exige una justificacion.
     */
    public void categorizarOrganizador(
            EvaluacionOrganizador evaluacion,
            String categoria,
            String justificacion) {

        validarAdministrador();

        serviceAdministracion.categorizarOrganizador(
                evaluacion,
                categoria,
                justificacion);
    }

    /**
     * Registra el resultado de la validacion adicional
     * del presupuesto.
     */
    public void validarPresupuesto(
            EvaluacionOrganizador evaluacion,
            boolean cumpleCondiciones) {

        validarAdministrador();

        serviceAdministracion.validarPresupuesto(
                evaluacion,
                cumpleCondiciones);
    }

    // ---------------------------------------------------------
    // DECISION ADMINISTRATIVA
    // ---------------------------------------------------------

    /**
     * Confirma la aprobacion del organizador.
     *
     * El servicio verifica los documentos, la identidad,
     * los datos del perfil y el presupuesto antes de guardar.
     */
    public void aprobarOrganizador(
            EvaluacionOrganizador evaluacion) {

        validarAdministrador();

        serviceAdministracion
                .aprobarOrganizador(evaluacion);
    }

    /**
     * Variante con el identificador del organizador.
     */
    public void aprobarOrganizador(int idOrganizador) {

        validarAdministrador();

        serviceAdministracion
                .aprobarOrganizador(idOrganizador);
    }

    /**
     * Rechaza el organizador con un motivo obligatorio.
     */
    public void rechazarOrganizador(
            EvaluacionOrganizador evaluacion,
            String motivo) {

        validarAdministrador();

        serviceAdministracion
                .rechazarOrganizador(evaluacion, motivo);
    }

    /**
     * Variante con el identificador del organizador.
     */
    public void rechazarOrganizador(
            int idOrganizador, String motivo) {

        validarAdministrador();

        serviceAdministracion
                .rechazarOrganizador(idOrganizador, motivo);
    }

    // ---------------------------------------------------------
    // DOCUMENTOS Y CORRECCIONES
    // ---------------------------------------------------------

    /**
     * Guarda un documento asociado al organizador.
     */
    public void guardarDocumentoOrganizador(
            DocumentoOrganizador documento) {

        validarAdministrador();

        serviceAdministracion
                .guardarDocumentoOrganizador(documento);
    }

    /**
     * Actualiza el estado de un documento revisado.
     */
    public void actualizarEstadoDocumento(
            int idDocumento,
            String estado,
            String observacion) {

        validarAdministrador();

        serviceAdministracion.actualizarEstadoDocumento(
                idDocumento,
                estado,
                observacion);
    }

    /**
     * Solicita al organizador que corrija o complete informacion.
     * La solicitud se registra en el canal interno.
     */
    public void solicitarCorrecciones(
            int idOrganizador,
            String mensaje) {

        validarAdministrador();

        serviceAdministracion.solicitarCorrecciones(
                idOrganizador, mensaje);
    }

    // ---------------------------------------------------------
    // CANAL INTERNO DE COMUNICACION
    // ---------------------------------------------------------

    /**
     * Consulta los mensajes del organizador.
     */
    public List<MensajeOrganizador> consultarMensajes(
            int idOrganizador) {

        validarAdministrador();

        return serviceAdministracion
                .consultarMensajes(idOrganizador);
    }

    /**
     * Envia un mensaje del administrador al organizador.
     * El remitente se obtiene de la sesion, no de la vista.
     */
    public void enviarMensaje(
            int idOrganizador,
            String contenido,
            String tipoMensaje) {

        validarAdministrador();

        Map<String, Object> detalle =
                serviceAdministracion
                        .consultarDetalleOrganizador(idOrganizador);

        int idUsuarioOrganizador =
                ((Number) detalle.get("idUsuario")).intValue();

        serviceAdministracion.enviarMensaje(
                idOrganizador,
                sesion.getIdUsuario(),
                idUsuarioOrganizador,
                contenido,
                tipoMensaje);
    }

    // ---------------------------------------------------------
    // SESION Y SEGURIDAD
    // ---------------------------------------------------------

    public int obtenerIdAdministrador() {
        validarAdministrador();

        return sesion.getIdUsuario();
    }

    private void validarAdministrador() {

        if (sesion.getIdUsuario() <= 0
                || !"ADMINISTRADOR".equalsIgnoreCase(
                        sesion.getTipoUsuario())
                || !"ACTIVO".equalsIgnoreCase(
                        sesion.getEstadoUsuario())) {

            throw new IllegalStateException(
                    "La operacion requiere una sesion activa "
                    + "de administrador.");
        }
    }
}
