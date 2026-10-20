package repositories;

import config.DatabaseConnection;
import entities.SesionUsuario;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RepositoryAvisosProveedor {
    private final String url;
    public RepositoryAvisosProveedor() { this(null); }
    public RepositoryAvisosProveedor(String url) { this.url = url; }

    public List<String> listar(SesionUsuario sesion) {
        try (Connection c = url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url)) {
            try (PreparedStatement perfil = c.prepareStatement("SELECT 1 FROM Usuario u JOIN Proveedor p ON p.id_usuario=u.id_usuario "
                    + "WHERE u.id_usuario=? AND p.id_proveedor=? AND UPPER(u.estado)='ACTIVO' "
                    + "AND UPPER(u.tipo_usuario)='PROVEEDOR' AND UPPER(p.estado) IN ('APROBADO','VERIFICADO')")) {
                perfil.setInt(1, sesion.getIdUsuario()); perfil.setInt(2, sesion.getIdActor());
                try (ResultSet r = perfil.executeQuery()) {
                    if (!r.next()) throw new IllegalStateException("Tu cuenta de proveedor ya no está habilitada.");
                }
            }
            List<String> avisos = new ArrayList<>();
            try (PreparedStatement consulta = c.prepareStatement("SELECT mensaje FROM Notificacion_interna "
                    + "WHERE id_usuario=? ORDER BY id_notificacion DESC")) {
                consulta.setInt(1, sesion.getIdUsuario());
                try (ResultSet r = consulta.executeQuery()) { while (r.next()) avisos.add(r.getString(1)); }
            }
            return avisos;
        } catch (SQLException e) { throw new IllegalStateException("No se pudieron consultar tus avisos. Vuelve a intentarlo.", e); }
    }
}
