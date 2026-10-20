package entities;

public record Administrador(int id, String nombre) {
    @Override public String toString() { return nombre; }
}
