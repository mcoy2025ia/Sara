package controllers;

import entities.DocumentoProveedor;
import entities.NotificacionInterna;
import entities.Proveedor;
import services.ServiceAdministracion;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador del modulo de Administracion.
 *
 * Recibe las acciones de la interfaz y las delega al servicio,
 * manteniendo separadas la presentacion y las reglas de negocio.
 */
public class ControllerAdministracion {

    private final ServiceAdministracion serviceAdministracion;

    public ControllerAdministracion(
            ServiceAdministracion serviceAdministracion) {

        if (serviceAdministracion == null) {
            throw new IllegalArgumentException(
                    "El servicio de administracion es obligatorio.");
        }

        this.serviceAdministracion = serviceAdministracion;
    }

    // ---------------------------------------------------------
    // CONSULTA DE PROVEEDORES
    // ---------------------------------------------------------

    public List<Proveedor> consultarProveedoresPendientes() {
        return serviceAdministracion.consultarProveedoresPendientes();
    }

    public Proveedor consultarProveedor(int idProveedor) {
        return serviceAdministracion.consultarProveedor(idProveedor);
    }

    // ---------------------------------------------------------
    // VERIFICACION DE INFORMACION
    // ---------------------------------------------------------

    public boolean verificarInformacionProveedor(int idProveedor) {
        return serviceAdministracion.verificarInformacionProveedor(
                idProveedor);
    }

    public List<String> consultarServiciosProveedor(int idProveedor) {
        return serviceAdministracion.consultarServiciosProveedor(
                idProveedor);
    }

    // ---------------------------------------------------------
    // DOCUMENTOS DEL PROVEEDOR
    // ---------------------------------------------------------

    public List<DocumentoProveedor> consultarDocumentosProveedor(
            int idProveedor) {

        return serviceAdministracion.consultarDocumentosProveedor(
                idProveedor);
    }

    public int registrarDocumento(DocumentoProveedor documento) {
        return serviceAdministracion.registrarDocumento(documento);
    }

    public void actualizarDocumentoCorregido(
            int idProveedor,
            int idDocumento,
            String nombreArchivo,
            String rutaArchivo,
            LocalDate fechaVencimiento) {

        serviceAdministracion.actualizarDocumentoCorregido(
                idProveedor,
                idDocumento,
                nombreArchivo,
                rutaArchivo,
                fechaVencimiento);
    }

    public void marcarDocumentoValidado(
            int idProveedor, int idDocumento) {

        serviceAdministracion.marcarDocumentoValidado(
                idProveedor, idDocumento);
    }

    public void marcarDocumentoPorCorregir(
            int idProveedor,
            int idDocumento,
            String observacion) {

        serviceAdministracion.marcarDocumentoPorCorregir(
                idProveedor, idDocumento, observacion);
    }

    // ---------------------------------------------------------
    // SOLICITUD DE CORRECCIONES
    // ---------------------------------------------------------

    public void solicitarCorrecciones(
            int idProveedor, String observacion) {

        serviceAdministracion.solicitarCorrecciones(
                idProveedor, observacion);
    }

    // ---------------------------------------------------------
    // APROBACION Y RECHAZO
    // ---------------------------------------------------------

    public void aprobarProveedor(int idProveedor) {
        serviceAdministracion.aprobarProveedor(idProveedor);
    }

    public void rechazarProveedor(int idProveedor, String motivo) {
        serviceAdministracion.rechazarProveedor(
                idProveedor, motivo);
    }

    // ---------------------------------------------------------
    // NOTIFICACIONES INTERNAS
    // ---------------------------------------------------------

    public List<NotificacionInterna> consultarNotificacionesPorUsuario(
            int idUsuario) {

        return serviceAdministracion.consultarNotificacionesPorUsuario(
                idUsuario);
    }
}