package views;

import app.AccesoInvitado;
import app.Navegacion;
import controllers.ControllerInvitados;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import services.ServiceInvitados;

/**
 * Abre el acceso del invitado directamente en la pantalla del CU-16 (Gestionar respuesta a la invitación).
 * Usa el mismo menú y la misma navegación que la aplicación completa.
 */
public class LanzadorRespuestaInvitacion extends Application {
    @Override
    public void start(Stage stage) {
        AccesoInvitado acceso = new AccesoInvitado(
                new ControllerInvitados(new ServiceInvitados()), Platform::exit);
        Scene scene = acceso.crearEscena(Navegacion.Pantalla.MIS_INVITACIONES, 1440, 900);
        stage.setTitle("NextEventux | Mis invitaciones");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
