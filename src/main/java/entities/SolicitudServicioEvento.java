package entities;

import java.math.BigDecimal;

public record SolicitudServicioEvento(int id, String servicio, String proveedor, BigDecimal costo, String estado) {
    @Override public String toString() {
        return servicio+" · "+proveedor+" · "+estado+" · "+(costo==null ? "costo histórico por conciliar" : "$"+costo.toPlainString()+" COP");
    }
}
