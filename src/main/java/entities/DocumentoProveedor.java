package entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa un documento presentado por un proveedor
 * durante el proceso de verificacion del CU-028.
 */
public class DocumentoProveedor {

    public static final String ESTADO_PRESENTADO = "PRESENTADO";
    public static final String ESTADO_VALIDADO = "VALIDADO";
    public static final String ESTADO_POR_CORREGIR = "POR_CORREGIR";
    public static final String ESTADO_VENCIDO = "VENCIDO";

    private final int idDocumento;
    private final int idProveedor;
    private final String tipoDocumento;
    private final String nombreArchivo;
    private final String rutaArchivo;
    private final LocalDate fechaVencimiento;

    private String estado;
    private String observacion;

    private final LocalDateTime fechaCarga;

    public DocumentoProveedor(
            int idDocumento,
            int idProveedor,
            String tipoDocumento,
            String nombreArchivo,
            String rutaArchivo,
            LocalDate fechaVencimiento,
            String estado,
            String observacion,
            LocalDateTime fechaCarga) {

        this.idDocumento = idDocumento;
        this.idProveedor = idProveedor;
        this.tipoDocumento = tipoDocumento;
        this.nombreArchivo = nombreArchivo;
        this.rutaArchivo = rutaArchivo;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = estado;
        this.observacion = observacion;
        this.fechaCarga = fechaCarga;
    }

    /**
     * Constructor para registrar un documento nuevo.
     * La base de datos genera el identificador y la fecha de carga.
     */
    public DocumentoProveedor(
            int idProveedor,
            String tipoDocumento,
            String nombreArchivo,
            String rutaArchivo,
            LocalDate fechaVencimiento) {

        this(
                0,
                idProveedor,
                tipoDocumento,
                nombreArchivo,
                rutaArchivo,
                fechaVencimiento,
                ESTADO_PRESENTADO,
                null,
                null
        );
    }

    public int getIdDocumento() {
        return idDocumento;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public String getEstado() {
        return estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}