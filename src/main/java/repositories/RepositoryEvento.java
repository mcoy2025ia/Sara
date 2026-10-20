package repositories;

import config.DatabaseConnection;
import entities.Evento;
import entities.EdicionEvento;
import entities.Organizador;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RepositoryEvento {
    private final String url;

    public RepositoryEvento() {
        this(null);
    }

    // Permite utilizar una base aislada en las pruebas.
    public RepositoryEvento(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public void guardarEvento(Evento evento) {
        String sql = "INSERT INTO Evento (id_organizador, nombre, tipo, descripcion, fecha_inicio, "
                + "fecha_fin, ubicacion, presupuesto, invitados_iniciales, estado, fecha_creacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try (PreparedStatement organizador = connection.prepareStatement(
                    "SELECT id_organizador FROM Organizador WHERE id_organizador = ?")) {
                organizador.setInt(1, evento.getIdOrganizador());
                try (ResultSet result = organizador.executeQuery()) {
                    if (!result.next()) {
                        throw new IllegalArgumentException("El organizador no existe. Usa el identificador de un organizador registrado.");
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, evento.getIdOrganizador());
                statement.setString(2, evento.getNombre());
                statement.setString(3, evento.getTipo());
                statement.setString(4, evento.getDescripcion());
                statement.setTimestamp(5, Timestamp.valueOf(evento.getFechaInicio()));
                statement.setTimestamp(6, Timestamp.valueOf(evento.getFechaFin()));
                statement.setString(7, evento.getUbicacion());
                statement.setBigDecimal(8, evento.getPresupuesto());
                statement.setObject(9, evento.getInvitadosIniciales());
                statement.setString(10, evento.getEstado());
                statement.setTimestamp(11, Timestamp.valueOf(evento.getFechaCreacion()));
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se generó el identificador del evento.");
                    int idEvento = keys.getInt(1);
                    connection.commit();
                    evento.setIdEvento(idEvento);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el evento. Revisa la conexión con la base de datos.", e);
        }
    }

    public List<Organizador> listarOrganizadores() {
        List<Organizador> organizadores = new ArrayList<>();
        String sql = "SELECT o.id_organizador, u.nombre FROM Organizador o "
                + "JOIN Usuario u ON u.id_usuario = o.id_usuario ORDER BY u.nombre, o.id_organizador";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                organizadores.add(new Organizador(result.getInt("id_organizador"), result.getString("nombre")));
            }
            return organizadores;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los organizadores. Revisa la conexión con la base de datos.", e);
        }
    }

    public List<Evento> listarEventos() {
        List<Evento> eventos = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM Evento ORDER BY id_evento");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                eventos.add(leerEvento(result));
            }
            return eventos;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar los eventos. Revisa la conexión con la base de datos.", e);
        }
    }
    public boolean organizadorActivo(int usuario, int organizador) {
        String sql="SELECT 1 FROM Organizador o JOIN Usuario u ON u.id_usuario=o.id_usuario "
                + "WHERE o.id_organizador=? AND u.id_usuario=? AND UPPER(o.estado)='ACTIVO' "
                + "AND UPPER(u.estado)='ACTIVO' AND UPPER(u.tipo_usuario)='ORGANIZADOR'";
        try (Connection connection=conectar(); PreparedStatement statement=connection.prepareStatement(sql)) {
            statement.setInt(1,organizador); statement.setInt(2,usuario);
            try(ResultSet result=statement.executeQuery()) { return result.next(); }
        } catch(SQLException e) { throw new IllegalStateException("No se pudo validar tu sesión. Vuelve a intentarlo.",e); }
    }

    public List<Evento> listarEventosPorOrganizador(int organizador) {
        List<Evento> eventos=new ArrayList<>();
        try(Connection connection=conectar(); PreparedStatement statement=connection.prepareStatement(
                "SELECT * FROM Evento WHERE id_organizador=? ORDER BY id_evento")) {
            statement.setInt(1,organizador);
            try(ResultSet result=statement.executeQuery()) { while(result.next()) eventos.add(leerEvento(result)); }
            return eventos;
        } catch(SQLException e) { throw new IllegalStateException("No se pudieron cargar tus eventos. Vuelve a intentarlo.",e); }
    }

    public Evento buscarEvento(int evento, int organizador) {
        try (Connection connection=conectar(); PreparedStatement statement=connection.prepareStatement(
                "SELECT * FROM Evento WHERE id_evento=? AND id_organizador=?")) {
            statement.setInt(1,evento); statement.setInt(2,organizador);
            try (ResultSet result=statement.executeQuery()) { return result.next()?leerEvento(result):null; }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo cargar el evento. Vuelve a intentarlo.",e); }
    }

    public void editarEvento(int evento, int organizador, EdicionEvento datos) {
        // Un solo UPDATE es atómico; su condición vuelve a verificar estado y pertenencia al guardar.
        String sql="UPDATE Evento SET nombre=?,tipo=?,fecha_inicio=?,fecha_fin=?,ubicacion=? "
                + "WHERE id_evento=? AND id_organizador=? AND UPPER(estado)='PLANEADO'";
        try (Connection connection=conectar(); PreparedStatement statement=connection.prepareStatement(sql)) {
            statement.setString(1,datos.nombre()); statement.setString(2,datos.tipo());
            statement.setTimestamp(3,Timestamp.valueOf(datos.inicio())); statement.setTimestamp(4,Timestamp.valueOf(datos.fin()));
            statement.setString(5,datos.lugar()); statement.setInt(6,evento); statement.setInt(7,organizador);
            if(statement.executeUpdate()!=1) {
                Evento actual=buscarEvento(evento,organizador);
                if(actual==null) throw new IllegalArgumentException("Selecciona un evento existente del organizador elegido.");
                throw new IllegalArgumentException("No se puede editar: el evento está en estado " + actual.getEstado() + ".");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron guardar los cambios. Los datos anteriores se conservaron; vuelve a intentarlo.",e);
        }
    }

    private Evento leerEvento(ResultSet result) throws SQLException {
        return new Evento(result.getInt("id_evento"),result.getInt("id_organizador"),
                result.getString("nombre"),result.getString("tipo"),result.getString("descripcion"),
                result.getTimestamp("fecha_inicio").toLocalDateTime(),result.getTimestamp("fecha_fin").toLocalDateTime(),
                result.getString("ubicacion"),result.getBigDecimal("presupuesto"),result.getObject("invitados_iniciales",Integer.class),
                result.getString("estado"),result.getTimestamp("fecha_creacion").toLocalDateTime());
    }

}
