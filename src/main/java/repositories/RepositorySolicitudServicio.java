package repositories;

import config.DatabaseConnection;
import entities.*;
import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RepositorySolicitudServicio {
    private final String url;
    private static final String VERIFICADO="UPPER(p.estado) IN ('VERIFICADO','APROBADO') AND UPPER(u.estado)='ACTIVO' AND UPPER(u.tipo_usuario)='PROVEEDOR'";
    private static final String CONTRATO_ACTIVO="UPPER(c.estado) IN ('ACTIVA','CONFIRMADA','FINALIZADA')";
    public RepositorySolicitudServicio() { this(null); }
    public RepositorySolicitudServicio(String url) { this.url=url; }
    private Connection conectar() throws SQLException {
        return url==null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public boolean hayProveedorVerificado() {
        try(Connection c=conectar(); PreparedStatement s=c.prepareStatement(
                "SELECT 1 FROM Proveedor p JOIN Usuario u ON u.id_usuario=p.id_usuario WHERE "+VERIFICADO);
                ResultSet r=s.executeQuery()) { return r.next(); }
        catch(SQLException e) { throw new IllegalStateException("No se pudo consultar el directorio de proveedores.",e); }
    }

    public List<ServicioSolicitable> listarServicios() {
        List<ServicioSolicitable> servicios=new ArrayList<>();
        String sql="SELECT s.id_servicio,s.nombre,p.nombre,s.precio FROM Servicio s "
                + "JOIN Proveedor p ON p.id_proveedor=s.id_proveedor JOIN Usuario u ON u.id_usuario=p.id_usuario "
                + "WHERE "+VERIFICADO+" AND UPPER(s.estado)='DISPONIBLE' AND s.precio>=0 ORDER BY s.nombre,s.id_servicio";
        try(Connection c=conectar(); PreparedStatement s=c.prepareStatement(sql); ResultSet r=s.executeQuery()) {
            while(r.next()) servicios.add(new ServicioSolicitable(r.getInt(1),r.getString(2),r.getString(3),r.getBigDecimal(4)));
            return servicios;
        } catch(SQLException e) { throw new IllegalStateException("No se pudieron consultar los servicios disponibles.",e); }
    }

    public PresupuestoEvento presupuesto(int evento, int organizador) {
        try(Connection c=conectar()) { return new PresupuestoEvento(evento(c,evento,organizador,false),comprometido(c,evento)); }
        catch(SQLException e) { throw new IllegalStateException("No se pudo consultar el presupuesto del evento.",e); }
    }

    public List<SolicitudServicioEvento> listarSolicitudes(int evento, int organizador) {
        List<SolicitudServicioEvento> solicitudes=new ArrayList<>();
        try(Connection c=conectar()) {
            evento(c,evento,organizador,false);
            try(PreparedStatement s=c.prepareStatement("SELECT s.id_solicitud,COALESCE(v.nombre,'Servicio histórico'),p.nombre,s.costo_comprometido,s.estado "
                    + "FROM Solicitud_Ejecucion s JOIN Proveedor p ON p.id_proveedor=s.id_proveedor "
                    + "LEFT JOIN Servicio v ON v.id_servicio=s.id_servicio WHERE s.id_evento=? ORDER BY s.id_solicitud")) {
                s.setInt(1,evento);
                try(ResultSet r=s.executeQuery()) {
                    while(r.next()) solicitudes.add(new SolicitudServicioEvento(r.getInt(1),r.getString(2),r.getString(3),r.getBigDecimal(4),r.getString(5)));
                }
            }
            return solicitudes;
        } catch(SQLException e) { throw new IllegalStateException("No se pudieron consultar las solicitudes del evento.",e); }
    }

    public int solicitar(int evento, int organizador, int servicio) {
        try(Connection c=conectar()) {
            c.setAutoCommit(false);
            try {
                BigDecimal total=evento(c,evento,organizador,true);
                BigDecimal costo; int proveedor, usuario;
                try(PreparedStatement s=c.prepareStatement("SELECT v.precio,p.id_proveedor,p.id_usuario FROM Servicio v "
                        + "JOIN Proveedor p ON p.id_proveedor=v.id_proveedor JOIN Usuario u ON u.id_usuario=p.id_usuario "
                        + "WHERE v.id_servicio=? AND UPPER(v.estado)='DISPONIBLE' AND "+VERIFICADO+" FOR UPDATE")) {
                    s.setInt(1,servicio);
                    try(ResultSet r=s.executeQuery()) {
                        if(!r.next()) throw new IllegalArgumentException("Selecciona un servicio disponible de un proveedor verificado.");
                        costo=r.getBigDecimal(1); proveedor=r.getInt(2); usuario=r.getInt(3);
                    }
                }
                if(costo==null || costo.signum()<0) throw new IllegalArgumentException("El servicio no tiene un precio válido.");
                comprobarPresupuesto(total,comprometido(c,evento).add(costo));
                int necesidad;
                try (PreparedStatement s=c.prepareStatement("INSERT INTO Necesidad_Servicio_Evento (id_evento,descripcion,estado) VALUES (?,?,'ABIERTA')",Statement.RETURN_GENERATED_KEYS)) {
                    s.setInt(1,evento); s.setString(2,"Servicio solicitado para el evento"); s.executeUpdate();
                    try (ResultSet r=s.getGeneratedKeys()) { if(!r.next()) throw new SQLException("No se generó la necesidad."); necesidad=r.getInt(1); }
                }
                int solicitud;
                try(PreparedStatement s=c.prepareStatement("INSERT INTO Solicitud_Ejecucion "
                        + "(id_proveedor,id_evento,id_servicio,fecha_solicitud,estado,costo_comprometido,id_necesidad_servicio) VALUES (?,?,?,CURRENT_TIMESTAMP,'PENDIENTE',?,?)",Statement.RETURN_GENERATED_KEYS)) {
                    s.setInt(1,proveedor); s.setInt(2,evento); s.setInt(3,servicio); s.setBigDecimal(4,costo); s.setInt(5,necesidad); s.executeUpdate();
                    try(ResultSet r=s.getGeneratedKeys()) { if(!r.next()) throw new SQLException("No se generó la solicitud."); solicitud=r.getInt(1); }
                }
                aviso(c,usuario,evento,"Recibiste una solicitud de servicio. Consulta tus solicitudes de ejecución.");
                c.commit(); return solicitud;
            } catch(SQLException | RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw new IllegalStateException("No se pudo registrar la solicitud. No se comprometió presupuesto; vuelve a intentarlo.",e); }
    }

    public void ajustarPresupuesto(int evento, int organizador, BigDecimal total) {
        try(Connection c=conectar()) {
            c.setAutoCommit(false);
            try {
                evento(c,evento,organizador,true);
                comprobarPresupuesto(total,comprometido(c,evento));
                try(PreparedStatement s=c.prepareStatement("UPDATE Evento SET presupuesto=? WHERE id_evento=?")) {
                    s.setBigDecimal(1,total); s.setInt(2,evento); s.executeUpdate();
                }
                c.commit();
            } catch(SQLException | RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw new IllegalStateException("No se pudo ajustar el presupuesto. El valor anterior se conservó.",e); }
    }

    public void retirar(int evento, int organizador, int solicitud) {
        try(Connection c=conectar()) {
            c.setAutoCommit(false);
            try {
                evento(c,evento,organizador,true);
                try(PreparedStatement s=c.prepareStatement("UPDATE Solicitud_Ejecucion SET estado='RETIRADA' "
                        + "WHERE id_solicitud=? AND id_evento=? AND UPPER(estado)='PENDIENTE' "
                        + "AND NOT EXISTS (SELECT 1 FROM Propuesta p WHERE p.id_solicitud=Solicitud_Ejecucion.id_solicitud)")) {
                    s.setInt(1,solicitud); s.setInt(2,evento);
                    if(s.executeUpdate()!=1) throw new IllegalArgumentException("Solo puedes retirar una solicitud pendiente que aún no haya recibido una propuesta.");
                }
                try (PreparedStatement s=c.prepareStatement("UPDATE Necesidad_Servicio_Evento SET estado='CANCELADA' "
                        + "WHERE id_necesidad_servicio=(SELECT id_necesidad_servicio FROM Solicitud_Ejecucion WHERE id_solicitud=?) "
                        + "AND NOT EXISTS (SELECT 1 FROM Solicitud_Ejecucion r WHERE r.id_necesidad_servicio=Necesidad_Servicio_Evento.id_necesidad_servicio "
                        + "AND UPPER(r.estado) NOT IN ('RECHAZADA','CANCELADA','RETIRADA'))")) {
                    s.setInt(1,solicitud); s.executeUpdate();
                }
                try(PreparedStatement s=c.prepareStatement("SELECT p.id_usuario FROM Solicitud_Ejecucion s JOIN Proveedor p ON p.id_proveedor=s.id_proveedor WHERE s.id_solicitud=?")) {
                    s.setInt(1,solicitud);
                    try(ResultSet r=s.executeQuery()) { r.next(); aviso(c,r.getInt(1),evento,"El organizador retiró una solicitud de servicio pendiente."); }
                }
                c.commit();
            } catch(SQLException | RuntimeException e) { c.rollback(); throw e; }
        } catch(SQLException e) { throw new IllegalStateException("No se pudo retirar la solicitud. El presupuesto anterior se conservó.",e); }
    }

    private BigDecimal evento(Connection c, int evento, int organizador, boolean modificar) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT presupuesto,estado FROM Evento WHERE id_evento=? AND id_organizador=?"+(modificar?" FOR UPDATE":""))) {
            s.setInt(1,evento); s.setInt(2,organizador);
            try(ResultSet r=s.executeQuery()) {
                if(!r.next()) throw new IllegalArgumentException("Selecciona un evento existente de tu cuenta.");
                if(modificar && !"PLANEADO".equalsIgnoreCase(r.getString(2)))
                    throw new IllegalArgumentException("Solo puedes gestionar solicitudes y presupuesto de eventos en planeación.");
                BigDecimal total=r.getBigDecimal(1);
                if(total==null || total.signum()<0) throw new IllegalArgumentException("El evento no tiene un presupuesto válido. Debe conciliarse antes de solicitar servicios.");
                return total;
            }
        }
    }

    private BigDecimal comprometido(Connection c, int evento) throws SQLException {
        BigDecimal total=BigDecimal.ZERO;
        // Una contratación sustituye el compromiso de su solicitud: nunca se cuentan ambos.
        String sql="SELECT c.valor_acordado FROM Contratacion c WHERE c.id_evento=? AND "+CONTRATO_ACTIVO
                + " UNION ALL SELECT s.costo_comprometido FROM Solicitud_Ejecucion s WHERE s.id_evento=? "
                + "AND UPPER(s.estado) NOT IN ('RECHAZADA','CANCELADA','RETIRADA') AND NOT EXISTS "
                + "(SELECT 1 FROM Contratacion c JOIN Propuesta p ON p.id_propuesta=c.id_propuesta "
                + "WHERE p.id_solicitud=s.id_solicitud AND c.id_evento=s.id_evento AND "+CONTRATO_ACTIVO+")";
        try(PreparedStatement s=c.prepareStatement(sql)) {
            s.setInt(1,evento); s.setInt(2,evento);
            try(ResultSet r=s.executeQuery()) {
                while(r.next()) {
                    BigDecimal valor=r.getBigDecimal(1);
                    if(valor==null || valor.signum()<0) throw new IllegalArgumentException("Hay solicitudes o contratos históricos sin costo válido. Deben conciliarse antes de comprometer presupuesto.");
                    total=total.add(valor);
                }
            }
        }
        return total;
    }

    void validarCompromiso(Connection c, int evento, BigDecimal total, BigDecimal costo) throws SQLException {
        if (costo == null || costo.signum() < 0) throw new IllegalArgumentException("El servicio no tiene un precio válido.");
        comprobarPresupuesto(total, comprometido(c, evento).add(costo));
    }

    private void comprobarPresupuesto(BigDecimal total, BigDecimal comprometido) {
        if(comprometido.compareTo(total)>0) throw new IllegalArgumentException(
                "Presupuesto insuficiente. Faltan $"+comprometido.subtract(total).toPlainString()+" COP para registrar el compromiso.");
    }
    private void aviso(Connection c, int usuario, int evento, String mensaje) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO Notificacion_interna (id_usuario,id_evento,mensaje) VALUES (?,?,?)")) {
            s.setInt(1,usuario); s.setInt(2,evento); s.setString(3,mensaje); s.executeUpdate();
        }
    }
}
