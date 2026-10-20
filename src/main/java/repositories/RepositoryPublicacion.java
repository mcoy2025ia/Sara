package repositories;

import config.DatabaseConnection;
import entities.Administrador;
import entities.PlantillaEvento;
import entities.SolicitudPublicacion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RepositoryPublicacion {
    private final String url;

    public RepositoryPublicacion() { this(null); }
    public RepositoryPublicacion(String url) { this.url = url; }

    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public List<Administrador> listarAdministradores() {
        try (Connection connection = conectar()) {
            return administradores(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los administradores. Vuelve a intentarlo.", e);
        }
    }

    private List<Administrador> administradores(Connection connection) throws SQLException {
        List<Administrador> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT u.id_usuario, u.nombre FROM Usuario u "
                + "JOIN Usuario_rol ur ON ur.id_usuario=u.id_usuario JOIN Rol r ON r.id_rol=ur.id_rol "
                + "WHERE r.nombre='ADMINISTRADOR' AND r.estado='ACTIVO' AND u.estado='ACTIVO' AND u.tipo_usuario='ADMINISTRADOR' ORDER BY u.nombre";
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) lista.add(new Administrador(result.getInt(1), result.getString(2)));
        }
        return lista;
    }

    public void solicitar(int idOrganizador, int idEvento) {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                String nombre;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT nombre, estado, id_organizador FROM Evento WHERE id_evento=? FOR UPDATE")) {
                    statement.setInt(1, idEvento);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next() || result.getInt(3) != idOrganizador)
                            throw new IllegalArgumentException("Selecciona un evento que pertenezca a tu cuenta.");
                        if (!"FINALIZADO".equalsIgnoreCase(result.getString(2)))
                            throw new IllegalArgumentException("Solo puedes solicitar la publicación de un evento finalizado.");
                        nombre = result.getString(1);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id_solicitud FROM Solicitud_publicacion WHERE id_evento=? AND estado IN ('PENDIENTE','APROBADA')")) {
                    statement.setInt(1, idEvento);
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) throw new IllegalArgumentException("Este evento ya tiene una solicitud pendiente o aprobada.");
                    }
                }
                List<Administrador> administradores = administradores(connection);
                if (administradores.isEmpty())
                    throw new IllegalArgumentException("No hay administradores disponibles para revisar la solicitud. Inténtalo más tarde.");
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO Solicitud_publicacion (id_evento,estado) VALUES (?,'PENDIENTE')")) {
                    statement.setInt(1, idEvento);
                    statement.executeUpdate();
                }
                for (Administrador administrador : administradores)
                    notificar(connection, administrador.id(), "Nueva solicitud de publicación: " + nombre + ". Revisa las solicitudes pendientes.");
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo enviar la solicitud. Tus datos se conservaron; vuelve a intentarlo.", e);
        }
    }

    public void revisar(int idSolicitud, int idAdministrador, boolean aprobar, String motivo) {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                if (administradores(connection).stream().noneMatch(admin -> admin.id() == idAdministrador))
                    throw new IllegalArgumentException("Selecciona un administrador activo con el rol correspondiente.");
                String sql = "SELECT s.estado, e.estado AS estado_evento, e.nombre, e.tipo, e.fecha_inicio, e.fecha_fin, "
                        + "o.id_usuario FROM Solicitud_publicacion s JOIN Evento e ON e.id_evento=s.id_evento "
                        + "JOIN Organizador o ON o.id_organizador=e.id_organizador WHERE s.id_solicitud=? FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, idSolicitud);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("La solicitud ya no existe. Actualiza la lista.");
                        if (!"PENDIENTE".equals(result.getString("estado")))
                            throw new IllegalArgumentException("Esta solicitud ya fue revisada. Actualiza la lista.");
                        if (aprobar && !"FINALIZADO".equalsIgnoreCase(result.getString("estado_evento")))
                            throw new IllegalArgumentException("El evento ya no está finalizado. No se puede publicar.");
                        if (aprobar) {
                            long duracion = java.time.Duration.between(
                                    result.getTimestamp("fecha_inicio").toLocalDateTime(),
                                    result.getTimestamp("fecha_fin").toLocalDateTime()).toMinutes();
                            if (duracion < 0) throw new IllegalArgumentException("Revisa las fechas del evento antes de publicarlo.");
                            try (PreparedStatement plantilla = connection.prepareStatement(
                                    "INSERT INTO Plantilla_evento (id_solicitud,nombre,tipo,duracion_minutos) VALUES (?,?,?,?)")) {
                                plantilla.setInt(1, idSolicitud);
                                String tipo = result.getString("tipo");
                                plantilla.setString(2, tipo == null || tipo.isBlank() ? "Plantilla de evento" : "Plantilla de evento " + tipo);
                                plantilla.setString(3, result.getString("tipo"));
                                plantilla.setLong(4, duracion);
                                plantilla.executeUpdate();
                            }
                        }
                        try (PreparedStatement revision = connection.prepareStatement(
                                "UPDATE Solicitud_publicacion SET estado=?, motivo=?, id_administrador=?, fecha_revision=CURRENT_TIMESTAMP WHERE id_solicitud=?")) {
                            revision.setString(1, aprobar ? "APROBADA" : "RECHAZADA");
                            revision.setString(2, aprobar ? null : motivo);
                            revision.setInt(3, idAdministrador);
                            revision.setInt(4, idSolicitud);
                            revision.executeUpdate();
                        }
                        notificar(connection, result.getInt("id_usuario"), aprobar
                                ? "Tu evento «" + result.getString("nombre") + "» se publicó como plantilla."
                                : "Se rechazó la publicación de «" + result.getString("nombre") + "». Motivo: " + motivo);
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo completar la revisión. No se guardaron cambios; vuelve a intentarlo.", e);
        }
    }

    private void notificar(Connection connection, int usuario, String mensaje) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Notificacion_interna (id_usuario,mensaje) VALUES (?,?)")) {
            statement.setInt(1, usuario);
            statement.setString(2, mensaje);
            statement.executeUpdate();
        }
    }

    public List<SolicitudPublicacion> listarSolicitudes(Integer idOrganizador) {
        List<SolicitudPublicacion> lista = new ArrayList<>();
        String sql = "SELECT s.id_solicitud,s.id_evento,e.nombre,s.estado,s.motivo "
                + "FROM Solicitud_publicacion s JOIN Evento e ON e.id_evento=s.id_evento "
                + (idOrganizador == null ? "WHERE s.estado='PENDIENTE'" : "WHERE e.id_organizador=?")
                + " ORDER BY s.id_solicitud DESC";
        try (Connection connection = conectar(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (idOrganizador != null) statement.setInt(1, idOrganizador);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) lista.add(new SolicitudPublicacion(result.getInt(1), result.getInt(2),
                        result.getString(3), result.getString(4), result.getString(5)));
            }
            return lista;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar las solicitudes. Vuelve a intentarlo.", e);
        }
    }

    public List<PlantillaEvento> listarPlantillas() {
        List<PlantillaEvento> lista = new ArrayList<>();
        try (Connection connection = conectar(); PreparedStatement statement = connection.prepareStatement(
                "SELECT id_plantilla,nombre,tipo,duracion_minutos FROM Plantilla_evento ORDER BY id_plantilla DESC");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) lista.add(new PlantillaEvento(result.getInt(1), result.getString(2), result.getString(3), result.getLong(4)));
            return lista;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar las plantillas. Vuelve a intentarlo.", e);
        }
    }

    public List<String> listarNotificaciones(int idUsuario) {
        List<String> lista = new ArrayList<>();
        try (Connection connection = conectar(); PreparedStatement statement = connection.prepareStatement(
                "SELECT mensaje FROM Notificacion_interna WHERE id_usuario=? ORDER BY id_notificacion DESC")) {
            statement.setInt(1, idUsuario);
            try (ResultSet result = statement.executeQuery()) { while (result.next()) lista.add(result.getString(1)); }
            return lista;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar las notificaciones. Vuelve a intentarlo.", e);
        }
    }

    public int usuarioOrganizador(int idOrganizador) {
        try (Connection connection = conectar(); PreparedStatement statement = connection.prepareStatement(
                "SELECT id_usuario FROM Organizador WHERE id_organizador=?")) {
            statement.setInt(1, idOrganizador);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Selecciona un organizador registrado.");
                return result.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el organizador. Vuelve a intentarlo.", e);
        }
    }
}
