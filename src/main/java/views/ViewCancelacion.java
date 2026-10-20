package views;

import controllers.ControllerEventos;
import entities.Evento;
import entities.SesionUsuario;
import views.components.MenuCompartido;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ViewCancelacion {
    private final ControllerEventos controller;
    private final Runnable volver;
    private final Integer eventoInicial;
    private final SesionUsuario sesion;
    private final Runnable abrirEventos, abrirMisEventos, abrirCatalogo, abrirInvitados, cerrarSesion;
    private final Label organizador = new Label();
    private final ComboBox<Evento> evento = new ComboBox<>();
    private final CheckBox confirmar = new CheckBox("Confirmo que deseo cancelar el evento y revocar sus invitaciones.");
    private final Label mensaje = new Label();
    private final ListView<String> avisos = new ListView<>();

    public ViewCancelacion(ControllerEventos controller, Runnable volver) {
        this(controller,volver,null,null,null,null,null,null,null);
    }

    public ViewCancelacion(ControllerEventos controller, Runnable volver, Integer eventoInicial,
            SesionUsuario sesion, Runnable abrirEventos, Runnable abrirMisEventos,
            Runnable abrirCatalogo, Runnable abrirInvitados, Runnable cerrarSesion) {
        this.controller=controller; this.volver=volver; this.eventoInicial=eventoInicial;
        this.sesion=sesion; this.abrirEventos=abrirEventos; this.abrirMisEventos=abrirMisEventos;
        this.abrirCatalogo=abrirCatalogo; this.abrirInvitados=abrirInvitados; this.cerrarSesion=cerrarSesion;
    }

    public Parent crearPantalla() {
        organizador.setId("nombreOrganizadorCancelacion");
        evento.setId("eventoCancelacion"); evento.setPromptText("Seleccionar evento");
        organizador.setMaxWidth(Double.MAX_VALUE); evento.setMaxWidth(Double.MAX_VALUE);
        evento.setCellFactory(lista->celdaEvento()); evento.setButtonCell(celdaEvento());
        confirmar.setId("confirmarCancelacion"); confirmar.setWrapText(true);
        mensaje.setId("mensajeCancelacion"); mensaje.setWrapText(true); mensaje.setVisible(false); mensaje.setManaged(false);
        avisos.setId("avisosCancelacion"); avisos.setPrefHeight(150); avisos.setPlaceholder(new Label("No hay avisos de cancelación."));
        avisos.setCellFactory(lista->new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item,empty); setText(null);
                if(empty || item==null) { setGraphic(null); return; }
                Label texto=new Label(item); texto.setWrapText(true); texto.maxWidthProperty().bind(avisos.widthProperty().subtract(40));
                setGraphic(texto);
            }
        });
        evento.setOnAction(e-> { confirmar.setSelected(false); cargarAvisos(); });
        Button cancelar=new Button("Cancelar evento"); cancelar.setId("cancelarEvento"); cancelar.getStyleClass().add("btn-primary");
        cancelar.disableProperty().bind(evento.valueProperty().isNull().or(confirmar.selectedProperty().not()));
        cancelar.setOnAction(e->cancelar());
        Button regresar=new Button("← Crear evento"); regresar.setOnAction(e->volver.run()); regresar.getStyleClass().add("btn-secondary");
        Label titulo=new Label("Cancelar evento"); titulo.getStyleClass().add("page-title");
        Label ayuda=new Label("Se liberarán las reservas del evento y se revocarán las invitaciones enviadas. No se puede cancelar un evento finalizado.");
        ayuda.getStyleClass().add("muted"); ayuda.setWrapText(true);
        VBox tarjeta=new VBox(16,new Label("Organizador de la sesión"),organizador,new Label("Evento"),evento,confirmar,cancelar,
                mensaje,new Separator(),new Label("Avisos internos registrados para los afectados"),avisos);
        tarjeta.getStyleClass().add("card");
        VBox contenido=new VBox(22,regresar,titulo,ayuda,tarjeta);
        contenido.getStyleClass().add("content"); contenido.setMaxWidth(950);
        StackPane centro=new StackPane(contenido); centro.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll=new ScrollPane(centro); scroll.setFitToWidth(true); scroll.getStyleClass().add("content-scroll");
        BorderPane raiz=new BorderPane(scroll); raiz.getStyleClass().add("app");
        if(sesion!=null) {
            contenido.getChildren().remove(regresar);
            BorderPane principal=new BorderPane(scroll);
            principal.setTop(MenuCompartido.crearBarraSuperior(sesion,"Cancelar evento","Mis eventos",volver));
            raiz.setCenter(principal);
            raiz.setLeft(MenuCompartido.crear(sesion,MenuCompartido.Pagina.MIS_EVENTOS,
                    abrirEventos,abrirInvitados,abrirCatalogo,null,cerrarSesion,abrirMisEventos));
        }
        try {
            organizador.setText(controller.nombreOrganizadorCancelacion()); cargarEventos();
            if(eventoInicial!=null) {
                Evento seleccionado=evento.getItems().stream().filter(e->eventoInicial.equals(e.getIdEvento())).findFirst()
                        .orElseThrow(()->new IllegalStateException("El evento seleccionado ya no está disponible para esta cuenta."));
                evento.setValue(seleccionado); evento.setDisable(true);
                cargarAvisos();
            }
        }
        catch(IllegalStateException e) { mostrar(e.getMessage(),false); }
        return raiz;
    }

    private void cancelar() {
        if (evento.getValue() == null) return;
        try {
            controller.cancelarEvento(evento.getValue().getIdEvento(), confirmar.isSelected());
        } catch (IllegalArgumentException | IllegalStateException e) { mostrar(e.getMessage(),false); return; }
        confirmar.setSelected(false);
        try {
            cargarEventos();
            mostrar("El evento fue cancelado. Se liberaron sus compromisos y se registraron los avisos internos.",true);
        } catch (IllegalStateException e) {
            mostrar("El evento fue cancelado, pero no se pudo actualizar la pantalla. Vuelve a abrirla para consultar los avisos.",false);
        }
    }

    private void cargarEventos() {
        Evento anterior=evento.getValue(); confirmar.setSelected(false); avisos.getItems().clear();
        evento.getItems().clear();
        evento.getItems().setAll(controller.listarEventosParaCancelar());
        if(anterior!=null) evento.getItems().stream().filter(e->e.getIdEvento().equals(anterior.getIdEvento())).findFirst()
                .ifPresent(e->evento.setValue(e));
    }

    private void cargarAvisos() {
        avisos.getItems().clear();
        if(evento.getValue()==null) return;
        try { avisos.getItems().setAll(controller.listarAvisosCancelacion(evento.getValue().getIdEvento())); }
        catch(IllegalStateException e) { mostrar(e.getMessage(),false); }
    }

    private void mostrar(String texto, boolean exito) {
        mensaje.setText(texto); mensaje.getStyleClass().setAll("message",exito?"success":"error");
        mensaje.setVisible(true); mensaje.setManaged(true);
    }

    private ListCell<Evento> celdaEvento() {
        return new ListCell<>() {
            @Override protected void updateItem(Evento item, boolean empty) {
                super.updateItem(item,empty);
                setText(item==null ? "Seleccionar evento" : item.getNombre()+" — "+item.getEstado());
            }
        };
    }
}
