package views;

import entities.SesionUsuario;
import services.ServiceAvisosProveedor;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import views.components.MenuCompartido;

public class ViewAvisosProveedor {
    private final ServiceAvisosProveedor service;
    private final SesionUsuario sesion;
    private final ListView<String> avisos = new ListView<>();
    private final Label mensaje = new Label();
    private final Runnable volver, cerrarSesion;

    public ViewAvisosProveedor(ServiceAvisosProveedor service, SesionUsuario sesion) {
        this(service,sesion,null,null);
    }
    public ViewAvisosProveedor(ServiceAvisosProveedor service, SesionUsuario sesion, Runnable volver, Runnable cerrarSesion) {
        this.service = service; this.sesion = sesion;
        this.volver=volver; this.cerrarSesion=cerrarSesion;
    }

    public Parent crearPantalla() {
        avisos.setId("avisosProveedor");
        avisos.setPlaceholder(new Label("No tienes avisos."));
        avisos.setCellFactory(lista -> new ListCell<>() {
            @Override protected void updateItem(String texto, boolean empty) {
                super.updateItem(texto, empty); setText(null);
                if (empty || texto == null) { setGraphic(null); return; }
                Label contenido = new Label(texto); contenido.setWrapText(true);
                contenido.maxWidthProperty().bind(avisos.widthProperty().subtract(40)); setGraphic(contenido);
            }
        });
        Button actualizar = new Button("Actualizar avisos"); actualizar.setId("actualizarAvisosProveedor");
        actualizar.setOnAction(e -> actualizar());
        Label titulo = new Label("Mis avisos"); titulo.getStyleClass().add("page-title");
        mensaje.setWrapText(true);
        VBox contenido = new VBox(18, titulo, actualizar, mensaje, avisos);
        contenido.getStyleClass().addAll("app", "content"); VBox.setVgrow(avisos, Priority.ALWAYS);
        BorderPane principal=new BorderPane(contenido);
        principal.setTop(MenuCompartido.crearBarraSuperior(sesion,"Mis avisos","Mis servicios",volver));
        BorderPane raiz=new BorderPane(principal);
        raiz.setLeft(MenuCompartido.crear(sesion,MenuCompartido.Pagina.MIS_SERVICIOS,null,null,null,volver,cerrarSesion));
        raiz.getStyleClass().add("app");
        actualizar(); return raiz;
    }

    private void actualizar() {
        avisos.getItems().clear(); mensaje.setText("");
        try { avisos.getItems().setAll(service.listar(sesion)); }
        catch (IllegalStateException e) { mensaje.setText(e.getMessage()); }
    }
}
