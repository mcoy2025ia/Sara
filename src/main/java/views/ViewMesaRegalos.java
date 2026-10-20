package views;

import controllers.ControllerInvitados;
import entities.InvitacionRecibida;
import entities.Regalo;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import views.components.MenuCompartido;

/**
 * CU-20 - Interactuar con la mesa de regalos virtual (vista del invitado).
 *
 * Corresponde a mostrarMesaRegalos() de ViewInvitados en el diagrama de clases.
 * Flujo del diagrama VP-04: el invitado selecciona un regalo; si está disponible lo
 * reserva y queda asociado a él; si otro invitado ya lo reservó, se le avisa (R43).
 *
 * Los datos llegan de H2 por ControllerInvitados -> ServiceInvitados -> Repository.
 * Esta clase no contiene SQL ni reglas de negocio.
 */
public final class ViewMesaRegalos {

    private static final String CSS = "/styles/invitados.css";
    private static final double ANCHO_PANEL = 300;
    /** Ancho de ventana desde el cual el panel derecho va al lado de los regalos. */
    private static final double ANCHO_PANEL_LATERAL = 1300;
    private static final double ANCHO_TARJETA = 212;
    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES);

    private static final String DISPONIBLE = "DISPONIBLE";
    private static final String RESERVADO = "RESERVADO";
    private static final String MIO = "MIO";
    /** Rangos del filtro de presupuesto (precio estimado del regalo, en pesos). */
    private static final String[] PRESUPUESTOS = {
            "Cualquier precio", "Hasta $30.000", "$30.000 a $60.000", "$60.000 a $100.000", "Más de $100.000"};

    private final ControllerInvitados controller;
    private final NumberFormat pesos = NumberFormat.getNumberInstance(ES);

    private List<Regalo> regalos = List.of();
    /** Invitación (evento) cuya mesa se está mostrando. */
    private InvitacionRecibida actual;
    private final TextField correo = new TextField();
    private final TextField telefono = new TextField();
    /** Datos con los que se verificó al invitado en la última consulta; se reenvían en cada acción. */
    private String correoVerificado;
    private String telefonoVerificado;
    private final ComboBox<InvitacionRecibida> selectorEvento = new ComboBox<>();
    private final TextField buscador = new TextField();
    private final ComboBox<String> filtro = new ComboBox<>();
    private final ComboBox<String> presupuesto = new ComboBox<>();
    private final FlowPane tarjetas = new FlowPane(16, 16);
    private final VBox misReservas = new VBox(10);
    private final Label mensaje = new Label();

    private final Label disponiblesValor = new Label("0");
    private final Label reservadosValor = new Label("0");
    private final Label misReservasValor = new Label("0");
    private final Label detalleEvento = new Label();

    /** Acciones del menú compartido del invitado. */
    private Runnable abrirInvitaciones;
    private Runnable abrirMesaRegalos;
    private Runnable salir;
    /** Avisa el correo y el teléfono verificados para no pedirlos otra vez en la otra pantalla. */
    private BiConsumer<String, String> alVerificar;

    public ViewMesaRegalos(ControllerInvitados controller, Runnable abrirInvitaciones, Runnable abrirMesaRegalos,
                    Runnable salir, BiConsumer<String, String> alVerificar) {
        this(controller);
        this.abrirInvitaciones = abrirInvitaciones;
        this.abrirMesaRegalos = abrirMesaRegalos;
        this.salir = salir;
        this.alVerificar = alVerificar;
    }

    private ViewMesaRegalos(ControllerInvitados controller) {
        this.controller = controller;
        pesos.setMaximumFractionDigits(0);

        correo.setId("correoInvitado");
        correo.setPromptText("Ej. camila@email.com");
        correo.setPrefWidth(260);
        correo.setMinWidth(180);
        correo.setOnAction(e -> consultar());

        telefono.setId("telefonoInvitado");
        telefono.setPromptText("Tu teléfono");
        telefono.setPrefWidth(170);
        telefono.setMinWidth(140);
        telefono.setOnAction(e -> consultar());

        selectorEvento.setId("selectorEvento");
        selectorEvento.setPromptText("Selecciona un evento");
        selectorEvento.setPrefWidth(240);
        selectorEvento.setMinWidth(180);
        selectorEvento.setConverter(new StringConverter<>() {
            @Override
            public String toString(InvitacionRecibida inv) {
                return inv == null ? "" : inv.getEvento();
            }

            @Override
            public InvitacionRecibida fromString(String texto) {
                return null;
            }
        });
        selectorEvento.valueProperty().addListener((obs, anterior, actual) -> mostrarEvento(actual));

        buscador.setId("buscarRegalo");
        buscador.setPromptText("Buscar regalo...");
        buscador.setPrefWidth(190);
        buscador.setMinWidth(150);
        buscador.textProperty().addListener((obs, anterior, actual) -> pintarRegalos());

        filtro.setId("filtroRegalos");
        filtro.getItems().addAll("Todos", "Disponibles", "Reservados", "Mis reservas");
        filtro.getSelectionModel().selectFirst();
        filtro.setPrefWidth(160);
        filtro.setMinWidth(150);
        filtro.valueProperty().addListener((obs, anterior, actual) -> pintarRegalos());

        presupuesto.setId("filtroPresupuesto");
        presupuesto.getItems().addAll(PRESUPUESTOS);
        presupuesto.getSelectionModel().selectFirst();
        presupuesto.setPrefWidth(200);
        presupuesto.setMinWidth(190);
        presupuesto.valueProperty().addListener((obs, anterior, actual) -> pintarRegalos());

        mensaje.setId("mensaje");
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);

        tarjetas.setId("tarjetasRegalos");
        tarjetas.setPrefWrapLength(ANCHO_TARJETA * 3 + 32);
        detalleEvento.getStyleClass().add("muted");
        detalleEvento.setWrapText(true);
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

        VBox titulo = new VBox(8, etiqueta("Mesa de regalos", "page-title"),
                etiqueta("Elige el regalo que quieres obsequiar en el evento.", "muted"));

        VBox regalosCard = crearTarjetaRegalos();
        regalosCard.setMinWidth(0);
        GridPane.setValignment(regalosCard, VPos.TOP);
        VBox tarjetaReservas = crearTarjetaMisReservas();
        VBox tarjetaInfo = informacion("Información importante",
                "Al reservar un regalo queda registrado a tu nombre para que el organizador lo sepa. "
                        + "Si cambias de idea, puedes cancelar tu reserva.");
        tarjetaInfo.getStyleClass().add("highlight");
        Region[] panel = {tarjetaReservas, tarjetaInfo};

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

        GridPane estadisticas = crearEstadisticas();
        contenido.getChildren().addAll(titulo, mensaje, crearTarjetaBusqueda(), estadisticas, columnas);

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
            columnas.add(regalosCard, 0, 0);
            GridPane.setHgrow(regalosCard, Priority.ALWAYS);
            if (lateral) {
                columnas.getColumnConstraints().add(derecha);
                for (Region r : panel) {
                    r.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    HBox.setHgrow(r, Priority.NEVER);
                }
                panelVertical.getChildren().addAll(panel);
                columnas.add(panelVertical, 1, 0);
            } else {
                for (Region r : panel) {
                    r.setPrefWidth(0);
                    r.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(r, Priority.ALWAYS);
                }
                panelHorizontal.getChildren().addAll(panel);
                columnas.add(panelHorizontal, 0, 1);
                GridPane.setHgrow(panelHorizontal, Priority.ALWAYS);
            }
        };
        organizar.run();
        raiz.widthProperty().addListener((obs, anterior, actual) -> {
            organizar.run();
            sidebar.setVisible(actual.doubleValue() >= 1000);
            sidebar.setManaged(sidebar.isVisible());
        });

        mostrarEvento(null);
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
        return MenuCompartido.crearInvitado(MenuCompartido.Pagina.MESA_REGALOS,
                abrirInvitaciones, abrirMesaRegalos, salir);
    }

    private HBox crearBarraSuperior() {
        return MenuCompartido.crearBarraSuperior(
                "Mesa de regalos",
                null);
    }

    private VBox crearTarjetaBusqueda() {
        VBox textos = new VBox(6, etiqueta("Mesa de regalos del evento", "card-title"), detalleEvento);
        textos.setMinWidth(0);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button consultar = new Button("Consultar");
        consultar.setId("consultarMesa");
        consultar.getStyleClass().add("btn-primary");
        consultar.setMinWidth(Region.USE_PREF_SIZE);
        consultar.setOnAction(e -> consultar());

        HBox cabecera = new HBox(16, etiqueta("♡", "heading-icon"), textos);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(correo, Priority.ALWAYS);
        correo.setMaxWidth(Double.MAX_VALUE);
        VBox campoCorreo = new VBox(8, etiqueta("Tu correo", "field-label"), correo);
        HBox.setHgrow(campoCorreo, Priority.ALWAYS);
        VBox campoTelefono = new VBox(8, etiqueta("Tu teléfono", "field-label"), telefono);
        VBox campoEvento = new VBox(8, etiqueta("Evento", "field-label"), selectorEvento);
        HBox fila = new HBox(14, campoCorreo, campoTelefono, campoEvento, consultar);
        fila.setAlignment(Pos.BOTTOM_LEFT);
        VBox tarjeta = new VBox(20, cabecera, fila);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private GridPane crearEstadisticas() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / 3);
            c.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(c);
        }
        grid.add(estadistica("Disponibles", disponiblesValor, "Regalos que puedes elegir", "♡"), 0, 0);
        grid.add(estadistica("Reservados", reservadosValor, "Ya elegidos por otros invitados", "✓"), 1, 0);
        grid.add(estadistica("Mis reservas", misReservasValor, "Regalos a tu nombre", "★"), 2, 0);
        return grid;
    }

    private VBox estadistica(String titulo, Label valor, String descripcion, String icono) {
        valor.getStyleClass().add("stat-value");
        Label nombre = etiqueta(titulo, "field-label");
        nombre.setMinWidth(0);
        HBox encabezado = new HBox(10, nombre, espacioHorizontal(), etiqueta(icono, "heading-icon"));
        encabezado.setAlignment(Pos.CENTER_LEFT);
        Label detalle = etiqueta(descripcion, "hint");
        detalle.setWrapText(true);
        VBox tarjeta = new VBox(8, encabezado, valor, detalle);
        tarjeta.getStyleClass().add("info-card");
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        tarjeta.setMinWidth(0);
        return tarjeta;
    }

    private VBox crearTarjetaRegalos() {
        Label ayuda = etiqueta("Selecciona el regalo que quieres obsequiar.", "muted");
        ayuda.setWrapText(true);
        VBox textos = new VBox(6, etiqueta("Regalos", "card-title"), ayuda);
        textos.setMinWidth(0);
        HBox.setHgrow(buscador, Priority.ALWAYS);
        buscador.setMaxWidth(Double.MAX_VALUE);
        HBox filtros = new HBox(12, buscador, filtro, presupuesto);
        filtros.setAlignment(Pos.CENTER_LEFT);
        VBox tarjeta = new VBox(18, textos, filtros, tarjetas);
        tarjeta.getStyleClass().add("card");
        tarjeta.setMaxHeight(Region.USE_PREF_SIZE);
        return tarjeta;
    }

    private VBox crearTarjetaMisReservas() {
        VBox tarjeta = new VBox(16, etiqueta("Mis reservas", "card-title"), misReservas);
        tarjeta.getStyleClass().add("info-card");
        return tarjeta;
    }

    // ------------------------------------------------------------------
    // Comportamiento (Controller)
    // ------------------------------------------------------------------
    /** R37: busca las invitaciones activas del correo; cada una tiene su mesa de regalos. */
    private void consultar() {
        String texto = correo.getText() == null ? "" : correo.getText().trim();
        String tel = telefono.getText() == null ? "" : telefono.getText().trim();
        if (texto.isEmpty() || tel.isEmpty()) {
            mostrarMensaje("Escribe el correo con el que recibiste la invitación y tu teléfono.", false);
            return;
        }
        correoVerificado = null;
        telefonoVerificado = null;
        List<InvitacionRecibida> invitaciones;
        try {
            invitaciones = controller.consultarInvitacionesPorCorreo(texto, tel);
        } catch (IllegalArgumentException | IllegalStateException e) {
            selectorEvento.getItems().clear();
            mostrarEvento(null);
            mostrarMensaje(e.getMessage(), false);
            return;
        }
        int idAnterior = actual == null ? -1 : actual.getIdInvitado();
        selectorEvento.getItems().setAll(invitaciones);
        if (invitaciones.isEmpty()) {
            selectorEvento.setValue(null);
            mostrarEvento(null);
            mostrarMensaje("No encontramos invitaciones activas para ese correo y teléfono. "
                    + "Si el organizador no registró tu teléfono, pídele que lo agregue.", false);
            return;
        }
        correoVerificado = texto;
        telefonoVerificado = tel;
        if (alVerificar != null) alVerificar.accept(texto, tel);
        ocultarMensaje();
        InvitacionRecibida elegida = invitaciones.stream().filter(inv -> inv.getIdInvitado() == idAnterior)
                .findFirst().orElse(invitaciones.get(0));
        if (elegida == selectorEvento.getValue()) mostrarEvento(elegida);
        else selectorEvento.setValue(elegida);
    }

    private void mostrarEvento(InvitacionRecibida invitacion) {
        actual = invitacion;
        if (invitacion == null) {
            regalos = List.of();
            detalleEvento.setText("Escribe tu correo y tu teléfono y presiona Consultar para ver la mesa de regalos de tus eventos.");
        } else {
            detalleEvento.setText(invitacion.getInicio().format(FECHA) + " · " + invitacion.getLugar()
                    + " · Invitado: " + invitacion.getNombreInvitado());
            cargarRegalos();
        }
        pintarRegalos();
    }

    private void cargarRegalos() {
        try {
            regalos = controller.consultarRegalosDisponibles(actual.getIdInvitado(), correoVerificado, telefonoVerificado);
        } catch (IllegalArgumentException | IllegalStateException e) {
            regalos = List.of();
            mostrarMensaje(e.getMessage(), false);
        }
    }

    /** VP-04: si el regalo sigue disponible queda reservado a nombre del invitado. */
    private void reservar(Regalo regalo) {
        try {
            controller.reservarRegalo(actual.getIdInvitado(), correoVerificado, telefonoVerificado, regalo.getIdRegalo());
            mostrarMensaje("Reservaste \"" + regalo.getNombre() + "\". Quedó registrado a tu nombre.", true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarMensaje(e.getMessage(), false);
        }
        cargarRegalos();
        pintarRegalos();
    }

    private void cancelarReserva(Regalo regalo) {
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
        confirmar.setTitle("Cancelar reserva");
        confirmar.setHeaderText("¿Cancelar tu reserva de \"" + regalo.getNombre() + "\"?");
        confirmar.setContentText("El regalo quedará disponible para que otro invitado lo elija.");
        prepararDialogo(confirmar);
        ButtonType cancelar = new ButtonType("Cancelar reserva", ButtonBar.ButtonData.OK_DONE);
        ButtonType volver = new ButtonType("Volver", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmar.getButtonTypes().setAll(volver, cancelar);
        confirmar.getDialogPane().lookupButton(cancelar).getStyleClass().add("btn-primary");
        confirmar.getDialogPane().lookupButton(volver).getStyleClass().add("btn-secondary");
        if (confirmar.showAndWait().orElse(volver) != cancelar) return;
        try {
            controller.cancelarReservaRegalo(actual.getIdInvitado(), correoVerificado, telefonoVerificado, regalo.getIdRegalo());
            mostrarMensaje("Cancelaste tu reserva de \"" + regalo.getNombre() + "\".", true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarMensaje(e.getMessage(), false);
        }
        cargarRegalos();
        pintarRegalos();
    }

    /** Estado del regalo para este invitado: disponible, reservado por otro o reservado por él. */
    private String estadoVista(Regalo regalo) {
        if (actual != null && regalo.estaReservadoPor(actual.getIdInvitado())) return MIO;
        return regalo.estaReservado() ? RESERVADO : DISPONIBLE;
    }

    private void pintarRegalos() {
        String texto = buscador.getText() == null ? "" : buscador.getText().trim().toLowerCase(ES);
        String vista = filtro.getValue() == null ? "Todos" : filtro.getValue();
        tarjetas.getChildren().clear();
        int disponibles = 0, reservados = 0, mios = 0;
        for (Regalo regalo : regalos) {
            String estado = estadoVista(regalo);
            switch (estado) {
                case DISPONIBLE -> disponibles++;
                case MIO -> mios++;
                default -> reservados++;
            }
            String descripcion = regalo.getDescripcion() == null ? "" : regalo.getDescripcion();
            boolean coincideTexto = texto.isEmpty() || (regalo.getNombre() + " " + descripcion).toLowerCase(ES).contains(texto);
            boolean coincideFiltro = switch (vista) {
                case "Disponibles" -> estado.equals(DISPONIBLE);
                case "Reservados" -> estado.equals(RESERVADO);
                case "Mis reservas" -> estado.equals(MIO);
                default -> true;
            };
            if (coincideTexto && coincideFiltro && dentroDelPresupuesto(regalo.getPrecioEstimado()))
                tarjetas.getChildren().add(tarjetaRegalo(regalo, estado));
        }
        if (tarjetas.getChildren().isEmpty()) {
            String vacio = actual == null ? "Consulta con tu correo y tu teléfono para ver los regalos."
                    : regalos.isEmpty() ? "El organizador todavía no ha agregado regalos a esta mesa."
                    : "Ningún regalo coincide con la búsqueda.";
            tarjetas.getChildren().add(etiqueta(vacio, "muted"));
        }
        disponiblesValor.setText(String.valueOf(disponibles));
        reservadosValor.setText(String.valueOf(reservados));
        misReservasValor.setText(String.valueOf(mios));
        pintarMisReservas();
    }

    private boolean dentroDelPresupuesto(BigDecimal precio) {
        if (precio == null) return true;
        double valor = precio.doubleValue();
        String rango = presupuesto.getValue() == null ? PRESUPUESTOS[0] : presupuesto.getValue();
        if (rango.equals(PRESUPUESTOS[1])) return valor <= 30000;
        if (rango.equals(PRESUPUESTOS[2])) return valor > 30000 && valor <= 60000;
        if (rango.equals(PRESUPUESTOS[3])) return valor > 60000 && valor <= 100000;
        if (rango.equals(PRESUPUESTOS[4])) return valor > 100000;
        return true;
    }

    private Node tarjetaRegalo(Regalo regalo, String estadoVista) {
        Label inicial = new Label(regalo.getNombre().substring(0, 1).toUpperCase(ES));
        inicial.getStyleClass().add("gift-thumb");
        inicial.setMaxWidth(Double.MAX_VALUE);
        Label estado = pill(estadoVista);
        StackPane imagen = new StackPane(inicial, estado);
        StackPane.setAlignment(estado, Pos.TOP_RIGHT);
        StackPane.setMargin(estado, new Insets(8));
        Label nombre = etiqueta(regalo.getNombre(), "summary-value");
        nombre.setStyle("-fx-font-size: 14px;");
        nombre.setWrapText(true);
        Label descripcion = etiqueta(regalo.getDescripcion() == null ? "" : regalo.getDescripcion(), "hint");
        descripcion.setWrapText(true);
        descripcion.setMinHeight(32);
        Label precio = etiqueta(precio(regalo.getPrecioEstimado()), "gift-price");

        Button accion = new Button();
        accion.setMaxWidth(Double.MAX_VALUE);
        switch (estadoVista) {
            case DISPONIBLE -> {
                accion.setText("Reservar");
                accion.getStyleClass().add("btn-primary");
                accion.setOnAction(e -> reservar(regalo));
            }
            case MIO -> {
                accion.setText("Cancelar reserva");
                accion.getStyleClass().add("btn-secondary");
                accion.setOnAction(e -> cancelarReserva(regalo));
            }
            default -> {
                accion.setText("No disponible");
                accion.getStyleClass().add("btn-secondary");
                accion.setDisable(true);
            }
        }
        VBox tarjeta = new VBox(10, imagen, nombre, descripcion, precio, accion);
        tarjeta.getStyleClass().add("gift-card");
        if (estadoVista.equals(MIO)) tarjeta.getStyleClass().add("mine");
        tarjeta.setPrefWidth(ANCHO_TARJETA);
        tarjeta.setMaxWidth(ANCHO_TARJETA);
        return tarjeta;
    }

    private void pintarMisReservas() {
        misReservas.getChildren().clear();
        for (Regalo regalo : regalos) {
            if (!estadoVista(regalo).equals(MIO)) continue;
            Label nombre = etiqueta(regalo.getNombre(), "summary-value");
            nombre.setWrapText(true);
            VBox textos = new VBox(4, nombre, etiqueta(precio(regalo.getPrecioEstimado()), "hint"));
            textos.setMinWidth(0);
            HBox.setHgrow(textos, Priority.ALWAYS);
            Button cancelar = new Button("Cancelar");
            cancelar.getStyleClass().addAll("btn-danger", "btn-small");
            cancelar.setMinWidth(Region.USE_PREF_SIZE);
            cancelar.setOnAction(e -> cancelarReserva(regalo));
            HBox item = new HBox(10, textos, cancelar);
            item.setAlignment(Pos.CENTER_LEFT);
            item.getStyleClass().add("invitation-item");
            misReservas.getChildren().add(item);
        }
        if (misReservas.getChildren().isEmpty()) {
            Label vacio = etiqueta("Todavía no has reservado regalos.", "muted");
            vacio.setWrapText(true);
            misReservas.getChildren().add(vacio);
        }
    }

    private String precio(BigDecimal valor) {
        return valor == null ? "-" : "$" + pesos.format(valor);
    }

    private void prepararDialogo(Dialog<?> dialogo) {
        URL css = getClass().getResource(CSS);
        if (css != null) dialogo.getDialogPane().getStylesheets().add(css.toExternalForm());
        if (correo.getScene() != null) dialogo.initOwner(correo.getScene().getWindow());
    }

    private Label pill(String estado) {
        String texto = switch (estado) {
            case DISPONIBLE -> "DISPONIBLE";
            case MIO -> "TU RESERVA";
            default -> "RESERVADO";
        };
        String clase = switch (estado) {
            case DISPONIBLE -> "status-confirmada";
            case MIO -> "status-enviada";
            default -> "status-revocada";
        };
        Label pill = new Label(texto);
        pill.getStyleClass().setAll("label", "status", clase);
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
    // Utilidades (mismas que las otras vistas)
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
