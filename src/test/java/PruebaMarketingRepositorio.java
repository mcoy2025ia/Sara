import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import entities.ClienteMarketing;
import entities.SolicitudEvento;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import repositories.RepositoryMarketing;

/** Verificación manual de RepositoryMarketing sobre una base temporal con el seed. */
public class PruebaMarketingRepositorio {

    public static void main(String[] args) throws Exception {
        Path directorio = Files.createTempDirectory("marketing-repositorio-");
        String url = "jdbc:h2:" + directorio.resolve("prueba");

        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
        }

        RepositoryMarketing repositorio = new RepositoryMarketing(url);
        List<ClienteMarketing> clientes = repositorio.listarClientesActivos();
        List<SolicitudEvento> solicitudes = repositorio.contarSolicitudesPorTipo();

        int sinCiudad = 0;
        int sinMarketing = 0;
        int idAna = -1;
        for (ClienteMarketing cliente : clientes) {
            if (cliente.getCiudad() == null) sinCiudad++;
            if (!cliente.isAceptaMarketing()) sinMarketing++;
            if (cliente.getNombre().equals("Ana Martínez")) idAna = cliente.getIdUsuario();
        }

        int totalCantidad = 0;
        int solicitudesAna = 0;
        for (SolicitudEvento solicitud : solicitudes) {
            totalCantidad += solicitud.getCantidad();
            if (solicitud.getIdCliente() == idAna) solicitudesAna++;
        }

        System.out.println("Clientes activos: " + clientes.size());
        System.out.println("Clientes sin ciudad: " + sinCiudad);
        System.out.println("Clientes que no aceptan marketing: " + sinMarketing);
        System.out.println("Filas de solicitudes (cliente + tipo): " + solicitudes.size());
        System.out.println("Total de solicitudes: " + totalCantidad);
        System.out.println("Tipos distintos de Ana: " + solicitudesAna);

        comprobar(clientes.size() == 22, "Debe haber 22 clientes activos.");
        comprobar(sinCiudad == 2, "Debe haber 2 clientes sin ciudad.");
        comprobar(sinMarketing == 2, "Debe haber 2 clientes que no aceptan marketing.");
        comprobar(solicitudes.size() == 41, "Debe haber 41 combinaciones cliente + tipo.");
        comprobar(totalCantidad == 58, "Debe haber 58 solicitudes en total (56 búsquedas + 2 eventos).");
        comprobar(solicitudesAna == 3, "Ana debe tener 3 tipos: Corporativo, Social y Boda.");
        System.out.println("RepositoryMarketing verificado correctamente.");
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}