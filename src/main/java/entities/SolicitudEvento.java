package entities;

//Cuantas veces el cliente busco un evento
public class SolicitudEvento {
    private final int idCliente;
    private final String tipo;
    private final int cantidad;

    public SolicitudEvento(int idCliente, String tipo, int cantidad) {
        this.idCliente = idCliente;
        this.tipo = tipo;
        this.cantidad = cantidad;
    }

    public int getIdCliente() { return idCliente; }
    public String getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
}