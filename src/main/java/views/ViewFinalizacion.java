package views;

import controllers.ControllerEventos;
import entities.Evento;
import entities.Fotografia;
import entities.SesionUsuario;
import views.components.MenuCompartido;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

public class ViewFinalizacion {
    private final ControllerEventos controller;
    private final Runnable volver;
    private final IntConsumer calificar;
    private final Integer eventoInicial;
    private final SesionUsuario sesion;
    private final Runnable abrirEventos, abrirMisEventos, abrirCatalogo, abrirInvitados, cerrarSesion;
    private final ComboBox<Evento> evento=new ComboBox<>();
    private final List<Fotografia> fotos=new ArrayList<>();
    private final FlowPane miniaturas=new FlowPane(12,12);
    private final Label mensaje=new Label();
    private final Label detalle=new Label();
    private final ListView<String> proveedores=new ListView<>();
    private final Button finalizar=new Button("Finalizar evento / reintentar envío");
    private final Button abrirCalificacion=new Button("Calificar proveedores");
    private final javafx.beans.property.BooleanProperty admiteFotos=new javafx.beans.property.SimpleBooleanProperty(false);
    private boolean pendiente;

    public ViewFinalizacion(ControllerEventos controller, Runnable volver) { this(controller,volver,null); }
    public ViewFinalizacion(ControllerEventos controller, Runnable volver, IntConsumer calificar) {
        this(controller,volver,calificar,null,null,null,null,null,null,null);
    }

    public ViewFinalizacion(ControllerEventos controller, Runnable volver, IntConsumer calificar,
            Integer eventoInicial, SesionUsuario sesion, Runnable abrirEventos, Runnable abrirMisEventos,
            Runnable abrirCatalogo, Runnable abrirInvitados, Runnable cerrarSesion) {
        this.controller=controller; this.volver=volver; this.calificar=calificar;
        this.eventoInicial=eventoInicial; this.sesion=sesion; this.abrirEventos=abrirEventos;
        this.abrirMisEventos=abrirMisEventos; this.abrirCatalogo=abrirCatalogo;
        this.abrirInvitados=abrirInvitados; this.cerrarSesion=cerrarSesion;
    }

    public Parent crearPantalla() {
        Label organizador=new Label(); organizador.setId("nombreOrganizadorFinalizacion");
        evento.setId("eventoFinalizacion"); evento.setPromptText("Seleccionar evento"); evento.setMaxWidth(Double.MAX_VALUE);
        evento.setCellFactory(lista->celda()); evento.setButtonCell(celda());
        evento.setOnAction(e-> { fotos.clear(); miniaturas.getChildren().clear(); actualizarDetalle(); });
        Button cargar=new Button("Seleccionar fotografías"); cargar.setId("cargarGaleria");
        cargar.setOnAction(e->cargar()); cargar.getStyleClass().add("btn-secondary");
        Button limpiar=new Button("Quitar selección"); limpiar.setOnAction(e-> { fotos.clear(); miniaturas.getChildren().clear(); });
        limpiar.getStyleClass().add("btn-secondary");
        cargar.disableProperty().bind(admiteFotos.not()); limpiar.disableProperty().bind(admiteFotos.not());
        finalizar.setId("finalizarEvento"); finalizar.getStyleClass().add("btn-primary");
        finalizar.setOnAction(e->finalizar()); finalizar.setDisable(true);
        mensaje.setId("mensajeFinalizacion"); mensaje.setWrapText(true); mensaje.setVisible(false); mensaje.setManaged(false);
        detalle.setId("detalleFinalizacion"); detalle.setWrapText(true);
        miniaturas.setId("fotosFinalizacion");
        proveedores.setId("proveedoresCalificables"); proveedores.setPrefHeight(110);
        proveedores.setPlaceholder(new Label("Se habilitan después de finalizar el evento."));
        abrirCalificacion.setId("calificarProveedores"); abrirCalificacion.setDisable(true);
        abrirCalificacion.setOnAction(e-> { if(calificar!=null && evento.getValue()!=null) calificar.accept(evento.getValue().getIdEvento()); });
        Label pendiente=new Label(calificar==null ? "El formulario de reseñas depende de CU14; este caso deja habilitados los proveedores del evento finalizado." : "");
        pendiente.setWrapText(true); pendiente.getStyleClass().add("muted");
        Button regresar=new Button("← Volver"); regresar.setOnAction(e->volver.run());
        regresar.getStyleClass().add("btn-secondary");
        Label titulo=new Label("Finalizar evento"); titulo.getStyleClass().add("page-title");
        Label ayuda=new Label("Selecciona un evento confirmado que ya terminó. Comparte hasta 10 fotografías PNG o JPG (5 MB y 20 megapíxeles por foto). Las recibirán los invitados que confirmaron asistencia.");
        ayuda.setWrapText(true); ayuda.getStyleClass().add("muted");
        Label reintento=new Label("Si falla el envío, las reservas quedan liberadas y las fotografías guardadas. Selecciona el mismo evento y pulsa reintentar sin volver a cargar archivos.");
        reintento.setWrapText(true);
        VBox tarjeta=new VBox(16,new Label("Organizador de la sesión"),organizador,new Label("Evento"),evento,detalle,
                cargar,limpiar,miniaturas,finalizar,mensaje,reintento,new Separator(),new Label("Proveedores habilitados para calificar"),proveedores,abrirCalificacion,pendiente);
        tarjeta.getStyleClass().add("card");
        VBox contenido=new VBox(22,regresar,titulo,ayuda,tarjeta); contenido.getStyleClass().add("content"); contenido.setMaxWidth(950);
        StackPane centro=new StackPane(contenido); centro.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll=new ScrollPane(centro); scroll.setFitToWidth(true); scroll.getStyleClass().add("content-scroll");
        BorderPane raiz=new BorderPane(scroll); raiz.getStyleClass().add("app");
        if(sesion!=null) {
            contenido.getChildren().remove(regresar);
            BorderPane principal=new BorderPane(scroll);
            principal.setTop(MenuCompartido.crearBarraSuperior(sesion,"Finalizar evento","Mis eventos",volver));
            raiz.setCenter(principal);
            raiz.setLeft(MenuCompartido.crear(sesion,MenuCompartido.Pagina.MIS_EVENTOS,
                    abrirEventos,abrirInvitados,abrirCatalogo,null,cerrarSesion,abrirMisEventos));
        }
        raiz.getStylesheets().add(getClass().getResource("/styles/eventos.css").toExternalForm());
        try {
            organizador.setText(controller.nombreOrganizadorFinalizacion()); evento.getItems().setAll(controller.listarEventosParaFinalizar());
            if(eventoInicial!=null) {
                Evento seleccionado=evento.getItems().stream().filter(e->eventoInicial.equals(e.getIdEvento())).findFirst()
                        .orElseThrow(()->new IllegalStateException("El evento seleccionado ya no está disponible para esta cuenta."));
                evento.setValue(seleccionado); evento.setDisable(true);
                actualizarDetalle();
            }
        }
        catch(IllegalStateException e) { mostrar(e.getMessage(),false); }
        return raiz;
    }

    private void cargar() {
        FileChooser selector=new FileChooser(); selector.setTitle("Fotografías del evento");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fotografías PNG y JPG","*.png","*.jpg","*.jpeg"));
        List<File> archivos=selector.showOpenMultipleDialog(evento.getScene().getWindow());
        if(archivos==null) return;
        try {
            if(archivos.size()>10) throw new IllegalArgumentException("Selecciona hasta 10 fotografías.");
            List<Fotografia> nuevas=new ArrayList<>();
            for(File archivo:archivos) {
                if(Files.size(archivo.toPath())>5*1024*1024) throw new IllegalArgumentException("Cada fotografía debe pesar hasta 5 MB.");
                nuevas.add(new Fotografia(archivo.getName(),Files.readAllBytes(archivo.toPath())));
            }
            // Validar también en el servicio antes de leer la imagen para la vista previa.
            services.ServiceEventos.validarFotografias(nuevas);
            fotos.clear(); fotos.addAll(nuevas); miniaturas.getChildren().clear();
            for(Fotografia foto:fotos) {
                ImageView imagen=new ImageView(new Image(new java.io.ByteArrayInputStream(foto.contenido()),130,90,true,true));
                Label nombre=new Label(foto.nombre()); nombre.setMaxWidth(140); nombre.setWrapText(true);
                miniaturas.getChildren().add(new VBox(6,imagen,nombre));
            }
            mostrar("Fotografías seleccionadas: "+fotos.size(),true);
        } catch(java.io.IOException | IllegalArgumentException e) { mostrar(e.getMessage(),false); }
    }

    private void finalizar() {
        Evento seleccionado=evento.getValue(); if(seleccionado==null) return;
        try { controller.finalizarEvento(seleccionado.getIdEvento(),pendiente?List.of():List.copyOf(fotos)); }
        catch(IllegalArgumentException | IllegalStateException e) { actualizarDetalle(); mostrar(e.getMessage(),false); return; }
        seleccionado.setEstado("FINALIZADO"); evento.setButtonCell(celda());
        fotos.clear(); miniaturas.getChildren().clear();
        actualizarDetalle();
        mostrar("Evento finalizado. Se liberaron sus reservas y se compartió la galería con los invitados confirmados.",true);
    }

    private void actualizarDetalle() {
        proveedores.getItems().clear(); abrirCalificacion.setDisable(true);
        admiteFotos.set(false); pendiente=false;
        Evento actual=evento.getValue(); finalizar.setDisable(actual==null || !"CONFIRMADO".equalsIgnoreCase(actual.getEstado()));
        detalle.setText(actual==null ? "" : "Estado: "+actual.getEstado()+" · Finaliza: "+actual.getFechaFin().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        if(actual==null) return;
        try {
            pendiente=controller.galeriaPendiente(actual.getIdEvento());
            admiteFotos.set("CONFIRMADO".equalsIgnoreCase(actual.getEstado()) && !pendiente);
            if(pendiente) detalle.setText(detalle.getText()+" · Galería guardada; reintenta el envío sin cargar más archivos.");
            proveedores.getItems().setAll(controller.proveedoresParaCalificar(actual.getIdEvento()));
            abrirCalificacion.setDisable(calificar==null || proveedores.getItems().isEmpty());
        } catch(IllegalStateException e) { mostrar(e.getMessage(),false); }
    }
    private void mostrar(String texto, boolean exito) {
        mensaje.setText(texto); mensaje.getStyleClass().setAll("message",exito?"success":"error"); mensaje.setVisible(true); mensaje.setManaged(true);
    }
    private ListCell<Evento> celda() {
        return new ListCell<>() {
            @Override protected void updateItem(Evento item, boolean empty) {
                super.updateItem(item,empty); setText(empty || item==null ? null : item.getNombre()+" — "+item.getEstado());
            }
        };
    }
}
