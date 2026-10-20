import config.CargarDemoPublicacion;
import config.DatabaseConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryEvento;
import repositories.RepositoryPublicacion;
import services.ServicePublicacion;
import repositories.RepositoryAutenticacion;
import services.ServiceAutenticacion;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class PublicacionDemoTest {
    @TempDir Path directorio;
    @Test void preparaElFlujoCompletoSoloEnBaseVacia() throws Exception {
        String url="jdbc:h2:"+directorio.resolve("demo");
        try(Connection connection=DatabaseConnection.getConnection(url)) {
            CargarDemoPublicacion.cargar(connection);
            assertThrows(SQLException.class,()->CargarDemoPublicacion.cargar(connection));
        }
        var service=new ServicePublicacion(new RepositoryPublicacion(url),new RepositoryEvento(url));
        var login=new ServiceAutenticacion(new RepositoryAutenticacion(url));
        var organizador=login.iniciarSesion("laura.gomez","clave123");
        var administrador=login.iniciarSesion("admin.demo","solo-demostracion");
        var finalizado=service.listarEventos(organizador).stream().filter(e->"FINALIZADO".equals(e.getEstado())).findFirst().orElseThrow();
        service.solicitar(organizador,finalizado.getIdEvento());
        var solicitud=service.listarSolicitudes(organizador).get(0);
        service.revisar(administrador,solicitud.id(),true,null);
        assertEquals(1,service.listarPlantillas(organizador).size());
        assertEquals(1,service.notificaciones(organizador).size());
    }
}
