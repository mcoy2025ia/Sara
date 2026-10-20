package views;

import controllers.ControllerInvitados;
import entities.InvitacionRecibida;
import entities.Invitado;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import views.components.MenuCompartido;

/**
 * CU-16 - Gestionar respuesta a la invitación (vista del invitado).
 *
 * Corresponde a mostrarRespuesta() de ViewInvitados en el diagrama de clases.
 * Flujo del diagrama VP-04: el invitado consulta su invitación (R37) y responde si
 * asistirá (R38); al rechazar libera su cupo (R39).
 * Los datos llegan de H2 por ControllerInvitados -> ServiceInvitados -> Repository.
 */
public final class ViewRespuestaInvitacion {

    private static final String CSS = "/styles/invitados.css";
    private static final double ANCHO_PANEL = 300;
    /** Ancho de ventana desde el cual el panel derecho va al lado del detalle. */
    private static final double ANCHO_PANEL_LATERAL = 1300;
    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("d MMM yyyy", ES);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("h:mm a", ES);

    private final ControllerInvitados controller;
    private List<InvitacionRecibida> encontradas = List.of();
    private final ToggleGroup respuesta = new ToggleGroup();
    private ToggleButton opcionSi;
    private ToggleButton opcionNo;
    private final Label estadoRespuesta = new Label();

    private final TextField correo = new TextField();
    private final TextField telefono = new TextField();
    /** Datos con los que se verificó al invitado en la última consulta; se reenvían al responder. */
    private String correoVerificado;
    private String telefonoVerificado;
    private final VBox listaInvitaciones = new VBox(10);
    private final Label cantidad = new Label();
    private final Label mensaje = new Label();

    private final Label detalleEvento = new Label();
    private final Label detalleTipo = new Label();
    private final Label detalleFecha = new Label();
    private final Label detalleHora = new Label();
    private final Label detalleLugar = new Label();
    private final Label detalleDireccion = new Label();
    private final Label detalleMensaje = new Label();
    private final Label detalleEstado = new Label();
    private final Label resumenCupos = new Label();
    private final ProgressBar barraCupos = new ProgressBar(0);

    private InvitacionRecibida seleccionada;
    private final ListView<String> avisos = new ListView<>();
    private final FlowPane galeria = new FlowPane(16,16);

    /** Acciones del menú compartido del invitado. */
    private Runnable abrirInvitaciones;
    private Runnable abrirMesaRegalos;
    private Runnable salir;
    /** Avisa el correo y el teléfono verificados para no pedirlos otra vez en la otra pantalla. */
    private BiConsumer<String, String> alVerificar;

    public ViewRespuestaInvitacion(ControllerInvitados controller, Runnable abrirInvitaciones, Runnable abrirMesaRegalos,
                    Runnable salir, BiConsumer<String, String> alVerificar) {
        this(controller);
        this.abrirInvitaciones = abrirInvitaciones;
        this.abrirMesaRegalos = abrirMesaRegalos;
        this.salir = salir;
        this.alVerificar = alVerificar;
    }

    private ViewRespuestaInvitacion(ControllerInvitados controller) {
        this.controller = controller;
        correo.setId("correoInvitado");
        correo.setPromptText("Ej. sofia@email.com");
        correo.setOnAction(e -> consultar());
        HBox.setHgrow(correo, Priority.ALWAYS);
        telefono.setId("telefonoInvitado");
        telefono.setPromptText("Tu teléfono");
        telefono.setOnAction(e -> consultar());

        mensaje.setId("mensaje");
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);

        detalleEvento.getStyleClass().add("page-title");
        detalleEvento.setStyle("-fx-font-size: 24px;");
        detalleEvento.setWrapText(true);
        detalleMensaje.getStyleClass().add("quote");
        detalleMensaje.setWrapText(true);
        detalleMensaje.setMaxWidth(Double.MAX_VALUE);
        barraCupos.setMaxWidth(Double.MAX_VALUE);
        barraCupos.getStyleClass().add("cupos");
        resumenCupos.getStyleClass().add("hint");
        estadoRespuesta.getStyleClass().add("muted");
        estadoRespuesta.setWrapText(true);
    }

    // ------------------------------------------------------------------
    // Construcción de la pantalla
    // ------------------------------------------------------------------
    public Parent crearPantalla() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");
        URL css = getClass().getResource(CSS);
        if (css != null) raiz.getStylesheets().add(css.toExternalForm());

        VBox sidebar = crearMenu();
        raiz.setLeft(sidebar);

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());

        VBox contenido = new VBox(25);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1320);

        VBox titulo = new VBox(8, etiqueta("Mis invitaciones", "page-title"),
                etiqueta("Consulta tus invitaciones y confirma si asistirás al evento.", "muted"));

        VBox detalle = new VBox(25, crearTarjetaDetalle(), crearTarjetaRespuesta());
        detalle.setMinWidth(0);
        VBox tarjetaLista = crearTarjetaLista();
        VBox tarjetaInfo = informacion("Información importante",
                "Si confirmas tu asistencia se reservará tu cupo en el evento. Si indicas que no asistirás, "
                        + "tu cupo se liberará y el organizador recibirá el aviso.");
        tarjetaInfo.getStyleClass().add("highlight");
        Region[] panel = {tarjetaLista, tarjetaInfo};

        VBox panelVertical = new VBox(20);
        panelVertical.setPrefWidth(ANCHO_PANEL);
        panelVertical.setMinWidth(ANCHO_PANEL);
        HBox panelHorizontal = new HBox(20);

        GridPane columnas = new GridPane();
        columnas.setHgap(20);
        columnas.setVgap(25);
        ColumnConstraints izquierda = new ColumnConstraints();
        izquierda.setHgrow(Priority.ALWAYS);
        izquierda.setMinWidth(0);
        ColumnConstraints derecha = new ColumnConstraints(ANCHO_PANEL);
        GridPane.setValignment(detalle, VPos.TOP);

        avisos.setId("avisosInvitado"); avisos.setPrefHeight(150);
        avisos.setPlaceholder(new Label("No hay avisos para el correo consultado."));
        galeria.setId("galeriaInvitado");
        contenido.getChildren().addAll(titulo, mensaje, crearTarjetaBusqueda(),
                etiqueta("Avisos de tus eventos", "section-title"), avisos,
                etiqueta("Fotografías compartidas contigo", "section-title"), galeria, columnas);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("content-scroll");
        principal.setCenter(scroll);
        raiz.setCenter(principal);

        // Mismo comportamiento que las otras pantallas: con ventana angosta el panel baja.
        boolean[] lateralActual = {false};
        boolean[] inicializado = {false};
        Runnable organizar = () -> {
            double ancho = raiz.getWidth() <= 0 ? 1440 : raiz.getWidth();
            boolean lateral = ancho >= ANCHO_PANEL_LATERAL;
            if (inicializado[0] && lateral == lateralActual[0]) return;
            inicializado[0] = true;
            lateralActual[0] = lateral;
            panelVertical.getChildren().clear();
            panelHorizontal.getChildren().clear();
            columnas.getChildren().clear();
            columnas.getColumnConstraints().setAll(izquierda);
            if (lateral) {
                columnas.getColumnConstraints().add(derecha);
                for (Region r : panel) {
                    r.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    HBox.setHgrow(r, Priority.NEVER);
                }
                panelVertical.getChildren().addAll(panel);
                columnas.add(detalle, 0, 0);
                columnas.add(panelVertical, 1, 0);
            } else {
                for (Region r : panel) {
                    r.setPrefWidth(0);
                    r.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(r, Priority.ALWAYS);
                }
                panelHorizontal.getChildren().addAll(panel);
                columnas.add(panelHorizontal, 0, 0);
                columnas.add(detalle, 0, 1);
            }
            GridPane.setHgrow(detalle, Priority.ALWAYS);
        };
        organizar.run();
        raiz.widthProperty().addListener((obs, anterior, actual) -> {
            organizar.run();
            sidebar.setVisible(actual.doubleValue() >= 1000);
            sidebar.setManaged(sidebar.isVisible());
        });

        mostrarListaVacia("Escribe tu correo y pulsa Consultar para ver tus invitaciones.");
        seleccionar(null);
        return raiz;
    }

    /** Llena correo y teléfono ya verificados en la otra pantalla y consulta, si aún no se ha hecho. */
    public void usarDatos(String correoInvitado, String telefonoInvitado) {
        if (correoInvitado == null || telefonoInvitado == null) return;
        if (correoInvitado.equals(correoVerificado) && telefonoInvitado.equals(telefonoVerificado)) return;
        correo.setText(correoInvitado);
        telefono.setText(telefonoInvitado);
        consultar();
    }

    private VBox crearMenu() {
        return MenuCompartido.crearInvitado(MenuCompartido.Pagina.MIS_INVITACIONES,
                abrirInvitaciones, abrirMesaRegalos, salir);
    }

    private HBox crearBarraSuperior() {
        return MenuCompartido.crearBarraSuperior(
                "Mis invitaciones",
                null);
    }

    private VBox crearTarjetaBusqueda() {
        Label ayuda = etiqueta("Escribe el correo con el que recibiste la invitación y tu teléfono.", "muted");
        ayuda.setWrapText(true);
        VBox textos = new VBox(6, etiqueta("Buscar mis invitaciones", "card-title"), ayuda);
        textos.setMinWidth(0);
        HBox.setHgrow(textos, Priority.ALWAYS);

        correo.setPrefWidth(260);
        correo.setMinWidth(180);
        telefono.setPrefWidth(160);
        telefono.setMinWidth(130);
        Button consultar = new Button("Consultar");
        consultar.setId("consultarInvitaciones");
        consultar.getStyleClass().add("btn-primary");
        consultar.setMinWidth(Region.USE_PREF_SIZE);
        consultar.setOnAction(e -> consultar());

        HBox fila = new HBox(16, etiqueta("✉", "heading-icon"), textos, correo, telefono, consultar);
        fila.setAlignment(Pos.CENTER_LEFT);
        VBox tarjeta = new VBox(fila);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private VBox crearTarjetaLista() {
        cantidad.getStyleClass().add("hint");
        VBox encabezado = new VBox(4, etiqueta("Tus invitaciones", "card-title"), cantidad);
        VBox tarjeta = new VBox(16, encabezado, listaInvitaciones);
        tarjeta.getStyleClass().add("info-card");
        return tarjeta;
    }

    private VBox crearTarjetaDetalle() {
        detalleEstado.setMinWidth(Region.USE_PREF_SIZE);
        HBox cabecera = new HBox(16, new VBox(6, detalleEvento, detalleTipo), espacioHorizontal(), detalleEstado);
        cabecera.setAlignment(Pos.TOP_LEFT);
        detalleTipo.getStyleClass().add("muted");

        GridPane datos = new GridPane();
        datos.setHgap(23);
        datos.setVgap(18);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(50);
            datos.getColumnConstraints().add(c);
        }
        datos.add(dato("▦", "Fecha", detalleFecha), 0, 0);
        datos.add(dato("◷", "Hora", detalleHora), 1, 0);
        datos.add(dato("⌂", "Lugar", detalleLugar), 0, 1);
        datos.add(dato("➤", "Dirección", detalleDireccion), 1, 1);

        VBox tarjeta = new VBox(22, cabecera, new Separator(), datos,
                etiqueta("Mensaje del organizador", "section-title"), detalleMensaje,
                new VBox(8, resumenCupos, barraCupos));
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private HBox dato(String icono, String titulo, Label valor) {
        valor.getStyleClass().add("summary-value");
        valor.setStyle("-fx-font-size: 14px;");
        valor.setWrapText(true);
        VBox textos = new VBox(4, etiqueta(titulo, "hint"), valor);
        textos.setMinWidth(0);
        HBox fila = new HBox(12, etiqueta(icono, "heading-icon"), textos);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private VBox crearTarjetaRespuesta() {
        HBox titulo = new HBox(13, etiqueta("✓", "heading-icon"), etiqueta("¿Asistirás al evento?", "card-title"));
        titulo.setAlignment(Pos.CENTER_LEFT);

        ToggleButton si = opcion("Sí, asistiré", "Confirmo mi asistencia y reservo mi cupo.", Invitado.ESTADO_CONFIRMADO);
        ToggleButton no = opcion("No podré asistir", "Rechazo la invitación y libero mi cupo.", Invitado.ESTADO_RECHAZADO);
        si.setId("respuestaSi");
        no.setId("respuestaNo");
        opcionSi = si;
        opcionNo = no;
        GridPane opciones = new GridPane();
        opciones.setHgap(16);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(50);
            opciones.getColumnConstraints().add(c);
        }
        opciones.add(si, 0, 0);
        opciones.add(no, 1, 0);

        Label notaAcompanantes = etiqueta("Después de confirmar podrás registrar a tus acompañantes.", "hint");
        notaAcompanantes.setWrapText(true);

        Button cancelar = new Button("Cancelar");
        cancelar.setId("cancelarRespuesta");
        cancelar.getStyleClass().add("btn-secondary");
        cancelar.setOnAction(e -> respuesta.selectToggle(null));
        Button enviar = new Button("Enviar respuesta");
        enviar.setId("enviarRespuesta");
        enviar.getStyleClass().add("btn-primary");
        enviar.disableProperty().bind(respuesta.selectedToggleProperty().isNull());
        cancelar.disableProperty().bind(respuesta.selectedToggleProperty().isNull());
        enviar.setOnAction(e -> enviarRespuesta());
        HBox acciones = new HBox(12, notaAcompanantes, espacioHorizontal(), cancelar, enviar);
        acciones.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(notaAcompanantes, Priority.SOMETIMES);

        VBox tarjeta = new VBox(22, titulo, estadoRespuesta, opciones, new Separator(), acciones);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private ToggleButton opcion(String titulo, String descripcion, String valor) {
        Label t = etiqueta(titulo, "option-title");
        Label d = etiqueta(descripcion, "hint");
        d.setWrapText(true);
        VBox contenido = new VBox(6, t, d);
        ToggleButton boton = new ToggleButton();
        boton.setGraphic(contenido);
        boton.setUserData(valor);
        boton.setToggleGroup(respuesta);
        boton.getStyleClass().add("option-card");
        boton.setMaxWidth(Double.MAX_VALUE);
        // Alto fijo: evita que el texto ajustado haga crecer la tarjeta.
        boton.setMinHeight(84);
        boton.setPrefHeight(84);
        boton.setMaxHeight(84);
        contenido.setMinWidth(0);
        boton.setAlignment(Pos.CENTER_LEFT);
        return boton;
    }

    // ------------------------------------------------------------------
    // Comportamiento (Controller)
    // ------------------------------------------------------------------
    /** R37: consulta en H2 las invitaciones activas del correo escrito. */
    private void consultar() {
        consultar(seleccionada == null ? -1 : seleccionada.getIdInvitado());
    }

    private void consultar(int idInvitadoSeleccionar) {
        avisos.getItems().clear();
        galeria.getChildren().clear();
        String texto = correo.getText() == null ? "" : correo.getText().trim();
        String tel = telefono.getText() == null ? "" : telefono.getText().trim();
        if (texto.isEmpty() || tel.isEmpty()) {
            mostrarMensaje("Escribe el correo con el que recibiste la invitación y tu teléfono.", false);
            return;
        }
        correoVerificado = null;
        telefonoVerificado = null;
        try {
            for (entities.Fotografia foto:controller.consultarGaleria(texto,tel)) {
                Image imagen=new Image(new java.io.ByteArrayInputStream(foto.contenido()));
                ImageView miniatura=new ImageView(imagen); miniatura.setFitWidth(180); miniatura.setFitHeight(120); miniatura.setPreserveRatio(true);
                Button abrir=new Button("Ver fotografía",miniatura); abrir.setContentDisplay(ContentDisplay.TOP);
                abrir.setOnAction(e-> {
                    ImageView completa=new ImageView(imagen); completa.setFitWidth(720); completa.setFitHeight(480); completa.setPreserveRatio(true);
                    Dialog<Void> dialogo=new Dialog<>(); dialogo.initOwner(correo.getScene().getWindow());
                    dialogo.setTitle(foto.nombre()); dialogo.getDialogPane().setContent(completa);
                    dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE); dialogo.show();
                });
                Label nombre=new Label(foto.nombre()); nombre.setMaxWidth(200); nombre.setWrapText(true);
                galeria.getChildren().add(new VBox(8,abrir,nombre));
            }
            if(galeria.getChildren().isEmpty()) galeria.getChildren().add(new Label("Todavía no tienes fotografías compartidas."));
            avisos.getItems().setAll(controller.consultarAvisosPorCorreo(texto));
            encontradas = controller.consultarInvitacionesPorCorreo(texto, tel);
        } catch (IllegalArgumentException | IllegalStateException e) {
            encontradas = List.of();
            mostrarListaVacia("No se pudieron consultar las invitaciones.");
            seleccionar(null);
            mostrarMensaje(e.getMessage(), false);
            return;
        }
        int n = encontradas.size();
        if (n == 0) {
            mostrarListaVacia("No encontramos invitaciones activas con esos datos.");
            seleccionar(null);
            mostrarMensaje("No encontramos invitaciones activas para ese correo y teléfono. "
                    + "Si el organizador no registró tu teléfono, pídele que lo agregue.", false);
            return;
        }
        correoVerificado = texto;
        telefonoVerificado = tel;
        if (alVerificar != null) alVerificar.accept(texto, tel);
        ocultarMensaje();
        listaInvitaciones.getChildren().clear();
        for (InvitacionRecibida inv : encontradas) listaInvitaciones.getChildren().add(itemInvitacion(inv));
        cantidad.setText(n + (n == 1 ? " invitación encontrada" : " invitaciones encontradas"));
        seleccionar(encontradas.stream().filter(inv -> inv.getIdInvitado() == idInvitadoSeleccionar)
                .findFirst().orElse(encontradas.get(0)));
    }

    private void mostrarListaVacia(String texto) {
        listaInvitaciones.getChildren().clear();
        Label vacio = etiqueta(texto, "muted");
        vacio.setWrapText(true);
        listaInvitaciones.getChildren().add(vacio);
        cantidad.setText("0 invitaciones");
    }

    private Node itemInvitacion(InvitacionRecibida inv) {
        Label nombre = etiqueta(inv.getEvento(), "summary-value");
        nombre.setStyle("-fx-font-size: 13px;");
        nombre.setWrapText(true);
        Label fecha = etiqueta(inv.getInicio().format(FECHA_CORTA) + " · " + inv.getLugar(), "hint");
        fecha.setWrapText(true);
        Label estado = pill(inv.getEstadoInvitado());
        VBox item = new VBox(6, nombre, fecha, estado);
        item.setMinWidth(0);
        item.getStyleClass().add("invitation-item");
        item.setUserData(inv);
        item.setOnMouseClicked(e -> {
            ocultarMensaje();
            seleccionar(inv);
        });
        return item;
    }

    private void seleccionar(InvitacionRecibida inv) {
        seleccionada = inv;
        respuesta.selectToggle(null);
        for (Node n : listaInvitaciones.getChildren()) {
            n.getStyleClass().remove("selected");
            if (inv != null && n.getUserData() == inv) n.getStyleClass().add("selected");
        }
        if (inv == null) {
            detalleEvento.setText("Sin invitación seleccionada");
            for (Label l : new Label[]{detalleTipo, detalleFecha, detalleHora, detalleLugar, detalleDireccion, detalleMensaje})
                l.setText("-");
            detalleEstado.setText("");
            detalleEstado.setVisible(false);
            resumenCupos.setText("");
            barraCupos.setProgress(0);
            estadoRespuesta.setText("Consulta tus invitaciones para poder responder.");
            opcionSi.setDisable(true);
            opcionNo.setDisable(true);
            return;
        }
        detalleEvento.setText(inv.getEvento());
        detalleTipo.setText((inv.getTipoEvento() == null ? "Evento" : inv.getTipoEvento())
                + " · Invitación #" + inv.getIdInvitacion() + " · Para " + inv.getNombreInvitado());
        String fecha = inv.getInicio().format(FECHA_LARGA);
        detalleFecha.setText(Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1));
        detalleHora.setText(inv.getInicio().format(HORA) + " – " + inv.getFin().format(HORA));
        detalleLugar.setText(inv.getLugar());
        detalleDireccion.setText(inv.getDireccion() == null ? "-" : inv.getDireccion());
        detalleMensaje.setText("“" + (inv.getMensaje() == null ? "" : inv.getMensaje()) + "”");
        detalleEstado.setText(textoEstado(inv.getEstadoInvitado()));
        detalleEstado.getStyleClass().setAll("label", "status", claseEstado(inv.getEstadoInvitado()));
        detalleEstado.setVisible(true);
        int libres = Math.max(0, inv.getCapacidad() - inv.getOcupados());
        resumenCupos.setText("Cupos del lugar: " + inv.getOcupados() + " ocupados de " + inv.getCapacidad()
                + " (" + libres + " disponibles)");
        barraCupos.setProgress(inv.getCapacidad() == 0 ? 0 : Math.min(1, (double) inv.getOcupados() / inv.getCapacidad()));

        String estado = estadoDe(inv);
        boolean comenzo = !inv.getInicio().isAfter(LocalDateTime.now());
        boolean cerrado = "CANCELADO".equalsIgnoreCase(inv.getEstadoEvento()) || "FINALIZADO".equalsIgnoreCase(inv.getEstadoEvento());
        opcionSi.setDisable(comenzo || cerrado || estado.equals(Invitado.ESTADO_CONFIRMADO));
        opcionNo.setDisable(comenzo || cerrado || estado.equals(Invitado.ESTADO_RECHAZADO));
        if (comenzo || cerrado) {
            estadoRespuesta.setText("Este evento ya no recibe respuestas.");
        } else if (estado.equals(Invitado.ESTADO_CONFIRMADO)) {
            estadoRespuesta.setText("Ya confirmaste tu asistencia. Si cambias de planes, indica que no podrás asistir.");
        } else if (estado.equals(Invitado.ESTADO_RECHAZADO)) {
            estadoRespuesta.setText("Indicaste que no asistirás. Si cambias de opinión puedes confirmar, si aún hay cupo.");
        } else {
            estadoRespuesta.setText("Elige una opción y envía tu respuesta al organizador.");
        }
    }

    /** R38: guarda la respuesta del invitado; rechazar pide confirmación porque libera el cupo (R39). */
    private void enviarRespuesta() {
        if (seleccionada == null || respuesta.getSelectedToggle() == null) return;
        String nueva = (String) respuesta.getSelectedToggle().getUserData();
        boolean asiste = Invitado.ESTADO_CONFIRMADO.equals(nueva);
        if (!asiste && !confirmarRechazo()) return;
        InvitacionRecibida actual = seleccionada;
        try {
            controller.gestionarRespuestaInvitacion(actual.getIdInvitado(), correoVerificado, telefonoVerificado, nueva);
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarMensaje(e.getMessage(), false);
            return;
        }
        consultar(actual.getIdInvitado());
        mostrarMensaje(asiste
                ? "¡Listo! Confirmaste tu asistencia a " + actual.getEvento() + "."
                : "Registramos que no asistirás a " + actual.getEvento() + ". Tu cupo quedó libre.", true);
    }

    private boolean confirmarRechazo() {
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
        confirmar.setTitle("No podré asistir");
        confirmar.setHeaderText("¿Confirmas que no asistirás a " + seleccionada.getEvento() + "?");
        confirmar.setContentText("Tu cupo y el de tus acompañantes quedarán libres para otros invitados.");
        URL css = getClass().getResource(CSS);
        if (css != null) confirmar.getDialogPane().getStylesheets().add(css.toExternalForm());
        if (correo.getScene() != null) confirmar.initOwner(correo.getScene().getWindow());
        ButtonType si = new ButtonType("Sí, no asistiré", ButtonBar.ButtonData.OK_DONE);
        ButtonType volver = new ButtonType("Volver", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmar.getButtonTypes().setAll(volver, si);
        confirmar.getDialogPane().lookupButton(si).getStyleClass().add("btn-primary");
        confirmar.getDialogPane().lookupButton(volver).getStyleClass().add("btn-secondary");
        return confirmar.showAndWait().filter(si::equals).isPresent();
    }

    private static String estadoDe(InvitacionRecibida inv) {
        return inv.getEstadoInvitado() == null ? Invitado.ESTADO_PENDIENTE : inv.getEstadoInvitado().toUpperCase(Locale.ROOT);
    }

    private static String textoEstado(String estado) {
        String e = estado == null ? "" : estado.toUpperCase(Locale.ROOT);
        return switch (e) {
            case Invitado.ESTADO_CONFIRMADO -> "ASISTIRÁ";
            case Invitado.ESTADO_RECHAZADO -> "NO ASISTIRÁ";
            default -> "SIN RESPONDER";
        };
    }

    private static String claseEstado(String estado) {
        String e = estado == null ? "" : estado.toUpperCase(Locale.ROOT);
        return switch (e) {
            case Invitado.ESTADO_CONFIRMADO -> "status-confirmada";
            case Invitado.ESTADO_RECHAZADO -> "status-rechazada";
            default -> "status-pendiente";
        };
    }

    private Label pill(String estado) {
        Label pill = new Label(textoEstado(estado));
        pill.getStyleClass().setAll("label", "status", claseEstado(estado));
        pill.setMinWidth(Region.USE_PREF_SIZE);
        return pill;
    }

    private void mostrarMensaje(String texto, boolean exito) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("label", "message", exito ? "success" : "error");
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private void ocultarMensaje() {
        mensaje.setVisible(false);
        mensaje.setManaged(false);
    }

    // ------------------------------------------------------------------
    // Utilidades (mismas que ViewEventos y ViewInvitados)
    // ------------------------------------------------------------------
    private Label etiqueta(String texto, String estilo) {
        Label label = new Label(texto);
        label.getStyleClass().add(estilo);
        return label;
    }

    private Region espacioHorizontal() {
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        return espacio;
    }

    private VBox informacion(String titulo, String texto) {
        Label descripcion = etiqueta(texto, "muted");
        descripcion.setWrapText(true);
        Label encabezado = etiqueta(titulo, "card-title");
        encabezado.setWrapText(true);
        VBox tarjeta = new VBox(14, encabezado, descripcion);
        tarjeta.getStyleClass().add("info-card");
        return tarjeta;
    }
}
