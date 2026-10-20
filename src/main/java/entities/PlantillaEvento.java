package entities;

/** Proyección pública: no contiene datos del organizador, invitados ni presupuesto. */
public record PlantillaEvento(int id, String nombre, String tipo, long duracionMinutos) {
    @Override public String toString() {
        return nombre + " · Duración: " + duracionMinutos + " min";
    }
}
