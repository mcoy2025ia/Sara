import java.math.BigDecimal;
import java.time.LocalDateTime;
import controllers.ControllerEventos;
import entities.Evento;
import repositories.RepositoryEvento;
import services.ServiceEventos;

public class PruebaCU01 {

    public static void main(String[] args) throws Exception {

        String url = "jdbc:h2:" + java.nio.file.Files.createTempDirectory("cu01-ejemplo-").resolve("eventos");
        try (var c=config.DatabaseConnection.getConnection(url)) { config.CargarDatosEjemplo.cargar(c); }
        RepositoryEvento repositoryEvento = new RepositoryEvento(url);

        ServiceEventos serviceEventos =
                new ServiceEventos(repositoryEvento, new repositories.RepositoryCancelacion(url),
                        new repositories.RepositoryFinalizacion(url, java.time.Clock.systemDefaultZone()),
                        new repositories.RepositorySolicitudServicio(url));

        ControllerEventos controllerEventos =
                new ControllerEventos(serviceEventos, new services.ServiceAutenticacion(
                        new repositories.RepositoryAutenticacion(url)).iniciarSesion("laura.gomez","clave123"));

        Evento evento = new Evento(
                1, // Organizador de ejemplo; JDBC deberá comprobar que exista.
                "Festival NextEventux",
                "Corporativo",
                "Festival corporativo",
                LocalDateTime.of(2026, 10, 20, 9, 0),
                LocalDateTime.of(2026, 10, 20, 18, 0),
                "Bogotá",
                new BigDecimal("15000000.00"),
                100
        );

        controllerEventos.crearEvento(evento);
        if (repositoryEvento.listarEventos().size() != 3) {
            throw new AssertionError("No se guardó el evento.");
        }

        System.out.println(evento);
        System.out.println(
                "Eventos guardados: "
                + repositoryEvento.listarEventos().size()
        );
    }
}