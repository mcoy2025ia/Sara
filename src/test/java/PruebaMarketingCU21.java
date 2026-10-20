import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import controllers.ControllerMarketing;
import entities.Segmentacion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import repositories.RepositoryMarketing;
import services.ServiceMarketing;

//Verificar de manera manual el flujo del CU21
public class PruebaMarketingCU21 {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 18);

    public static void main(String[] args) throws Exception {
        Path directorio = Files.createTempDirectory("marketing-cu21-");
        String url = "jdbc:h2:" + directorio.resolve("prueba");
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
        }

        ServiceMarketing servicio = new ServiceMarketing(new RepositoryMarketing(url));
        int idAdmin = contar(url, "SELECT id_usuario FROM Usuario WHERE usuario = 'admin.nexteventux'");

        // 1. Primera segmentación: el seed ya trae 3 segmentos de ejemplo activos.
        List<Segmentacion> primera = servicio.segmentarClientes(idAdmin, HOY);
        for (Segmentacion segmento : primera) {
            System.out.println(segmento.getIdSegmento() + " | " + segmento.getNombre()
                    + " | " + segmento.getCantidadClientes() + " clientes");
            comprobar(segmento.getIdSegmento() != null, "Cada segmento guardado debe tener identificador.");
            comprobar(segmento.getCriterios().startsWith(Segmentacion.PREFIJO_CRITERIOS_AUTOMATICOS),
                    "Los criterios deben llevar el prefijo de segmento automático.");
        }
        comprobar(primera.size() == 3, "Deben guardarse 3 segmentos.");
        comprobar(primera.get(0).getNombre().equals("Bogotá 18-25 - Social"), "Primer segmento incorrecto.");
        comprobar(primera.get(1).getNombre().equals("Bogotá 26-35 - Corporativo"), "Segundo segmento incorrecto.");
        comprobar(primera.get(2).getNombre().equals("Medellín 26-35 - Cultural"), "Tercer segmento incorrecto.");
        comprobar(contar(url, "SELECT COUNT(*) FROM Segmentacion WHERE estados = 'ACTIVA'") == 6,
                "Deben quedar 6 segmentos activos (3 de ejemplo + 3 nuevos).");
        comprobar(contar(url, "SELECT COUNT(*) FROM Segmento_cliente") == 17,
                "Deben guardarse 17 relaciones segmento-cliente (6 + 6 + 5).");
        // Los segmentos activos se pueden volver a leer de la base de datos.
        List<Segmentacion> activos = servicio.listarSegmentosActivos();
        int clientesEnActivos = 0;
        for (Segmentacion activo : activos) {
            clientesEnActivos += activo.getCantidadClientes();
        }
        comprobar(activos.size() == 6, "Deben listarse 6 segmentos activos.");
        comprobar(clientesEnActivos == 17, "Los segmentos activos deben sumar 17 clientes (los de ejemplo no tienen).");
        // 2. Regenerar: los segmentos automáticos anteriores pasan a inactivos; los de ejemplo no se tocan.
        servicio.segmentarClientes(idAdmin, HOY);
        comprobar(contar(url, "SELECT COUNT(*) FROM Segmentacion WHERE estados = 'ACTIVA'") == 6,
                "Tras regenerar deben seguir 6 activos.");
        comprobar(contar(url, "SELECT COUNT(*) FROM Segmentacion WHERE estados = 'INACTIVA'") == 3,
                "Los 3 segmentos automáticos anteriores deben quedar inactivos.");
        comprobar(contar(url, "SELECT COUNT(*) FROM Segmentacion WHERE criterios NOT LIKE 'AUTO|%' AND estados = 'ACTIVA'") == 3,
                "Los 3 segmentos de ejemplo deben seguir activos.");

        // 3. Caso E1: sin datos suficientes no se guarda nada y no se desactiva nada.
        List<Segmentacion> vacia = servicio.segmentarClientes(idAdmin, LocalDate.of(2010, 1, 1));
        comprobar(vacia.isEmpty(), "Sin datos suficientes debe devolver una lista vacía.");
        comprobar(contar(url, "SELECT COUNT(*) FROM Segmentacion WHERE estados = 'ACTIVA'") == 6,
                "Con datos insuficientes los segmentos activos no deben cambiar.");
        comprobar(contar(url, "SELECT COUNT(*) FROM Segmentacion") == 9,
                "Con datos insuficientes no debe crearse ningún segmento.");

        // 4. Solo un administrador activo puede segmentar.
        comprobarRechazo(servicio, 1);    // es un organizador
        comprobarRechazo(servicio, 9999); // no existe

        // 5. El controlador entrega el resultado del servicio.
        ControllerMarketing controlador = new ControllerMarketing(servicio);
        comprobar(controlador.segmentarClientes(idAdmin) != null, "El controlador debe devolver una lista.");

        System.out.println("CU21 verificado correctamente.");
    }

    private static void comprobarRechazo(ServiceMarketing servicio, int idUsuario) {
        try {
            servicio.segmentarClientes(idUsuario, HOY);
            throw new AssertionError("Debió rechazar al usuario " + idUsuario + ".");
        } catch (IllegalArgumentException esperado) {
            // Es el comportamiento correcto.
        }
    }

    // Ejecuta una consulta que devuelve un solo número.
    private static int contar(String url, String sql) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}