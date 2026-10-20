package views;

import controllers.ControllerEventos;
import entities.Evento;
import entities.SesionUsuario;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import views.components.MenuCompartido;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

public class ViewMisEventos {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ControllerEventos controller;
    private final SesionUsuario sesion;
    private final Runnable abrirEventos;
    private final Runnable abrirMisEventos;
    private final Runnable abrirCatalogo;
    private final Runnable abrirInvitados;
    private final Runnable cerrarSesion;
    private final Consumer<Evento> editar;
    private final Consumer<Evento> publicar;
    private final Consumer<Evento> cancelar;
    private final Consumer<Evento> finalizar;
    // CU06: acción opcional "Ver finanzas"; sin ella el botón no se muestra.
    private Consumer<Evento> verFinanzas;
    private final ListView<Evento> eventos = new ListView<>();
    private final Label detalle = new Label("Selecciona un evento para ver las acciones disponibles.");
    private final Label mensaje = new Label();
    private final Button botonEditar = boton("Editar", "editarEvento");
    private final Button botonPublicar = boton("Subir al repositorio", "publicarEvento");
    private final Button botonCancelar = boton("Cancelar", "cancelarEvento");
    private final Button botonFinalizar = boton("Finalizar evento", "abrirFinalizacion");

    private Consumer<Evento> abrirSolicitudes;
    private final Button botonServicios = boton("Servicios y presupuesto", "serviciosPresupuestoEvento");

    public ViewMisEventos conSolicitudes(Consumer<Evento> accion) {
        abrirSolicitudes = accion;
        return this;
    }

    private final Button botonFinanzas = boton("Ver finanzas", "verFinanzasEvento");

    /** CU06: agrega la acción "Ver finanzas" para el evento seleccionado. */
    public ViewMisEventos conFinanzas(Consumer<Evento> accion) {
        verFinanzas = accion;
        return this;
    }

    public ViewMisEventos(
            ControllerEventos controller,
            SesionUsuario sesion,
            Runnable abrirEventos,
            Runnable abrirMisEventos,
            Runnable abrirCatalogo,
            Runnable abrirInvitados,
            Runnable cerrarSesion,
            Consumer<Evento> editar,
            Consumer<Evento> publicar,
            Consumer<Evento> cancelar,
            Consumer<Evento> finalizar) {

        this.controller = controller;
        this.sesion = sesion;
        this.abrirEventos = abrirEventos;
        this.abrirMisEventos = abrirMisEventos;
        this.abrirCatalogo = abrirCatalogo;
        this.abrirInvitados = abrirInvitados;
        this.cerrarSesion = cerrarSesion;
        this.editar = editar;
        this.publicar = publicar;
        this.cancelar = cancelar;
        this.finalizar = finalizar;
    }

    public Parent crearPantalla() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");
        raiz.setLeft(MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.MIS_EVENTOS,
                abrirEventos,
                abrirInvitados,
                abrirCatalogo,
                null,
                cerrarSesion,
                abrirMisEventos));

        BorderPane principal = new BorderPane();
        principal.setTop(MenuCompartido.crearBarraSuperior(sesion, "Mis eventos"));

        Label titulo = new Label("Mis eventos");
        titulo.getStyleClass().add("page-title");
        Label ayuda = new Label("Selecciona uno de tus eventos para consultar las acciones disponibles según su estado.");
        ayuda.getStyleClass().add("muted");
        ayuda.setWrapText(true);

        eventos.setId("listaMisEventos");
        eventos.getStyleClass().add("event-list");
        eventos.setPrefHeight(270);
        eventos.setMinHeight(190);
        eventos.setPlaceholder(new Label("No tienes eventos registrados."));
        eventos.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Evento evento, boolean vacio) {
                super.updateItem(evento, vacio);
                setText(null);
                if (vacio || evento == null) {
                    setGraphic(null);
                    return;
                }

                Label nombre = new Label(evento.getNombre());
                nombre.getStyleClass().add("event-row-title");
                Label fecha = new Label(evento.getFechaInicio().format(FECHA));
                fecha.getStyleClass().add("muted");
                VBox informacion = new VBox(5, nombre, fecha);
                HBox.setHgrow(informacion, Priority.ALWAYS);

                Label estado = new Label(mostrarEstado(evento.getEstado()));
                estado.getStyleClass().add("event-row-status");
                HBox fila = new HBox(14, informacion, estado);
                fila.setAlignment(Pos.CENTER_LEFT);
                setGraphic(fila);
            }
        });
        eventos.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, seleccionado) -> actualizarSeleccion(seleccionado));

        detalle.setWrapText(true);
        mensaje.setWrapText(true);
        botonServicios.setOnAction(e -> ejecutarSeleccionado(abrirSolicitudes));
        botonEditar.setOnAction(evento -> ejecutarSeleccionado(editar));
        botonPublicar.setOnAction(evento -> ejecutarSeleccionado(publicar));
        botonCancelar.setOnAction(evento -> ejecutarSeleccionado(cancelar));
        botonFinalizar.setOnAction(evento -> ejecutarSeleccionado(finalizar));
        botonFinanzas.setOnAction(evento -> ejecutarSeleccionado(verFinanzas));

        HBox acciones = new HBox(10, botonEditar, botonPublicar, botonCancelar, botonFinalizar,
                botonServicios, botonFinanzas);
        acciones.setAlignment(Pos.CENTER_LEFT);
        VBox tarjeta = new VBox(14, eventos, detalle, acciones, mensaje);
        tarjeta.getStyleClass().add("card");
        VBox.setVgrow(eventos, Priority.ALWAYS);
        tarjeta.setMaxWidth(1050);

        VBox contenido = new VBox(18, titulo, ayuda, tarjeta);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1120);
        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        principal.setCenter(scroll);
        raiz.setCenter(principal);

        try {
            eventos.getItems().setAll(controller.listarMisEventos());
        } catch (IllegalStateException e) {
            mostrarMensaje(e.getMessage());
        }
        return raiz;
    }

    private void actualizarSeleccion(Evento seleccionado) {
        boolean planeado = seleccionado != null
                && "PLANEADO".equalsIgnoreCase(seleccionado.getEstado());
        boolean finalizado = seleccionado != null
                && "FINALIZADO".equalsIgnoreCase(seleccionado.getEstado());
        boolean cancelable = seleccionado != null
                && !"CANCELADO".equalsIgnoreCase(seleccionado.getEstado())
                && !finalizado;

        mostrar(botonServicios, seleccionado != null && abrirSolicitudes != null);
        mostrar(botonEditar, planeado);
        mostrar(botonPublicar, finalizado);
        mostrar(botonCancelar, cancelable);
        boolean confirmado=seleccionado!=null && "CONFIRMADO".equalsIgnoreCase(seleccionado.getEstado());
        mostrar(botonFinalizar, confirmado && seleccionado.getFechaFin().isBefore(LocalDateTime.now()));
        mostrar(botonFinanzas, seleccionado != null && verFinanzas != null);
        mensaje.setText("");
        mensaje.setVisible(false);
        mensaje.setManaged(false);

        if (seleccionado == null) {
            detalle.setText("Selecciona un evento para ver las acciones disponibles.");
            return;
        }

        detalle.setText(seleccionado.getNombre() + "\n"
                + "Estado: " + mostrarEstado(seleccionado.getEstado()) + "\n"
                + "Fecha de fin: " + seleccionado.getFechaFin().format(FECHA));

        if(confirmado) {
            try {
                boolean pendiente=controller.galeriaPendiente(seleccionado.getIdEvento());
                botonFinalizar.setText(pendiente ? "Reintentar envío de galería" : "Finalizar evento");
                if(pendiente) {
                    mostrar(botonCancelar,false);
                    mostrarAviso("Las reservas ya están liberadas y las fotos guardadas. Reintenta el envío de la galería.");
                } else if(!seleccionado.getFechaFin().isBefore(LocalDateTime.now())) {
                    mostrarAviso("Podrás finalizar el evento después del "+seleccionado.getFechaFin().format(FECHA)+".");
                }
            } catch(IllegalStateException e) {
                mostrar(botonFinalizar,false); mostrar(botonCancelar,false); mostrarMensaje(e.getMessage());
            }
        }
    }

    private void ejecutarSeleccionado(Consumer<Evento> accion) {
        Evento seleccionado = eventos.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            accion.accept(seleccionado);
        }
    }

    private static void mostrar(Button boton, boolean visible) {
        boton.setVisible(visible);
        boton.setManaged(visible);
    }

    private void mostrarMensaje(String texto) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("message", "error");
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private void mostrarAviso(String texto) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("hint");
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private static String mostrarEstado(String estado) {
        if (estado == null) {
            return "Desconocido";
        }
        return switch (estado.trim().toUpperCase(Locale.ROOT)) {
            case "PLANEADO" -> "En planeación";
            case "CONFIRMADO" -> "Confirmado";
            case "FINALIZADO" -> "Finalizado";
            case "CANCELADO" -> "Cancelado";
            default -> estado;
        };
    }

    private static Button boton(String texto, String id) {
        Button boton = new Button(texto);
        boton.setId(id);
        boton.getStyleClass().add("btn-secondary");
        boton.setVisible(false);
        boton.setManaged(false);
        return boton;
    }
}
