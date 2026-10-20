package views;

import java.math.BigDecimal;
import java.util.List;

import controllers.ControllerSolicitudes;
import entities.MensajeSolicitud;
import entities.PropuestaServicio;
import entities.SolicitudEjecucion;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import views.components.MenuCompartido;

public class ViewSolicitudesProveedor {

    private final ControllerSolicitudes controller;
    private final int idProveedor;
    private final int idUsuario;
    private final Runnable volverServicios;
    private final SesionUsuario sesion;
    private final Runnable abrirServicios;
    private final Runnable abrirSolicitudes;
    private final Runnable cerrarSesion;

    private final ListView<SolicitudEjecucion> lista
            = new ListView<>();

    public ViewSolicitudesProveedor(
            ControllerSolicitudes controller,
            int idProveedor,
            int idUsuario,
            Runnable volverServicios) {

        this(controller, idProveedor, idUsuario, volverServicios, null,
                volverServicios, null, null);
    }

    public ViewSolicitudesProveedor(
            ControllerSolicitudes controller,
            int idProveedor,
            int idUsuario,
            Runnable volverServicios,
            SesionUsuario sesion,
            Runnable abrirServicios,
            Runnable abrirSolicitudes,
            Runnable cerrarSesion) {

        this.controller = controller;
        this.idProveedor = idProveedor;
        this.idUsuario = idUsuario;
        this.volverServicios = volverServicios;
        this.sesion = sesion;
        this.abrirServicios = abrirServicios;
        this.abrirSolicitudes = abrirSolicitudes;
        this.cerrarSesion = cerrarSesion;
    }

    public Parent crearPantalla() {

        BorderPane raiz = new BorderPane();

        raiz.getStyleClass().add("app");

        Label titulo = new Label("Solicitudes recibidas");
        titulo.setStyle(
                "-fx-font-size: 26px; -fx-font-weight: bold;");

        Button volver = new Button("Volver a mis servicios");

        volver.setOnAction(event -> volverServicios.run());

        HBox encabezado = new HBox(20, titulo, volver);
        encabezado.setAlignment(Pos.CENTER_LEFT);

        lista.getStyleClass().add("cu12-list");
        lista.setCellFactory(vista -> new ListCell<>() {

            @Override
            protected void updateItem(
                    SolicitudEjecucion solicitud,
                    boolean vacia) {

                super.updateItem(solicitud, vacia);

                if (vacia || solicitud == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label nombre = new Label(
                        "Solicitud #" + solicitud.getIdSolicitud());

                nombre.getStyleClass().add("cu12-card-title");

                Label detalle = new Label(
                        "Evento #" + solicitud.getIdEvento()
                        + "  •  Servicio #" + solicitud.getIdServicio());

                detalle.getStyleClass().add("cu12-card-detail");

                String fecha = solicitud.getFechaSolicitud()
                        .format(java.time.format.DateTimeFormatter
                                .ofPattern("dd/MM/yyyy  HH:mm"));

                Label fechaSolicitud = new Label(
                        "Recibida el " + fecha);

                fechaSolicitud.getStyleClass().add("cu12-card-detail");

                String estado = solicitud.getEstado();

                Label etiquetaEstado = new Label(
                        estado.replace("_", " "));

                etiquetaEstado.getStyleClass().add("cu12-state");

                if ("PENDIENTE".equalsIgnoreCase(estado)) {
                    etiquetaEstado.getStyleClass().add("cu12-pending");
                } else if ("EN_NEGOCIACION".equalsIgnoreCase(estado)) {
                    etiquetaEstado.getStyleClass().add("cu12-negotiating");
                } else if ("RECHAZADA".equalsIgnoreCase(estado)) {
                    etiquetaEstado.getStyleClass().add("cu12-rejected");
                } else {
                    etiquetaEstado.getStyleClass().add("cu12-neutral");
                }

                HBox encabezadoTarjeta = new HBox(
                        15, nombre, etiquetaEstado);

                encabezadoTarjeta.setAlignment(Pos.CENTER_LEFT);

                VBox tarjeta = new VBox(
                        9,
                        encabezadoTarjeta,
                        detalle,
                        fechaSolicitud);

                setText(null);
                setGraphic(tarjeta);
            }
        });

        Button enviarMensaje = new Button("Enviar mensaje");

        enviarMensaje.setOnAction(
                event -> enviarMensajeSeleccionado());

        Button verMensajes = new Button("Ver conversación");

        verMensajes.setOnAction(
                event -> verMensajesSeleccionados());
        Button rechazar = new Button("Rechazar solicitud");

        rechazar.setOnAction(
                event -> rechazarSeleccionada());

        Button enviarPropuesta = new Button("Enviar propuesta");
        enviarPropuesta.getStyleClass().add("btn-primary");

        enviarPropuesta.setOnAction(
                event -> enviarPropuestaSeleccionada());

        Button verHistorial = new Button("Ver historial de propuestas");

        verHistorial.setOnAction(
                event -> mostrarHistorialSeleccionado());

        javafx.scene.layout.FlowPane acciones
                = new javafx.scene.layout.FlowPane();

        acciones.setHgap(12);
        acciones.setVgap(12);

        acciones.getChildren().addAll(
                enviarPropuesta,
                verHistorial,
                enviarMensaje,
                verMensajes,
                rechazar);

        acciones.setAlignment(Pos.CENTER_LEFT);

        VBox contenido = new VBox(
                20,
                encabezado,
                lista,
                acciones);

        contenido.getStyleClass().add("cu12-content");

        contenido.setPadding(new Insets(30));
        VBox.setVgrow(lista, javafx.scene.layout.Priority.ALWAYS);

        VBox menu = crearMenu();
        raiz.setLeft(menu);

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());
        principal.setCenter(contenido);

        raiz.setCenter(principal);

        raiz.widthProperty().addListener(
                (observable, anterior, actual) -> {

                    boolean mostrarMenu
                    = actual.doubleValue() >= 1000;

                    menu.setVisible(mostrarMenu);
                    menu.setManaged(mostrarMenu);
                });

        recargarSolicitudes();

        return raiz;
    }

    private HBox crearBarraSuperior() {

        return MenuCompartido.crearBarraSuperior(
                sesion,
                "Solicitudes recibidas");
    }

    private VBox crearMenu() {
        return MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.SOLICITUDES_PROVEEDOR,
                null,
                null,
                null,
                abrirServicios,
                cerrarSesion,
                null,
                abrirSolicitudes,
                null);
    }

    private void recargarSolicitudes() {

        try {
            List<SolicitudEjecucion> solicitudes
                    = controller.listarSolicitudesProveedor(idProveedor);

            lista.getItems().setAll(solicitudes);

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    private void rechazarSeleccionada() {

        SolicitudEjecucion seleccionada
                = lista.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            mostrarError("Selecciona una solicitud.");
            return;
        }

        if (!"PENDIENTE".equalsIgnoreCase(
                seleccionada.getEstado())) {

            mostrarError(
                    "Solo puedes rechazar solicitudes pendientes.");
            return;
        }

        TextInputDialog dialogo = new TextInputDialog();

        dialogo.setTitle("Rechazar solicitud");
        dialogo.setHeaderText(
                "Solicitud #" + seleccionada.getIdSolicitud());
        dialogo.setContentText("Motivo del rechazo:");

        dialogo.showAndWait().ifPresent(motivo -> {

            try {
                controller.rechazarSolicitud(
                        idProveedor,
                        seleccionada.getIdSolicitud(),
                        motivo);

                Alert confirmacion = new Alert(
                        Alert.AlertType.INFORMATION,
                        "Solicitud rechazada correctamente.",
                        ButtonType.OK);

                confirmacion.showAndWait();

                recargarSolicitudes();

            } catch (RuntimeException e) {
                mostrarError(e.getMessage());
            }
        });
    }

    private void enviarPropuestaSeleccionada() {

        SolicitudEjecucion seleccionada
                = lista.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            mostrarError("Selecciona una solicitud.");
            return;
        }

        String estado = seleccionada.getEstado();

        if (!"PENDIENTE".equalsIgnoreCase(estado)
                && !"EN_NEGOCIACION".equalsIgnoreCase(estado)) {

            mostrarError(
                    "Esta solicitud ya no permite enviar propuestas.");
            return;
        }

        TextInputDialog dialogoPrecio = new TextInputDialog();

        dialogoPrecio.setTitle("Enviar propuesta");
        dialogoPrecio.setHeaderText(
                "Solicitud #" + seleccionada.getIdSolicitud());
        dialogoPrecio.setContentText(
                "Precio final del servicio:");

        var precioIngresado = dialogoPrecio.showAndWait();

        if (precioIngresado.isEmpty()) {
            return;
        }

        BigDecimal valor;

        try {
            valor = PrecioPropuestaParser.parsear(
                    precioIngresado.get());

        } catch (NumberFormatException e) {
            mostrarError(
                    "El precio debe ser un numero valido. "
                    + "Ejemplo: 1500000");
            return;
        }

        if (valor.signum() <= 0) {
            mostrarError(
                    "El precio final debe ser mayor que cero.");
            return;
        }

        TextInputDialog dialogoDescripcion
                = new TextInputDialog();

        dialogoDescripcion.setTitle("Enviar propuesta");
        dialogoDescripcion.setHeaderText(
                "Alcance del servicio");
        dialogoDescripcion.setContentText(
                "Describe lo que incluye la propuesta:");

        var descripcionIngresada
                = dialogoDescripcion.showAndWait();

        if (descripcionIngresada.isEmpty()) {
            return;
        }

        TextInputDialog dialogoCondiciones
                = new TextInputDialog();

        dialogoCondiciones.setTitle("Enviar propuesta");
        dialogoCondiciones.setHeaderText(
                "Condiciones del servicio");
        dialogoCondiciones.setContentText(
                "Indica las condiciones de la propuesta:");

        var condicionesIngresadas
                = dialogoCondiciones.showAndWait();

        if (condicionesIngresadas.isEmpty()) {
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Confirmas el envio de esta propuesta?",
                ButtonType.YES,
                ButtonType.NO);

        confirmacion.setHeaderText(
                "Confirmar propuesta");

        if (confirmacion.showAndWait().orElse(
                ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            int idPropuesta = controller.enviarPropuesta(
                    idProveedor,
                    seleccionada.getIdSolicitud(),
                    valor,
                    descripcionIngresada.get(),
                    condicionesIngresadas.get());

            Alert resultado = new Alert(
                    Alert.AlertType.INFORMATION,
                    "Propuesta #" + idPropuesta
                    + " guardada correctamente.",
                    ButtonType.OK);

            resultado.setHeaderText(
                    "Propuesta enviada");

            resultado.showAndWait();

            recargarSolicitudes();

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    private void mostrarHistorialSeleccionado() {

        SolicitudEjecucion seleccionada
                = lista.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            mostrarError("Selecciona una solicitud.");
            return;
        }

        try {
            List<PropuestaServicio> historial
                    = controller.listarHistorialPropuestas(
                            idProveedor,
                            seleccionada.getIdSolicitud());

            if (historial.isEmpty()) {

                Alert informacion = new Alert(
                        Alert.AlertType.INFORMATION,
                        "Esta solicitud todavia no tiene propuestas.",
                        ButtonType.OK);

                informacion.showAndWait();
                return;
            }

            StringBuilder contenido = new StringBuilder();

            for (PropuestaServicio propuesta : historial) {

                contenido.append("Version ")
                        .append(propuesta.getNumeroVersion());

                if (propuesta.isVigente()) {
                    contenido.append(" (actual)");
                }

                contenido.append("\nPrecio: $")
                        .append(propuesta.getValor())
                        .append("\nDescripcion: ")
                        .append(propuesta.getDescripcion())
                        .append("\nCondiciones: ")
                        .append(propuesta.getCondiciones())
                        .append("\nEstado: ")
                        .append(propuesta.getEstado())
                        .append("\n\n");
            }

            Alert informacion = new Alert(
                    Alert.AlertType.INFORMATION);

            informacion.setTitle("Historial de propuestas");
            informacion.setHeaderText(
                    "Solicitud #" + seleccionada.getIdSolicitud());

            informacion.setContentText(
                    contenido.toString());

            informacion.setResizable(true);
            informacion.getDialogPane().setPrefWidth(550);

            informacion.showAndWait();

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    private void enviarMensajeSeleccionado() {

        SolicitudEjecucion seleccionada
                = lista.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            mostrarError("Selecciona una solicitud.");
            return;
        }

        TextInputDialog dialogo = new TextInputDialog();

        dialogo.setTitle("Enviar mensaje");
        dialogo.setHeaderText(
                "Solicitud #" + seleccionada.getIdSolicitud());
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
                    seleccionada.getIdSolicitud(),
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

        SolicitudEjecucion seleccionada
                = lista.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            mostrarError("Selecciona una solicitud.");
            return;
        }

        try {
            List<MensajeSolicitud> mensajes
                    = controller.listarMensajes(
                            seleccionada.getIdSolicitud(),
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
                    "Solicitud #" + seleccionada.getIdSolicitud());
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

        alerta.setHeaderText("No se pudo completar la operación");
        alerta.showAndWait();
    }
}
