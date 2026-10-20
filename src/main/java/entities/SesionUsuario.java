package entities;

public class SesionUsuario {

    private int idUsuario;
    private String nombre;
    private String tipoUsuario;
    private String estadoUsuario;
    private int idActor;
    private String estadoActor;

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(String tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public String getEstadoUsuario() {
        return estadoUsuario;
    }

    public void setEstadoUsuario(String estadoUsuario) {
        this.estadoUsuario = estadoUsuario;
    }

    public int getIdActor() {
        return idActor;
    }

    public void setIdActor(int idActor) {
        this.idActor = idActor;
    }

    public String getEstadoActor() {
        return estadoActor;
    }

    public void setEstadoActor(String estadoActor) {
        this.estadoActor = estadoActor;
    }
    public void cerrar() {
        idUsuario = 0;
        idActor = 0;
        nombre = null;
        tipoUsuario = null;
        estadoUsuario = null;
        estadoActor = null;
    }
}
