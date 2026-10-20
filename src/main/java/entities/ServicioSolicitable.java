package entities;

import java.math.BigDecimal;

public record ServicioSolicitable(int id, String nombre, String proveedor, BigDecimal costo) {
    @Override public String toString() { return nombre+" · "+proveedor+" · $"+costo.toPlainString()+" COP"; }
}
