package entities;

import java.time.LocalDateTime;

/** Únicamente los campos que CU02 permite modificar. */
public record EdicionEvento(String nombre, String tipo, LocalDateTime inicio, LocalDateTime fin, String lugar) {}
