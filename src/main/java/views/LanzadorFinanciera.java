package views;

import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import controllers.ControllerFinanciera;
import entities.SesionUsuario;
import java.sql.Connection;
import java.sql.Statement;
import javafx.application.Application;
import javafx.stage.Stage;
import repositories.RepositoryEvento;
import repositories.RepositoryMovimientoFinanciero;
import repositories.RepositoryPlanificacion;
import services.ServiceFinanciera;

/**
 * Abre directamente la pantalla de Gestión Financiera (CU06) para pruebas.
 *
 * La base real (data/nexteventux) solo crea las tablas: seed.sql no se carga solo,
 * así que ahí el evento 1 no tiene movimientos ni planificación y la pantalla sale vacía.
 * Este lanzador usa una base en memoria con los datos de ejemplo, sin tocar la base real.
 */
public class LanzadorFinanciera extends Application {

    private static final String URL_DEMO = "jdbc:h2:mem:cu06_demo;DB_CLOSE_DELAY=-1";

    @Override
    public void init() throws Exception {
        System.setProperty("nexteventux.db.url", URL_DEMO);
        try (Connection connection = DatabaseConnection.getConnection()) {
            CargarDatosEjemplo.cargar(connection);
            // En seed.sql las solicitudes no indican el servicio; se enlazan aquí
            // para que el "Presupuesto por categoría" tenga datos en la demostración.
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE Solicitud_Ejecucion SET id_servicio = 1 WHERE id_solicitud = 1");
                statement.executeUpdate("UPDATE Solicitud_Ejecucion SET id_servicio = 3 WHERE id_solicitud = 2");
            }
        }
    }

    @Override
    public void start(Stage stage) {
        RepositoryMovimientoFinanciero repoMov = new RepositoryMovimientoFinanciero();
        RepositoryPlanificacion repoPlan = new RepositoryPlanificacion();

        // Sesión de laura.gomez (usuario 1, organizador 1), dueña del evento 1 en seed.sql.
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(1);
        sesion.setIdActor(1);
        sesion.setNombre("Laura Gómez");
        sesion.setTipoUsuario("ORGANIZADOR");
        sesion.setEstadoUsuario("ACTIVO");
        sesion.setEstadoActor("ACTIVO");

        ServiceFinanciera service = new ServiceFinanciera(repoMov, repoPlan, new RepositoryEvento());
        ControllerFinanciera controller = new ControllerFinanciera(service, sesion);

        ViewFinanciera vista = new ViewFinanciera(controller);

        // Evento 1 = "Boda Laura y Daniel" en seed.sql (pasa 0 para probar el aviso).
        vista.mostrarEstadoFinanciero(1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}