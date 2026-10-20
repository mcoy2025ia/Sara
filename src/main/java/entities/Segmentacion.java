package entities;

import java.util.List;

//Segmento de clientes guardado en la tabla Segmentacion, junto con los clientes que lo forman.
public class Segmentacion {
    public static final String ESTADO_ACTIVA = "ACTIVA";
    public static final String ESTADO_INACTIVA = "INACTIVA";
    // Los segmentos generados por el sistema empiezan sus criterios con este texto.
    public static final String PREFIJO_CRITERIOS_AUTOMATICOS = "AUTO|";

    private Integer idSegmento;
    private final int idUsuario;
    private final String nombre;
    private final String criterios;
    private final String descripcion;
    private final String estado;
    private final List<Integer> idsClientes;

    public Segmentacion(int idUsuario, String nombre, String criterios, String descripcion,
                        String estado, List<Integer> idsClientes) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.criterios = criterios;
        this.descripcion = descripcion;
        this.estado = estado;
        this.idsClientes = List.copyOf(idsClientes);
    }

    public Integer getIdSegmento() {
        return idSegmento;
    }

    public void setIdSegmento(Integer idSegmento) {
        this.idSegmento = idSegmento;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCriterios() {
        return criterios;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public List<Integer> getIdsClientes() {
        return idsClientes;
    }

    public int getCantidadClientes() {
        return idsClientes.size();
    }
    //Segmentos que ya existen en la BD
    public Segmentacion(Integer idSegmento, int idUsuario, String nombre, String criterios, String descripcion, String estado, List<Integer> idsClientes) {
        this(idUsuario, nombre, criterios, descripcion, estado, idsClientes);
        this.idSegmento = idSegmento;
    }
}