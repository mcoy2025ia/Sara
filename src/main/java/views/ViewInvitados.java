package views;

import java.math.BigDecimal;
import java.net.URL;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import controllers.ControllerInvitados;
import entities.Evento;
import entities.Invitacion;
import entities.Invitado;
import entities.Lugar;
import entities.Regalo;
import entities.SesionUsuario;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import views.components.MenuCompartido;

/**
 * CU-17 - Gestionar invitaciones del evento.
 *
 * Sigue el mismo patrón que ViewEventos: no extiende Application; construye la
 * pantalla en crearPantalla() y quien la abre la pone en una Scene. Los datos
 * llegan de H2 por ControllerInvitados -> ServiceInvitados -> Repository. Esta
 * clase no contiene SQL ni reglas de negocio.
 */
public final class ViewInvitados {

    private static final String CSS = "/styles/invitados.css";
    private static final double ANCHO_PANEL = 270;
    /** Alto de cada fila de la tabla de invitaciones (dos filas de botones en Acciones). */
    private static final double ALTO_FILA = 86;
    /**
     * Ancho de ventana desde el cual el panel derecho va al lado de la tabla.
     */
    private static final double ANCHO_PANEL_LATERAL = 1400;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA_LARGA
            = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));

    /**
     * Fila de la tabla: la invitación con el nombre del lugar y sus asistentes.
     */
    public record FilaInvitacion(Invitacion invitacion, String nombreLugar, int asistentes) {

        int id() {
            return invitacion.getIdInvitacion();
        }

        String estado() {
            return invitacion.getEstadoRespuesta() == null ? "" : invitacion.getEstadoRespuesta();
        }

        boolean activa() {
            return invitacion.estaActiva();
        }
    }

    private final ControllerInvitados controller;
    private final SesionUsuario sesion;
    private final Runnable abrirEventos;
    private final Runnable abrirInvitados;
    private final Runnable abrirCatalogo;
    private final Runnable cerrarSesion;
    private final Runnable abrirMisEventos;

    private final ObservableList<FilaInvitacion> filas = FXCollections.observableArrayList();
    private final FilteredList<FilaInvitacion> filtradas = new FilteredList<>(filas, fila -> true);
    private final Map<Integer, Lugar> lugares = new HashMap<>();

    private final ComboBox<Evento> selectorEvento = new ComboBox<>();
    private final TextField buscador = new TextField();
    private final TableView<FilaInvitacion> tabla = new TableView<>(filtradas);
    private final Label cantidadRegistros = new Label();
    private final Label mensaje = new Label();

    private final Label totalValor = new Label("0");
    private final Label enviadasValor = new Label("0");
    private final Label pendientesValor = new Label("0");
    private final Label respuestasValor = new Label("0");

    private final Label detalleNombre = new Label();
    private final Label detalleFecha = new Label();
    private final Label detalleLugar = new Label();
    private final Label detalleAforo = new Label();

    public ViewInvitados(ControllerInvitados controller) {
        this(controller, null, null, null, null, null);
    }

    public ViewInvitados(
            ControllerInvitados controller,
            Runnable abrirCatalogo,
            Runnable cerrarSesion) {

        this(controller, null, null, abrirCatalogo, cerrarSesion, null);
    }

    public ViewInvitados(
            ControllerInvitados controller,
            Runnable abrirEventos,
            Runnable abrirCatalogo,
            Runnable cerrarSesion,
            SesionUsuario sesion) {

        this(
                controller,
                abrirEventos,
                null,
                abrirCatalogo,
                cerrarSesion,
                sesion);
    }

    public ViewInvitados(
            ControllerInvitados controller,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable cerrarSesion,
            SesionUsuario sesion) {

        this(controller, abrirEventos, abrirInvitados, abrirCatalogo,
                cerrarSesion, sesion, null);
    }

    public ViewInvitados(
            ControllerInvitados controller,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable cerrarSesion,
            SesionUsuario sesion,
            Runnable abrirMisEventos) {

        this.controller = controller;
        this.abrirEventos = abrirEventos;
        this.abrirInvitados = abrirInvitados;
        this.abrirCatalogo = abrirCatalogo;
        this.cerrarSesion = cerrarSesion;
        this.sesion = sesion;
        this.abrirMisEventos = abrirMisEventos;

        selectorEvento.setId("selectorEvento");
        selectorEvento.setPromptText("Selecciona un evento");
        selectorEvento.setPrefWidth(280);
        selectorEvento.setMinWidth(200);
        selectorEvento.setConverter(new StringConverter<>() {
            @Override
            public String toString(Evento evento) {
                return evento == null ? "" : evento.getNombre();
            }

            @Override
            public Evento fromString(String texto) {
                return null;
            }
        });
        selectorEvento.setPlaceholder(new Label("No hay eventos registrados"));
        selectorEvento.valueProperty().addListener((obs, anterior, actual) -> cargarInvitaciones());

        buscador.setId("buscador");
        buscador.setPromptText("Buscar invitación...");
        buscador.setPrefWidth(230);
        buscador.setMinWidth(150);
        buscador.textProperty().addListener((obs, anterior, actual) -> actualizarVista());

        mensaje.setId("mensaje");
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);

        cantidadRegistros.setId("cantidadRegistros");
        configurarTabla();
    }

    // ------------------------------------------------------------------
    // Construcción de la pantalla
    // ------------------------------------------------------------------
    public Parent crearPantalla() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");
        conectarCss(raiz);

        VBox sidebar = crearMenu();
        raiz.setLeft(sidebar);

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());

        VBox contenido = new VBox(25);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1320);

        VBox titulo = new VBox(8, etiqueta("Invitaciones", "page-title"),
                etiqueta("Gestiona las invitaciones enviadas para tus eventos.", "muted"));

        VBox tablaCard = crearTarjetaTabla();
        tablaCard.setMinWidth(0);
        tablaCard.setMaxHeight(Region.USE_PREF_SIZE);
        GridPane.setValignment(tablaCard, VPos.TOP);

        VBox tarjetaDetalles = crearTarjetaDetalles();
        VBox tarjetaAcciones = crearTarjetaAcciones();
        VBox tarjetaInfo = informacion("Información importante",
                "Cada invitado puede tener una sola invitación activa por evento y el total de invitados y "
                + "acompañantes no puede superar el aforo del lugar. Revocar una invitación libera sus cupos.");
        tarjetaInfo.getStyleClass().add("highlight");
        Region[] tarjetasPanel = {tarjetaDetalles, tarjetaAcciones, tarjetaInfo};

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
        contenido.getChildren().addAll(titulo, mensaje, crearTarjetaEvento(), estadisticas, columnas);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("content-scroll");
        principal.setCenter(scroll);
        raiz.setCenter(principal);

        // Igual que ViewEventos: con ventana angosta el panel baja debajo de la tabla
        // (sus tarjetas quedan en fila) y con ventana muy angosta se oculta el menú.
        Runnable[] organizar = new Runnable[1];
        boolean[] lateralActual = {false};
        boolean[] inicializado = {false};
        organizar[0] = () -> {
            double ancho = raiz.getWidth() <= 0 ? 1440 : raiz.getWidth();
            organizarEstadisticas(estadisticas, ancho >= 1280);
            boolean lateral = ancho >= ANCHO_PANEL_LATERAL;
            if (inicializado[0] && lateral == lateralActual[0]) return;
            inicializado[0] = true;
            lateralActual[0] = lateral;
            panelVertical.getChildren().clear();
            panelHorizontal.getChildren().clear();
            columnas.getChildren().clear();
            columnas.getColumnConstraints().setAll(izquierda);
            columnas.add(tablaCard, 0, 0);
            GridPane.setHgrow(tablaCard, Priority.ALWAYS);
            if (lateral) {
                columnas.getColumnConstraints().add(derecha);
                for (Region tarjeta : tarjetasPanel) {
                    tarjeta.setMaxWidth(Double.MAX_VALUE);
                    tarjeta.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    HBox.setHgrow(tarjeta, Priority.NEVER);
                }
                panelVertical.getChildren().addAll(tarjetasPanel);
                columnas.add(panelVertical, 1, 0);
            } else {
                for (Region tarjeta : tarjetasPanel) {
                    tarjeta.setMaxWidth(Double.MAX_VALUE);
                    tarjeta.setPrefWidth(0);
                    HBox.setHgrow(tarjeta, Priority.ALWAYS);
                }
                panelHorizontal.getChildren().addAll(tarjetasPanel);
                columnas.add(panelHorizontal, 0, 1);
                GridPane.setHgrow(panelHorizontal, Priority.ALWAYS);
            }
        };
        organizar[0].run();
        raiz.widthProperty().addListener((obs, anterior, actual) -> {
            organizar[0].run();
            sidebar.setVisible(actual.doubleValue() >= 1000);
            sidebar.setManaged(sidebar.isVisible());
        });
        // Al mostrarse (también al volver desde otra pantalla del menú) se recargan los datos de H2.
        raiz.sceneProperty().addListener((obs, anterior, actual) -> {
            if (actual != null) recargar();
        });
        return raiz;
    }

    private void conectarCss(Parent raiz) {
        URL css = getClass().getResource(CSS);
        if (css != null) {
            raiz.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("[ViewInvitados] No se encontró " + CSS + " en el classpath.");
        }
    }

    private VBox crearMenu() {
        return MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.INVITADOS,
                abrirEventos,
                abrirInvitados,
                abrirCatalogo,
                null,
                cerrarSesion,
                abrirMisEventos);
    }

    private HBox crearBarraSuperior() {
        return MenuCompartido.crearBarraSuperior(sesion, "Invitados");
    }

    private VBox crearTarjetaEvento() {
        Label ayuda = etiqueta("Selecciona un evento para gestionar sus invitaciones.", "muted");
        ayuda.setWrapText(true);
        VBox textos = new VBox(6, etiqueta("Evento seleccionado", "card-title"), ayuda);
        textos.setMinWidth(0);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button nueva = botonNuevaInvitacion("nuevaInvitacion");
        nueva.setMinWidth(Region.USE_PREF_SIZE);

        HBox fila = new HBox(16, etiqueta("▦", "heading-icon"), textos, selectorEvento, nueva);
        fila.setAlignment(Pos.CENTER_LEFT);
        VBox tarjeta = new VBox(fila);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private Button botonNuevaInvitacion(String id) {
        Button nueva = new Button("+ Nueva invitación");
        nueva.setId(id);
        nueva.getStyleClass().add("btn-primary");
        nueva.disableProperty().bind(selectorEvento.valueProperty().isNull());
        nueva.setOnAction(e -> abrirFormularioInvitacion(null));
        return nueva;
    }

    private GridPane crearEstadisticas() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.getChildren().addAll(
                estadistica("Total invitaciones", totalValor, "Registradas", "▤"),
                estadistica("Enviadas", enviadasValor, "Vigentes (no revocadas)", "➤"),
                estadistica("Pendientes", pendientesValor, "Sin respuesta", "◷"),
                estadistica("Respuestas", respuestasValor, "Aceptadas o rechazadas", "✓"));
        organizarEstadisticas(grid, true);
        return grid;
    }

    /**
     * Cuatro tarjetas en fila; en ventanas angostas, cuadrícula de 2 x 2.
     */
    private void organizarEstadisticas(GridPane grid, boolean enFila) {
        int columnas = enFila ? 4 : 2;
        if (grid.getColumnConstraints().size() == columnas) return;
        grid.getColumnConstraints().clear();
        for (int i = 0; i < columnas; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(100.0 / columnas);
            columna.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(columna);
        }
        for (int i = 0; i < grid.getChildren().size(); i++) {
            Node tarjeta = grid.getChildren().get(i);
            GridPane.setConstraints(tarjeta, i % columnas, i / columnas);
        }
    }

    private VBox estadistica(String titulo, Label valor, String descripcion, String icono) {
        valor.getStyleClass().add("stat-value");
        Label nombre = etiqueta(titulo, "field-label");
        nombre.setWrapText(true);
        nombre.setMinWidth(0);
        HBox encabezado = new HBox(10, nombre, espacioHorizontal(), etiqueta(icono, "heading-icon"));
        encabezado.setAlignment(Pos.CENTER_LEFT);
        VBox tarjeta = new VBox(8, encabezado, valor, etiqueta(descripcion, "hint"));
        tarjeta.getStyleClass().add("info-card");
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        tarjeta.setMinWidth(0);
        return tarjeta;
    }

    private VBox crearTarjetaTabla() {
        VBox textos = new VBox(6, etiqueta("Invitaciones", "card-title"),
                etiqueta("Lista de invitaciones del evento seleccionado.", "muted"));
        textos.setMinWidth(0);
        cantidadRegistros.getStyleClass().add("user-badge");
        cantidadRegistros.setMinWidth(Region.USE_PREF_SIZE);
        HBox encabezado = new HBox(14, textos, espacioHorizontal(), buscador, cantidadRegistros);
        encabezado.setAlignment(Pos.CENTER_LEFT);

        VBox tarjeta = new VBox(22, encabezado, tabla);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    @SuppressWarnings("deprecation") // CONSTRAINED_RESIZE_POLICY sigue funcionando en JavaFX 17 y 21
    private static void ajustarColumnas(TableView<?> tabla) {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void configurarTabla() {
        tabla.setId("tablaInvitaciones");
        ajustarColumnas(tabla);
        tabla.setFixedCellSize(ALTO_FILA);
        tabla.setPlaceholder(etiqueta("No hay invitaciones para mostrar.", "muted"));
        // La tabla crece con sus filas; el desplazamiento lo hace el ScrollPane de la página.
        tabla.prefHeightProperty().bind(Bindings.createDoubleBinding(
                () -> 50 + ALTO_FILA * Math.max(filtradas.size(), 2), filtradas));
        tabla.minHeightProperty().bind(tabla.prefHeightProperty());
        tabla.maxHeightProperty().bind(tabla.prefHeightProperty());

        TableColumn<FilaInvitacion, String> id = columnaTexto("ID", fila -> String.valueOf(fila.id()), 40, 45);
        TableColumn<FilaInvitacion, String> texto = columnaTexto("Mensaje",
                fila -> fila.invitacion().getMensaje() == null ? "" : fila.invitacion().getMensaje(), 95, 180);
        TableColumn<FilaInvitacion, String> lugar = columnaTexto("Lugar", FilaInvitacion::nombreLugar, 90, 115);
        TableColumn<FilaInvitacion, String> fecha = columnaTexto("Fecha de envío",
                fila -> fila.invitacion().getFechaEnvio().format(FORMATO_FECHA), 108, 115);

        TableColumn<FilaInvitacion, String> estado = columnaTexto("Estado", FilaInvitacion::estado, 95, 100);
        estado.setCellFactory(col -> new TableCell<>() {
            private final Label pill = new Label();

            {
                pill.setMinWidth(Region.USE_PREF_SIZE);
            }

            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null) {
                    setGraphic(null);
                } else {
                    pill.setText(valor);
                    pill.getStyleClass().setAll("label", "status", claseEstado(valor));
                    setGraphic(pill);
                }
                setText(null);
            }
        });

        TableColumn<FilaInvitacion, FilaInvitacion> acciones = new TableColumn<>("Acciones");
        acciones.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        acciones.setMinWidth(222);
        acciones.setPrefWidth(222);
        acciones.setSortable(false);
        acciones.setCellFactory(col -> new TableCell<>() {
            private final Button invitados = new Button("Invitados");
            private final Button regalos = new Button("Regalos");
            private final Button editar = new Button("Editar");
            private final Button revocar = new Button("Revocar");
            private final GridPane caja = new GridPane();
            {
                invitados.getStyleClass().addAll("btn-secondary", "btn-small");
                regalos.getStyleClass().addAll("btn-secondary", "btn-small");
                editar.getStyleClass().addAll("btn-secondary", "btn-small");
                revocar.getStyleClass().addAll("btn-danger", "btn-small");
                for (Button boton : new Button[]{invitados, regalos, editar, revocar}) {
                    boton.setMinWidth(Region.USE_PREF_SIZE);
                    boton.setMaxWidth(Double.MAX_VALUE);
                    GridPane.setHgrow(boton, Priority.ALWAYS);
                }
                // Dos filas de botones para que la tabla no se salga de la tarjeta.
                caja.setHgap(6);
                caja.setVgap(6);
                caja.addRow(0, invitados, regalos);
                caja.addRow(1, editar, revocar);
                caja.setAlignment(Pos.CENTER_LEFT);
                invitados.setOnAction(e -> abrirInvitados(getItem()));
                regalos.setOnAction(e -> abrirRegalos(getItem()));
                editar.setOnAction(e -> abrirFormularioInvitacion(getItem()));
                revocar.setOnAction(e -> revocar(getItem()));
            }

            @Override
            protected void updateItem(FilaInvitacion fila, boolean vacio) {
                super.updateItem(fila, vacio);
                if (!vacio && fila != null) {
                    invitados.setText("Invitados (" + fila.asistentes() + ")");
                    editar.setDisable(!fila.activa());
                    revocar.setDisable(!fila.activa());
                }
                setGraphic(vacio || fila == null ? null : caja);
                setText(null);
            }
        });

        tabla.getColumns().addAll(List.of(id, texto, lugar, fecha, estado, acciones));
    }

    private <T> TableColumn<T, String> columnaTexto(String titulo, Function<T, String> valor, double min, double pref) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new SimpleStringProperty(valor.apply(c.getValue())));
        columna.setMinWidth(min);
        columna.setPrefWidth(pref);
        // Si la columna queda angosta, el texto baja a una segunda línea en vez de cortarse con "...".
        columna.setCellFactory(col -> {
            TableCell<T, String> celda = new TableCell<>() {
                @Override
                protected void updateItem(String texto, boolean vacio) {
                    super.updateItem(texto, vacio);
                    setText(vacio ? null : texto);
                }
            };
            celda.setWrapText(true);
            return celda;
        });
        return columna;
    }

    private VBox crearTarjetaDetalles() {
        detalleNombre.getStyleClass().add("section-title");
        detalleNombre.setWrapText(true);
        detalleAforo.setId("detalleAforo");
        Button ver = new Button("Ver detalles del evento");
        ver.setId("verDetallesEvento");
        ver.getStyleClass().add("btn-secondary");
        ver.setMaxWidth(Double.MAX_VALUE);
        ver.setWrapText(true);
        ver.disableProperty().bind(selectorEvento.valueProperty().isNull());
        ver.setOnAction(e -> mostrarDetalleEvento());
        VBox tarjeta = new VBox(14, etiqueta("Detalles del evento", "card-title"), detalleNombre,
                filaResumen("Fecha", detalleFecha), new Separator(),
                filaResumen("Lugar", detalleLugar), new Separator(),
                filaResumen("Aforo", detalleAforo), ver);
        tarjeta.getStyleClass().add("info-card");
        return tarjeta;
    }

    private VBox crearTarjetaAcciones() {
        Label texto = etiqueta("Gestiona las invitaciones de manera rápida y sencilla.", "muted");
        texto.setWrapText(true);
        Button nueva = botonNuevaInvitacion("nuevaInvitacionRapida");
        nueva.setMaxWidth(Double.MAX_VALUE);
        VBox tarjeta = new VBox(14, etiqueta("Acciones rápidas", "card-title"), texto, nueva);
        tarjeta.getStyleClass().add("info-card");
        return tarjeta;
    }

    // ------------------------------------------------------------------
    // Carga de datos (Controller)
    // ------------------------------------------------------------------
    /**
     * Recarga eventos y lugares conservando el evento seleccionado.
     */
    public void recargar() {
        try {
            Integer seleccionado = selectorEvento.getValue() == null ? null : selectorEvento.getValue().getIdEvento();
            lugares.clear();
            for (Lugar lugar : controller.listarLugares()) {
                lugares.put(lugar.getIdLugar(), lugar);
            }
            List<Evento> eventos = controller.listarEventos();
            selectorEvento.getItems().setAll(eventos);
            Evento elegir = eventos.stream().filter(e -> e.getIdEvento().equals(seleccionado)).findFirst()
                    .orElse(eventos.isEmpty() ? null : eventos.get(0));
            if (elegir == selectorEvento.getValue()) {
                cargarInvitaciones();
            } else {
                selectorEvento.setValue(elegir);
            }
        } catch (IllegalStateException e) {
            mostrarMensaje(e.getMessage(), false);
        }
    }

    private void cargarInvitaciones() {
        Evento evento = selectorEvento.getValue();
        if (evento == null) {
            filas.clear();
            actualizarVista();
            return;
        }
        try {
            Map<Integer, Integer> asistentes = controller.contarAsistentesPorInvitacion(evento.getIdEvento());
            filas.setAll(controller.listarInvitaciones(evento.getIdEvento()).stream()
                    .map(inv -> new FilaInvitacion(inv, nombreLugar(inv.getIdLugar()),
                            asistentes.getOrDefault(inv.getIdInvitacion(), 0)))
                    .toList());
        } catch (IllegalStateException e) {
            filas.clear();
            mostrarMensaje(e.getMessage(), false);
        }
        actualizarVista();
    }

    // ------------------------------------------------------------------
    // Comportamiento
    // ------------------------------------------------------------------
    private void actualizarVista() {
        String[] palabras = normalizar(buscador.getText()).split("\\s+");
        filtradas.setPredicate(fila -> coincide(fila, palabras));

        int n = filtradas.size();
        cantidadRegistros.setText(n + (n == 1 ? " registro" : " registros"));

        int enviadas = 0, pendientes = 0, respuestas = 0;
        for (FilaInvitacion fila : filas) {
            if (!fila.activa()) {
                continue;
            }
            enviadas++;
            switch (fila.estado().toUpperCase(Locale.ROOT)) {
                case Invitacion.ESTADO_ENVIADA ->
                    pendientes++;
                case Invitacion.ESTADO_ACEPTADA, Invitacion.ESTADO_RECHAZADA ->
                    respuestas++;
                default -> {
                }
            }
        }
        totalValor.setText(String.valueOf(filas.size()));
        enviadasValor.setText(String.valueOf(enviadas));
        pendientesValor.setText(String.valueOf(pendientes));
        respuestasValor.setText(String.valueOf(respuestas));
        actualizarDetalle();
    }

    private void actualizarDetalle() {
        Evento evento = selectorEvento.getValue();
        if (evento == null) {
            detalleNombre.setText("Sin eventos");
            detalleFecha.setText("-");
            detalleLugar.setText("-");
            detalleAforo.setText("-");
            return;
        }
        detalleNombre.setText(evento.getNombre());
        detalleFecha.setText(evento.getFechaInicio().format(FORMATO_FECHA_LARGA));
        detalleLugar.setText(evento.getUbicacion() == null || evento.getUbicacion().isBlank() ? "-" : evento.getUbicacion());
        Lugar lugar = lugarPrincipal(evento);
        if (lugar == null) {
            detalleAforo.setText("Sin lugar asignado");
            return;
        }
        try {
            int ocupados = controller.contarAsistentes(evento.getIdEvento(), lugar.getIdLugar());
            detalleAforo.setText(ocupados + " de " + lugar.getCapacidad());
        } catch (IllegalStateException e) {
            detalleAforo.setText("-");
        }
    }

    /**
     * Lugar del evento: el registrado con el nombre de su ubicación o, si no,
     * el de su primera invitación vigente.
     */
    private Lugar lugarPrincipal(Evento evento) {
        Lugar porNombre = lugarPorNombre(evento.getUbicacion());
        if (porNombre != null) {
            return porNombre;
        }
        return filas.stream().filter(FilaInvitacion::activa).map(f -> lugares.get(f.invitacion().getIdLugar()))
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }

    private Lugar lugarPorNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        String buscado = normalizar(nombre);
        return lugares.values().stream().filter(l -> normalizar(l.getNombre()).equals(buscado)).findFirst().orElse(null);
    }

    /**
     * Cada palabra escrita debe aparecer en algún dato de la invitación.
     */
    private boolean coincide(FilaInvitacion fila, String[] palabras) {
        String texto = normalizar(fila.id() + " " + fila.nombreLugar() + " "
                + fila.invitacion().getFechaEnvio().format(FORMATO_FECHA) + " " + fila.estado() + " "
                + (fila.invitacion().getMensaje() == null ? "" : fila.invitacion().getMensaje()));
        for (String palabra : palabras) {
            if (!palabra.isEmpty() && !texto.contains(palabra)) {
                return false;
            }
        }
        return true;
    }

    private String nombreLugar(int idLugar) {
        Lugar lugar = lugares.get(idLugar);
        return lugar == null ? "Lugar #" + idLugar : lugar.getNombre();
    }

    /**
     * Minúsculas y sin tildes, para que "bogota" encuentre "Bogotá".
     */
    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }

    private static String claseEstado(String estado) {
        return switch (estado.toUpperCase(Locale.ROOT)) {
            case Invitacion.ESTADO_ENVIADA ->
                "status-enviada";
            case "CONFIRMADA", "CONFIRMADO", Invitacion.ESTADO_ACEPTADA ->
                "status-confirmada";
            case Invitacion.ESTADO_RECHAZADA ->
                "status-rechazada";
            case Invitacion.ESTADO_REVOCADA ->
                "status-revocada";
            default ->
                "status-pendiente";
        };
    }

    // ------------------------------------------------------------------
    // Crear / editar invitación
    // ------------------------------------------------------------------
    /**
     * Con fila nula crea una invitación nueva; con fila, edita su lugar y
     * mensaje.
     */
    private void abrirFormularioInvitacion(FilaInvitacion fila) {
        Evento evento = selectorEvento.getValue();
        if (evento == null) {
            return;
        }
        boolean nueva = fila == null;

        ComboBox<Lugar> lugar = new ComboBox<>(FXCollections.observableArrayList(lugares.values().stream()
                .sorted(java.util.Comparator.comparing(Lugar::getNombre)).toList()));
        lugar.setId("formLugar");
        lugar.setMaxWidth(Double.MAX_VALUE);
        lugar.setPromptText("Selecciona un lugar");
        lugar.setValue(nueva ? lugarPorNombre(evento.getUbicacion()) : lugares.get(fila.invitacion().getIdLugar()));

        TextArea texto = new TextArea(nueva ? "" : fila.invitacion().getMensaje());
        texto.setId("formMensaje");
        texto.setPromptText("Ej. Te invitamos a celebrar con nosotros este día especial.");
        texto.setWrapText(true);
        texto.setPrefRowCount(4);
        Label contador = etiqueta("", "hint");
        contador.textProperty().bind(Bindings.createStringBinding(
                () -> texto.getText().length() + " / 500 caracteres", texto.textProperty()));
        Label error = mensajeDialogo();

        Label explicacion = etiqueta(nueva ? "La invitación quedará registrada como ENVIADA con la fecha y hora actuales."
                : "Invitación #" + fila.id() + " enviada el "
                + fila.invitacion().getFechaEnvio().format(FORMATO_FECHA) + ".", "muted");
        explicacion.setWrapText(true);
        explicacion.setMinHeight(Region.USE_PREF_SIZE);
        VBox contenido = new VBox(14, explicacion,
                grupo("Evento", etiqueta(evento.getNombre(), "summary-value")),
                grupo("Lugar *", lugar),
                grupo("Mensaje *", texto, contador),
                error);
        contenido.setPrefWidth(460);

        Dialog<ButtonType> dialogo = crearDialogo(nueva ? "Nueva invitación" : "Editar invitación", contenido);
        ButtonType guardar = new ButtonType(nueva ? "Crear y enviar" : "Guardar cambios", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, guardar);
        estilizarBotones(dialogo, guardar);

        Invitacion datos = new Invitacion();
        dialogo.getDialogPane().lookupButton(guardar).addEventFilter(ActionEvent.ACTION, e -> {
            datos.setIdLugar(lugar.getValue() == null ? 0 : lugar.getValue().getIdLugar());
            datos.setMensaje(texto.getText());
            try {
                if (nueva) {
                    controller.crearInvitacion(evento.getIdEvento(), datos);
                } else {
                    controller.actualizarInvitacion(fila.id(), datos);
                }
            } catch (IllegalArgumentException | IllegalStateException ex) {
                mostrarError(error, ex.getMessage());
                e.consume();
            }
        });

        if (dialogo.showAndWait().filter(guardar::equals).isPresent()) {
            cargarInvitaciones();
            if (nueva) {
                mostrarMensaje("Invitación #" + datos.getIdInvitacion() + " creada y enviada. Ahora agrega sus invitados.", true);
                filas.stream().filter(f -> f.id() == datos.getIdInvitacion()).findFirst().ifPresent(this::abrirInvitados);
            } else {
                mostrarMensaje("Invitación #" + fila.id() + " actualizada.", true);
            }
        }
    }

    private void revocar(FilaInvitacion fila) {
        if (fila == null) {
            return;
        }
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
        confirmar.setTitle("Revocar invitación");
        confirmar.setHeaderText("¿Revocar la invitación #" + fila.id() + "?");
        confirmar.setContentText("Sus " + fila.asistentes() + " asistentes dejarán de contar para el aforo "
                + "y la invitación no se podrá volver a editar.");
        prepararDialogo(confirmar);
        ButtonType revocar = new ButtonType("Revocar", ButtonBar.ButtonData.OK_DONE);
        confirmar.getButtonTypes().setAll(ButtonType.CANCEL, revocar);
        estilizarBotones(confirmar, revocar);
        if (confirmar.showAndWait().filter(revocar::equals).isEmpty()) {
            return;
        }
        try {
            controller.revocarInvitacion(fila.id());
            mostrarMensaje("Invitación #" + fila.id() + " revocada.", true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarMensaje(e.getMessage(), false);
        }
        cargarInvitaciones();
    }

    // ------------------------------------------------------------------
    // Invitados de una invitación
    // ------------------------------------------------------------------
    private void abrirInvitados(FilaInvitacion fila) {
        if (fila == null) {
            return;
        }
        Evento evento = selectorEvento.getValue();
        Lugar lugar = lugares.get(fila.invitacion().getIdLugar());
        ObservableList<Invitado> lista = FXCollections.observableArrayList();
        Label aforo = etiqueta("", "summary-value");
        Label error = mensajeDialogo();

        TableView<Invitado> tablaInvitados = new TableView<>(lista);
        tablaInvitados.setId("tablaInvitados");
        tablaInvitados.setFixedCellSize(50);
        tablaInvitados.setPrefHeight(50 + 50 * 4);
        tablaInvitados.setPlaceholder(etiqueta("Esta invitación todavía no tiene invitados.", "muted"));
        TableColumn<Invitado, Invitado> quitar = new TableColumn<>("");
        quitar.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        quitar.setSortable(false);
        quitar.setMinWidth(85);
        quitar.setPrefWidth(85);

        Runnable refrescar = () -> {
            try {
                lista.setAll(controller.listarInvitados(fila.id()));
                aforo.setText(lugar == null ? "-" : controller.contarAsistentes(evento.getIdEvento(), lugar.getIdLugar())
                        + " de " + lugar.getCapacidad() + " cupos ocupados en " + lugar.getNombre());
            } catch (IllegalStateException e) {
                mostrarError(error, e.getMessage());
            }
        };
        quitar.setCellFactory(col -> new TableCell<>() {
            private final Button boton = new Button("Quitar");

            {
                boton.getStyleClass().addAll("btn-danger", "btn-small");
                boton.setOnAction(e -> {
                    try {
                        controller.quitarInvitado(getItem().getIdInvitado());
                        error.setVisible(false);
                        error.setManaged(false);
                        refrescar.run();
                    } catch (IllegalArgumentException | IllegalStateException ex) {
                        mostrarError(error, ex.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Invitado invitado, boolean vacio) {
                super.updateItem(invitado, vacio);
                setGraphic(vacio || invitado == null || !fila.activa() ? null : boton);
            }
        });
        ajustarColumnas(tablaInvitados);
        tablaInvitados.getColumns().addAll(List.of(
                this.<Invitado>columnaTexto("Nombre", Invitado::getNombre, 120, 150),
                this.<Invitado>columnaTexto("Correo", Invitado::getCorreo, 140, 180),
                this.<Invitado>columnaTexto("Teléfono", i -> i.getTelefono() == null ? "-" : i.getTelefono(), 90, 100),
                this.<Invitado>columnaTexto("Estado", i -> i.getEstado() == null ? "-" : i.getEstado(), 90, 100),
                quitar));

        TextField nombre = campoDialogo("invNombre", "Nombre completo *");
        TextField correo = campoDialogo("invCorreo", "correo@ejemplo.com *");
        TextField telefono = campoDialogo("invTelefono", "Teléfono (lo usa el invitado para responder)");
        TextField direccion = campoDialogo("invDireccion", "Dirección");
        Button agregar = new Button("Agregar invitado");
        agregar.setId("agregarInvitado");
        agregar.getStyleClass().add("btn-primary");
        agregar.setDefaultButton(true);
        agregar.setOnAction(e -> {
            try {
                controller.agregarInvitado(fila.id(), new Invitado(nombre.getText(), correo.getText(),
                        telefono.getText(), direccion.getText()));
                for (TextField campo : new TextField[]{nombre, correo, telefono, direccion}) {
                    campo.clear();
                }
                error.setVisible(false);
                error.setManaged(false);
                nombre.requestFocus();
                refrescar.run();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                mostrarError(error, ex.getMessage());
            }
        });
        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(12);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(50);
            formulario.getColumnConstraints().add(columna);
        }
        formulario.addRow(0, nombre, correo);
        formulario.addRow(1, telefono, direccion);
        HBox barraAgregar = new HBox(agregar);
        barraAgregar.setAlignment(Pos.CENTER_RIGHT);
        VBox agregarCaja = new VBox(12, etiqueta("Agregar invitado", "section-title"), formulario, barraAgregar);
        if (!fila.activa()) {
            agregarCaja = new VBox(etiqueta("La invitación fue revocada; ya no se pueden modificar sus invitados.", "muted"));
        }

        HBox resumen = new HBox(10, etiqueta("Aforo", "hint"), espacioHorizontal(), aforo);
        resumen.setAlignment(Pos.CENTER_LEFT);
        VBox contenido = new VBox(16,
                etiqueta(evento.getNombre() + "  ·  " + (fila.invitacion().getMensaje() == null ? "" : fila.invitacion().getMensaje()), "muted"),
                resumen, tablaInvitados, new Separator(), agregarCaja, error);
        contenido.setPrefWidth(680);
        ((Label) contenido.getChildren().get(0)).setWrapText(true);

        Dialog<ButtonType> dialogo = crearDialogo("Invitados de la invitación #" + fila.id(), contenido);
        dialogo.getDialogPane().getButtonTypes().add(new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE));
        estilizarBotones(dialogo, null);
        refrescar.run();
        dialogo.showAndWait();
        cargarInvitaciones();
    }

    // ------------------------------------------------------------------
    // Mesa de regalos de una invitación (CU-20, lado del organizador)
    // ------------------------------------------------------------------
    private void abrirRegalos(FilaInvitacion fila) {
        if (fila == null) return;
        Evento evento = selectorEvento.getValue();
        NumberFormat pesos = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));
        pesos.setMaximumFractionDigits(0);
        ObservableList<Regalo> lista = FXCollections.observableArrayList();
        Label resumenValor = etiqueta("", "summary-value");
        Label error = mensajeDialogo();

        TableView<Regalo> tablaRegalos = new TableView<>(lista);
        tablaRegalos.setId("tablaRegalos");
        tablaRegalos.setFixedCellSize(50);
        tablaRegalos.setPrefHeight(66 + 50 * 4);
        tablaRegalos.setPlaceholder(etiqueta("Esta invitación todavía no tiene regalos en su mesa.", "muted"));

        Runnable refrescar = () -> {
            try {
                lista.setAll(controller.listarRegalos(fila.id()));
                long reservados = lista.stream().filter(Regalo::estaReservado).count();
                resumenValor.setText(lista.size() + (lista.size() == 1 ? " regalo" : " regalos") + "  ·  "
                        + reservados + " reservados  ·  " + (lista.size() - reservados) + " disponibles");
            } catch (IllegalStateException e) {
                mostrarError(error, e.getMessage());
            }
        };

        TableColumn<Regalo, String> estado = columnaTexto("Estado",
                r -> r.estaReservado() ? Regalo.ESTADO_RESERVADO : Regalo.ESTADO_DISPONIBLE, 100, 110);
        estado.setCellFactory(col -> new TableCell<>() {
            private final Label pill = new Label();
            { pill.setMinWidth(Region.USE_PREF_SIZE); }
            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null) {
                    setGraphic(null);
                } else {
                    pill.setText(valor);
                    pill.getStyleClass().setAll("label", "status",
                            Regalo.ESTADO_DISPONIBLE.equals(valor) ? "status-confirmada" : "status-enviada");
                    setGraphic(pill);
                }
                setText(null);
            }
        });
        TableColumn<Regalo, Regalo> quitar = new TableColumn<>("");
        quitar.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        quitar.setSortable(false);
        quitar.setMinWidth(85);
        quitar.setPrefWidth(85);
        quitar.setCellFactory(col -> new TableCell<>() {
            private final Button boton = new Button("Quitar");
            {
                boton.getStyleClass().addAll("btn-danger", "btn-small");
                boton.setOnAction(e -> {
                    try {
                        controller.quitarRegalo(getItem().getIdRegalo());
                        error.setVisible(false);
                        error.setManaged(false);
                        refrescar.run();
                    } catch (IllegalArgumentException | IllegalStateException ex) {
                        mostrarError(error, ex.getMessage());
                    }
                });
            }
            @Override
            protected void updateItem(Regalo regalo, boolean vacio) {
                super.updateItem(regalo, vacio);
                if (!vacio && regalo != null) {
                    boton.setDisable(regalo.estaReservado());
                    boton.setTooltip(regalo.estaReservado()
                            ? new Tooltip("Reservado por " + regalo.getReservadoPor() + "; no se puede quitar.") : null);
                }
                setGraphic(vacio || regalo == null || !fila.activa() ? null : boton);
            }
        });
        ajustarColumnas(tablaRegalos);
        tablaRegalos.getColumns().addAll(List.of(
                this.<Regalo>columnaTexto("Regalo", Regalo::getNombre, 130, 160),
                this.<Regalo>columnaTexto("Descripción", r -> r.getDescripcion() == null ? "-" : r.getDescripcion(), 150, 210),
                this.<Regalo>columnaTexto("Precio estimado",
                        r -> r.getPrecioEstimado() == null ? "-" : "$" + pesos.format(r.getPrecioEstimado()), 100, 110),
                estado, quitar));

        TextField nombre = campoDialogo("regNombre", "Nombre del regalo *");
        TextField precio = campoDialogo("regPrecio", "Precio estimado * (ej. 50000)");
        TextField descripcion = campoDialogo("regDescripcion", "Descripción");
        Button agregar = new Button("Agregar regalo");
        agregar.setId("agregarRegalo");
        agregar.getStyleClass().add("btn-primary");
        agregar.setDefaultButton(true);
        agregar.setOnAction(e -> {
            try {
                controller.agregarRegalo(fila.id(), new Regalo(nombre.getText(), descripcion.getText(), leerPrecio(precio.getText())));
                for (TextField campo : new TextField[]{nombre, precio, descripcion}) campo.clear();
                error.setVisible(false);
                error.setManaged(false);
                nombre.requestFocus();
                refrescar.run();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                mostrarError(error, ex.getMessage());
            }
        });
        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(12);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(50);
            formulario.getColumnConstraints().add(columna);
        }
        formulario.addRow(0, nombre, precio);
        formulario.add(descripcion, 0, 1, 2, 1);
        HBox barraAgregar = new HBox(agregar);
        barraAgregar.setAlignment(Pos.CENTER_RIGHT);
        Label nota = etiqueta("Si quieren varias unidades de algo (por ejemplo pañitos), agrégalas como regalos separados. "
                + "Los regalos reservados no se pueden quitar.", "hint");
        nota.setWrapText(true);
        nota.setMinHeight(Region.USE_PREF_SIZE);
        VBox agregarCaja = new VBox(12, etiqueta("Agregar regalo", "section-title"), formulario, nota, barraAgregar);
        if (!fila.activa()) {
            agregarCaja = new VBox(etiqueta("La invitación fue revocada; ya no se puede modificar su mesa de regalos.", "muted"));
        }

        HBox resumen = new HBox(10, etiqueta("Mesa de regalos", "hint"), espacioHorizontal(), resumenValor);
        resumen.setAlignment(Pos.CENTER_LEFT);
        Label subtitulo = etiqueta(evento.getNombre() + "  ·  "
                + (fila.invitacion().getMensaje() == null ? "" : fila.invitacion().getMensaje()), "muted");
        subtitulo.setWrapText(true);
        VBox contenido = new VBox(16, subtitulo, resumen, tablaRegalos, new Separator(), agregarCaja, error);
        contenido.setPrefWidth(720);

        Dialog<ButtonType> dialogo = crearDialogo("Regalos de la invitación #" + fila.id(), contenido);
        dialogo.getDialogPane().getButtonTypes().add(new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE));
        estilizarBotones(dialogo, null);
        refrescar.run();
        dialogo.showAndWait();
    }

    /** Convierte el precio escrito (acepta "50000", "50.000" o "$50.000") a número; vacío o inválido da null. */
    private static BigDecimal leerPrecio(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String limpio = texto.replace("$", "").replace(" ", "");
        // Formato colombiano con puntos de miles (50.000 o 1.250.000,50); si no, el punto o la coma son decimales.
        limpio = limpio.matches("\\d{1,3}(\\.\\d{3})+(,\\d+)?")
                ? limpio.replace(".", "").replace(",", ".") : limpio.replace(",", ".");
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Escribe el precio estimado solo con números, por ejemplo 50000.");
        }
    }

    // ------------------------------------------------------------------
    // Mensajes y diálogos
    // ------------------------------------------------------------------
    private void mostrarDetalleEvento() {
        Evento evento = selectorEvento.getValue();
        if (evento == null) {
            return;
        }
        mostrarAviso("Detalles del evento", evento.getNombre()
                + "\n\nTipo: " + valor(evento.getTipo())
                + "\nInicio: " + evento.getFechaInicio().format(FORMATO_FECHA)
                + "\nFin: " + evento.getFechaFin().format(FORMATO_FECHA)
                + "\nUbicación: " + valor(evento.getUbicacion())
                + "\nEstado: " + valor(evento.getEstado())
                + "\nInvitados iniciales: " + (evento.getInvitadosIniciales() == null ? "-" : evento.getInvitadosIniciales())
                + "\nDescripción: " + valor(evento.getDescripcion()));
    }

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }

    /**
     * Mensaje de éxito o error debajo del título (equivalente a mostrarMensaje
     * del diagrama de clases).
     */
    public void mostrarMensaje(String texto, boolean exito) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("label", "message", exito ? "success" : "error");
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private Label mensajeDialogo() {
        Label error = new Label();
        error.setWrapText(true);
        error.setMinHeight(Region.USE_PREF_SIZE);
        error.getStyleClass().setAll("label", "message", "error");
        error.setVisible(false);
        error.setManaged(false);
        return error;
    }

    private void mostrarError(Label error, String texto) {
        error.setText(texto);
        error.setVisible(true);
        error.setManaged(true);
        // El diálogo crece para que el mensaje se lea completo.
        if (error.getScene() != null && error.getScene().getWindow() != null) {
            error.getScene().getWindow().sizeToScene();
        }
    }

    private Dialog<ButtonType> crearDialogo(String titulo, Node contenido) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.getDialogPane().setContent(contenido);
        prepararDialogo(dialogo);
        return dialogo;
    }

    private void prepararDialogo(Dialog<?> dialogo) {
        if (tabla.getScene() != null) {
            dialogo.initOwner(tabla.getScene().getWindow());
        }
        URL css = getClass().getResource(CSS);
        if (css != null) {
            dialogo.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
    }

    private void estilizarBotones(Dialog<?> dialogo, ButtonType principal) {
        for (ButtonType tipo : dialogo.getDialogPane().getButtonTypes()) {
            Node boton = dialogo.getDialogPane().lookupButton(tipo);
            boton.getStyleClass().add(tipo == principal ? "btn-primary" : "btn-secondary");
        }
    }

    private TextField campoDialogo(String id, String ayuda) {
        TextField campo = new TextField();
        campo.setId(id);
        campo.setPromptText(ayuda);
        campo.setMaxWidth(Double.MAX_VALUE);
        return campo;
    }

    private VBox grupo(String titulo, Node... controles) {
        VBox grupo = new VBox(8, etiqueta(titulo, "field-label"));
        grupo.getChildren().addAll(controles);
        return grupo;
    }

    private void mostrarAviso(String titulo, String texto) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(texto);
        prepararDialogo(alerta);
        estilizarBotones(alerta, ButtonType.OK);
        alerta.showAndWait();
    }

    // ------------------------------------------------------------------
    // Utilidades (mismas que ViewEventos)
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

    private HBox filaResumen(String titulo, Label valor) {
        valor.getStyleClass().add("summary-value");
        valor.setWrapText(true);
        valor.setMinWidth(0);
        valor.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);
        Label nombre = etiqueta(titulo, "hint");
        nombre.setMinWidth(Region.USE_PREF_SIZE);
        HBox fila = new HBox(10, nombre, espacioHorizontal(), valor);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }
}
