package services;

import entities.DocumentoProveedor;
import entities.NotificacionInterna;
import entities.Proveedor;
import entities.SesionUsuario;
import repositories.RepositoryProveedor;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reglas de negocio del caso de uso CU-028:
 * Aprobar proveedores.
 */
public class ServiceAdministracion {

    private static final Pattern CORREO =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final RepositoryProveedor repositoryProveedor;
    private final SesionUsuario sesion;

    public ServiceAdministracion(RepositoryProveedor repositoryProveedor) {
        this(repositoryProveedor,null);
    }

    public ServiceAdministracion(RepositoryProveedor repositoryProveedor, SesionUsuario sesion) {
        if (repositoryProveedor == null) {
            throw new IllegalArgumentException(
                    "El repositorio de proveedores es obligatorio.");
        }

        this.repositoryProveedor = repositoryProveedor;
        this.sesion=sesion;
    }

    // ---------------------------------------------------------
    // CONSULTA DE PROVEEDORES
    // ---------------------------------------------------------

    public List<Proveedor> consultarProveedoresPendientes() {
        validarSesionAdministrativa();
        return repositoryProveedor.listarProveedoresPendientes();
    }

    public Proveedor consultarProveedor(int idProveedor) {
        validarSesionAdministrativa();
        validarIdentificador(idProveedor);

        return repositoryProveedor.buscarProveedor(idProveedor);
    }

    // ---------------------------------------------------------
    // DOCUMENTOS Y SERVICIOS
    // ---------------------------------------------------------

    public List<DocumentoProveedor> consultarDocumentosProveedor(
            int idProveedor) {
        validarSesionAdministrativa();

        exigirProveedorPendiente(idProveedor);

        return repositoryProveedor.listarDocumentosPorProveedor(idProveedor);
    }

    public List<String> consultarServiciosProveedor(int idProveedor) {
        validarSesionAdministrativa();
        exigirProveedorPendiente(idProveedor);

        return repositoryProveedor.listarServiciosProveedor(idProveedor);
    }

    /**
     * Registra los datos de un documento presentado por un proveedor.
     * El repositorio conserva su identificador y sus metadatos.
     */
    public int registrarDocumento(DocumentoProveedor documento) {
        validarSesionAdministrativa();
        if (documento == null) {
            throw new IllegalArgumentException(
                    "Debes proporcionar los datos del documento.");
        }

        exigirProveedorPendiente(documento.getIdProveedor());

        return repositoryProveedor.guardarDocumento(documento);
    }

    /**
     * Registra la información de un documento corregido.
     * Después de actualizarlo, el documento queda pendiente de revisión.
     */
    public void actualizarDocumentoCorregido(
            int idProveedor,
            int idDocumento,
            String nombreArchivo,
            String rutaArchivo,
            LocalDate fechaVencimiento) {
        validarSesionAdministrativa();

        exigirProveedorPendiente(idProveedor);

        repositoryProveedor.actualizarDocumentoCorregido(
                idDocumento,
                idProveedor,
                nombreArchivo,
                rutaArchivo,
                fechaVencimiento
        );
    }

    /**
     * El administrador confirma que ha revisado un documento.
     */
    public void marcarDocumentoValidado(
            int idProveedor, int idDocumento) {
        validarSesionAdministrativa();

        exigirProveedorPendiente(idProveedor);

        List<DocumentoProveedor> documentos =
                repositoryProveedor.listarDocumentosPorProveedor(idProveedor);

        DocumentoProveedor documento = buscarDocumento(
                documentos, idDocumento);

        if (documento == null) {
            throw new IllegalArgumentException(
                    "El documento no pertenece al proveedor seleccionado.");
        }

        if (estaVencido(documento)) {
            repositoryProveedor.actualizarEstadoDocumento(
                    idDocumento,
                    DocumentoProveedor.ESTADO_VENCIDO,
                    "El documento ha superado su fecha de vencimiento."
            );

            throw new IllegalArgumentException(
                    "No se puede validar un documento vencido.");
        }

        repositoryProveedor.actualizarEstadoDocumento(
                idDocumento,
                DocumentoProveedor.ESTADO_VALIDADO,
                null
        );
    }

    /**
     * El administrador indica que un documento necesita correcciones.
     */
    public void marcarDocumentoPorCorregir(
            int idProveedor,
            int idDocumento,
            String observacion) {
        validarSesionAdministrativa();

        exigirProveedorPendiente(idProveedor);

        if (observacion == null || observacion.isBlank()) {
            throw new IllegalArgumentException(
                    "Explica qué debe corregirse en el documento.");
        }

        List<DocumentoProveedor> documentos =
                repositoryProveedor.listarDocumentosPorProveedor(idProveedor);

        DocumentoProveedor documento = buscarDocumento(
                documentos, idDocumento);

        if (documento == null) {
            throw new IllegalArgumentException(
                    "El documento no pertenece al proveedor seleccionado.");
        }

        repositoryProveedor.actualizarEstadoDocumento(
                idDocumento,
                DocumentoProveedor.ESTADO_POR_CORREGIR,
                observacion.trim()
        );
    }

    // ---------------------------------------------------------
    // VERIFICACIÓN DE INFORMACIÓN
    // ---------------------------------------------------------

    /**
     * Comprueba los datos disponibles del proveedor y sus documentos.
     *
     * Para aprobar, los datos básicos deben ser válidos, debe existir
     * al menos un documento y todos los documentos deben estar validados
     * y vigentes.
     */
    public boolean verificarInformacionProveedor(int idProveedor) {
        validarSesionAdministrativa();
        Proveedor proveedor = obtenerProveedorPendiente(idProveedor);

        if (proveedor == null || !datosBasicosValidos(proveedor)) {
            return false;
        }

        List<DocumentoProveedor> documentos =
                repositoryProveedor.listarDocumentosPorProveedor(idProveedor);

        if (documentos.isEmpty()) {
            return false;
        }

        for (DocumentoProveedor documento : documentos) {
            if (estaVencido(documento)) {
                repositoryProveedor.actualizarEstadoDocumento(
                        documento.getIdDocumento(),
                        DocumentoProveedor.ESTADO_VENCIDO,
                        "El documento ha superado su fecha de vencimiento."
                );

                return false;
            }

            if (!DocumentoProveedor.ESTADO_VALIDADO.equals(
                    documento.getEstado())) {
                return false;
            }
        }

        return true;
    }

    private boolean datosBasicosValidos(Proveedor proveedor) {
        if (proveedor.getNombre() == null
                || proveedor.getNombre().isBlank()) {
            return false;
        }

        String correo = proveedor.getCorreo();

        return correo != null
                && !correo.isBlank()
                && CORREO.matcher(correo.trim()).matches();
    }

    private boolean estaVencido(DocumentoProveedor documento) {
        LocalDate vencimiento = documento.getFechaVencimiento();

        return vencimiento != null
                && vencimiento.isBefore(LocalDate.now());
    }

    // ---------------------------------------------------------
    // APROBACIÓN
    // ---------------------------------------------------------

    /**
     * Aprueba al proveedor cuando la información y todos sus documentos
     * han sido revisados. El repositorio guarda el estado y la notificación
     * dentro de una misma transacción.
     */
    public void aprobarProveedor(int idProveedor) {
        validarSesionAdministrativa();
        Proveedor proveedor = exigirProveedorPendiente(idProveedor);

        if (!verificarInformacionProveedor(idProveedor)) {
            throw new IllegalArgumentException(
                    "No se puede aprobar el proveedor. Revisa sus datos "
                    + "y confirma que tenga documentos válidos y vigentes.");
        }

        repositoryProveedor.aprobarProveedorConNotificacion(
                proveedor.getIdProveedor()
        );
    }

    // ---------------------------------------------------------
    // SOLICITUD DE CORRECCIONES
    // ---------------------------------------------------------

    /**
     * Mantiene al proveedor pendiente y registra qué información necesita
     * corregir. El repositorio guarda la observación y su notificación.
     */
    public void solicitarCorrecciones(
            int idProveedor, String observacion) {
        validarSesionAdministrativa();

        exigirProveedorPendiente(idProveedor);

        if (observacion == null || observacion.isBlank()) {
            throw new IllegalArgumentException(
                    "Indica la información o los documentos que debe corregir.");
        }

        if (observacion.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "La observación debe tener máximo 500 caracteres.");
        }

        repositoryProveedor.solicitarCorrecciones(
                idProveedor, observacion.trim()
        );
    }

    // ---------------------------------------------------------
    // RECHAZO
    // ---------------------------------------------------------

    /**
     * Rechaza al proveedor únicamente si continúa pendiente.
     * El repositorio registra el motivo y la notificación.
     */
    public void rechazarProveedor(int idProveedor, String motivo) {
        validarSesionAdministrativa();
        exigirProveedorPendiente(idProveedor);

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Escribe el motivo del rechazo.");
        }

        String motivoLimpio = motivo.trim();

        if (motivoLimpio.length() > 500) {
            throw new IllegalArgumentException(
                    "El motivo del rechazo debe tener máximo 500 caracteres.");
        }

        repositoryProveedor.rechazarProveedor(
                idProveedor, motivoLimpio
        );
    }

    // ---------------------------------------------------------
    // NOTIFICACIONES
    // ---------------------------------------------------------

    public List<NotificacionInterna> consultarNotificacionesPorUsuario(
            int idUsuario) {
        validarSesionAdministrativa();

        if (idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del usuario no es válido.");
        }

        return repositoryProveedor.listarNotificacionesPorUsuario(idUsuario);
    }

    // ---------------------------------------------------------
    // UTILIDADES
    // ---------------------------------------------------------

    private void validarSesionAdministrativa() {
        if(sesion==null || sesion.getIdUsuario()<=0
                || !"ADMINISTRADOR".equalsIgnoreCase(sesion.getTipoUsuario())
                || !"ACTIVO".equalsIgnoreCase(sesion.getEstadoUsuario())
                || !repositoryProveedor.administradorActivo(sesion.getIdUsuario()))
            throw new IllegalStateException("Inicia sesión como administrador activo para gestionar proveedores.");
    }

    private Proveedor exigirProveedorPendiente(int idProveedor) {
        Proveedor proveedor = obtenerProveedorPendiente(idProveedor);

        if (proveedor == null) {
            throw new IllegalArgumentException(
                    "El proveedor no existe o ya no está pendiente "
                    + "de verificación.");
        }

        return proveedor;
    }

    private Proveedor obtenerProveedorPendiente(int idProveedor) {
        validarIdentificador(idProveedor);

        Proveedor proveedor =
                repositoryProveedor.buscarProveedor(idProveedor);

        if (proveedor == null || !esEstadoPendiente(proveedor.getEstado())) {
            return null;
        }

        return proveedor;
    }

    private void validarIdentificador(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un identificador válido.");
        }
    }

    private boolean esEstadoPendiente(String estado) {
        if (estado == null) {
            return false;
        }

        String normalizado = estado.trim().toUpperCase()
                .replace(' ', '_')
                .replace('-', '_')
                .replace('Á', 'A')
                .replace('É', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ú', 'U');

        return normalizado.equals("PENDIENTE")
                || normalizado.equals("PENDIENTE_VERIFICACION")
                || normalizado.equals("PENDIENTE_DE_VERIFICACION")
                || normalizado.equals("PENDIENTE_REVISION")
                || normalizado.equals("PENDIENTE_DE_REVISION");
    }

    private DocumentoProveedor buscarDocumento(
            List<DocumentoProveedor> documentos, int idDocumento) {

        for (DocumentoProveedor documento : documentos) {
            if (documento.getIdDocumento() == idDocumento) {
                return documento;
            }
        }

        return null;
    }
}