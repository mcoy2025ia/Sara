package entities;

// Representa uno de los servicios que un proveedor puede ofrecer en la plataforma
public class Servicio {

    // Identificadores para saber que servicio es y a que proveedor pertenece
    private int idServicio;
    private int idProveedor;

    // Informacion principal que se muestra del servicio
    private String nombre;
    private String descripcion;
    private double precio;
    private String categoria;
    // Datos que definen las condiciones en las que se puede ofrecer el servicio
    private String tipo;
    private Integer cantidadMaximaPersonas;
    private String ciudadZona;
    private int anticipacionMinima;

    // Sirve para saber si el servicio esta activo, retirado, etc.
    private String estado;

    // Permite consultar el identificador del servicio
    public int getIdServicio() {
        return idServicio;
    }

    // Permite asignar el identificador del servicio
    public void setIdServicio(int idServicio) {
        this.idServicio = idServicio;
    }

    // Permite saber a que proveedor pertenece el servicio
    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getCantidadMaximaPersonas() {
        return cantidadMaximaPersonas;
    }

    public void setCantidadMaximaPersonas(Integer cantidadMaximaPersonas) {
        this.cantidadMaximaPersonas = cantidadMaximaPersonas;
    }

    public String getCiudadZona() {
        return ciudadZona;
    }

    public void setCiudadZona(String ciudadZona) {
        this.ciudadZona = ciudadZona;
    }

    public int getAnticipacionMinima() {
        return anticipacionMinima;
    }

    public void setAnticipacionMinima(int anticipacionMinima) {
        this.anticipacionMinima = anticipacionMinima;
    }
}
