package entities;

//Rangos de edad validos para la segmentacion
public enum RangoEdad {
    DE_18_A_25("18-25", 18, 25),
    DE_26_A_35("26-35", 26, 35),
    DE_36_A_50("36-50", 36, 50),
    DE_51_EN_ADELANTE("51+", 51, Integer.MAX_VALUE);

    private final String etiqueta;
    private final int edadMinima;
    private final int edadMaxima;

    RangoEdad(String etiqueta, int edadMinima, int edadMaxima) {
        this.etiqueta = etiqueta;
        this.edadMinima = edadMinima;
        this.edadMaxima = edadMaxima;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    // Devuelve null si la edad no pertenece a ningún rango (por ejemplo, menores de 18).
    public static RangoEdad paraEdad(int edad) {
        for (RangoEdad rango : values()) {
            if (edad >= rango.edadMinima && edad <= rango.edadMaxima) {
                return rango;
            }
        }
        return null;
    }
}