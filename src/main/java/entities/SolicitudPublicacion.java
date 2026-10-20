package entities;

public record SolicitudPublicacion(int id, int idEvento, String nombreEvento, String estado, String motivo) {
    @Override public String toString() {
        String etiqueta = switch (estado) {
            case "PENDIENTE" -> "Pendiente";
            case "APROBADA" -> "Aprobada";
            case "RECHAZADA" -> "Rechazada";
            default -> estado;
        };
        return nombreEvento + " — " + etiqueta + (motivo == null ? "" : " · " + motivo);
    }
}
