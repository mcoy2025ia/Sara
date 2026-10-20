package entities;

/** Persona invitada a través de una invitación (tabla Invitado). */
public class Invitado {
    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_CONFIRMADO = "CONFIRMADO";
    public static final String ESTADO_RECHAZADO = "RECHAZADO";

    private int idInvitado;
    private int idInvitacion;
    private String nombre;
    private String telefono;
    private String direccion;
    private String estado;
    private String correo;

    public Invitado() {
    }

    public Invitado(String nombre, String correo, String telefono, String direccion) {
        this(0, 0, nombre, telefono, direccion, ESTADO_PENDIENTE, correo);
    }

    public Invitado(int idInvitado, int idInvitacion, String nombre, String telefono,
                    String direccion, String estado, String correo) {
        this.idInvitado = idInvitado;
        this.idInvitacion = idInvitacion;
        this.nombre = nombre;
        this.telefono = telefono;
        this.direccion = direccion;
        this.estado = estado;
        this.correo = correo;
    }

    public int getIdInvitado() { return idInvitado; }
    public void setIdInvitado(int idInvitado) { this.idInvitado = idInvitado; }
    public int getIdInvitacion() { return idInvitacion; }
    public void setIdInvitacion(int idInvitacion) { this.idInvitacion = idInvitacion; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
}
