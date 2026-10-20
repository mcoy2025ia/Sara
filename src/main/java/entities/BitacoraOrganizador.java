
package entities;

import java.time.LocalDateTime;

/**
 * Representa una entrada de auditoria del proceso
 * de verificacion y aprobacion de organizadores (CU-030).
 */
public class BitacoraOrganizador {

    private int idBitacora;
    private int idOrganizador;
    private int idAdministrador;

    private String accion;

    private String estadoAnterior;
    private String estadoNuevo;

    private String categoriaAnterior;
    private String categoriaNueva;

    private Integer puntaje;
    private String justificacion;
    private String detalles;

    private LocalDateTime fecha;

    public BitacoraOrganizador() {
    }

    public BitacoraOrganizador(
            int idOrganizador,
            int idAdministrador,
            String accion,
            String estadoAnterior,
            String estadoNuevo,
            String categoriaAnterior,
            String categoriaNueva,
            Integer puntaje,
            String justificacion,
            String detalles) {

        this.idOrganizador = idOrganizador;
        this.idAdministrador = idAdministrador;
        this.accion = accion;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.categoriaAnterior = categoriaAnterior;
        this.categoriaNueva = categoriaNueva;
        this.puntaje = puntaje;
        this.justificacion = justificacion;
        this.detalles = detalles;
    }

    public int getIdBitacora() {
        return idBitacora;
    }

    public void setIdBitacora(int idBitacora) {
        this.idBitacora = idBitacora;
    }

    public int getIdOrganizador() {
        return idOrganizador;
    }

    public void setIdOrganizador(int idOrganizador) {
        this.idOrganizador = idOrganizador;
    }

    public int getIdAdministrador() {
        return idAdministrador;
    }

    public void setIdAdministrador(int idAdministrador) {
        this.idAdministrador = idAdministrador;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(String estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getCategoriaAnterior() {
        return categoriaAnterior;
    }

    public void setCategoriaAnterior(String categoriaAnterior) {
        this.categoriaAnterior = categoriaAnterior;
    }

    public String getCategoriaNueva() {
        return categoriaNueva;
    }

    public void setCategoriaNueva(String categoriaNueva) {
        this.categoriaNueva = categoriaNueva;
    }

    public Integer getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(Integer puntaje) {
        this.puntaje = puntaje;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public void setJustificacion(String justificacion) {
        this.justificacion = justificacion;
    }

    public String getDetalles() {
        return detalles;
    }

    public void setDetalles(String detalles) {
        this.detalles = detalles;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    /**
     * Valida los datos minimos antes de registrar la auditoria.
     */
    public boolean esValida() {
        return idOrganizador > 0
                && idAdministrador > 0
                && accion != null
                && !accion.isBlank()
                && accion.trim().length() <= 80
                && (puntaje == null
                    || (puntaje >= 0 && puntaje <= 100))
                && (justificacion == null
                    || justificacion.length() <= 500)
                && (detalles == null
                    || detalles.length() <= 1000);
    }
}
