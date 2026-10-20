package entities;

/**
 * Representa un proveedor registrado en NextEventux.
 * Los estados se mantienen como String porque el esquema compartido utiliza VARCHAR.
 */
public class Proveedor {
    public static final String ESTADO_PENDIENTE_VERIFICACION = "PENDIENTE_VERIFICACION";
    public static final String ESTADO_VERIFICADO = "VERIFICADO";
    public static final String ESTADO_RECHAZADO = "RECHAZADO";

    private final int idProveedor;
    private final int idUsuario;
    private final String nombre;
    private final String empresa;
    private final String correo;
    private final String telefono;
    private final String direccion;
    private String estado;
    private String motivoRechazo;

    public Proveedor(int idProveedor, int idUsuario, String nombre, String empresa,
                     String correo, String telefono, String direccion, String estado,
                     String motivoRechazo) {
        this.idProveedor = idProveedor;
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.empresa = empresa;
        this.correo = correo;
        this.telefono = telefono;
        this.direccion = direccion;
        this.estado = estado;
        this.motivoRechazo = motivoRechazo;
    }

    public Proveedor(int idProveedor, int idUsuario, String nombre, String empresa,
                     String correo, String telefono, String direccion, String estado) {
        this(idProveedor, idUsuario, nombre, empresa, correo, telefono, direccion, estado, null);
    }

    public int getIdProveedor() { return idProveedor; }
    public int getIdUsuario() { return idUsuario; }
    public String getNombre() { return nombre; }
    public String getEmpresa() { return empresa; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getDireccion() { return direccion; }
    public String getEstado() { return estado; }
    public String getMotivoRechazo() { return motivoRechazo; }

    public void setEstado(String estado) { this.estado = estado; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
}
