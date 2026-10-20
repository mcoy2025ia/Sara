package views;

import java.util.List;

import controllers.ControllerSolicitudes;
import entities.MensajeSolicitud;
import entities.PropuestaServicio;
import entities.SesionUsuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import views.components.MenuCompartido;

public class ViewPropuestasOrganizador {

    private final ControllerSolicitudes controller;
    private final int idOrganizador;
    private final int idUsuario;
    private final Runnable volverEventos;
    private final SesionUsuario sesion;
    private final Runnable abrirEventos;
    private final Runnable abrirInvitados;
    private final Runnable abrirCatalogo;
    private final Runnable abrirMisEventos;
    private final Runnable abrirPropuestas;
    private final Runnable cerrarSesion;

    private final ListView<PropuestaServicio> lista
            = new ListView<>();

    public ViewPropuestasOrganizador(
            ControllerSolicitudes controller,
            int idOrganizador,
            int idUsuario,
            Runnable volverEventos) {

        this(controller, idOrganizador, idUsuario, volverEventos, null,
                volverEventos, null, null, null, null, null);
    }

    public ViewPropuestasOrganizador(
            ControllerSolicitudes controller,
            int idOrganizador,
            int idUsuario,
            Runnable volverEventos,
            SesionUsuario sesion,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirMisEventos,
            Runnable abrirPropuestas,
            Runnable cerrarSesion) {

        this.controller = controller;
        this.idOrganizador = idOrganizador;
        this.idUsuario = idUsuario;
        this.volverEventos = volverEventos;
        this.sesion = sesion;
        this.abrirEventos = abrirEventos;
        this.abrirInvitados = abrirInvitados;
        this.abrirCatalogo = abrirCatalogo;
        this.abrirMisEventos = abrirMisEventos;
        this.abrirPropuestas = abrirPropuestas;
        this.cerrarSesion = cerrarSesion;
    }

    public Parent crearPantalla() {

        BorderPane raiz = new BorderPane();

        raiz.getStyleClass().add("app");

        VBox menu = crearMenu();
        raiz.setLeft(menu);

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());

        raiz.widthProperty().addListener(
                (observable, anterior, actual) -> {

                    boolean mostrarMenu
                    = actual.doubleValue() >= 1000;

                    menu.setVisible(mostrarMenu);
                    menu.setManaged(mostrarMenu);
                });

        Label titulo = new Label("Propuestas recibidas");

        titulo.setStyle(
                "-fx-font-size: 26px; -fx-font-weight: bold;");

        Label ayuda = new Label(
                "Consulta las propuestas vigentes enviadas "
                + "por los proveedores para tus eventos.");

        ayuda.setWrapText(true);

        Button volver = new Button("Volver a eventos");

        volver.setOnAction(
                event -> volverEventos.run());

        Button actualizar = new Button("Actualizar");

        actualizar.setOnAction(
                event -> recargarPropuestas());

        Button rechazarPropuesta = new Button("Rechazar propuesta");

        rechazarPropuesta.setOnAction(
                event -> rechazarPropuestaSeleccionada());

        Button solicitarAjuste = new Button("Solicitar ajuste");

        solicitarAjuste.setOnAction(
                event -> solicitarAjusteSeleccionado());

        Button enviarMensaje = new Button("Enviar mensaje");

        enviarMensaje.setOnAction(
                event -> enviarMensajeSeleccionado());

        Button verMensajes = new Button("Ver conversación");

        verMensajes.setOnAction(
                event -> verMensajesSeleccionados());

        Button seleccionar = new Button("Seleccionar propuesta");
        seleccionar.getStyleClass().add("btn-primary");

        seleccionar.setOnAction(
                event -> seleccionarPropuesta());

        HBox encabezado = new HBox(
                20,
                titulo,
                volver);

        encabezado.setAlignment(Pos.CENTER_LEFT);

        lista.setPlaceholder(
                new Label("Todavia no tienes propuestas recibidas."));

        lista.getStyleClass().add("cu12-list");

        lista.setCellFactory(vista -> new ListCell<>() {

            @Override
            protected void updateItem(
                    PropuestaServicio propuesta,
                    boolean vacia) {

                super.updateItem(propuesta, vacia);

                if (vacia || propuesta == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label tituloTarjeta = new Label(
                        "Propuesta #" + propuesta.getIdPropuesta());

                tituloTarjeta.getStyleClass().add("cu12-card-title");

                Label estado = new Label(
                        propuesta.getEstado().replace("_", " "));

                estado.getStyleClass().add("cu12-state");

                if ("SELECCIONADA".equalsIgnoreCase(
                        propuesta.getEstado())) {
                    estado.getStyleClass().add("cu12-selected");
                } else {
                    estado.getStyleClass().add("cu12-negotiating");
                }

                HBox cabecera = new HBox(15, tituloTarjeta, estado);
                cabecera.setAlignment(Pos.CENTER_LEFT);

                String precio = java.text.NumberFormat
                        .getCurrencyInstance(
                                java.util.Locale.forLanguageTag("es-CO"))
                        .format(propuesta.getValor());

                Label valor = new Label(precio);
                valor.getStyleClass().add("cu12-price");

                Label detalle = new Label(
                        "Solicitud #" + propuesta.getIdSolicitud()
                        + "  •  Versión " + propuesta.getNumeroVersion());

                detalle.getStyleClass().add("cu12-card-detail");

                Label alcance = new Label(
                        "Alcance: " + propuesta.getDescripcion());

                alcance.setWrapText(true);
                alcance.getStyleClass().add("cu12-card-detail");

                Label condiciones = new Label(
                        "Condiciones: " + propuesta.getCondiciones());

                condiciones.setWrapText(true);
                condiciones.getStyleClass().add("cu12-card-detail");

                VBox tarjeta = new VBox(
                        9, cabecera, valor, detalle, alcance, condiciones);

                setText(null);
                setGraphic(tarjeta);
            }
        });

        FlowPane acciones = new FlowPane(
                12,
                12,
                actualizar,
                seleccionar,
                solicitarAjuste,
                rechazarPropuesta,
                enviarMensaje,
                verMensajes);

        acciones.setAlignment(Pos.CENTER_LEFT);

        VBox contenido = new VBox(
                20,
                encabezado,
                ayuda,
                lista,
                acciones);

        contenido.getStyleClass().addAll(
                "content",
                "cu12-content");
        contenido.setMaxWidth(1320);

        lista.setPrefHeight(500);

        VBox.setVgrow(lista, Priority.ALWAYS);

        principal.setCenter(contenido);
        raiz.setCenter(principal);

        recargarPropuestas();

        return raiz;
    }

    private VBox crearMenu() {
        return MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.PROPUESTAS_ORGANIZADOR,
                abrirEventos,
                abrirInvitados,
                abrirCatalogo,
                null,
                cerrarSesion,
                abrirMisEventos,
                null,
                abrirPropuestas);
    }

    private HBox crearBarraSuperior() {
        return MenuCompartido.crearBarraSuperior(
                sesion,
                "Propuestas recibidas");
    }

    private void seleccionarPropuesta() {

        PropuestaServicio seleccionada
                = lista.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {

            Alert alerta = new Alert(
                    Alert.AlertType.WARNING,
                    "Selecciona una propuesta de la lista.",
                    ButtonType.OK);

            alerta.showAndWait();
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Deseas seleccionar la propuesta #"
                + seleccionada.getIdPropuesta()
                + " por un valor de $"
                + seleccionada.getValor()
                + "?",
                ButtonType.YES,
                ButtonType.NO);

        confirmacion.setHeaderText(
                "Confirmar seleccion");

        if (confirmacion.showAndWait().orElse(
                ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            controller.seleccionarPropuesta(
                    idOrganizador,
                    seleccionada.getIdPropuesta());

            Alert resultado = new Alert(
                    Alert.AlertType.INFORMATION,
                    "La propuesta fue seleccionada correctamente.",
                    ButtonType.OK);

            resultado.showAndWait();

            recargarPropuestas();

        } catch (RuntimeException e) {

            Alert alerta = new Alert(
                    Alert.AlertType.ERROR,
                    e.getMessage(),
                    ButtonType.OK);

            alerta.setHeaderText(
                    "No se pudo seleccionar la propuesta");

            alerta.showAndWait();
        }
    }

    private void rechazarPropuestaSeleccionada() {

        PropuestaServicio propuesta
                = lista.getSelectionModel().getSelectedItem();

        if (propuesta == null) {
            mostrarError("Selecciona una propuesta.");
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Seguro que deseas rechazar la propuesta #"
                + propuesta.getIdPropuesta()
                + "? Esta accion cerrara la negociacion.",
                ButtonType.YES,
                ButtonType.NO);

        confirmacion.setHeaderText(
                "Confirmar rechazo de propuesta");

        if (confirmacion.showAndWait().orElse(
                ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            controller.rechazarPropuesta(
                    idOrganizador,
                    propuesta.getIdPropuesta());

            Alert resultado = new Alert(
                    Alert.AlertType.INFORMATION,
                    "La propuesta fue rechazada correctamente.",
                    ButtonType.OK);

            resultado.showAndWait();

            recargarPropuestas();

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    private void solicitarAjusteSeleccionado() {

        PropuestaServicio propuesta
                = lista.getSelectionModel().getSelectedItem();

        if (propuesta == null) {
            mostrarError("Selecciona una propuesta.");
            return;
        }

        if ("SELECCIONADA".equalsIgnoreCase(
                propuesta.getEstado())) {

            mostrarError(
                    "No puedes solicitar ajustes sobre una propuesta "
                    + "que ya esta seleccionada. Cambia la seleccion "
                    + "antes de continuar la negociacion.");
            return;
        }

        TextInputDialog dialogo = new TextInputDialog();

        dialogo.setTitle("Solicitar ajuste");
        dialogo.setHeaderText(
                "Propuesta #" + propuesta.getIdPropuesta()
                + " - Version " + propuesta.getNumeroVersion());
        dialogo.setContentText(
                "¿Que necesitas que modifique el proveedor?");

        var respuesta = dialogo.showAndWait();

        if (respuesta.isEmpty()) {
            return;
        }

        if (respuesta.get().isBlank()) {
            mostrarError("Debes explicar el ajuste solicitado.");
            return;
        }

        String mensaje
                = "SOLICITUD DE AJUSTE - Propuesta #"
                + propuesta.getIdPropuesta()
                + ", version "
                + propuesta.getNumeroVersion()
                + ": "
                + respuesta.get().trim();

        try {
            controller.enviarMensaje(
                    propuesta.getIdSolicitud(),
                    idUsuario,
                    mensaje,
                    null);

            Alert confirmacion = new Alert(
                    Alert.AlertType.INFORMATION,
                    "La solicitud de ajuste fue enviada al proveedor.",
                    ButtonType.OK);

            confirmacion.showAndWait();

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    private void enviarMensajeSeleccionado() {

        PropuestaServicio propuesta
                = lista.getSelectionModel().getSelectedItem();

        if (propuesta == null) {
            mostrarError("Selecciona una propuesta.");
            return;
        }

        TextInputDialog dialogo = new TextInputDialog();

        dialogo.setTitle("Enviar mensaje");
        dialogo.setHeaderText(
                "Propuesta #" + propuesta.getIdPropuesta());
        dialogo.setContentText("Mensaje:");

        var contenido = dialogo.showAndWait();

        if (contenido.isEmpty()) {
            return;
        }

        TextInputDialog dialogoEnlace = new TextInputDialog();

        dialogoEnlace.setTitle("Adjunto opcional");
        dialogoEnlace.setHeaderText(
                "Puedes agregar un enlace de apoyo.");
        dialogoEnlace.setContentText(
                "Enlace (puede quedar vacio):");

        var enlace = dialogoEnlace.showAndWait();

        if (enlace.isEmpty()) {
            return;
        }

        try {
            controller.enviarMensaje(
                    propuesta.getIdSolicitud(),
                    idUsuario,
                    contenido.get(),
                    enlace.get());

            Alert confirmacion = new Alert(
                    Alert.AlertType.INFORMATION,
                    "Mensaje enviado correctamente.",
                    ButtonType.OK);

            confirmacion.showAndWait();

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    private void verMensajesSeleccionados() {

        PropuestaServicio propuesta
                = lista.getSelectionModel().getSelectedItem();

        if (propuesta == null) {
            mostrarError("Selecciona una propuesta.");
            return;
        }

        try {
            List<MensajeSolicitud> mensajes
                    = controller.listarMensajes(
                            propuesta.getIdSolicitud(),
                            idUsuario);

            StringBuilder historial = new StringBuilder();

            for (MensajeSolicitud mensaje : mensajes) {

                historial.append("Usuario #")
                        .append(mensaje.getIdUsuarioRemitente())
                        .append(" - ")
                        .append(mensaje.getFechaHora())
                        .append("\n")
                        .append(mensaje.getContenido())
                        .append("\n");

                if (mensaje.getAdjuntoUrl() != null
                        && !mensaje.getAdjuntoUrl().isBlank()) {

                    historial.append("Enlace: ")
                            .append(mensaje.getAdjuntoUrl())
                            .append("\n");
                }

                historial.append("\n");
            }

            if (mensajes.isEmpty()) {
                historial.append("Todavia no hay mensajes.");
            }

            Alert informacion = new Alert(
                    Alert.AlertType.INFORMATION);

            informacion.setTitle("Conversación");
            informacion.setHeaderText(
                    "Solicitud #" + propuesta.getIdSolicitud());
            informacion.setContentText(historial.toString());
            informacion.setResizable(true);
            informacion.getDialogPane().setPrefWidth(600);
            informacion.showAndWait();

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {

        Alert alerta = new Alert(
                Alert.AlertType.ERROR,
                mensaje,
                ButtonType.OK);

        alerta.showAndWait();
    }

    private void recargarPropuestas() {

        try {
            List<PropuestaServicio> propuestas
                    = controller.listarPropuestasOrganizador(
                            idOrganizador);

            lista.getItems().setAll(propuestas);

        } catch (RuntimeException e) {

            Alert alerta = new Alert(
                    Alert.AlertType.ERROR,
                    e.getMessage(),
                    ButtonType.OK);

            alerta.setHeaderText(
                    "No se pudieron consultar las propuestas");

            alerta.showAndWait();
        }
    }
}
