package entities;

import java.util.List;

//Resultado de la segmentación, un grupo de clientes que forma un segmento
public class SegmentoCalculado {
    private final String pais;
    private final String ciudad;
    private final RangoEdad rangoEdad;
    private final String tipoEvento;
    private final List<Integer> idsClientes;

    public SegmentoCalculado(String pais, String ciudad, RangoEdad rangoEdad,
                             String tipoEvento, List<Integer> idsClientes) {
        this.pais = pais;
        this.ciudad = ciudad;
        this.rangoEdad = rangoEdad;
        this.tipoEvento = tipoEvento;
        this.idsClientes = List.copyOf(idsClientes);
    }

    public String getPais() {
        return pais;
    }

    public String getCiudad() {
        return ciudad;
    }

    public RangoEdad getRangoEdad() {
        return rangoEdad;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public List<Integer> getIdsClientes() {
        return idsClientes;
    }

    public int getCantidadClientes() {
        return idsClientes.size();
    }
}