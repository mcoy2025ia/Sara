
package entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa un documento presentado por un organizador
 * para el proceso de verificacion del CU-030.
 */
public class DocumentoOrganizador {

    private int idDocumento;
    private int idOrganizador;
    private String tipoDocumento;
    private String nombreArchivo;
    private String rutaArchivo;
    private String numeroDocumento;
    private LocalDate fechaVencimiento;
    private String estado;
    private String observacion;
    private LocalDateTime fechaCarga;

    public DocumentoOrganizador() {
        this.estado = "PRESENTADO";
    }

    public DocumentoOrganizador(
            int idOrganizador,
            String tipoDocumento,
            String nombreArchivo,
            String rutaArchivo,
            String numeroDocumento,
            LocalDate fechaVencimiento) {

        this.idOrganizador = idOrganizador;
        this.tipoDocumento = tipoDocumento;
        this.nombreArchivo = nombreArchivo;
        this.rutaArchivo = rutaArchivo;
        this.numeroDocumento = numeroDocumento;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = "PRESENTADO";
    }

    public int getIdDocumento() {
        return idDocumento;
    }

    public void setIdDocumento(int idDocumento) {
        this.idDocumento = idDocumento;
    }

    public int getIdOrganizador() {
        return idOrganizador;
    }

    public void setIdOrganizador(int idOrganizador) {
        this.idOrganizador = idOrganizador;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(LocalDateTime fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    /**
     * Indica si el documento tiene una fecha de vencimiento
     * anterior al dia actual.
     */
    public boolean estaVencido() {
        return fechaVencimiento != null
                && fechaVencimiento.isBefore(LocalDate.now());
    }

    /**
     * Comprueba si el documento esta listo para la aprobacion.
     */
    public boolean estaValidado() {
        return "VALIDADO".equalsIgnoreCase(estado)
                && !estaVencido();
    }
}
