package views;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import controllers.ControllerProveedores;
import controllers.ControllerSolicitudes;
import entities.ImagenServicio;
import entities.PreguntaServicio;
import entities.Servicio;
import entities.SesionUsuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import views.components.MenuCompartido;


public class ViewDetalleServicio {

    private final ControllerProveedores controller;
    private final ControllerSolicitudes controllerSolicitudes;
    private final Servicio servicio;
    private final int idOrganizador;
    private final SesionUsuario sesion;
    private final Runnable volverCatalogo;
    private final Runnable abrirEventos;
    private final Runnable abrirInvitados;
    private final Runnable cerrarSesion;
    private final Runnable abrirMisEventos;

    private final FlowPane imagenes
            = new FlowPane();

    private final VBox preguntas
            = new VBox(10);

    private final TextArea nuevaPregunta
            = new TextArea();

    private final NumberFormat formatoCOP
            = NumberFormat.getNumberInstance(
                    Locale.forLanguageTag("es-CO"));

    public ViewDetalleServicio(
            ControllerProveedores controller,
            ControllerSolicitudes controllerSolicitudes,
            Servicio servicio,
            int idOrganizador,
            SesionUsuario sesion,
            Runnable volverCatalogo,
            Runnable abrirEventos,
            Runnable cerrarSesion) {

        this(controller, controllerSolicitudes, servicio, idOrganizador, sesion,
                volverCatalogo, abrirEventos, null, cerrarSesion, null);
    }

    public ViewDetalleServicio(
            ControllerProveedores controller,
            Servicio servicio,
            int idOrganizador,
            SesionUsuario sesion,
            Runnable volverCatalogo,
            Runnable abrirEventos,
            Runnable cerrarSesion) {

        this(controller, null, servicio, idOrganizador, sesion,
                volverCatalogo, abrirEventos, null, cerrarSesion, null);
    }

    public ViewDetalleServicio(
            ControllerProveedores controller,
            Servicio servicio,
            int idOrganizador,
            SesionUsuario sesion,
            Runnable volverCatalogo,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable cerrarSesion) {

        this(controller, servicio, idOrganizador, sesion, volverCatalogo,
                abrirEventos, abrirInvitados, cerrarSesion, null);
    }

    public ViewDetalleServicio(
            ControllerProveedores controller,
            Servicio servicio,
            int idOrganizador,
            SesionUsuario sesion,
            Runnable volverCatalogo,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable cerrarSesion,
            Runnable abrirMisEventos) {

        this(controller, null, servicio, idOrganizador, sesion,
                volverCatalogo, abrirEventos, abrirInvitados, cerrarSesion,
                abrirMisEventos);
    }

    public ViewDetalleServicio(
            ControllerProveedores controller,
            ControllerSolicitudes controllerSolicitudes,
            Servicio servicio,
            int idOrganizador,
            SesionUsuario sesion,
            Runnable volverCatalogo,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable cerrarSesion,
            Runnable abrirMisEventos) {

        this.controller = controller;
        this.controllerSolicitudes = controllerSolicitudes;
        this.servicio = servicio;
        this.idOrganizador = idOrganizador;
        this.sesion = sesion;
        this.volverCatalogo = volverCatalogo;
        this.abrirEventos = abrirEventos;
        this.abrirInvitados = abrirInvitados;
        this.cerrarSesion = cerrarSesion;
        this.abrirMisEventos = abrirMisEventos;

        formatoCOP.setMaximumFractionDigits(0);

        imagenes.setHgap(12);
        imagenes.setVgap(12);

        nuevaPregunta.setPromptText(
                "Escribe tu pregunta");

        nuevaPregunta.setWrapText(true);
        nuevaPregunta.setPrefRowCount(3);
    }

    public Parent crearPantalla() {

        BorderPane raiz
                = new BorderPane();

        raiz.getStyleClass().add("app");

        VBox sidebar
                = crearMenu();

        raiz.setLeft(sidebar);

        BorderPane principal
                = new BorderPane();

        principal.setTop(
                crearBarraSuperior());

        VBox contenido
                = new VBox(30);

        contenido.getStyleClass().add(
                "content");

        contenido.setMaxWidth(1320);

        VBox titulo
                = new VBox(
                        8,
                        etiqueta(
                                servicio.getNombre(),
                                "page-title"),
                        etiqueta(
                                seguro(
                                        servicio.getCategoria())
                                + " · "
                                + seguro(
                                        servicio.getCiudadZona()),
                                "muted"));

        HBox encabezado
                = new HBox(
                        20,
                        titulo,
                        espacioHorizontal(),
                        etiqueta(
                                "● Disponible",
                                "status"));

        encabezado.setAlignment(
                Pos.CENTER_LEFT);

        GridPane columnas
                = new GridPane();

        columnas.setHgap(25);
        columnas.setVgap(25);

        ColumnConstraints izquierda
                = new ColumnConstraints();

        izquierda.setHgrow(
                Priority.ALWAYS);

        ColumnConstraints derecha
                = new ColumnConstraints(330);

        columnas
                .getColumnConstraints()
                .addAll(
                        izquierda,
                        derecha);

        VBox informacion
                = crearInformacionServicio();

        VBox panelPreguntas
                = crearPanelPreguntas();

        columnas.add(
                informacion,
                0,
                0);

        columnas.add(
                panelPreguntas,
                1,
                0);

        GridPane.setHgrow(
                informacion,
                Priority.ALWAYS);

        contenido
                .getChildren()
                .addAll(
                        encabezado,
                        columnas);

        Button solicitarServicio
                = new Button("Solicitar este servicio");

        solicitarServicio.getStyleClass().add("btn-primary");

        solicitarServicio.setOnAction(
                event -> solicitarServicio());

        contenido.getChildren().add(solicitarServicio);

        StackPane centrado
                = new StackPane(contenido);

        centrado.setAlignment(
                Pos.TOP_CENTER);

        ScrollPane scroll
                = new ScrollPane(centrado);

        scroll.setFitToWidth(true);

        scroll
                .getStyleClass()
                .add("content-scroll");

        principal.setCenter(scroll);
        raiz.setCenter(principal);

        raiz.widthProperty().addListener(
                (observable, anterior, actual) -> {

                    boolean ancho
                    = actual.doubleValue() >= 1150;

                    columnas
                            .getChildren()
                            .clear();

                    columnas
                            .getColumnConstraints()
                            .setAll(izquierda);

                    if (ancho) {

                        columnas
                                .getColumnConstraints()
                                .add(derecha);
                    }

                    columnas.add(
                            informacion,
                            0,
                            0);

                    columnas.add(
                            panelPreguntas,
                            ancho ? 1 : 0,
                            ancho ? 0 : 1);

                    GridPane.setHgrow(
                            panelPreguntas,
                            Priority.ALWAYS);

                    sidebar.setVisible(
                            actual.doubleValue() >= 1000);

                    sidebar.setManaged(
                            sidebar.isVisible());
                });

        cargarImagenes();
        cargarPreguntas();

        return raiz;
    }

    private VBox crearMenu() {
        return MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.CATALOGO,
                abrirEventos,
                abrirInvitados,
                volverCatalogo,
                null,
                cerrarSesion,
                abrirMisEventos);
    }

    private HBox crearBarraSuperior() {
        return MenuCompartido.crearBarraSuperior(
                sesion,
                "Detalle del servicio",
                "Proveedores",
                volverCatalogo);
    }

    private VBox crearInformacionServicio() {

        VBox tarjeta
                = new VBox(24);

        tarjeta.getStyleClass().add(
                "card");

        HBox titulo
                = new HBox(
                        13,
                        etiqueta(
                                iconoCategoria(
                                        servicio.getCategoria()),
                                "heading-icon"),
                        etiqueta(
                                "Información del servicio",
                                "card-title"));

        titulo.setAlignment(
                Pos.CENTER_LEFT);

        GridPane resumen
                = new GridPane();

        resumen.setHgap(18);
        resumen.setVgap(16);

        ColumnConstraints izquierda
                = new ColumnConstraints();

        izquierda.setPercentWidth(50);

        ColumnConstraints derecha
                = new ColumnConstraints();

        derecha.setPercentWidth(50);

        resumen
                .getColumnConstraints()
                .addAll(
                        izquierda,
                        derecha);

        resumen.add(
                dato(
                        "Categoría",
                        seguro(
                                servicio.getCategoria())),
                0,
                0);

        resumen.add(
                dato(
                        "Precio",
                        "$"
                        + formatoCOP.format(
                                servicio.getPrecio())
                        + " COP"),
                1,
                0);

        resumen.add(
                dato(
                        "Ciudad o zona",
                        seguro(
                                servicio.getCiudadZona())),
                0,
                1);

        resumen.add(
                dato(
                        "Tipo de servicio",
                        seguro(
                                servicio.getTipo())),
                1,
                1);

        resumen.add(
                dato(
                        "Anticipación mínima",
                        servicio.getAnticipacionMinima()
                        + " días"),
                0,
                2);

        String capacidad
                = servicio.getCantidadMaximaPersonas() == null
                ? "No especificada"
                : servicio.getCantidadMaximaPersonas()
                + " personas";

        resumen.add(
                dato(
                        "Capacidad máxima",
                        capacidad),
                1,
                2);

        Label descripcion
                = etiqueta(
                        seguro(
                                servicio.getDescripcion()),
                        "muted");

        descripcion.setWrapText(true);

        VBox descripcionCaja
                = new VBox(
                        9,
                        etiqueta(
                                "Descripción",
                                "section-title"),
                        descripcion);

        VBox imagenesCaja
                = new VBox(
                        12,
                        etiqueta(
                                "Imágenes del servicio",
                                "section-title"),
                        imagenes);

        tarjeta
                .getChildren()
                .addAll(
                        titulo,
                        resumen,
                        new Separator(),
                        descripcionCaja,
                        new Separator(),
                        imagenesCaja);

        return tarjeta;
    }

    private VBox dato(
            String titulo,
            String valor) {

        Label nombre
                = etiqueta(
                        titulo,
                        "muted");

        Label contenido
                = etiqueta(
                        valor,
                        "summary-value");

        contenido.setWrapText(true);

        VBox caja
                = new VBox(
                        5,
                        nombre,
                        contenido);

        caja
                .getStyleClass()
                .add("service-detail-data");

        return caja;
    }

    private void solicitarServicio() {

        TextInputDialog dialogoEvento = new TextInputDialog();

        dialogoEvento.setTitle("Solicitar servicio");
        dialogoEvento.setHeaderText(
                "Solicitar: " + servicio.getNombre());
        dialogoEvento.setContentText(
                "ID del evento para el que necesitas el servicio:");

        var eventoIngresado = dialogoEvento.showAndWait();

        if (eventoIngresado.isEmpty()) {
            return;
        }

        int idEvento;

        try {
            idEvento = Integer.parseInt(
                    eventoIngresado.get().trim());

        } catch (NumberFormatException e) {
            mostrarMensajeSolicitud(
                    Alert.AlertType.ERROR,
                    "El ID del evento debe ser un numero entero.");
            return;
        }

        List<String> necesidades;

        try {
            necesidades
                    = controllerSolicitudes.listarNecesidadesOrganizador(
                            idOrganizador);

        } catch (RuntimeException e) {
            mostrarMensajeSolicitud(
                    Alert.AlertType.ERROR,
                    e.getMessage());
            return;
        }

        String opcionNueva = "Crear necesidad nueva";

        necesidades.add(0, opcionNueva);

        ChoiceDialog<String> selector
                = new ChoiceDialog<>(opcionNueva, necesidades);

        selector.setTitle("Necesidad del evento");
        selector.setHeaderText(
                "Selecciona una necesidad existente "
                + "o crea una nueva.");
        selector.setContentText("Necesidad:");

        var opcionSeleccionada = selector.showAndWait();

        if (opcionSeleccionada.isEmpty()) {
            return;
        }

        int idNecesidadExistente = 0;
        String descripcionNecesidad = null;

        if (opcionNueva.equals(opcionSeleccionada.get())) {

            TextInputDialog dialogoNecesidad
                    = new TextInputDialog();

            dialogoNecesidad.setTitle("Nueva necesidad");
            dialogoNecesidad.setHeaderText(
                    "Describe lo que necesitas para tu evento");
            dialogoNecesidad.setContentText(
                    "Descripcion de la necesidad:");

            var descripcionIngresada
                    = dialogoNecesidad.showAndWait();

            if (descripcionIngresada.isEmpty()) {
                return;
            }

            descripcionNecesidad = descripcionIngresada.get();

        } else {

            String identificador = opcionSeleccionada.get()
                    .split("\\|")[0]
                    .trim();

            idNecesidadExistente
                    = Integer.parseInt(identificador);
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Confirmas que deseas solicitar este servicio?",
                ButtonType.YES,
                ButtonType.NO);

        if (confirmacion.showAndWait().orElse(
                ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            int idSolicitud;

            if (idNecesidadExistente > 0) {

                idSolicitud
                        = controllerSolicitudes.crearSolicitudParaNecesidad(
                                idOrganizador,
                                idEvento,
                                servicio.getIdServicio(),
                                idNecesidadExistente);

            } else {

                idSolicitud = controllerSolicitudes.crearSolicitud(
                        idOrganizador,
                        idEvento,
                        servicio.getIdServicio(),
                        descripcionNecesidad);
            }

            mostrarMensajeSolicitud(
                    Alert.AlertType.INFORMATION,
                    "Solicitud #" + idSolicitud
                    + " registrada correctamente.");

        } catch (RuntimeException e) {
            mostrarMensajeSolicitud(
                    Alert.AlertType.ERROR,
                    e.getMessage());
        }
    }

    private void mostrarMensajeSolicitud(
            Alert.AlertType tipo,
            String mensaje) {

        Alert alerta = new Alert(
                tipo,
                mensaje,
                ButtonType.OK);

        alerta.showAndWait();
    }

    private VBox crearPanelPreguntas() {

        VBox preguntasTarjeta
                = new VBox(17);

        preguntasTarjeta
                .getStyleClass()
                .add("card");

        Label titulo
                = etiqueta(
                        "Preguntas públicas",
                        "card-title");

        Label ayuda
                = etiqueta(
                        "Consulta las dudas de otros organizadores o realiza una nueva pregunta.",
                        "muted");

        ayuda.setWrapText(true);

        preguntasTarjeta
                .getChildren()
                .addAll(
                        titulo,
                        ayuda,
                        preguntas,
                        new Separator(),
                        etiqueta(
                                "¿Tienes una duda?",
                                "section-title"),
                        nuevaPregunta);

        Button enviar
                = new Button(
                        "Enviar pregunta");

        enviar
                .getStyleClass()
                .add("btn-primary");

        enviar.setMaxWidth(
                Double.MAX_VALUE);

        enviar.setOnAction(
                event
                -> enviarPregunta());

        preguntasTarjeta
                .getChildren()
                .add(enviar);

        Label info
                = etiqueta(
                        "Las preguntas son públicas para que otros organizadores también puedan consultar la respuesta.",
                        "muted");

        info.setWrapText(true);

        VBox informacion
                = new VBox(
                        12,
                        etiqueta(
                                "ⓘ Información importante",
                                "info-title"),
                        info);

        informacion
                .getStyleClass()
                .add("info-card");

        return new VBox(
                22,
                preguntasTarjeta,
                informacion);
    }

    private void cargarImagenes() {

        imagenes
                .getChildren()
                .clear();

        try {

            List<ImagenServicio> lista
                    = controller
                            .listarImagenesServicio(
                                    servicio.getIdServicio());

            if (lista.isEmpty()) {

                imagenes
                        .getChildren()
                        .add(
                                etiqueta(
                                        "Este servicio aún no tiene imágenes.",
                                        "muted"));

                return;
            }

            for (ImagenServicio imagen
                    : lista) {

                try {

                    ImageView vista
                            = new ImageView(
                                    new Image(
                                            imagen.getRuta(),
                                            true));

                    vista.setFitWidth(190);
                    vista.setFitHeight(125);
                    vista.setPreserveRatio(true);
                    vista.setSmooth(true);

                    vista
                            .getStyleClass()
                            .add("service-image");

                    imagenes
                            .getChildren()
                            .add(vista);

                } catch (Exception e) {

                    imagenes
                            .getChildren()
                            .add(
                                    etiqueta(
                                            "Imagen no disponible",
                                            "muted"));
                }
            }

        } catch (Exception e) {

            mostrarError(
                    e.getMessage());
        }
    }

    private void cargarPreguntas() {

        preguntas
                .getChildren()
                .clear();

        try {

            List<PreguntaServicio> lista
                    = controller
                            .listarPreguntasPublicas(
                                    servicio.getIdServicio());

            if (lista.isEmpty()) {

                preguntas
                        .getChildren()
                        .add(
                                etiqueta(
                                        "Todavía no hay preguntas sobre este servicio.",
                                        "muted"));

                return;
            }

            for (PreguntaServicio pregunta
                    : lista) {

                Label preguntaLabel
                        = etiqueta(
                                pregunta.getPregunta(),
                                "profile-name");

                preguntaLabel.setWrapText(
                        true);

                String respuesta
                        = pregunta.getRespuesta();

                Label respuestaLabel
                        = etiqueta(
                                respuesta == null
                                || respuesta.isBlank()
                                ? "Pendiente de respuesta"
                                : "Respuesta: "
                                + respuesta,
                                "muted");

                respuestaLabel.setWrapText(
                        true);

                VBox caja
                        = new VBox(
                                6,
                                preguntaLabel,
                                respuestaLabel);

                caja
                        .getStyleClass()
                        .add("public-question");

                preguntas
                        .getChildren()
                        .add(caja);
            }

        } catch (Exception e) {

            mostrarError(
                    e.getMessage());
        }
    }

    private void enviarPregunta() {

        try {

            controller
                    .formularPreguntaPublica(
                            servicio.getIdServicio(),
                            idOrganizador,
                            nuevaPregunta.getText());

            nuevaPregunta.clear();

            cargarPreguntas();

        } catch (Exception e) {

            mostrarError(
                    e.getMessage());
        }
    }

    private String iconoCategoria(
            String categoria) {

        String valor
                = seguro(categoria)
                        .toUpperCase();

        if (valor.contains("ALIMENT")
                || valor.contains("CATER")
                || valor.contains("COMIDA")) {

            return "🍽";
        }

        if (valor.contains("DECOR")) {
            return "🎀";
        }

        if (valor.contains("FOTO")) {
            return "📷";
        }

        if (valor.contains("MUS")
                || valor.contains("SONIDO")) {

            return "♫";
        }

        return "✦";
    }

    private String seguro(
            String texto) {

        return texto == null
                ? ""
                : texto;
    }

    private Label etiqueta(
            String texto,
            String estilo) {

        Label label
                = new Label(texto);

        label
                .getStyleClass()
                .add(estilo);

        return label;
    }

    private Region espacioHorizontal() {

        Region espacio
                = new Region();

        HBox.setHgrow(
                espacio,
                Priority.ALWAYS);

        return espacio;
    }

    private void mostrarError(
            String mensaje) {

        Alert alert
                = new Alert(
                        Alert.AlertType.ERROR);

        alert.setTitle(
                "NextEventux");

        alert.setHeaderText(
                "No se pudo completar la acción");

        alert.setContentText(
                mensaje == null
                        ? "Ocurrió un error."
                        : mensaje);

        alert.showAndWait();
    }
}
