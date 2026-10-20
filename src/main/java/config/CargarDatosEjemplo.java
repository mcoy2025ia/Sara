package config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

import org.h2.tools.RunScript;

/**
 * Carga los datos de ejemplo del proyecto y prepara los
 * proveedores de demostracion del caso de uso CU-028.
 */
public class CargarDatosEjemplo {

    public static void main(String[] args) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            cargar(connection);
        }

        cargarProveedoresDemostracionCU28();

        System.out.println(
                "Datos de ejemplo y escenarios CU-028 preparados.");
    }

    /**
     * Carga los datos generales del proyecto.
     * Solo se permite cuando todas las tablas estan vacias.
     */
    public static void cargar(Connection connection) throws Exception {

        try (Statement tablas = connection.createStatement();
             ResultSet result = tablas.executeQuery(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                     + "WHERE TABLE_SCHEMA = 'PUBLIC' "
                     + "AND TABLE_TYPE = 'BASE TABLE' "
                     // CU-030: la configuración de evaluación se crea una sola vez con su migración.
                     + "AND TABLE_NAME <> 'CONFIGURACION_EVALUACION_ORGANIZADOR' "
                     // El registro de migraciones nunca está vacío después de inicializar.
                     + "AND TABLE_NAME <> 'ESQUEMA_VERSION'")) {

            while (result.next()) {
                String tabla = result.getString(1).replace("\"", "\"\"");

                try (Statement statement = connection.createStatement();
                     ResultSet registros = statement.executeQuery(
                             "SELECT COUNT(*) FROM \"" + tabla + "\"")) {

                    registros.next();

                    if (registros.getLong(1) != 0) {
                        throw new SQLException(
                                "Los ejemplos generales requieren una base "
                                + "vacia. No se modificaron los registros "
                                + "existentes.");
                    }
                }
            }
        }

        InputStream recurso =
                CargarDatosEjemplo.class.getResourceAsStream("/seed.sql");

        if (recurso == null) {
            throw new SQLException(
                    "No se encontro el archivo seed.sql.");
        }

        try (InputStreamReader reader = new InputStreamReader(
                recurso, StandardCharsets.UTF_8)) {

            RunScript.execute(connection, reader);
        }

        avanzarIdentidad(connection, "Usuario", "id_usuario");
        avanzarIdentidad(connection, "Usuario_rol", "id_usuario_rol");
        avanzarIdentidad(connection, "Proveedor", "id_proveedor");
    }

    /**
     * Agrega los diez escenarios de demostracion de CU-028.
     *
     * Es idempotente: identifica los registros mediante nombres de
     * usuario reservados para las pruebas y solo crea los que faltan.
     * Nunca borra proveedores ni modifica sus decisiones anteriores.
     */
    public static void cargarProveedoresDemostracionCU28()
            throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                int idRolProveedor = asegurarRolProveedor(connection);

                for (int caso = 1; caso <= 10; caso++) {
                    crearEscenarioProveedor(connection, idRolProveedor, caso);
                }

                connection.commit();

                System.out.println(
                        "Escenarios CU-028 comprobados. "
                        + "Los registros existentes se conservaron.");

            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }

                throw e;
            }
        }
    }

    // ---------------------------------------------------------
    // CREACION DE CADA ESCENARIO
    // ---------------------------------------------------------

    private static void crearEscenarioProveedor(
            Connection connection, int idRolProveedor, int caso)
            throws SQLException {

        String sufijo = String.format("%02d", caso);
        String usuario = "cu28_demo_" + sufijo;

        String nombreUsuario;
        String nombreProveedor;
        String empresa;
        String correo;
        String telefono = "30020000" + sufijo;
        String direccion = "Direccion de demostracion CU-028";

        switch (caso) {
            case 1:
                nombreUsuario = "Alicia Flores";
                nombreProveedor = "Alicia Flores";
                empresa = "Flores Amanecer";
                correo = "alicia@amanecer.test";
                break;

            case 2:
                nombreUsuario = "Bruno Catering";
                nombreProveedor = "Bruno Catering";
                empresa = "Catering del Parque";
                correo = "bruno@catering.test";
                break;

            case 3:
                nombreUsuario = "Carolina Fotografia";
                nombreProveedor = "Carolina Fotografia";
                empresa = "FotoLuz Eventos";
                correo = "carolina@fotoluz.test";
                break;

            case 4:
                nombreUsuario = "David Decoraciones";
                nombreProveedor = "David Decoraciones";
                empresa = "Decoraciones Violeta";
                correo = "david@violeta.test";
                break;

            case 5:
                nombreUsuario = "Elena Sonido";
                nombreProveedor = "Elena Sonido";
                empresa = "Sonido Total";
                correo = "elena@sonido.test";
                break;

            case 6:
                nombreUsuario = "Fabio Transporte";
                nombreProveedor = "Fabio Transporte";
                empresa = "Transporte Fiesta";
                correo = "correo-no-valido";
                break;

            case 7:
                nombreUsuario = "Gabriela Eventos";
                nombreProveedor = "Gabriela Eventos";
                empresa = "Eventos Gabriela";
                correo = "gabriela@eventos.test";
                break;

            case 8:
                nombreUsuario = "Hector Alquileres";
                nombreProveedor = "Hector Alquileres";
                empresa = "Alquileres Bogota";
                correo = "hector@alquileres.test";
                break;

            case 9:
                nombreUsuario = "Isabel Reposteria";
                nombreProveedor = "Isabel Reposteria";
                empresa = "Reposteria Luna";
                correo = "isabel@reposteria.test";
                break;

            default:
                nombreUsuario = "Julian Proveedor";
                nombreProveedor = "";
                empresa = "Proveedor con nombre incompleto";
                correo = "julian@incompleto.test";
                break;
        }

        int idUsuario = asegurarUsuario(
                connection, usuario, nombreUsuario);

        asegurarRolUsuario(connection, idUsuario, idRolProveedor);

        int idProveedor = asegurarProveedor(
                connection,
                idUsuario,
                nombreProveedor,
                empresa,
                correo,
                telefono,
                direccion
        );

        crearDocumentosEscenario(connection, idProveedor, caso);
        crearServiciosEscenario(connection, idProveedor, caso);
    }

    // ---------------------------------------------------------
    // USUARIO Y ROL
    // ---------------------------------------------------------

    private static int asegurarRolProveedor(Connection connection)
            throws SQLException {

        String consulta =
                "SELECT id_rol FROM Rol "
                + "WHERE UPPER(nombre) = 'PROVEEDOR'";

        try (PreparedStatement statement =
                     connection.prepareStatement(consulta);
             ResultSet result = statement.executeQuery()) {

            if (result.next()) {
                return result.getInt("id_rol");
            }
        }

        String insertar =
                "INSERT INTO Rol (nombre, descripcion, estado) "
                + "VALUES (?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(
                insertar, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, "PROVEEDOR");
            statement.setString(2, "Rol para proveedores");
            statement.setString(3, "ACTIVO");
            statement.executeUpdate();

            try (ResultSet result = statement.getGeneratedKeys()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
        }

        throw new SQLException(
                "No fue posible obtener el rol PROVEEDOR.");
    }

    private static int asegurarUsuario(
            Connection connection, String usuario, String nombre)
            throws SQLException {

        String consulta =
                "SELECT id_usuario FROM Usuario WHERE usuario = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(consulta)) {

            statement.setString(1, usuario);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("id_usuario");
                }
            }
        }

        String insertar =
                "INSERT INTO Usuario "
                + "(nombre, usuario, contraseña, estado, fecha_registro, "
                + "acepta_marketing, tipo_usuario) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, FALSE, ?)";

        try (PreparedStatement statement = connection.prepareStatement(
                insertar, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, nombre);
            statement.setString(2, usuario);
            statement.setString(3, "demo-cu28");
            statement.setString(4, "ACTIVO");
            statement.setString(5, "PROVEEDOR");

            statement.executeUpdate();

            try (ResultSet result = statement.getGeneratedKeys()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
        }

        throw new SQLException(
                "No fue posible crear el usuario de demostracion " + usuario);
    }

    private static void asegurarRolUsuario(
            Connection connection, int idUsuario, int idRol)
            throws SQLException {

        String consulta =
                "SELECT id_usuario_rol FROM Usuario_rol "
                + "WHERE id_usuario = ? AND id_rol = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(consulta)) {

            statement.setInt(1, idUsuario);
            statement.setInt(2, idRol);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }
        }

        String insertar =
                "INSERT INTO Usuario_rol (id_usuario, id_rol) "
                + "VALUES (?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(insertar)) {

            statement.setInt(1, idUsuario);
            statement.setInt(2, idRol);
            statement.executeUpdate();
        }
    }

    // ---------------------------------------------------------
    // PROVEEDOR
    // ---------------------------------------------------------

    private static int asegurarProveedor(
            Connection connection,
            int idUsuario,
            String nombre,
            String empresa,
            String correo,
            String telefono,
            String direccion) throws SQLException {

        String consulta =
                "SELECT id_proveedor FROM Proveedor WHERE id_usuario = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(consulta)) {

            statement.setInt(1, idUsuario);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("id_proveedor");
                }
            }
        }

        String insertar =
                "INSERT INTO Proveedor "
                + "(id_usuario, nombre, empresa, correo, telefono, "
                + "direccion, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'PENDIENTE_VERIFICACION')";

        try (PreparedStatement statement = connection.prepareStatement(
                insertar, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, idUsuario);
            statement.setString(2, nombre);
            statement.setString(3, empresa);
            statement.setString(4, correo);
            statement.setString(5, telefono);
            statement.setString(6, direccion);

            statement.executeUpdate();

            try (ResultSet result = statement.getGeneratedKeys()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
        }

        throw new SQLException(
                "No fue posible crear el proveedor de demostracion.");
    }

    // ---------------------------------------------------------
    // DOCUMENTOS POR ESCENARIO
    // ---------------------------------------------------------

    private static void crearDocumentosEscenario(
            Connection connection, int idProveedor, int caso)
            throws SQLException {

        switch (caso) {
            case 1:
                documentoSiFalta(connection, idProveedor,
                        "IDENTIFICACION", "demo_cu28_01.pdf",
                        30, "VALIDADO", null);
                break;

            case 2:
                // Escenario sin documentos.
                break;

            case 3:
                documentoSiFalta(connection, idProveedor,
                        "IDENTIFICACION", "demo_cu28_03.pdf",
                        30, "PRESENTADO", null);
                break;

            case 4:
                documentoSiFalta(connection, idProveedor,
                        "IDENTIFICACION", "demo_cu28_04.pdf",
                        -10, "PRESENTADO",
                        "Documento vencido para la prueba.");
                break;

            case 5:
                documentoSiFalta(connection, idProveedor,
                        "IDENTIFICACION", "demo_cu28_05.pdf",
                        30, "POR_CORREGIR",
                        "El documento necesita correcciones.");
                break;

            case 6:
                documentoSiFalta(connection, idProveedor,
                        "IDENTIFICACION", "demo_cu28_06.pdf",
                        30, "VALIDADO", null);
                break;

            case 7:
                documentoSiFalta(connection, idProveedor,
                        "IDENTIFICACION", "demo_cu28_07a.pdf",
                        30, "VALIDADO", null);

                documentoSiFalta(connection, idProveedor,
                        "CERTIFICADO", "demo_cu28_07b.pdf",
                        30, "PRESENTADO", null);
                break;

            case 8:
                documentoSiFalta(connection, idProveedor,
                        "IDENTIFICACION", "demo_cu28_08.pdf",
                        null, "VALIDADO", null);
                break;

            case 9:
                documentoSiFalta(connection, idProveedor,
                        "IDENTIFICACION", "demo_cu28_09a.pdf",
                        60, "VALIDADO", null);

                documentoSiFalta(connection, idProveedor,
                        "CERTIFICADO", "demo_cu28_09b.pdf",
                        60, "VALIDADO", null);
                break;

            case 10:
                documentoSiFalta(connection, idProveedor,
                        "IDENTIFICACION", "demo_cu28_10.pdf",
                        30, "VALIDADO", null);
                break;

            default:
                throw new IllegalArgumentException(
                        "Escenario CU-028 desconocido: " + caso);
        }
    }

    private static void documentoSiFalta(
            Connection connection,
            int idProveedor,
            String tipo,
            String archivo,
            Integer diasVencimiento,
            String estado,
            String observacion) throws SQLException {

        String consulta =
                "SELECT id_documento FROM Documento_Proveedor "
                + "WHERE id_proveedor = ? AND nombre_archivo = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(consulta)) {

            statement.setInt(1, idProveedor);
            statement.setString(2, archivo);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }
        }

        LocalDate vencimiento = diasVencimiento == null
                ? null
                : LocalDate.now().plusDays(diasVencimiento);

        String insertar =
                "INSERT INTO Documento_Proveedor "
                + "(id_proveedor, tipo_documento, nombre_archivo, "
                + "ruta_archivo, fecha_vencimiento, estado, observacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(insertar)) {

            statement.setInt(1, idProveedor);
            statement.setString(2, tipo);
            statement.setString(3, archivo);
            statement.setString(4, "demo/cu28/" + archivo);

            if (vencimiento == null) {
                statement.setDate(5, null);
            } else {
                statement.setDate(5, Date.valueOf(vencimiento));
            }

            statement.setString(6, estado);
            statement.setString(7, observacion);

            statement.executeUpdate();
        }
    }

    // ---------------------------------------------------------
    // SERVICIOS POR ESCENARIO
    // ---------------------------------------------------------

    private static void crearServiciosEscenario(
            Connection connection, int idProveedor, int caso)
            throws SQLException {

        String nombreServicio = String.format(
                "Demo CU28 - Servicio %02d", caso);

        servicioSiFalta(
                connection, idProveedor, nombreServicio,
                "Servicio ficticio para las pruebas del CU-028.",
                150000 + (caso * 10000)
        );

        if (caso == 9) {
            servicioSiFalta(
                    connection, idProveedor,
                    "Demo CU28 - Servicio adicional 09",
                    "Segundo servicio ficticio para revisar el detalle.",
                    250000
            );
        }
    }

    private static void servicioSiFalta(
            Connection connection,
            int idProveedor,
            String nombre,
            String descripcion,
            int precio) throws SQLException {

        String consulta =
                "SELECT id_servicio FROM Servicio "
                + "WHERE id_proveedor = ? AND nombre = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(consulta)) {

            statement.setInt(1, idProveedor);
            statement.setString(2, nombre);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }
        }

        String insertar =
                "INSERT INTO Servicio "
                + "(id_proveedor, nombre, descripcion, precio, categoria, estado) "
                + "VALUES (?, ?, ?, ?, 'PRUEBA_CU28', 'DISPONIBLE')";

        try (PreparedStatement statement =
                     connection.prepareStatement(insertar)) {

            statement.setInt(1, idProveedor);
            statement.setString(2, nombre);
            statement.setString(3, descripcion);
            statement.setInt(4, precio);
            statement.executeUpdate();
        }
    }

    // ---------------------------------------------------------
    // IDENTIDADES DEL ESQUEMA GENERAL
    // ---------------------------------------------------------

    private static void avanzarIdentidad(
            Connection connection, String tabla, String columna)
            throws SQLException {

        long siguiente;

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COALESCE(MAX(" + columna + "), 0) + 1 FROM "
                     + tabla)) {

            result.next();
            siguiente = result.getLong(1);
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "ALTER TABLE " + tabla + " ALTER COLUMN "
                    + columna + " RESTART WITH " + siguiente
            );
        }
    }
}