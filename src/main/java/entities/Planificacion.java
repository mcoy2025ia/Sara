package entities;

public class Planificacion {
    
    private int idPlanificacion;
    private int idEvento;
    private int version;
    private String descripcion;
    private String estado;

    // Constructor vacío 
    public Planificacion() {
    }

    // Constructor con todos los atributos
    public Planificacion(int idPlanificacion, int idEvento, int version, String descripcion, String estado) {
        this.idPlanificacion = idPlanificacion;
        this.idEvento = idEvento;
        this.version = version;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    // Getters y Setters
    public int getIdPlanificacion() {
        return idPlanificacion;
    }

    public void setIdPlanificacion(int idPlanificacion) {
        this.idPlanificacion = idPlanificacion;
    }

    public int getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(int idEvento) {
        this.idEvento = idEvento;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}