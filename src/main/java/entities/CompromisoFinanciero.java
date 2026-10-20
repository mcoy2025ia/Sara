package entities;

import java.math.BigDecimal;

/**
 * CU06: una contratación del evento que compromete parte de su presupuesto.
 * valor es null cuando la contratación no tiene valor acordado ni propuesta con valor.
 */
public class CompromisoFinanciero {

    private final int idContratacion;
    private final String nombre;
    private final String categoria;
    private final BigDecimal valor;
    private final String estado;

    public CompromisoFinanciero(int idContratacion, String nombre, String categoria,
                                BigDecimal valor, String estado) {
        this.idContratacion = idContratacion;
        this.nombre = nombre;
        this.categoria = categoria;
        this.valor = valor;
        this.estado = estado;
    }

    public int getIdContratacion() {
        return idContratacion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getEstado() {
        return estado;
    }

    public boolean tieneValor() {
        return valor != null;
    }
}
