package entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovimientoFinanciero {

    // Atributos basados en la tabla Movimiento_financiero
    private int idMovimiento;
    private int idPropuesta;
    private int idEvento;
    private int idContratacion;
    // Importe monetario: BigDecimal, igual que la columna DECIMAL(12,2).
    private BigDecimal valor;
    private LocalDateTime fecha;
    private String descripcion;
    private String tipo;
    private String estado;

    // Constructor vacío
    public MovimientoFinanciero() {
    }

    // Constructor con todos los atributos
    public MovimientoFinanciero(int idMovimiento, int idPropuesta, int idEvento, int idContratacion,
                                BigDecimal valor, LocalDateTime fecha, String descripcion,
                                String tipo, String estado) {
        this.idMovimiento = idMovimiento;
        this.idPropuesta = idPropuesta;
        this.idEvento = idEvento;
        this.idContratacion = idContratacion;
        this.valor = valor;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.estado = estado;
    }

    // Getters y Setters
    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public int getIdPropuesta() {
        return idPropuesta;
    }

    public void setIdPropuesta(int idPropuesta) {
        this.idPropuesta = idPropuesta;
    }

    public int getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(int idEvento) {
        this.idEvento = idEvento;
    }

    public int getIdContratacion() {
        return idContratacion;
    }

    public void setIdContratacion(int idContratacion) {
        this.idContratacion = idContratacion;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}