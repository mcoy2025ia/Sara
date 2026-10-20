package entities;

/** Lugar donde se realiza un evento (tabla Lugar). Solo lectura desde CU-17. */
public class Lugar {
    private final int idLugar;
    private final String nombre;
    private final String direccion;
    private final int capacidad;
    private final String descripcion;
    private final String estado;

    public Lugar(int idLugar, String nombre, String direccion, int capacidad, String descripcion, String estado) {
        this.idLugar = idLugar;
        this.nombre = nombre;
        this.direccion = direccion;
        this.capacidad = capacidad;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public int getIdLugar() { return idLugar; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public int getCapacidad() { return capacidad; }
    public String getDescripcion() { return descripcion; }
    public String getEstado() { return estado; }

    @Override
    public String toString() {
        return nombre + " (aforo " + capacidad + ")";
    }
}
