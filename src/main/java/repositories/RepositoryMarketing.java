package repositories;

import config.DatabaseConnection;
import entities.ClienteMarketing;
import entities.SolicitudEvento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;  
import java.sql.SQLException;
import entities.Segmentacion;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RepositoryMarketing {
    private static final String TIPO_CLIENTE = "CLIENTE";
    private static final String TIPO_ADMINISTRADOR = "ADMINISTRADOR";
    private static final String ESTADO_ACTIVO = "ACTIVO";

    private final String url;

    public RepositoryMarketing() {
        this(null);
    }

    // Permite utilizar una base aislada en las pruebas.
    public RepositoryMarketing(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public List<ClienteMarketing> listarClientesActivos() {
        String sql = "SELECT id_usuario, nombre, pais, ciudad, fecha_nacimiento, acepta_marketing "
                + "FROM Usuario WHERE tipo_usuario = ? AND estado = ? ORDER BY id_usuario";
        List<ClienteMarketing> clientes = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TIPO_CLIENTE);
            statement.setString(2, ESTADO_ACTIVO);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    clientes.add(new ClienteMarketing(
                            result.getInt("id_usuario"),
                            result.getString("nombre"),
                            result.getString("pais"),
                            result.getString("ciudad"),
                            result.getObject("fecha_nacimiento", LocalDate.class),
                            result.getBoolean("acepta_marketing")));
                }
            }
            return clientes;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar los clientes. Revisa la conexión con la base de datos.", e);
        }
    }

    public List<SolicitudEvento> contarSolicitudesPorTipo() {
        String sql = "SELECT id_usuario, tipo, COUNT(*) AS cantidad FROM ("
                + "SELECT id_usuario, tipo FROM Busqueda_evento "
                + "UNION ALL "
                + "SELECT id_cliente AS id_usuario, tipo FROM Evento "
                + "WHERE id_cliente IS NOT NULL AND tipo IS NOT NULL"
                + ") AS solicitudes GROUP BY id_usuario, tipo ORDER BY id_usuario, tipo";
        List<SolicitudEvento> solicitudes = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                solicitudes.add(new SolicitudEvento(
                        result.getInt("id_usuario"),
                        result.getString("tipo"),
                        result.getInt("cantidad")));
            }
            return solicitudes;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar las solicitudes de eventos. Revisa la conexión con la base de datos.", e);
        }
    }
     public boolean existeAdministradorActivo(int idUsuario) {
        String sql = "SELECT COUNT(*) FROM Usuario WHERE id_usuario = ? AND tipo_usuario = ? AND estado = ?";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setString(2, TIPO_ADMINISTRADOR);
            statement.setString(3, ESTADO_ACTIVO);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo verificar el administrador. Revisa la conexión con la base de datos.", e);
        }
    }

    public List<Segmentacion> reemplazarSegmentosAutomaticos(List<Segmentacion> segmentos) {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                desactivarSegmentosAutomaticos(connection);
                for (Segmentacion segmento : segmentos) {
                    guardarSegmento(connection, segmento);
                }
                connection.commit();
                return segmentos;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron guardar los segmentos. Revisa la conexión con la base de datos.", e);
        }
    }

    private void desactivarSegmentosAutomaticos(Connection connection) throws SQLException {
        String sql = "UPDATE Segmentacion SET estados = ? WHERE estados = ? AND criterios LIKE ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Segmentacion.ESTADO_INACTIVA);
            statement.setString(2, Segmentacion.ESTADO_ACTIVA);
            statement.setString(3, Segmentacion.PREFIJO_CRITERIOS_AUTOMATICOS + "%");
            statement.executeUpdate();
        }
    }

    private void guardarSegmento(Connection connection, Segmentacion segmento) throws SQLException {
        String sqlSegmento = "INSERT INTO Segmentacion (id_usuario, nombre, criterios, descripcion, estados) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sqlSegmento, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, segmento.getIdUsuario());
            statement.setString(2, segmento.getNombre());
            statement.setString(3, segmento.getCriterios());
            statement.setString(4, segmento.getDescripcion());
            statement.setString(5, segmento.getEstado());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No se generó el identificador del segmento.");
                }
                segmento.setIdSegmento(keys.getInt(1));
            }
        }

        String sqlClientes = "INSERT INTO Segmento_cliente (id_segmento, id_usuario) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sqlClientes)) {
            for (int idCliente : segmento.getIdsClientes()) {
                statement.setInt(1, segmento.getIdSegmento());
                statement.setInt(2, idCliente);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
    public List<Segmentacion> listarSegmentosActivos() {
        String sql = "SELECT id_segmento, id_usuario, nombre, criterios, descripcion, estados " + "FROM Segmentacion WHERE estados = ? ORDER BY id_segmento";
        List<Segmentacion> segmentos = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Segmentacion.ESTADO_ACTIVA);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int idSegmento = result.getInt("id_segmento");
                    segmentos.add(new Segmentacion(
                            idSegmento,
                            result.getInt("id_usuario"),
                            result.getString("nombre"),
                            result.getString("criterios"),
                            result.getString("descripcion"),
                            result.getString("estados"),
                            listarIdsClientes(connection, idSegmento)));
                }
            }
            return segmentos;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar los segmentos. Revisa la conexión con la base de datos.", e);
        }
    }

    private List<Integer> listarIdsClientes(Connection connection, int idSegmento) throws SQLException {
        String sql = "SELECT id_usuario FROM Segmento_cliente WHERE id_segmento = ? ORDER BY id_usuario";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idSegmento);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ids.add(result.getInt("id_usuario"));
                }
            }
        }
        return ids;
    }
}