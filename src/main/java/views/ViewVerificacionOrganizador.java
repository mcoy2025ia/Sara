
package views;

import controllers.ControllerVerificacionOrganizador;
import entities.BitacoraOrganizador;
import entities.DocumentoOrganizador;
import entities.EvaluacionOrganizador;
import entities.MensajeOrganizador;
import entities.Organizador;
import entities.SesionUsuario;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vista del caso de uso CU-030:
 * Verificar, categorizar y aprobar organizador.
 *
 * Permite consultar perfiles pendientes, revisar documentos,
 * evaluar el organizador, solicitar correcciones y registrar
 * una decision administrativa.
 */
public class ViewVerificacionOrganizador {

    private final ControllerVerificacionOrganizador controller;
    private final SesionUsuario sesion;

    private final ListView<Organizador> listaPendientes =
            new ListView<>();

    private final ListView<DocumentoOrganizador> listaDocumentos =
            new ListView<>();

    private final ListView<MensajeOrganizador> listaMensajes =
            new ListView<>();

    private final Label nombreOrganizador =
            new Label("Selecciona un organizador");

    private final Label empresa = new Label();
    private final Label documentoIdentidad = new Label();
    private final Label telefono = new Label();
    private final Label experiencia = new Label();
    private final Label especialidad = new Label();
    private final Label portafolio = new Label();
    private final Label tarifa = new Label();
    private final Label presupuestoMaximo = new Label();
    private final Label estadoActual = new Label();

    private final Label estadoPerfil =
            new Label("Pendiente de revisión");

    private final Label estadoDocumentos =
            new Label("Sin revisar");

    private final Label estadoIdentidad =
            new Label("Sin revisar");

    private final Label resultadoEvaluacion =
            new Label("Todavía no se ha calculado el puntaje.");

    private final Label estadoPresupuesto =
            new Label("Sin evaluación presupuestaria.");

    private final ComboBox<String> categoriaFinal =
            new ComboBox<>();

    private final ComboBox<String> estadoDocumento =
            new ComboBox<>();

    private final TextArea justificacionCategoria =
            new TextArea();

    private final TextArea observacionDocumento =
            new TextArea();

    private final TextArea mensajeEntrada =
            new TextArea();

    private final CheckBox presupuestoCumple =
            new CheckBox(
                    "Confirmo que el presupuesto cumple las condiciones.");

    private final Button aprobar =
            new Button("Aprobar organizador");

    private final Button rechazar =
            new Button("Rechazar organizador");

    private final Button calcularEvaluacion =
            new Button("Calcular evaluación");

    private final Button actualizarDocumento =
            new Button("Guardar estado del documento");

    private final Button solicitarCorreccionDocumento =
            new Button("Solicitar corrección");

    private final Button enviarMensaje =
            new Button("Enviar mensaje");

    private Label menuActivo;

    // Navegación opcional dentro de la ventana de Administración (CU-028).
    private Runnable abrirAprobacionProveedores;
    private Runnable cerrarSesion;

    public ViewVerificacionOrganizador conNavegacion(
            Runnable abrirAprobacionProveedores, Runnable cerrarSesion) {
        this.abrirAprobacionProveedores = abrirAprobacionProveedores;
        this.cerrarSesion = cerrarSesion;
        return this;
    }

    private Organizador organizadorSeleccionado;
    private EvaluacionOrganizador evaluacionActual;

    private Map<String, Object> detalleActual;

    private final NumberFormat formatoMoneda =
            NumberFormat.getCurrencyInstance(
                    Locale.forLanguageTag("es-CO"));

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ViewVerificacionOrganizador(
            ControllerVerificacionOrganizador controller,
            SesionUsuario sesion) {

        if (controller == null) {
            throw new IllegalArgumentException(
                    "El controlador de administracion es obligatorio.");
        }

        if (sesion == null
                || !"ADMINISTRADOR".equalsIgnoreCase(
                        sesion.getTipoUsuario())) {

            throw new IllegalArgumentException(
                    "La vista requiere una sesion de administrador.");
        }

        this.controller = controller;
        this.sesion = sesion;

        formatoMoneda.setMaximumFractionDigits(0);

        configurarControles();
    }

    // ---------------------------------------------------------
    // CONSTRUCCION DE LA PANTALLA
    // ---------------------------------------------------------

    public Parent crearPantalla() {

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");

        VBox sidebar = crearMenu();

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());

        VBox contenido = new VBox(25);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1400);

        VBox encabezado = new VBox(
                8,
                etiqueta(
                        "Verificar, categorizar y aprobar organizadores",
                        "page-title"),
                etiqueta(
                        "Revisa los perfiles, valida documentos y registra "
                        + "las decisiones de verificación.",
                        "muted"));

        GridPane columnas = new GridPane();
        columnas.setHgap(24);
        columnas.setVgap(24);

        ColumnConstraints izquierda = new ColumnConstraints();
        izquierda.setMinWidth(260);
        izquierda.setPrefWidth(300);
        izquierda.setHgrow(Priority.NEVER);

        ColumnConstraints derecha = new ColumnConstraints();
        derecha.setHgrow(Priority.ALWAYS);
        derecha.setMinWidth(420);

        columnas.getColumnConstraints().addAll(
                izquierda, derecha);

        VBox panelPendientes = crearPanelPendientes();
        VBox panelRevision = crearPanelRevision();

        columnas.add(panelPendientes, 0, 0);
        columnas.add(panelRevision, 1, 0);

        GridPane.setVgrow(panelPendientes, Priority.ALWAYS);
        GridPane.setVgrow(panelRevision, Priority.ALWAYS);

        contenido.getChildren().addAll(
                encabezado, columnas);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");

        principal.setCenter(scroll);

        raiz.setLeft(sidebar);
        raiz.setCenter(principal);

        raiz.widthProperty().addListener(
                (observable, anterior, actual) -> {

                    boolean mostrarSidebar =
                            actual.doubleValue() >= 1000;

                    sidebar.setVisible(mostrarSidebar);
                    sidebar.setManaged(mostrarSidebar);

                    if (actual.doubleValue() < 850) {
                        columnas.getColumnConstraints().setAll(
                                new ColumnConstraints());
                        columnas.getColumnConstraints()
                                .get(0)
                                .setHgrow(Priority.ALWAYS);

                        if (columnas.getChildren().contains(
                                panelRevision)) {

                            columnas.getChildren().remove(panelRevision);
                            columnas.add(panelRevision, 0, 1);
                        }

                    } else {
                        columnas.getColumnConstraints().setAll(
                                izquierda, derecha);

                        if (columnas.getChildren().contains(
                                panelRevision)) {

                            GridPane.setColumnIndex(
                                    panelRevision, 1);
                            GridPane.setRowIndex(
                                    panelRevision, 0);
                        }
                    }
                });

        cargarOrganizadoresPendientes();

        return raiz;
    }

    private VBox crearMenu() {

        VBox menu = new VBox(7);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);

        ImageView logo = new ImageView(
                new Image(
                        getClass()
                                .getResource(
                                        "/images/nexteventux-logo.png")
                                .toExternalForm()));

        logo.setViewport(
                new Rectangle2D(75, 225, 1110, 725));

        logo.setFitWidth(210);
        logo.setFitHeight(138);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);

        VBox marca = new VBox(
                5,
                logo,
                etiqueta("GESTIÓN DE EVENTOS", "brand-caption"));

        marca.setAlignment(Pos.CENTER);
        VBox.setMargin(marca, new Insets(0, 0, 38, 0));

        Label tituloMenu =
                etiqueta("MENÚ PRINCIPAL", "menu-title");

        Label pendientes = etiqueta(
                "✓  Verificar organizadores", "menu-item");

        pendientes.getStyleClass().add("active");
        pendientes.setMaxWidth(Double.MAX_VALUE);

        pendientes.setOnMouseClicked(
                event -> cargarOrganizadoresPendientes());

        menuActivo = pendientes;

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        String nombre = sesion.getNombre() == null
                ? "Administrador"
                : sesion.getNombre();

        Label avatar = etiqueta(
                nombre.substring(0, 1).toUpperCase(Locale.ROOT),
                "avatar");

        VBox perfil = new VBox(
                4,
                etiqueta(nombre, "profile-name"),
                etiqueta("Administrador", "hint"));

        HBox cuenta = new HBox(12, avatar, perfil);
        cuenta.setAlignment(Pos.CENTER_LEFT);
        cuenta.getStyleClass().add("sidebar-bottom");

        menu.getChildren().addAll(
                marca,
                tituloMenu,
                pendientes);

        if (abrirAprobacionProveedores != null) {
            Label proveedores = etiqueta("✓  Aprobar proveedores", "menu-item");
            proveedores.setId("abrirAprobacionProveedores");
            proveedores.setMaxWidth(Double.MAX_VALUE);
            proveedores.setOnMouseClicked(event -> abrirAprobacionProveedores.run());
            menu.getChildren().add(proveedores);
        }

        if (cerrarSesion != null) {
            javafx.scene.control.Button salir = new javafx.scene.control.Button("Cerrar sesión");
            salir.setId("cerrarSesionVerificacion");
            salir.getStyleClass().add("btn-secondary");
            salir.setOnAction(event -> cerrarSesion.run());
            perfil.getChildren().add(salir);
        }

        menu.getChildren().addAll(espacio, cuenta);

        return menu;
    }

    private HBox crearBarraSuperior() {

        Label ruta =
                etiqueta("Inicio /", "breadcrumb");

        Label actual =
                etiqueta("Verificación de organizadores",
                        "breadcrumb-current");

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        Label usuario = etiqueta(
                sesion.getNombre() + " · ADMINISTRADOR",
                "user-badge");

        HBox barra = new HBox(
                18, ruta, actual, espacio, usuario);

        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("topbar");

        return barra;
    }

    // ---------------------------------------------------------
    // PANEL DE ORGANIZADORES PENDIENTES
    // ---------------------------------------------------------

    private VBox crearPanelPendientes() {

        Label titulo =
                etiqueta("Organizadores pendientes", "card-title");

        Label ayuda = etiqueta(
                "Selecciona un perfil para revisar sus datos.",
                "muted");

        ayuda.setWrapText(true);

        Button actualizar = new Button("Actualizar lista");
        actualizar.getStyleClass().add("btn-secondary");
        actualizar.setMaxWidth(Double.MAX_VALUE);

        actualizar.setOnAction(
                event -> cargarOrganizadoresPendientes());

        VBox panel = new VBox(
                15,
                titulo,
                ayuda,
                listaPendientes,
                actualizar);

        panel.getStyleClass().add("info-card");

        VBox.setVgrow(listaPendientes, Priority.ALWAYS);

        return panel;
    }

    private void configurarControles() {

        listaPendientes.setId("listaOrganizadoresPendientes");
        listaPendientes.setPrefHeight(500);

        listaPendientes.setPlaceholder(
                new Label("No hay organizadores pendientes."));

        listaPendientes.setCellFactory(
                lista -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            Organizador item, boolean empty) {

                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(
                                    item.nombre()
                                    + "\nOrganizador #"
                                    + item.id()
                                    + "\nPendiente de verificación");
                        }
                    }
                });

        listaPendientes.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {
                    if (actual != null) {
                        seleccionarOrganizador(actual);
                    }
                });

        listaDocumentos.setId("listaDocumentosOrganizador");
        listaDocumentos.setPrefHeight(180);
        listaDocumentos.setPlaceholder(
                new Label("El organizador no tiene documentos registrados."));

        listaDocumentos.setCellFactory(
                lista -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            DocumentoOrganizador documento,
                            boolean vacio) {

                        super.updateItem(documento, vacio);

                        if (vacio || documento == null) {
                            setText(null);
                            return;
                        }

                        String vencimiento =
                                documento.getFechaVencimiento() == null
                                        ? "Sin fecha de vencimiento"
                                        : documento.getFechaVencimiento()
                                                .toString();

                        String aviso = documento.estaVencido()
                                ? " · VENCIDO"
                                : "";

                        setText(
                                documento.getTipoDocumento()
                                + "\nEstado: " + documento.getEstado()
                                + " · Vence: " + vencimiento
                                + aviso);
                    }
                });

        listaDocumentos.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {
                    if (actual != null) {
                        estadoDocumento.setValue(actual.getEstado());
                        observacionDocumento.setText(
                                actual.getObservacion() == null
                                        ? ""
                                        : actual.getObservacion());
                    }
                });

        estadoDocumento.getItems().setAll(
                "PRESENTADO",
                "VALIDADO",
                "POR_CORREGIR",
                "VENCIDO");

        estadoDocumento.setPromptText("Estado del documento");
        estadoDocumento.setMaxWidth(Double.MAX_VALUE);

        categoriaFinal.getItems().setAll("JUNIOR", "SENIOR");
        categoriaFinal.setPromptText("Seleccionar categoría");
        categoriaFinal.setMaxWidth(Double.MAX_VALUE);

        justificacionCategoria.setPromptText(
                "Obligatoria cuando cambies manualmente la categoría.");
        justificacionCategoria.setWrapText(true);
        justificacionCategoria.setPrefRowCount(2);

        observacionDocumento.setPromptText(
                "Observación para este documento.");
        observacionDocumento.setWrapText(true);
        observacionDocumento.setPrefRowCount(2);

        mensajeEntrada.setPromptText(
                "Escribe un mensaje para el organizador...");
        mensajeEntrada.setWrapText(true);
        mensajeEntrada.setPrefRowCount(3);

        listaMensajes.setId("historialMensajesOrganizador");
        listaMensajes.setPrefHeight(190);
        listaMensajes.setPlaceholder(
                new Label("Todavía no hay mensajes en esta conversación."));

        listaMensajes.setCellFactory(
                lista -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            MensajeOrganizador mensaje,
                            boolean vacio) {

                        super.updateItem(mensaje, vacio);

                        if (vacio || mensaje == null) {
                            setText(null);
                            return;
                        }

                        String fecha = mensaje.getFechaHora() == null
                                ? ""
                                : mensaje.getFechaHora()
                                        .format(FORMATO_FECHA);

                        setText(
                                fecha
                                + " · Usuario #" + mensaje.getIdRemitente()
                                + "\n" + mensaje.getTipoMensaje()
                                + " · " + mensaje.getEstado()
                                + "\n" + mensaje.getContenido());
                    }
                });

        calcularEvaluacion.getStyleClass().add("btn-secondary");
        calcularEvaluacion.setOnAction(
                event -> calcularEvaluacion());

        actualizarDocumento.getStyleClass().add("btn-secondary");
        actualizarDocumento.setOnAction(
                event -> guardarEstadoDocumento());

        solicitarCorreccionDocumento.getStyleClass().add("btn-secondary");
        solicitarCorreccionDocumento.setOnAction(
                event -> solicitarCorreccionDocumento());

        enviarMensaje.getStyleClass().add("btn-secondary");
        enviarMensaje.setOnAction(
                event -> enviarMensaje());

        aprobar.getStyleClass().add("btn-primary");
        aprobar.setOnAction(event -> aprobarOrganizador());

        rechazar.getStyleClass().add("btn-secondary");
        rechazar.setOnAction(event -> rechazarOrganizador());

        presupuestoCumple.setWrapText(true);
    }

    // ---------------------------------------------------------
    // PANEL DE REVISION
    // ---------------------------------------------------------

    private VBox crearPanelRevision() {

        VBox panel = new VBox(20);

        panel.getChildren().addAll(
                crearTarjetaPerfil(),
                crearTarjetaDocumentos(),
                crearTarjetaEvaluacion(),
                crearTarjetaMensajes());

        return panel;
    }

    private VBox crearTarjetaPerfil() {

        Label titulo =
                etiqueta("Perfil del organizador", "card-title");

        GridPane datos = new GridPane();
        datos.setHgap(15);
        datos.setVgap(12);

        ColumnConstraints etiquetaColumna =
                new ColumnConstraints(145);

        ColumnConstraints valorColumna =
                new ColumnConstraints();

        valorColumna.setHgrow(Priority.ALWAYS);

        datos.getColumnConstraints().addAll(
                etiquetaColumna,
                valorColumna);

        datos.add(etiqueta("Nombre", "field-label"), 0, 0);
        datos.add(nombreOrganizador, 1, 0);

        datos.add(etiqueta("Empresa", "field-label"), 0, 1);
        datos.add(empresa, 1, 1);

        datos.add(etiqueta("Documento de identidad", "field-label"), 0, 2);
        datos.add(documentoIdentidad, 1, 2);

        datos.add(etiqueta("Teléfono", "field-label"), 0, 3);
        datos.add(telefono, 1, 3);

        datos.add(etiqueta("Experiencia", "field-label"), 0, 4);
        datos.add(experiencia, 1, 4);

        datos.add(etiqueta("Especialidad", "field-label"), 0, 5);
        datos.add(especialidad, 1, 5);

        datos.add(etiqueta("Portafolio", "field-label"), 0, 6);
        datos.add(portafolio, 1, 6);

        datos.add(etiqueta("Tarifa base", "field-label"), 0, 7);
        datos.add(tarifa, 1, 7);

        datos.add(etiqueta("Presupuesto máximo", "field-label"), 0, 8);
        datos.add(presupuestoMaximo, 1, 8);

        datos.add(etiqueta("Estado actual", "field-label"), 0, 9);
        datos.add(estadoActual, 1, 9);

        estadoPerfil.getStyleClass().add("status");
        estadoDocumentos.setWrapText(true);
        estadoIdentidad.setWrapText(true);

        GridPane verificaciones = new GridPane();
        verificaciones.setHgap(14);
        verificaciones.setVgap(10);

        verificaciones.add(
                etiqueta("Información:", "field-label"), 0, 0);
        verificaciones.add(estadoPerfil, 1, 0);

        verificaciones.add(
                etiqueta("Documentos:", "field-label"), 0, 1);
        verificaciones.add(estadoDocumentos, 1, 1);

        verificaciones.add(
                etiqueta("Identidad:", "field-label"), 0, 2);
        verificaciones.add(estadoIdentidad, 1, 2);

        Button verificar = new Button("Verificar requisitos");
        verificar.getStyleClass().add("btn-secondary");

        verificar.setOnAction(event -> verificarRequisitos());

        VBox tarjeta = new VBox(
                16,
                titulo,
                datos,
                new Separator(),
                verificaciones,
                verificar);

        tarjeta.getStyleClass().add("card");

        return tarjeta;
    }

    private VBox crearTarjetaDocumentos() {

        Label titulo =
                etiqueta("Documentos presentados", "card-title");

        Label ayuda = etiqueta(
                "Revisa la vigencia, valida los documentos "
                + "y registra las observaciones.",
                "muted");

        ayuda.setWrapText(true);

        VBox tarjeta = new VBox(
                14,
                titulo,
                ayuda,
                listaDocumentos,
                etiqueta("Estado", "field-label"),
                estadoDocumento,
                etiqueta("Observación", "field-label"),
                observacionDocumento,
                actualizarDocumento,
                solicitarCorreccionDocumento);

        tarjeta.getStyleClass().add("card");

        return tarjeta;
    }

    private VBox crearTarjetaEvaluacion() {

        Label titulo =
                etiqueta("Puntaje y categoría", "card-title");

        resultadoEvaluacion.setWrapText(true);

        estadoPresupuesto.setWrapText(true);

        VBox tarjeta = new VBox(
                15,
                titulo,
                calcularEvaluacion,
                resultadoEvaluacion,
                new Separator(),
                etiqueta("Categoría final", "field-label"),
                categoriaFinal,
                etiqueta("Justificación de cambio manual", "field-label"),
                justificacionCategoria,
                new Separator(),
                estadoPresupuesto,
                presupuestoCumple);

        Button validarPresupuesto = new Button(
                "Registrar revisión de presupuesto");

        validarPresupuesto.getStyleClass().add("btn-secondary");

        validarPresupuesto.setOnAction(event -> validarPresupuesto());

        HBox acciones = new HBox(
                10,
                aprobar,
                rechazar);

        acciones.setAlignment(Pos.CENTER_LEFT);

        tarjeta.getChildren().addAll(
                validarPresupuesto,
                new Separator(),
                acciones);

        tarjeta.getStyleClass().add("card");

        return tarjeta;
    }

    private VBox crearTarjetaMensajes() {

        Label titulo =
                etiqueta("Comunicación interna", "card-title");

        Label ayuda = etiqueta(
                "Envía mensajes o solicita información y correcciones. "
                + "El historial se consulta desde la base de datos.",
                "muted");

        ayuda.setWrapText(true);

        VBox tarjeta = new VBox(
                14,
                titulo,
                ayuda,
                listaMensajes,
                mensajeEntrada,
                enviarMensaje);

        tarjeta.getStyleClass().add("card");

        return tarjeta;
    }

    // ---------------------------------------------------------
    // CONSULTA Y SELECCION
    // ---------------------------------------------------------

    private void cargarOrganizadoresPendientes() {

        try {
            List<Organizador> pendientes =
                    controller.consultarOrganizadoresPendientes();

            listaPendientes.getItems().setAll(pendientes);

            if (pendientes.isEmpty()) {
                limpiarSeleccion();

                mostrarMensaje(
                        "No hay organizadores pendientes de verificación.");

            } else if (organizadorSeleccionado != null) {

                boolean siguePendiente = pendientes.stream()
                        .anyMatch(item ->
                                item.id()
                                        == organizadorSeleccionado.id());

                if (!siguePendiente) {
                    limpiarSeleccion();
                }
            }

        } catch (RuntimeException e) {
            mostrarError(
                    "No fue posible consultar los organizadores pendientes.",
                    e);
        }
    }

    private void seleccionarOrganizador(
            Organizador organizador) {

        organizadorSeleccionado = organizador;
        evaluacionActual = null;

        if (organizador == null) {
            limpiarSeleccion();
            return;
        }

        try {
            detalleActual =
                    controller.consultarDetalleOrganizador(
                            organizador.id());

            mostrarDetalle(detalleActual);
            cargarDocumentos();
            cargarMensajes();
            verificarRequisitos();

            resultadoEvaluacion.setText(
                    "Pulsa «Calcular evaluación» para obtener el puntaje.");

            estadoPresupuesto.setText(
                    "Pendiente de cálculo.");

            categoriaFinal.setValue(null);
            justificacionCategoria.clear();
            presupuestoCumple.setSelected(false);

        } catch (RuntimeException e) {
            mostrarError(
                    "No fue posible cargar el perfil del organizador.",
                    e);
        }
    }

    private void mostrarDetalle(Map<String, Object> detalle) {

        nombreOrganizador.setText(
                valor(detalle, "nombre"));

        empresa.setText(
                valor(detalle, "empresa"));

        documentoIdentidad.setText(
                valor(detalle, "documentoIdentidad"));

        telefono.setText(
                valor(detalle, "telefono"));

        experiencia.setText(
                valor(detalle, "experienciaAnios") + " años");

        especialidad.setText(
                valor(detalle, "especialidad"));

        portafolio.setText(
                valor(detalle, "portafolio"));

        tarifa.setText(
                moneda(detalle.get("tarifaBase")));

        presupuestoMaximo.setText(
                moneda(detalle.get("presupuestoMaximo")));

        estadoActual.setText(
                valor(detalle, "estadoVerificacion"));

        nombreOrganizador.setWrapText(true);
        empresa.setWrapText(true);
        documentoIdentidad.setWrapText(true);
        especialidad.setWrapText(true);
        portafolio.setWrapText(true);
    }

    private void cargarDocumentos() {

        if (organizadorSeleccionado == null) {
            listaDocumentos.getItems().clear();
            return;
        }

        listaDocumentos.getItems().setAll(
                controller.consultarDocumentosOrganizador(
                        organizadorSeleccionado.id()));
    }

    private void cargarMensajes() {

        if (organizadorSeleccionado == null) {
            listaMensajes.getItems().clear();
            return;
        }

        listaMensajes.getItems().setAll(
                controller.consultarMensajes(
                        organizadorSeleccionado.id()));
    }

    private void verificarRequisitos() {

        if (organizadorSeleccionado == null) {
            return;
        }

        try {
            boolean perfilCompleto =
                    controller.verificarInformacionOrganizador(
                            organizadorSeleccionado.id());

            boolean documentosValidos =
                    controller.verificarDocumentacionOrganizador(
                            organizadorSeleccionado.id(), null);

            boolean identidadDuplicada =
                    controller.verificarIdentidadDuplicada(
                            organizadorSeleccionado.id());

            estadoPerfil.setText(
                    perfilCompleto ? "Completo" : "Incompleto");

            estadoDocumentos.setText(
                    documentosValidos
                            ? "Validados y vigentes"
                            : "Faltan documentos, validación o vigencia");

            estadoIdentidad.setText(
                    identidadDuplicada
                            ? "Posible duplicidad detectada"
                            : "Sin duplicidad detectada");

        } catch (RuntimeException e) {
            mostrarError(
                    "No fue posible verificar los requisitos.",
                    e);
        }
    }

    // ---------------------------------------------------------
    // EVALUACION Y CATEGORIZACION
    // ---------------------------------------------------------

    private void calcularEvaluacion() {

        if (!hayOrganizadorSeleccionado()) {
            return;
        }

        try {
            evaluacionActual =
                    controller.evaluarOrganizador(
                            organizadorSeleccionado.id());

            categoriaFinal.setValue(
                    evaluacionActual.getCategoriaSugerida());

            resultadoEvaluacion.setText(
                    "Puntaje: " + evaluacionActual.getPuntaje()
                    + " / 100"
                    + "\nCategoría sugerida: "
                    + evaluacionActual.getCategoriaSugerida());

            if (evaluacionActual.isRequiereRevisionPresupuesto()) {

                estadoPresupuesto.setText(
                        "El presupuesto máximo supera el límite configurado. "
                        + "Se requiere revisión adicional.");

                presupuestoCumple.setSelected(false);

            } else {

                estadoPresupuesto.setText(
                        "No se requiere revisión presupuestaria adicional.");

                presupuestoCumple.setSelected(true);
            }

            mostrarMensaje(
                    "Evaluación calculada. Revisa el puntaje, la categoría "
                    + "sugerida y los requisitos antes de decidir.");

        } catch (RuntimeException e) {
            mostrarError(
                    "No fue posible calcular la evaluación.",
                    e);
        }
    }

    private void validarPresupuesto() {

        if (evaluacionActual == null) {
            mostrarMensaje(
                    "Calcula la evaluación antes de validar el presupuesto.");
            return;
        }

        if (!evaluacionActual.isRequiereRevisionPresupuesto()) {
            evaluacionActual.setPresupuestoValidado(true);

            estadoPresupuesto.setText(
                    "No se requiere revisión adicional.");

            return;
        }

        try {
            controller.validarPresupuesto(
                    evaluacionActual,
                    presupuestoCumple.isSelected());

            estadoPresupuesto.setText(
                    "Presupuesto revisado y validado.");

            mostrarMensaje(
                    "La validación presupuestaria quedó registrada "
                    + "en la evaluación actual.");

        } catch (RuntimeException e) {
            estadoPresupuesto.setText(
                    "La validación no fue aprobada. "
                    + "El organizador permanece pendiente.");

            mostrarError(
                    "No se pudo completar la validación presupuestaria.",
                    e);
        }
    }

    // ---------------------------------------------------------
    // DOCUMENTOS Y SOLICITUDES DE CORRECCION
    // ---------------------------------------------------------

    private void guardarEstadoDocumento() {

        if (!hayOrganizadorSeleccionado()) {
            return;
        }

        DocumentoOrganizador documento =
                listaDocumentos.getSelectionModel()
                        .getSelectedItem();

        if (documento == null) {
            mostrarMensaje(
                    "Selecciona un documento antes de actualizar su estado.");
            return;
        }

        String nuevoEstado = estadoDocumento.getValue();

        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            mostrarMensaje("Selecciona un estado para el documento.");
            return;
        }

        try {
            controller.actualizarEstadoDocumento(
                    documento.getIdDocumento(),
                    nuevoEstado,
                    observacionDocumento.getText().trim());

            evaluacionActual = null;

            cargarDocumentos();
            verificarRequisitos();

            mostrarMensaje(
                    "Se actualizó el estado del documento. "
                    + "El perfil debe revisarse de nuevo antes de decidir.");

        } catch (RuntimeException e) {
            mostrarError(
                    "No se pudo actualizar el estado del documento.",
                    e);
        }
    }

    private void solicitarCorreccionDocumento() {

        if (!hayOrganizadorSeleccionado()) {
            return;
        }

        DocumentoOrganizador documento =
                listaDocumentos.getSelectionModel()
                        .getSelectedItem();

        if (documento == null) {
            mostrarMensaje(
                    "Selecciona el documento que requiere corrección.");
            return;
        }

        String observacion = observacionDocumento.getText().trim();

        if (observacion.isBlank()) {
            mostrarMensaje(
                    "Escribe una observación explicando qué debe corregirse.");
            return;
        }

        try {
            controller.actualizarEstadoDocumento(
                    documento.getIdDocumento(),
                    "POR_CORREGIR",
                    observacion);

            controller.solicitarCorrecciones(
                    organizadorSeleccionado.id(),
                    "Corrección del documento "
                    + documento.getTipoDocumento()
                    + ": " + observacion);

            evaluacionActual = null;

            cargarDocumentos();
            cargarMensajes();
            verificarRequisitos();

            mostrarMensaje(
                    "Se registró la solicitud de corrección y "
                    + "se notificó al organizador.");

        } catch (RuntimeException e) {
            mostrarError(
                    "No fue posible completar la solicitud de corrección.",
                    e);
        }
    }

    // ---------------------------------------------------------
    // COMUNICACION INTERNA
    // ---------------------------------------------------------

    private void enviarMensaje() {

        if (!hayOrganizadorSeleccionado()) {
            return;
        }

        String contenido = mensajeEntrada.getText().trim();

        if (contenido.isBlank()) {
            mostrarMensaje("Escribe un mensaje antes de enviarlo.");
            return;
        }

        try {
            controller.enviarMensaje(
                    organizadorSeleccionado.id(),
                    contenido,
                    MensajeOrganizador.TIPO_MENSAJE);

            mensajeEntrada.clear();
            cargarMensajes();

            mostrarMensaje(
                    "El mensaje se guardó y se notificó al organizador.");

        } catch (RuntimeException e) {
            mostrarError(
                    "El mensaje no pudo enviarse. Puedes intentarlo nuevamente.",
                    e);
        }
    }

    // ---------------------------------------------------------
    // APROBACION Y RECHAZO
    // ---------------------------------------------------------

    private void aprobarOrganizador() {

        if (!hayOrganizadorSeleccionado()) {
            return;
        }

        if (evaluacionActual == null) {
            mostrarMensaje(
                    "Primero calcula el puntaje y la categoría sugerida.");
            return;
        }

        try {
            controller.categorizarOrganizador(
                    evaluacionActual,
                    categoriaFinal.getValue(),
                    justificacionCategoria.getText().trim());

            if (evaluacionActual.isRequiereRevisionPresupuesto()
                    && !evaluacionActual.isPresupuestoValidado()) {

                mostrarMensaje(
                        "Antes de aprobar debes registrar la validación "
                        + "adicional del presupuesto.");
                return;
            }

            Alert confirmar = new Alert(
                    Alert.AlertType.CONFIRMATION);

            confirmar.setTitle("Confirmar aprobación");
            confirmar.setHeaderText(
                    "Aprobar a " + organizadorSeleccionado.nombre());

            confirmar.setContentText(
                    "Puntaje: " + evaluacionActual.getPuntaje()
                    + " / 100"
                    + "\nCategoría final: "
                    + evaluacionActual.getCategoriaFinal()
                    + "\n\nEl organizador quedará verificado y podrá "
                    + "aparecer en las búsquedas de los clientes.");

            if (confirmar.showAndWait().orElse(ButtonType.CANCEL)
                    != ButtonType.OK) {
                return;
            }

            int idAprobado = organizadorSeleccionado.id();

            controller.aprobarOrganizador(evaluacionActual);

            mostrarMensaje(
                    "El organizador fue aprobado correctamente.");

            cargarOrganizadoresPendientes();
            limpiarSeleccionDespuesDeDecision(idAprobado);

        } catch (RuntimeException e) {
            mostrarError(
                    "No fue posible aprobar al organizador. "
                    + "Su estado anterior debe conservarse.",
                    e);
        }
    }

    private void rechazarOrganizador() {

        if (!hayOrganizadorSeleccionado()) {
            return;
        }

        if (evaluacionActual == null) {
            try {
                evaluacionActual =
                        controller.evaluarOrganizador(
                                organizadorSeleccionado.id());

                categoriaFinal.setValue(
                        evaluacionActual.getCategoriaSugerida());

            } catch (RuntimeException e) {
                mostrarError(
                        "No fue posible evaluar el organizador.",
                        e);
                return;
            }
        }

        Alert solicitudMotivo = new Alert(
                Alert.AlertType.CONFIRMATION);

        solicitudMotivo.setTitle("Rechazar organizador");
        solicitudMotivo.setHeaderText(
                "¿Deseas rechazar a "
                + organizadorSeleccionado.nombre() + "?");

        TextArea motivo = new TextArea();
        motivo.setPromptText("Indica el motivo del rechazo...");
        motivo.setWrapText(true);
        motivo.setPrefRowCount(4);

        solicitudMotivo.getDialogPane()
                .setContent(new VBox(
                        10,
                        etiqueta("Motivo de rechazo (obligatorio)",
                                "field-label"),
                        motivo));

        solicitudMotivo.getButtonTypes().setAll(
                ButtonType.OK, ButtonType.CANCEL);

        if (solicitudMotivo.showAndWait()
                .orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        if (motivo.getText().trim().isBlank()) {
            mostrarMensaje(
                    "Debes indicar un motivo para rechazar al organizador.");
            return;
        }

        try {
            int idRechazado = organizadorSeleccionado.id();

            controller.rechazarOrganizador(
                    evaluacionActual,
                    motivo.getText().trim());

            mostrarMensaje(
                    "El rechazo quedó registrado y el organizador "
                    + "fue notificado.");

            cargarOrganizadoresPendientes();
            limpiarSeleccionDespuesDeDecision(idRechazado);

        } catch (RuntimeException e) {
            mostrarError(
                    "No fue posible guardar el rechazo.",
                    e);
        }
    }

    // ---------------------------------------------------------
    // UTILIDADES
    // ---------------------------------------------------------

    private boolean hayOrganizadorSeleccionado() {

        if (organizadorSeleccionado == null) {
            mostrarMensaje(
                    "Selecciona un organizador de la lista de pendientes.");
            return false;
        }

        return true;
    }

    private void limpiarSeleccionDespuesDeDecision(
            int idProcesado) {

        organizadorSeleccionado = null;
        evaluacionActual = null;
        detalleActual = null;

        listaPendientes.getItems().removeIf(
                item -> item.id() == idProcesado);

        limpiarSeleccion();
    }

    private void limpiarSeleccion() {

        organizadorSeleccionado = null;
        evaluacionActual = null;
        detalleActual = null;

        nombreOrganizador.setText(
                "Selecciona un organizador");

        empresa.setText("");
        documentoIdentidad.setText("");
        telefono.setText("");
        experiencia.setText("");
        especialidad.setText("");
        portafolio.setText("");
        tarifa.setText("");
        presupuestoMaximo.setText("");
        estadoActual.setText("");

        estadoPerfil.setText("Pendiente de revisión");
        estadoDocumentos.setText("Sin revisar");
        estadoIdentidad.setText("Sin revisar");

        resultadoEvaluacion.setText(
                "Todavía no se ha calculado el puntaje.");

        estadoPresupuesto.setText(
                "Sin evaluación presupuestaria.");

        listaDocumentos.getItems().clear();
        listaMensajes.getItems().clear();

        categoriaFinal.setValue(null);
        justificacionCategoria.clear();
        observacionDocumento.clear();
        mensajeEntrada.clear();
        presupuestoCumple.setSelected(false);
    }

    private String valor(
            Map<String, Object> datos, String clave) {

        Object valor = datos.get(clave);

        return valor == null
                || valor.toString().isBlank()
                ? "No registrado"
                : valor.toString();
    }

    private String moneda(Object valor) {

        if (!(valor instanceof BigDecimal)) {
            return "No registrado";
        }

        return formatoMoneda.format(valor);
    }

    private Label etiqueta(
            String texto, String estilo) {

        Label label = new Label(texto);
        label.getStyleClass().add(estilo);

        return label;
    }

    private void mostrarMensaje(String mensaje) {

        Alert alerta = new Alert(
                Alert.AlertType.INFORMATION);

        alerta.setTitle("Verificación de organizadores");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);

        alerta.showAndWait();
    }

    private void mostrarError(
            String mensaje, RuntimeException e) {

        String detalle = e.getMessage();

        Alert alerta = new Alert(
                Alert.AlertType.ERROR);

        alerta.setTitle("Error de verificación");
        alerta.setHeaderText(mensaje);

        alerta.setContentText(
                detalle == null || detalle.isBlank()
                        ? "Consulta los registros para conocer el detalle."
                        : detalle);

        alerta.showAndWait();
    }
}
