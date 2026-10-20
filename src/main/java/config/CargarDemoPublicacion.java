package config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/** Demostración explícita de CU03 en una base vacía; no ejecuta CU05. */
public class CargarDemoPublicacion {
    public static void main(String[] args) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) { cargar(connection); }
        System.out.println("Demostración CU03 cargada: inicia sesión como laura.gomez y abre Publicaciones.");
    }

    public static void cargar(Connection connection) throws Exception {
        // Esta operación se rechaza antes de escribir si la base ya contiene registros.
        CargarDatosEjemplo.cargar(connection);
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Usuario (nombre,usuario,contraseña,estado,fecha_registro,acepta_marketing,tipo_usuario) "
                + "VALUES ('Administrador de demostración','admin.demo','solo-demostracion','ACTIVO',CURRENT_TIMESTAMP,FALSE,'ADMINISTRADOR')",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                try (PreparedStatement rol = connection.prepareStatement(
                        "INSERT INTO Usuario_rol (id_usuario,id_rol) SELECT ?,id_rol FROM Rol WHERE nombre='ADMINISTRADOR'")) {
                    rol.setInt(1,keys.getInt(1)); rol.executeUpdate();
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Evento (id_organizador,nombre,tipo,fecha_inicio,fecha_fin,ubicacion,estado,"
                    + "fecha_creacion,presupuesto,invitados_iniciales) VALUES "
                    + "(1,'Evento finalizado de demostración','Social',TIMESTAMP '2026-09-01 09:00:00',"
                    + "TIMESTAMP '2026-09-01 18:00:00','Bogotá','FINALIZADO',CURRENT_TIMESTAMP,1000000,25)");
        }
    }
}
