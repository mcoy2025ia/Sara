package config;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

public class CargarOrganizadoresPruebaCU30 {

    private static final String CLAVE_PRUEBAS = "PruebasCU30";
    private static final String ADMIN_NOMBRE = "Administrador CU-030";
    private static final String ADMIN_USUARIO = "admin.cu030";
    private static final String ADMIN_CLAVE = "AdminCU0302026";

    /*
     * Los documentos y perfiles de este cargador son ficticios.
     * No se crean archivos reales ni se modifica información
     * de organizadores que ya existen.
     */

    private static final List<OrganizadorPrueba> ORGANIZADORES =
            List.of(

        new OrganizadorPrueba(
            "Valentina Eventos",
            "cu30.valentina",
            "CC-CU30-001",
            "Valentina Eventos S.A.S.",
            "3000001001",
            "Bogotá",
            "Bodas y eventos corporativos",
            "Portafolio de demostración con experiencia en bodas, congresos, "
                + "eventos empresariales, celebraciones sociales y coordinación logística.",
            "1800000", 12, "4000000",
            List.of(
                documento("IDENTIDAD", "VALIDADO", 2),
                documento("RUT", "VALIDADO", 2)
            )
        ),

        new OrganizadorPrueba(
            "Andrés Celebraciones",
            "cu30.andres",
            "CC-CU30-002",
            "Andrés Celebraciones",
            "3000001002",
            "Bogotá",
            "Celebraciones familiares",
            "Portafolio inicial de cumpleaños y reuniones familiares.",
            "850000", 2, "2500000",
            List.of(
                documento("IDENTIDAD", "VALIDADO", 2),
                documento("RUT", "VALIDADO", 2)
            )
        ),

        new OrganizadorPrueba(
            "Isabella Producciones",
            "cu30.isabella",
            "CC-CU30-003",
            "Isabella Producciones",
            "3000001003",
            "Medellín",
            "Producción de eventos",
            "Portafolio de producción de eventos sociales y corporativos, "
                + "con planificación, coordinación de proveedores y logística.",
            "1600000", 5, "3500000",
            List.of(
                documento("IDENTIDAD", "PRESENTADO", 2),
                documento("RUT", "PRESENTADO", 2)
            )
        ),

        new OrganizadorPrueba(
            "Mateo Fest",
            "cu30.mateo",
            "CC-CU30-004",
            "Mateo Fest",
            "3000001004",
            "Cali",
            "Eventos musicales",
            "Portafolio de eventos musicales, escenarios y coordinación técnica.",
            "1400000", 8, "4200000",
            List.of(
                documento("IDENTIDAD", "VENCIDO", -2),
                documento("RUT", "PRESENTADO", 2)
            )
        ),

        new OrganizadorPrueba(
            "Natalia Eventos",
            "cu30.natalia",
            "CC-CU30-DUPLICADO-05",
            "Natalia Eventos",
            "3000001005",
            "Bogotá",
            "Organización de bodas",
            "Portafolio de bodas, decoración, selección de proveedores y logística.",
            "1900000", 7, "4500000",
            List.of(
                documento("IDENTIDAD", "VALIDADO", 2),
                documento("RUT", "VALIDADO", 2)
            )
        ),

        new OrganizadorPrueba(
            "Samuel Experiencias",
            "cu30.samuel",
            "CC-CU30-006",
            "Samuel Experiencias",
            "3000001006",
            "Bogotá",
            "Eventos empresariales",
            "Portafolio amplio de convenciones, lanzamientos de productos, "
                + "eventos empresariales y coordinación de equipos.",
            "2800000", 10, "8500000",
            List.of(
                documento("IDENTIDAD", "VALIDADO", 2),
                documento("RUT", "VALIDADO", 2)
            )
        ),

        new OrganizadorPrueba(
            "Lucía Wedding Planner",
            "cu30.lucia",
            "CC-CU30-007",
            "Lucía Wedding Planner",
            "3000001007",
            "Cartagena",
            "Bodas y eventos premium",
            "Portafolio especializado en bodas, planificación integral, "
                + "presupuestos, coordinación de locaciones y gestión de proveedores.",
            "3200000", 15, "4800000",
            List.of(
                documento("IDENTIDAD", "VALIDADO", 2),
                documento("RUT", "VALIDADO", 2)
            )
        ),

        new OrganizadorPrueba(
            "Gabriel Eventos",
            "cu30.gabriel",
            null,
            "Gabriel Eventos",
            null,
            "Bogotá",
            null,
            null,
            null, null, null,
            List.of()
        ),

        new OrganizadorPrueba(
            "Mariana Producciones",
            "cu30.mariana",
            "CC-CU30-009",
            "Mariana Producciones",
            "3000001009",
            "Medellín",
            "Eventos culturales",
            "Portafolio de festivales culturales, muestras artísticas, "
                + "eventos sociales y coordinación de participantes.",
            "1500000", 6, "3900000",
            List.of(
                documento("IDENTIDAD", "VALIDADO", 2),
                documento("RUT", "VALIDADO", 2)
            )
        ),

        new OrganizadorPrueba(
            "Felipe Celebraciones",
            "cu30.felipe",
            "CC-CU30-010",
            "Felipe Celebraciones",
            "3000001010",
            "Barranquilla",
            "Celebraciones sociales",
            "Portafolio de eventos sociales, cumpleaños y reuniones empresariales.",
            "1100000", 3, "3000000",
            List.of(
                documento("IDENTIDAD", "PRESENTADO", 2),
                documento("RUT", "POR_CORREGIR", 2)
            )
        )
    );

    public static void main(String[] args) throws Exception {

        try (Connection connection = DatabaseConnection.getConnection()) {
            cargar(connection);
        }
    }

    public static void cargar(Connection connection)
            throws SQLException {

        boolean autoCommitAnterior = connection.getAutoCommit();
        connection.setAutoCommit(false);

        int perfilesNuevos = 0;
        int documentosNuevos = 0;

        try {
            asegurarAdministradorPruebas(connection);
            int rolOrganizador = obtenerOCrearRol(
                    connection,
                    "ORGANIZADOR",
                    "Usuario que gestiona y organiza eventos.");

            int rolCliente = obtenerOCrearRol(
                    connection,
                    "CLIENTE",
                    "Usuario de referencia para pruebas.");

            /*
             * Este usuario auxiliar permite probar una identidad
             * duplicada sin modificar los clientes existentes.
             */
            int idClienteReferencia = asegurarUsuario(
                    connection,
                    "Cliente de referencia CU-030",
                    "cu30.cliente.duplicado",
                    "CC-CU30-DUPLICADO-05",
                    "CLIENTE");

            asociarRol(
                    connection,
                    idClienteReferencia,
                    rolCliente);

            for (OrganizadorPrueba prueba : ORGANIZADORES) {

                int idUsuario = asegurarUsuario(
                        connection,
                        prueba.nombre(),
                        prueba.usuario(),
                        prueba.documentoIdentidad(),
                        "ORGANIZADOR");

                asociarRol(
                        connection,
                        idUsuario,
                        rolOrganizador);

                int idOrganizador = buscarOrganizador(
                        connection,
                        idUsuario);

                if (idOrganizador == 0) {

                    idOrganizador = crearOrganizador(
                            connection,
                            idUsuario,
                            prueba);

                    perfilesNuevos++;
                }

                for (DocumentoPrueba documento : prueba.documentos()) {

                    if (crearDocumentoSiFalta(
                            connection,
                            idOrganizador,
                            prueba.documentoIdentidad(),
                            documento)) {

                        documentosNuevos++;
                    }
                }
            }

            connection.commit();

            System.out.println();
            System.out.println("=== CARGA DE DATOS CU-030 COMPLETADA ===");
            System.out.println("Perfiles nuevos: " + perfilesNuevos);
            System.out.println("Documentos nuevos: " + documentosNuevos);
            System.out.println("Organizadores previstos: " + ORGANIZADORES.size());
            System.out.println();
            System.out.println("Credenciales de los organizadores:");
            System.out.println("Usuarios: cu30.valentina, cu30.andres,");
            System.out.println("cu30.isabella, cu30.mateo, cu30.natalia,");
            System.out.println("cu30.samuel, cu30.lucia, cu30.gabriel,");
            System.out.println("cu30.mariana, cu30.felipe");
            System.out.println("Contraseña común: " + CLAVE_PRUEBAS);
            System.out.println();
            System.out.println(
                    "Si ya existían perfiles, se conservaron sus estados y decisiones.");

        } catch (SQLException | RuntimeException e) {

            connection.rollback();
            throw e;

        } finally {
            connection.setAutoCommit(autoCommitAnterior);
        }
    }

    private static int asegurarUsuario(
            Connection connection,
            String nombre,
            String usuario,
            String documentoIdentidad,
            String tipoUsuario) throws SQLException {

        int idExistente = buscarUsuario(connection, usuario);

        if (idExistente != 0) {

            String sql =
                    "SELECT tipo_usuario FROM Usuario WHERE id_usuario = ?";

            try (PreparedStatement statement =
                        connection.prepareStatement(sql)) {

                statement.setInt(1, idExistente);

                try (ResultSet result = statement.executeQuery()) {

                    if (result.next()
                            && !tipoUsuario.equalsIgnoreCase(
                                    result.getString("tipo_usuario"))) {

                        throw new SQLException(
                                "El usuario " + usuario
                                + " ya existe con otro tipo de cuenta.");
                    }
                }
            }

            // Solo se actualiza la identidad del usuario auxiliar reservado.
            if ("cu30.cliente.duplicado".equals(usuario)) {

                try (PreparedStatement statement =
                            connection.prepareStatement(
                                    "UPDATE Usuario SET documento_identidad = ? "
                                    + "WHERE id_usuario = ?")) {

                    statement.setString(1, documentoIdentidad);
                    statement.setInt(2, idExistente);
                    statement.executeUpdate();
                }
            }

            return idExistente;
        }

        String sql =
                "INSERT INTO Usuario "
                + "(nombre, usuario, contraseña, estado, fecha_registro, "
                + "acepta_marketing, tipo_usuario, documento_identidad) "
                + "VALUES (?, ?, ?, 'ACTIVO', CURRENT_TIMESTAMP, FALSE, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, nombre);
            statement.setString(2, usuario);
            statement.setString(3, CLAVE_PRUEBAS);
            statement.setString(4, tipoUsuario);
            statement.setString(5, documentoIdentidad);

            statement.executeUpdate();

            try (ResultSet claves = statement.getGeneratedKeys()) {

                if (claves.next()) {
                    return claves.getInt(1);
                }
            }
        }

        throw new SQLException(
                "No se pudo obtener el ID del usuario " + usuario);
    }

    private static int crearOrganizador(
            Connection connection,
            int idUsuario,
            OrganizadorPrueba prueba) throws SQLException {

        String sql =
                "INSERT INTO Organizador "
                + "(id_usuario, empresa, telefono, estado, ciudad, "
                + "especialidad, portafolio, tarifa_base, "
                + "estado_verificacion, experiencia_anos, presupuesto_maximo) "
                + "VALUES (?, ?, ?, 'ACTIVO', ?, ?, ?, ?, "
                + "'PENDIENTE', ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, idUsuario);
            statement.setString(2, prueba.empresa());
            statement.setString(3, prueba.telefono());
            statement.setString(4, prueba.ciudad());
            statement.setString(5, prueba.especialidad());
            statement.setString(6, prueba.portafolio());

            if (prueba.tarifaBase() == null) {
                statement.setNull(7, Types.NUMERIC);
            } else {
                statement.setBigDecimal(
                        7,
                        new BigDecimal(prueba.tarifaBase()));
            }

            if (prueba.experiencia() == null) {
                statement.setNull(8, Types.INTEGER);
            } else {
                statement.setInt(8, prueba.experiencia());
            }

            if (prueba.presupuestoMaximo() == null) {
                statement.setNull(9, Types.NUMERIC);
            } else {
                statement.setBigDecimal(
                        9,
                        new BigDecimal(prueba.presupuestoMaximo()));
            }

            statement.executeUpdate();

            try (ResultSet claves = statement.getGeneratedKeys()) {

                if (claves.next()) {
                    return claves.getInt(1);
                }
            }
        }

        throw new SQLException(
                "No se pudo crear el perfil de " + prueba.nombre());
    }

    private static boolean crearDocumentoSiFalta(
            Connection connection,
            int idOrganizador,
            String numeroDocumentoUsuario,
            DocumentoPrueba documento) throws SQLException {

        String consulta =
                "SELECT 1 FROM Documento_Organizador "
                + "WHERE id_organizador = ? AND tipo_documento = ?";

        try (PreparedStatement statement =
                    connection.prepareStatement(consulta)) {

            statement.setInt(1, idOrganizador);
            statement.setString(2, documento.tipo());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return false;
                }
            }
        }

        String insertar =
                "INSERT INTO Documento_Organizador "
                + "(id_organizador, tipo_documento, nombre_archivo, "
                + "numero_documento, fecha_vencimiento, estado, observacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                    connection.prepareStatement(insertar)) {

            statement.setInt(1, idOrganizador);
            statement.setString(2, documento.tipo());

            statement.setString(
                    3,
                    "DEMO_CU30_" + idOrganizador
                    + "_" + documento.tipo() + ".pdf");

            statement.setString(
                    4,
                    numeroDocumentoUsuario);

            if (documento.vencimiento() == null) {
                statement.setNull(5, Types.DATE);
            } else {
                statement.setDate(
                        5,
                        Date.valueOf(documento.vencimiento()));
            }

            statement.setString(6, documento.estado());

            if ("VALIDADO".equals(documento.estado())) {
                statement.setString(
                        7,
                        "Documento ficticio para pruebas del CU-030.");
            } else if ("VENCIDO".equals(documento.estado())) {
                statement.setString(
                        7,
                        "Documento ficticio con fecha de vencimiento pasada.");
            } else {
                statement.setString(
                        7,
                        "Documento ficticio pendiente de revisión.");
            }

            statement.executeUpdate();
            return true;
        }
    }

    private static int obtenerOCrearRol(
            Connection connection,
            String nombre,
            String descripcion) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id_rol FROM Rol WHERE nombre = ?")) {

            statement.setString(1, nombre);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("id_rol");
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Rol (nombre, descripcion, estado) "
                + "VALUES (?, ?, 'ACTIVO')",
                Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, nombre);
            statement.setString(2, descripcion);
            statement.executeUpdate();

            try (ResultSet claves = statement.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getInt(1);
                }
            }
        }

        throw new SQLException("No se pudo obtener el rol " + nombre);
    }

    private static void asociarRol(
            Connection connection,
            int idUsuario,
            int idRol) throws SQLException {

        String sql =
                "INSERT INTO Usuario_rol (id_usuario, id_rol) "
                + "SELECT ?, ? WHERE NOT EXISTS "
                + "(SELECT 1 FROM Usuario_rol "
                + "WHERE id_usuario = ? AND id_rol = ?)";

        try (PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setInt(1, idUsuario);
            statement.setInt(2, idRol);
            statement.setInt(3, idUsuario);
            statement.setInt(4, idRol);

            statement.executeUpdate();
        }
    }

    private static int buscarUsuario(
            Connection connection,
            String usuario) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id_usuario FROM Usuario WHERE usuario = ?")) {

            statement.setString(1, usuario);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt("id_usuario") : 0;
            }
        }
    }

    private static int buscarOrganizador(
            Connection connection,
            int idUsuario) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id_organizador FROM Organizador WHERE id_usuario = ?")) {

            statement.setInt(1, idUsuario);

            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? result.getInt("id_organizador")
                        : 0;
            }
        }
    }

    private static DocumentoPrueba documento(
            String tipo,
            String estado,
            int aniosHastaVencimiento) {

        LocalDate vencimiento = switch (estado) {
            case "VENCIDO" -> LocalDate.now().minusYears(1);
            default -> LocalDate.now().plusYears(aniosHastaVencimiento);
        };

        return new DocumentoPrueba(
                tipo,
                estado,
                vencimiento);
    }

    private record OrganizadorPrueba(
            String nombre,
            String usuario,
            String documentoIdentidad,
            String empresa,
            String telefono,
            String ciudad,
            String especialidad,
            String portafolio,
            String tarifaBase,
            Integer experiencia,
            String presupuestoMaximo,
            List<DocumentoPrueba> documentos) {
    }

    private record DocumentoPrueba(
            String tipo,
            String estado,
            LocalDate vencimiento) {
    }
    private static void asegurarAdministradorPruebas(
        Connection connection) throws SQLException {

    String actualizar =
            "UPDATE Usuario SET nombre = ?, contraseña = ?, "
            + "estado = 'ACTIVO', acepta_marketing = FALSE, "
            + "tipo_usuario = 'ADMINISTRADOR' WHERE usuario = ?";

    try (PreparedStatement statement =
                 connection.prepareStatement(actualizar)) {

        statement.setString(1, ADMIN_NOMBRE);
        statement.setString(2, ADMIN_CLAVE);
        statement.setString(3, ADMIN_USUARIO);

        if (statement.executeUpdate() > 0) {
            return;
        }
    }

    String insertar =
            "INSERT INTO Usuario "
            + "(nombre, usuario, contraseña, estado, fecha_registro, "
            + "acepta_marketing, tipo_usuario) "
            + "VALUES (?, ?, ?, 'ACTIVO', CURRENT_TIMESTAMP, "
            + "FALSE, 'ADMINISTRADOR')";

    try (PreparedStatement statement =
                 connection.prepareStatement(insertar)) {

        statement.setString(1, ADMIN_NOMBRE);
        statement.setString(2, ADMIN_USUARIO);
        statement.setString(3, ADMIN_CLAVE);
        statement.executeUpdate();
    }
}
}