package repositories;

import config.DatabaseConnection;
import entities.Fotografia;
import java.sql.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RepositoryFinalizacion {
    private final String url;
    private final Clock reloj;
    public RepositoryFinalizacion() { this(null, Clock.systemDefaultZone()); }
    public RepositoryFinalizacion(String url, Clock reloj) { this.url=url; this.reloj=reloj; }
    private Connection conectar() throws SQLException {
        return url==null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public void finalizar(int evento, int organizador, List<Fotografia> fotos) {
        preparar(evento, organizador, fotos);
        enviar(evento, organizador);
    }

    // E2: la liberación y las fotos quedan guardadas aunque falle el envío posterior.
    private void preparar(int evento, int organizador, List<Fotografia> fotos) {
        try (Connection c=conectar()) {
            c.setAutoCommit(false);
            try {
                validarEvento(c, evento, organizador);
                if (!iniciada(c, evento)) {
                    if (fotos.isEmpty()) throw new IllegalArgumentException("Carga al menos una fotografía del evento.");
                    ReservasEvento.preparar(c, evento);
                    actualizar(c, "INSERT INTO Finalizacion_evento (id_evento) VALUES (?)", evento);
                    for (Fotografia foto:fotos)
                        actualizar(c, "INSERT INTO Fotografia_evento (id_evento,nombre,contenido) VALUES (?,?,?)",
                                evento, foto.nombre(), foto.contenido());
                    actualizar(c, "INSERT INTO Entrega_galeria (id_evento,id_invitado) "
                            + "SELECT ?,g.id_invitado FROM Invitado g JOIN Invitacion i ON i.id_invitacion=g.id_invitacion "
                            + "WHERE i.id_evento=? AND UPPER(g.estado)='CONFIRMADO' "
                            + "AND (i.estado_respuesta IS NULL OR UPPER(i.estado_respuesta) NOT IN ('BORRADOR','REVOCADA'))", evento, evento);
                    ReservasEvento.liberar(c, evento);
                    actualizar(c, "UPDATE Contratacion SET estado='FINALIZADA' WHERE id_evento=? AND UPPER(estado) IN ('ACTIVA','CONFIRMADA')", evento);
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo preparar la finalización. No se guardaron cambios; vuelve a intentarlo.", e);
        }
    }

    private void enviar(int evento, int organizador) {
        try (Connection c=conectar()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement s=c.prepareStatement("SELECT estado FROM Evento WHERE id_evento=? AND id_organizador=? FOR UPDATE")) {
                    s.setInt(1,evento); s.setInt(2,organizador);
                    try (ResultSet r=s.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("Selecciona un evento existente de tu cuenta.");
                        if ("FINALIZADO".equalsIgnoreCase(r.getString(1))) { c.commit(); return; }
                        if (!"CONFIRMADO".equalsIgnoreCase(r.getString(1)))
                            throw new IllegalArgumentException("Solo puedes finalizar un evento confirmado.");
                    }
                }
                actualizar(c, "INSERT INTO Notificacion_interna (id_invitado,id_evento,mensaje) "
                        + "SELECT id_invitado,id_evento,'La galería de fotografías del evento está disponible en Mis invitaciones.' "
                        + "FROM Entrega_galeria WHERE id_evento=?", evento);
                actualizar(c, "UPDATE Finalizacion_evento SET enviada=TRUE WHERE id_evento=?", evento);
                actualizar(c, "UPDATE Evento SET estado='FINALIZADO' WHERE id_evento=?", evento);
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo enviar la galería. Las reservas ya están liberadas y las fotos guardadas. Reintenta el envío.", e);
        }
    }

    private void validarEvento(Connection c, int evento, int organizador) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("SELECT estado,fecha_fin FROM Evento WHERE id_evento=? AND id_organizador=? FOR UPDATE")) {
            s.setInt(1,evento); s.setInt(2,organizador);
            try (ResultSet r=s.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Selecciona un evento existente de tu cuenta.");
                if (!"CONFIRMADO".equalsIgnoreCase(r.getString(1)))
                    throw new IllegalArgumentException("Solo puedes finalizar un evento confirmado; no puede estar cancelado ni finalizado.");
                LocalDateTime fin=r.getTimestamp(2).toLocalDateTime();
                if (!LocalDateTime.now(reloj).isAfter(fin))
                    throw new IllegalArgumentException("Podrás finalizar el evento después del "
                            + fin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ".");
            }
        }
    }

    private boolean iniciada(Connection c, int evento) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("SELECT id_evento FROM Finalizacion_evento WHERE id_evento=?")) {
            s.setInt(1,evento);
            try (ResultSet r=s.executeQuery()) { return r.next(); }
        }
    }

    public boolean galeriaPendiente(int evento, int organizador) {
        try(Connection c=conectar(); PreparedStatement s=c.prepareStatement(
                "SELECT f.id_evento FROM Finalizacion_evento f JOIN Evento e ON e.id_evento=f.id_evento "
                + "WHERE f.id_evento=? AND e.id_organizador=? AND f.enviada=FALSE")) {
            s.setInt(1,evento); s.setInt(2,organizador);
            try(ResultSet r=s.executeQuery()) { return r.next(); }
        } catch(SQLException e) { throw new IllegalStateException("No se pudo consultar el envío de la galería.",e); }
    }

    public List<String> proveedoresParaCalificar(int evento, int organizador) {
        List<String> nombres=new ArrayList<>();
        String sql="SELECT DISTINCT p.nombre FROM Contratacion c JOIN Propuesta pr ON pr.id_propuesta=c.id_propuesta "
                + "JOIN Solicitud_Ejecucion s ON s.id_solicitud=pr.id_solicitud JOIN Proveedor p ON p.id_proveedor=s.id_proveedor "
                + "JOIN Evento e ON e.id_evento=c.id_evento WHERE e.id_evento=? AND e.id_organizador=? "
                + "AND e.estado='FINALIZADO' AND c.estado='FINALIZADA' ORDER BY p.nombre";
        try (Connection c=conectar(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setInt(1,evento); s.setInt(2,organizador);
            try (ResultSet r=s.executeQuery()) { while(r.next()) nombres.add(r.getString(1)); }
            return nombres;
        } catch (SQLException e) { throw new IllegalStateException("No se pudieron consultar los proveedores del evento.",e); }
    }

    private void actualizar(Connection c, String sql, Object... valores) throws SQLException {
        try (PreparedStatement s=c.prepareStatement(sql)) {
            for (int i=0;i<valores.length;i++) s.setObject(i+1,valores[i]);
            s.executeUpdate();
        }
    }
}
