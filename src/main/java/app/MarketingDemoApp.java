package app;

import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import controllers.ControllerMarketing;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import repositories.RepositoryMarketing;
import services.ServiceMarketing;
import views.ViewMarketing;

// Demostración de la pantalla de segmentos con una base propia y datos de ejemplo. 
public class MarketingDemoApp extends Application {
    private static final String URL_BASE_DEMO = "jdbc:h2:./data/marketing-demo";
    // El seed crea al administrador con este identificador.
    private static final int ID_ADMINISTRADOR_DEMO = 7;
    private static final double ANCHO_VENTANA = 1440;
    private static final double ALTO_VENTANA = 900;
    // La ventana ocupa como máximo esta fracción de la pantalla disponible.
    private static final double PROPORCION_PANTALLA = 0.9;

    @Override
    public void start(Stage stage) throws Exception {
        prepararBaseDemo();
        ControllerMarketing controller = new ControllerMarketing(
                new ServiceMarketing(new RepositoryMarketing(URL_BASE_DEMO)));
        
        Rectangle2D areaVisible = Screen.getPrimary().getVisualBounds();
        double ancho = Math.min(ANCHO_VENTANA, areaVisible.getWidth() * PROPORCION_PANTALLA);
        double alto = Math.min(ALTO_VENTANA, areaVisible.getHeight() * PROPORCION_PANTALLA);

        Scene scene = new Scene(new ViewMarketing(controller, ID_ADMINISTRADOR_DEMO).crearPantalla(), ancho, alto);

        scene.getStylesheets().add(getClass().getResource("/styles/eventos.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/marketing.css").toExternalForm());
        stage.setTitle("NextEventux | Segmentos de clientes");
        stage.setMinWidth(800);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    // Carga los datos de ejemplo solo si la base de demostración está vacía.
    private void prepararBaseDemo() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(URL_BASE_DEMO)) {
            if (contarUsuarios(connection) == 0) {
                CargarDatosEjemplo.cargar(connection);
            }
        }
    }

    private int contarUsuarios(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM Usuario")) {
            result.next();
            return result.getInt(1);
        }
    }
}