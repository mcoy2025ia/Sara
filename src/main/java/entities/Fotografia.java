package entities;

public record Fotografia(String nombre, byte[] contenido) {
    public Fotografia {
        contenido = contenido == null ? null : contenido.clone();
    }
    @Override public byte[] contenido() { return contenido == null ? null : contenido.clone(); }
}
