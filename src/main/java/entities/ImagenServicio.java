package entities;

public class ImagenServicio {

    private int idImagen;
    private int idServicio;
    private String ruta;
    private int ordenImagen;

    public int getIdImagen() {
        return idImagen;
    }

    public void setIdImagen(int idImagen) {
        this.idImagen = idImagen;
    }

    public int getIdServicio() {
        return idServicio;
    }

    public void setIdServicio(int idServicio) {
        this.idServicio = idServicio;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public int getOrdenImagen() {
        return ordenImagen;
    }

    public void setOrdenImagen(int ordenImagen) {
        this.ordenImagen = ordenImagen;
    }
}
