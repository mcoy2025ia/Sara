package entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa una solicitud para contratar a un organizador
 * dentro del caso de uso CU-026.
 */
public class Contratacion {

    private int idContratacion;
    private int idCliente;
    private int idOrganizador;
    private String tipoEvento;
    private String ciudad;
    private LocalDate fechaAproximada;
    private BigDecimal presupuestoEstimado;
    private String criteriosBusqueda;
    private String estado;
    private LocalDateTime fechaSolicitud;

    public Contratacion() {
        this.estado = "PENDIENTE";
    }

    public Contratacion(
            int idCliente,
            int idOrganizador,
            String tipoEvento,
            String ciudad,
            LocalDate fechaAproximada,
            BigDecimal presupuestoEstimado,
            String criteriosBusqueda) {

        this.idCliente = idCliente;
        this.idOrganizador = idOrganizador;
        this.tipoEvento = tipoEvento;
        this.ciudad = ciudad;
        this.fechaAproximada = fechaAproximada;
        this.presupuestoEstimado = presupuestoEstimado;
        this.criteriosBusqueda = criteriosBusqueda;
        this.estado = "PENDIENTE";
    }

    public int getIdContratacion() {
        return idContratacion;
    }

    public void setIdContratacion(int idContratacion) {
        this.idContratacion = idContratacion;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public int getIdOrganizador() {
        return idOrganizador;
    }

    public void setIdOrganizador(int idOrganizador) {
        this.idOrganizador = idOrganizador;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public LocalDate getFechaAproximada() {
        return fechaAproximada;
    }

    public void setFechaAproximada(LocalDate fechaAproximada) {
        this.fechaAproximada = fechaAproximada;
    }

    public BigDecimal getPresupuestoEstimado() {
        return presupuestoEstimado;
    }

    public void setPresupuestoEstimado(BigDecimal presupuestoEstimado) {
        this.presupuestoEstimado = presupuestoEstimado;
    }

    public String getCriteriosBusqueda() {
        return criteriosBusqueda;
    }

    public void setCriteriosBusqueda(String criteriosBusqueda) {
        this.criteriosBusqueda = criteriosBusqueda;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }
}