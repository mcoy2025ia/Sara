package repositories;

import config.DatabaseConnection;
import entities.Invitacion;
import entities.InvitacionRecibida;
import entities.Invitado;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Acceso a la tabla Invitado y consultas de cupo para el CU-17. */
public class RepositoryInvitado {
    public List<entities.Fotografia> listarGaleria(String correo, String telefono) {
        List<entities.Fotografia> fotos=new ArrayList<>();
        String sql="SELECT e.nombre AS evento,f.nombre,f.contenido FROM Fotografia_evento f "
                + "JOIN Evento e ON e.id_evento=f.id_evento JOIN Finalizacion_evento fin ON fin.id_evento=e.id_evento "
                + "WHERE fin.enviada=TRUE AND EXISTS (SELECT 1 FROM Entrega_galeria d JOIN Invitado g ON g.id_invitado=d.id_invitado "
                + "WHERE d.id_evento=e.id_evento AND LOWER(TRIM(g.correo))=LOWER(TRIM(?)) "
                + "AND REGEXP_REPLACE(COALESCE(g.telefono,''),'[^0-9]','')=?) ORDER BY e.id_evento,f.id_fotografia";
        try(Connection c=conectar(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setString(1,correo); s.setString(2,telefono);
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) fotos.add(new entities.Fotografia(r.getString("evento")+" · "+r.getString("nombre"),r.getBytes("contenido")));
            }
            return fotos;
        } catch(SQLException e) { throw new IllegalStateException("No se pudo consultar la galería. Vuelve a intentarlo.",e); }
    }

    private static final String COLUMNAS =
            "id_invitado, id_invitacion, nombre, telefono, direccion, estado, correo";
    private static final String INVITACION_ACTIVA =
            "(i.estado_respuesta IS NULL OR UPPER(i.estado_respuesta) <> '" + Invitacion.ESTADO_REVOCADA + "')";

    /** Quien rechazó la invitación libera su cupo y el de sus acompañantes (R39). */
    private static final String INVITADO_ASISTE =
            "(v.estado IS NULL OR UPPER(v.estado) <> '" + Invitado.ESTADO_RECHAZADO + "')";

    private final String url;

    public RepositoryInvitado() {
        this(null);
    }

    // Permite utilizar una base aislada en las pruebas.
    public RepositoryInvitado(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public void guardarInvitado(Invitado invitado) {
        String sql = "INSERT INTO Invitado (id_invitacion, nombre, telefono, direccion, estado, correo) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, invitado.getIdInvitacion());
            statement.setString(2, invitado.getNombre());
            statement.setString(3, invitado.getTelefono());
            statement.setString(4, invitado.getDireccion());
            statement.setString(5, invitado.getEstado());
            statement.setString(6, invitado.getCorreo());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se generó el identificador del invitado.");
                invitado.setIdInvitado(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el invitado. Revisa la conexión con la base de datos.", e);
        }
    }

    public Invitado buscarInvitado(int idInvitado) {
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT " + COLUMNAS + " FROM Invitado WHERE id_invitado = ?")) {
            statement.setInt(1, idInvitado);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? convertir(result) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el invitado. Revisa la conexión con la base de datos.", e);
        }
    }

    public List<Invitado> listarInvitados(int idInvitacion) {
        List<Invitado> invitados = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT " + COLUMNAS + " FROM Invitado WHERE id_invitacion = ? ORDER BY id_invitado")) {
            statement.setInt(1, idInvitacion);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) invitados.add(convertir(result));
            }
            return invitados;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar los invitados. Revisa la conexión con la base de datos.", e);
        }
    }

    public void eliminarInvitado(int idInvitado) {
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM Invitado WHERE id_invitado = ?")) {
            statement.setInt(1, idInvitado);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo quitar el invitado. Revisa la conexión con la base de datos.", e);
        }
    }

    /** Acompañantes, necesidades especiales o reservas de regalos que dependen del invitado. */
    public boolean tieneRegistrosAsociados(int idInvitado) {
        String sql = "SELECT (SELECT COUNT(*) FROM Acompanante WHERE id_invitado = ?) "
                + "+ (SELECT COUNT(*) FROM Necesidad_Especial WHERE id_invitado = ?) "
                + "+ (SELECT COUNT(*) FROM Reserva_regalo WHERE id_invitado = ?)";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 1; i <= 3; i++) statement.setInt(i, idInvitado);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron revisar los datos del invitado. Revisa la conexión con la base de datos.", e);
        }
    }

    /** R40: el correo ya está en una invitación activa del mismo evento. */
    public boolean existeCorreoEnEvento(int idEvento, String correo) {
        String sql = "SELECT COUNT(*) FROM Invitado v JOIN Invitacion i ON i.id_invitacion = v.id_invitacion "
                + "WHERE i.id_evento = ? AND " + INVITACION_ACTIVA + " AND LOWER(TRIM(v.correo)) = LOWER(TRIM(?))";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idEvento);
            statement.setString(2, correo);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo validar el invitado. Revisa la conexión con la base de datos.", e);
        }
    }

    /** Invitados y acompañantes de las invitaciones activas de un evento en un lugar. */
    public int contarAsistentes(int idEvento, int idLugar) {
        String filtro = " JOIN Invitacion i ON i.id_invitacion = v.id_invitacion "
                + "WHERE i.id_evento = ? AND i.id_lugar = ? AND " + INVITACION_ACTIVA + " AND " + INVITADO_ASISTE;
        String sql = "SELECT (SELECT COUNT(*) FROM Invitado v" + filtro + ") + "
                + "(SELECT COUNT(*) FROM Acompanante a JOIN Invitado v ON v.id_invitado = a.id_invitado" + filtro + ")";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idEvento);
            statement.setInt(2, idLugar);
            statement.setInt(3, idEvento);
            statement.setInt(4, idLugar);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo calcular el aforo. Revisa la conexión con la base de datos.", e);
        }
    }

    /** Asistentes (invitados + acompañantes) de cada invitación de un evento. */
    public Map<Integer, Integer> contarAsistentesPorInvitacion(int idEvento) {
        String invitados = "SELECT v.id_invitacion, COUNT(*) FROM Invitado v "
                + "JOIN Invitacion i ON i.id_invitacion = v.id_invitacion WHERE i.id_evento = ? GROUP BY v.id_invitacion";
        String acompanantes = "SELECT v.id_invitacion, COUNT(*) FROM Acompanante a "
                + "JOIN Invitado v ON v.id_invitado = a.id_invitado "
                + "JOIN Invitacion i ON i.id_invitacion = v.id_invitacion WHERE i.id_evento = ? GROUP BY v.id_invitacion";
        Map<Integer, Integer> conteo = new HashMap<>();
        try (Connection connection = conectar()) {
            for (String sql : new String[]{invitados, acompanantes}) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, idEvento);
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) conteo.merge(result.getInt(1), result.getInt(2), Integer::sum);
                    }
                }
            }
            return conteo;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron contar los invitados. Revisa la conexión con la base de datos.", e);
        }
    }

    /** CU-16: cambia el estado de respuesta del invitado (PENDIENTE, CONFIRMADO o RECHAZADO). */
    public void actualizarEstado(int idInvitado, String estado) {
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE Invitado SET estado = ? WHERE id_invitado = ?")) {
            statement.setString(1, estado);
            statement.setInt(2, idInvitado);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la respuesta. Revisa la conexión con la base de datos.", e);
        }
    }

    public int contarAcompanantes(int idInvitado) {
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM Acompanante WHERE id_invitado = ?")) {
            statement.setInt(1, idInvitado);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron contar los acompañantes. Revisa la conexión con la base de datos.", e);
        }
    }

    /** CU-16 (R37): invitaciones activas en las que aparece el correo, con su evento y lugar. */
    public List<InvitacionRecibida> listarInvitacionesPorCorreo(String correo) {
        String sql = "SELECT v.id_invitado, v.id_invitacion, v.nombre, v.estado, i.estado_respuesta, i.mensaje, "
                + "e.id_evento, e.nombre AS evento, e.tipo, e.estado AS estado_evento, e.fecha_inicio, e.fecha_fin, "
                + "l.id_lugar, l.nombre AS lugar, l.direccion, l.capacidad "
                + "FROM Invitado v JOIN Invitacion i ON i.id_invitacion = v.id_invitacion "
                + "JOIN Evento e ON e.id_evento = i.id_evento JOIN Lugar l ON l.id_lugar = i.id_lugar "
                + "WHERE LOWER(TRIM(v.correo)) = LOWER(TRIM(?)) AND " + INVITACION_ACTIVA + " "
                + "ORDER BY e.fecha_inicio, v.id_invitado";
        List<InvitacionRecibida> invitaciones = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, correo);
            try (ResultSet r = statement.executeQuery()) {
                while (r.next()) {
                    invitaciones.add(new InvitacionRecibida(r.getInt("id_invitado"), r.getInt("id_invitacion"),
                            r.getString("nombre"), r.getString("estado"), r.getString("estado_respuesta"),
                            r.getString("mensaje"), r.getInt("id_evento"), r.getString("evento"), r.getString("tipo"),
                            r.getString("estado_evento"), r.getTimestamp("fecha_inicio").toLocalDateTime(),
                            r.getTimestamp("fecha_fin").toLocalDateTime(), r.getInt("id_lugar"), r.getString("lugar"),
                            r.getString("direccion"), r.getInt("capacidad")));
                }
            }
            return invitaciones;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar tus invitaciones. Revisa la conexión con la base de datos.", e);
        }
    }

    /** Los avisos se conservan aunque la cancelación haya revocado la invitación. */
    public List<String> listarAvisosPorCorreo(String correo) {
        List<String> avisos = new ArrayList<>();
        try (Connection c = conectar(); PreparedStatement consulta = c.prepareStatement(
                "SELECT n.mensaje FROM Notificacion_interna n JOIN Invitado v ON v.id_invitado=n.id_invitado "
                + "WHERE LOWER(TRIM(v.correo))=LOWER(TRIM(?)) ORDER BY n.id_notificacion DESC")) {
            consulta.setString(1, correo);
            try (ResultSet r = consulta.executeQuery()) { while (r.next()) avisos.add(r.getString(1)); }
            return avisos;
        } catch (SQLException e) { throw new IllegalStateException("No se pudieron consultar los avisos. Vuelve a intentarlo.", e); }
    }

    private Invitado convertir(ResultSet result) throws SQLException {
        return new Invitado(result.getInt("id_invitado"), result.getInt("id_invitacion"), result.getString("nombre"),
                result.getString("telefono"), result.getString("direccion"), result.getString("estado"),
                result.getString("correo"));
    }
}
