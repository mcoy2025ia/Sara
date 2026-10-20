package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import config.DatabaseConnection;
import entities.SesionUsuario;

public class RepositoryAutenticacion {
    private final String url;

    public RepositoryAutenticacion() { this(null); }
    public RepositoryAutenticacion(String url) { this.url = url; }


    public SesionUsuario autenticar(
            String usuario,
            String contrasena) {

        String sql = """
                SELECT
                    u.id_usuario,
                    u.nombre,
                    u.tipo_usuario,
                    u.estado AS estado_usuario,
                    CASE
                        WHEN u.tipo_usuario = 'PROVEEDOR'
                            THEN p.id_proveedor
                        WHEN u.tipo_usuario = 'ORGANIZADOR'
                            THEN o.id_organizador
                    END AS id_actor,
                    CASE
                        WHEN u.tipo_usuario = 'PROVEEDOR'
                            THEN p.estado
                        WHEN u.tipo_usuario = 'ORGANIZADOR'
                            THEN o.estado
                    END AS estado_actor
                FROM Usuario u
                LEFT JOIN Proveedor p
                    ON p.id_usuario = u.id_usuario
                LEFT JOIN Organizador o
                    ON o.id_usuario = u.id_usuario
                WHERE u.usuario = ?
                  AND u.contraseña = ?
                """;

        try (Connection connection
                = (url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url)); PreparedStatement statement
                = connection.prepareStatement(sql)) {

            statement.setString(1, usuario);
            statement.setString(2, contrasena);

            try (ResultSet result
                    = statement.executeQuery()) {

                if (!result.next()) {
                    return null;
                }

                SesionUsuario sesion
                        = new SesionUsuario();

                sesion.setIdUsuario(
                        result.getInt("id_usuario"));

                sesion.setNombre(
                        result.getString("nombre"));

                sesion.setTipoUsuario(
                        result.getString("tipo_usuario"));

                sesion.setEstadoUsuario(
                        result.getString("estado_usuario"));

                Object idActor
                        = result.getObject("id_actor");

                if (idActor != null) {
                    sesion.setIdActor(
                            ((Number) idActor).intValue());
                }

                sesion.setEstadoActor(
                        result.getString("estado_actor"));

                return sesion;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo iniciar sesion.",
                    e);
        }
    }
}
