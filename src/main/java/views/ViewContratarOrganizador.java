
package views;

import controllers.ControllerContratacionOrganizador;
import entities.Contratacion;
import entities.Organizador;
import entities.SesionUsuario;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Vista del caso de uso CU-026: Contratar organizador.
 *
 * Permite al cliente consultar perfiles disponibles, comparar
 * organizadores, revisar su disponibilidad y enviar una solicitud.
 */
public class ViewContratarOrganizador {

    private final ControllerContratacionOrganizador controller;
    private final SesionUsuario sesion;
    private TabPane pestanasPrincipal;
    private Tab pestanaMisSolicitudes;

    private final TextField tipoEvento = new TextField();
    private final TextField ciudad = new TextField();
    private final DatePicker fechaAproximada = new DatePicker();
    private final TextField presupuesto = new TextField();
    private final TextField criterios = new TextField();

    private final ListView<Organizador> listaOrganizadores =
            new ListView<>();

    private final ListView<Contratacion> listaContrataciones =
            new ListView<>();

    private final Label detalleNombre =
            new Label("Selecciona un organizador");

    private final Label detalleEmpresa = new Label();
    private final Label detalleCiudad = new Label();
    private final Label detalleEspecialidad = new Label();
    private final Label detallePortafolio = new Label();
    private final Label detalleTarifa = new Label();
    private final Label detalleResenas = new Label();

    private final Label disponibilidad = new Label(
            "Busca organizadores para consultar su disponibilidad.");

    private final Button solicitarContratacion =
            new Button("Solicitar contratación");

    private final NumberFormat formatoMoneda =
            NumberFormat.getCurrencyInstance(
                    Locale.forLanguageTag("es-CO"));

    private Organizador organizadorSeleccionado;

    private Label menuBuscar;
    private Label menuSolicitudes;

    public ViewContratarOrganizador(
            ControllerContratacionOrganizador controller) {
        this(controller, null);
    }

    public ViewContratarOrganizador(
            ControllerContratacionOrganizador controller,
            SesionUsuario sesion) {

        if (controller == null) {
            throw new IllegalArgumentException(
                    "El controlador de administración es obligatorio.");
        }

        this.controller = controller;
        this.sesion = sesion;

        formatoMoneda.setMaximumFractionDigits(0);

        configurarFormulario();
    }

    private void configurarFormulario() {

        tipoEvento.setId("tipoEvento");
        tipoEvento.setPromptText(
                "Ej. Boda, cumpleaños o evento empresarial");

        ciudad.setId("ciudad");
        ciudad.setPromptText("Ciudad donde se realizará el evento");

        fechaAproximada.setId("fechaAproximada");
        fechaAproximada.setPromptText("Fecha aproximada");
        fechaAproximada.setEditable(false);
        fechaAproximada.setMaxWidth(Double.MAX_VALUE);
        fechaAproximada.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacia) {
                super.updateItem(fecha, vacia);
                setDisable(vacia || fecha.isBefore(LocalDate.now()));
            }
        });

        presupuesto.setId("presupuestoEstimado");
        presupuesto.setPromptText("Ej. 2500000");

        criterios.setId("criteriosBusqueda");
        criterios.setPromptText(
                "Características adicionales (opcional)");

        listaOrganizadores.setId("listaOrganizadores");
        listaOrganizadores.setPlaceholder(
                new Label("Completa los criterios y pulsa Buscar."));
        listaOrganizadores.setPrefHeight(390);
        listaOrganizadores.setMinHeight(250);

        listaContrataciones.setId("listaContrataciones");
        listaContrataciones.setPlaceholder(
                new Label("Todavía no tienes solicitudes registradas."));
        listaContrataciones.setPrefHeight(390);

        detalleNombre.getStyleClass().add("card-title");
        detalleNombre.setWrapText(true);

        detallePortafolio.setWrapText(true);
        disponibilidad.setWrapText(true);

        solicitarContratacion.setId("solicitarContratacion");
        solicitarContratacion.getStyleClass().add("btn-primary");
        solicitarContratacion.setDisable(true);
        solicitarContratacion.setMaxWidth(Double.MAX_VALUE);
        solicitarContratacion.setOnAction(
                event -> enviarSolicitud());

        configurarListaOrganizadores();
        configurarListaContrataciones();
    }

    // Acción para cerrar la sesión del cliente; sin ella el botón no se muestra.
    private Runnable cerrarSesion;

    public ViewContratarOrganizador conCerrarSesion(Runnable accion) {
        cerrarSesion = accion;
        return this;
    }

    public Parent crearPantalla() {

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");

        Tab buscar = new Tab(
                "Buscar organizadores",
                crearVistaBusqueda());
        buscar.setClosable(false);

        Tab solicitudes = new Tab(
        "Mis solicitudes",
        crearVistaContrataciones());
solicitudes.setClosable(false);

this.pestanaMisSolicitudes = solicitudes;

TabPane pestanas = new TabPane(buscar, solicitudes);
this.pestanasPrincipal = pestanas;

        pestanas.getStyleClass().add("publicacion");
        pestanas.setTabClosingPolicy(
                TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox sidebar = crearMenu(buscar, solicitudes);

        pestanas.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {

                    actualizarMenuActivo(actual, buscar, solicitudes);

                    if (actual == solicitudes) {
                        mostrarContrataciones();
                    }
                });

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());

        VBox contenido = new VBox(25);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1320);

        Label titulo = etiqueta(
                "Contratar organizador", "page-title");

        Label subtitulo = etiqueta(
                "Encuentra, compara y solicita la contratación "
                + "de un organizador para tu evento.",
                "muted");

        subtitulo.setWrapText(true);

        VBox encabezado = new VBox(8, titulo, subtitulo);

        contenido.getChildren().addAll(encabezado, pestanas);
        VBox.setVgrow(pestanas, Priority.ALWAYS);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");

        principal.setCenter(scroll);

        raiz.setLeft(sidebar);
        raiz.setCenter(principal);

        // En pantallas estrechas se oculta la barra lateral.
        raiz.widthProperty().addListener(
                (observable, anterior, actual) -> {

                    boolean mostrarSidebar =
                            actual.doubleValue() >= 1000;

                    sidebar.setVisible(mostrarSidebar);
                    sidebar.setManaged(mostrarSidebar);
                });

        return raiz;
    }

    // ---------------------------------------------------------
    // BARRA LATERAL
    // ---------------------------------------------------------

    private VBox crearMenu(Tab buscar, Tab solicitudes) {

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

        // Recorta el margen blanco, igual que en ViewEventos.
        logo.setViewport(
                new Rectangle2D(75, 225, 1110, 725));

        logo.setFitWidth(210);
        logo.setFitHeight(138);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setAccessibleText("Logo de NextEventux");

        Label descripcionMarca = etiqueta(
                "GESTIÓN DE EVENTOS", "brand-caption");

        VBox marca = new VBox(5, logo, descripcionMarca);
        marca.setAlignment(Pos.CENTER);
        VBox.setMargin(
                marca, new Insets(0, 0, 38, 0));

        Label tituloMenu = etiqueta(
                "MENÚ PRINCIPAL", "menu-title");

        Label inicio = etiqueta(
                "⌂  Inicio", "menu-item");

        inicio.setMaxWidth(Double.MAX_VALUE);
        inicio.setOnMouseClicked(
                event -> seleccionarPestana(buscar));

        menuBuscar = etiqueta(
                "♧  Buscar organizadores", "menu-item");

        menuBuscar.setMaxWidth(Double.MAX_VALUE);
        menuBuscar.getStyleClass().add("active");
        menuBuscar.setOnMouseClicked(
                event -> seleccionarPestana(buscar));

        menuSolicitudes = etiqueta(
                "▤  Mis solicitudes", "menu-item");

        menuSolicitudes.setMaxWidth(Double.MAX_VALUE);
        menuSolicitudes.setOnMouseClicked(
                event -> seleccionarPestana(solicitudes));

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        String nombre = sesion == null
                || sesion.getNombre() == null
                || sesion.getNombre().isBlank()
                ? "Cliente"
                : sesion.getNombre();

        String inicial = nombre.substring(0, 1)
                .toUpperCase(Locale.ROOT);

        Label avatar = etiqueta(inicial, "avatar");

        VBox perfil = new VBox(
                4,
                etiqueta(nombre, "profile-name"),
                etiqueta("Mi cuenta", "hint"));

        if (cerrarSesion != null) {
            Button salir = new Button("Cerrar sesión");
            salir.setId("cerrarSesionCliente");
            salir.getStyleClass().add("btn-secondary");
            salir.setOnAction(event -> cerrarSesion.run());
            perfil.getChildren().add(salir);
        }

        HBox cuenta = new HBox(12, avatar, perfil);
        cuenta.setAlignment(Pos.CENTER_LEFT);
        cuenta.getStyleClass().add("sidebar-bottom");

        menu.getChildren().addAll(
                marca,
                tituloMenu,
                inicio,
                menuBuscar,
                menuSolicitudes,
                espacio,
                cuenta);

        return menu;
    }

    private HBox crearBarraSuperior() {

        Label ruta = etiqueta(
                "Inicio /", "breadcrumb");

        Label actual = etiqueta(
                "Contratar organizador", "breadcrumb-current");

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        String nombre = sesion == null
                || sesion.getNombre() == null
                ? "Cliente"
                : sesion.getNombre();

        Label usuario = etiqueta(
                nombre + " · CLIENTE", "user-badge");

        HBox barra = new HBox(
                18, ruta, actual, espacio, usuario);

        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("topbar");

        return barra;
    }

    private void seleccionarPestana(Tab pestana) {

        if (pestana.getTabPane() != null) {
            pestana.getTabPane()
                    .getSelectionModel()
                    .select(pestana);
        }
    }

    private void actualizarMenuActivo(
            Tab actual, Tab buscar, Tab solicitudes) {

        if (menuBuscar == null || menuSolicitudes == null) {
            return;
        }

        menuBuscar.getStyleClass().remove("active");
        menuSolicitudes.getStyleClass().remove("active");

        if (actual == buscar) {
            menuBuscar.getStyleClass().add("active");
        } else if (actual == solicitudes) {
            menuSolicitudes.getStyleClass().add("active");
        }
    }

    // ---------------------------------------------------------
    // PESTAÑA DE BÚSQUEDA
    // ---------------------------------------------------------

    private Parent crearVistaBusqueda() {

        VBox contenido = new VBox(22);
        contenido.setPadding(new Insets(0, 0, 10, 0));

        VBox tarjetaFiltros = new VBox(20);
        tarjetaFiltros.getStyleClass().add("card");

        HBox tituloFiltros = new HBox(
                12,
                etiqueta("⌕", "heading-icon"),
                etiqueta("Información del evento", "card-title"));

        tituloFiltros.setAlignment(Pos.CENTER_LEFT);

        Label ayuda = etiqueta(
                "Completa los campos obligatorios para encontrar "
                + "organizadores compatibles con tu evento.",
                "muted");

        ayuda.setWrapText(true);

        GridPane formulario = crearGrilla();

        formulario.add(
                grupo("Tipo de evento *", tipoEvento),
                0, 0);

        formulario.add(
                grupo("Ciudad *", ciudad),
                1, 0);

        formulario.add(
                grupo("Fecha aproximada *", fechaAproximada),
                0, 1);

        formulario.add(
                grupo("Presupuesto máximo (COP) *", presupuesto),
                1, 1);

        formulario.add(
                grupo("Criterios adicionales", criterios),
                0, 2, 2, 1);

        Button buscar = new Button("⌕  Buscar organizadores");
        buscar.setId("buscarOrganizadores");
        buscar.getStyleClass().add("btn-primary");
        buscar.setDefaultButton(true);
        buscar.setOnAction(
                event -> mostrarOrganizadores());

        HBox acciones = new HBox(buscar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        tarjetaFiltros.getChildren().addAll(
                tituloFiltros,
                ayuda,
                formulario,
                new Separator(),
                acciones);

        Label tituloResultados = etiqueta(
                "Organizadores disponibles", "section-title");

        VBox resultados = new VBox(
                12, tituloResultados, listaOrganizadores);

        VBox.setVgrow(listaOrganizadores, Priority.ALWAYS);

        VBox perfil = crearPanelPerfil();

        HBox columnas = new HBox(24, resultados, perfil);
        HBox.setHgrow(resultados, Priority.ALWAYS);

        perfil.setPrefWidth(320);
        perfil.setMinWidth(280);
        perfil.setMaxWidth(360);

        columnas.widthProperty().addListener(
                (observable, anterior, actual) -> {

                    if (actual.doubleValue() < 650) {
                        perfil.setPrefWidth(
                                Math.max(220, actual.doubleValue() * 0.4));
                    } else {
                        perfil.setPrefWidth(320);
                    }
                });

        columnas.setAlignment(Pos.TOP_LEFT);

        contenido.getChildren().addAll(
                tarjetaFiltros, columnas);

        return contenido;
    }

    private VBox crearPanelPerfil() {

        Label encabezado = etiqueta(
                "Perfil del organizador", "card-title");

        VBox panel = new VBox(
                15,
                encabezado,
                detalleNombre,
                new Separator(),
                detalleEmpresa,
                detalleCiudad,
                detalleEspecialidad,
                detallePortafolio,
                detalleTarifa,
                detalleResenas,
                new Separator(),
                disponibilidad,
                solicitarContratacion);

        panel.getStyleClass().add("info-card");
        panel.setMinWidth(0);

        detalleNombre.setWrapText(true);
        detalleEmpresa.setWrapText(true);
        detalleCiudad.setWrapText(true);
        detalleEspecialidad.setWrapText(true);
        detalleTarifa.setWrapText(true);
        detalleResenas.setWrapText(true);

        return panel;
    }

    private void configurarListaOrganizadores() {

        listaOrganizadores.setCellFactory(
                lista -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            Organizador item, boolean empty) {

                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setText(null);
                            return;
                        }

                        String empresa = texto(
                                item.empresa(), "Empresa no registrada");

                        String ciudadOrganizador = texto(
                                item.ciudad(), "Ciudad no registrada");

                        String tarifa = formatoMoneda.format(
                                item.tarifaBase() == null
                                        ? BigDecimal.ZERO
                                        : item.tarifaBase());

                        BigDecimal promedio =
                                item.calificacionPromedio() == null
                                        ? BigDecimal.ZERO
                                        : item.calificacionPromedio();

                        setText(
                                item.nombre()
                                + "\n" + empresa
                                + " · " + ciudadOrganizador
                                + "\nEspecialidad: "
                                + texto(item.especialidad(), "No registrada")
                                + "\nTarifa base: " + tarifa
                                + "\nCalificación: "
                                + promedio.setScale(
                                        1,
                                        java.math.RoundingMode.HALF_UP)
                                + " / 5 · "
                                + item.totalResenas() + " reseñas");
                    }
                });

        listaOrganizadores.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) ->
                        mostrarPerfil(actual));
    }

    /**
     * Valida los campos antes de convertir el presupuesto
     * y consultar los organizadores.
     */
    public void mostrarOrganizadores() {

        String tipo = tipoEvento.getText().trim();
        String ciudadBusqueda = ciudad.getText().trim();
        LocalDate fecha = fechaAproximada.getValue();
        String presupuestoIngresado = presupuesto.getText().trim();

        if (tipo.isEmpty()
                || ciudadBusqueda.isEmpty()
                || fecha == null
                || presupuestoIngresado.isEmpty()) {

            mostrarMensaje(
                    "Completa los campos obligatorios antes de buscar:\n\n"
                    + "- Tipo de evento\n"
                    + "- Ciudad\n"
                    + "- Fecha aproximada\n"
                    + "- Presupuesto máximo");

            return;
        }

        BigDecimal valorPresupuesto;

        try {
            valorPresupuesto = leerPresupuesto(
                    presupuestoIngresado);

        } catch (NumberFormatException e) {
            mostrarMensaje(
                    "Ingresa un presupuesto válido, por ejemplo: 2500000.");
            return;
        }

        if (valorPresupuesto.compareTo(BigDecimal.ZERO) < 0) {
            mostrarMensaje("El presupuesto no puede ser negativo.");
            return;
        }

        try {
            Map<String, Object> filtros = new HashMap<>();

            filtros.put("tipoEvento", tipo);
            filtros.put("ciudad", ciudadBusqueda);
            filtros.put("fechaAproximada", fecha);
            filtros.put("presupuestoEstimado", valorPresupuesto);

            if (!criterios.getText().trim().isEmpty()) {
                filtros.put(
                        "criterios",
                        criterios.getText().trim());
            }

            listaOrganizadores.getItems().clear();
            limpiarPerfil();

            List<Organizador> coincidencias =
                    controller.consultarOrganizadores(filtros);

            listaOrganizadores.getItems().setAll(coincidencias);

            if (coincidencias.isEmpty()) {
                mostrarMensaje(
                        "No encontramos organizadores para esos criterios. "
                        + "Prueba con otra ciudad, fecha o presupuesto.");
            }

        } catch (RuntimeException e) {
            mostrarMensaje(
                    mensajeDeError(
                            e,
                            "No fue posible buscar organizadores."));
        }
    }

    /**
     * Consulta y presenta los datos del perfil seleccionado.
     */
    private void mostrarPerfil(Organizador organizador) {

        organizadorSeleccionado = null;
        solicitarContratacion.setDisable(true);

        if (organizador == null) {
            limpiarPerfil();
            return;
        }

        try {
            Organizador perfil =
                    controller.seleccionarOrganizador(
                            organizador.id());
            
        organizadorSeleccionado = perfil;

            detalleNombre.setText(perfil.nombre());

            detalleEmpresa.setText(
                    "Empresa: "
                    + texto(perfil.empresa(), "No registrada"));

            detalleCiudad.setText(
                    "Ciudad: "
                    + texto(perfil.ciudad(), "No registrada"));

            detalleEspecialidad.setText(
                    "Especialidad: "
                    + texto(perfil.especialidad(), "No registrada"));

            detallePortafolio.setText(
                    "Portafolio: "
                    + texto(perfil.portafolio(), "No registrado"));

            detalleTarifa.setText(
                    "Tarifa base: "
                    + formatoMoneda.format(
                            perfil.tarifaBase() == null
                                    ? BigDecimal.ZERO
                                    : perfil.tarifaBase()));

            BigDecimal promedio =
                    perfil.calificacionPromedio() == null
                            ? BigDecimal.ZERO
                            : perfil.calificacionPromedio();

            detalleResenas.setText(
                    "Calificación: "
                    + promedio.setScale(
                            1,
                            java.math.RoundingMode.HALF_UP)
                    + " / 5 · "
                    + perfil.totalResenas() + " reseñas");

            LocalDate fecha = fechaAproximada.getValue();

            if (fecha == null) {
                disponibilidad.setText(
                        "Selecciona una fecha para comprobar "
                        + "la disponibilidad.");
                return;
            }

            boolean disponible =
                    controller.verificarDisponibilidadOrganizador(
                            perfil.id(), fecha);

            if (disponible) {
                disponibilidad.setText(
                        "Disponible para la fecha indicada.");

                solicitarContratacion.setDisable(false);

            } else {
                disponibilidad.setText(
                        "No está disponible o no cumple las condiciones "
                        + "para la fecha indicada. Selecciona otro organizador.");
            }

        } catch (RuntimeException e) {
            limpiarPerfil();

            mostrarMensaje(
                    mensajeDeError(
                            e,
                            "No se pudo consultar el perfil."));
        }
    }

    // ---------------------------------------------------------
    // CONFIRMACIÓN Y ENVÍO
    // ---------------------------------------------------------

    private void enviarSolicitud() {

        if (organizadorSeleccionado == null) {
            mostrarMensaje("Selecciona un organizador.");
            return;
        }

        try {
            LocalDate fecha = fechaAproximada.getValue();

            if (fecha == null) {
                mostrarMensaje(
                        "Ingresa la fecha aproximada del evento.");
                return;
            }

            BigDecimal valorPresupuesto =
                    leerPresupuesto(presupuesto.getText());

            boolean disponible =
                    controller.verificarDisponibilidadOrganizador(
                            organizadorSeleccionado.id(), fecha);

            if (!disponible) {
                solicitarContratacion.setDisable(true);

                mostrarMensaje(
                        "El organizador ya no está disponible. "
                        + "Busca nuevamente y selecciona otra opción.");

                return;
            }

            Alert confirmacion = new Alert(
                    Alert.AlertType.CONFIRMATION);

            confirmacion.setTitle("Confirmar contratación");

            confirmacion.setHeaderText(
                    "Enviar solicitud a "
                    + organizadorSeleccionado.nombre());

            confirmacion.setContentText(
                    "Tipo de evento: " + tipoEvento.getText().trim()
                    + "\nCiudad: " + ciudad.getText().trim()
                    + "\nFecha aproximada: " + fecha
                    + "\nPresupuesto máximo: "
                    + formatoMoneda.format(valorPresupuesto)
                    + "\n\nLa solicitud quedará en estado Pendiente.");

            Optional<ButtonType> respuesta =
                    confirmacion.showAndWait();

            if (respuesta.isEmpty()
                    || respuesta.get() != ButtonType.OK) {
                return;
            }

            Contratacion datos = new Contratacion(
                    0,
                    organizadorSeleccionado.id(),
                    tipoEvento.getText().trim(),
                    ciudad.getText().trim(),
                    fecha,
                    valorPresupuesto,
                    criterios.getText().trim());

            controller.contratarOrganizador(
                    organizadorSeleccionado.id(),
                    datos);

            // La solicitud ya fue guardada correctamente.
int idSolicitud = datos.getIdContratacion();

organizadorSeleccionado = null;

listaOrganizadores.getSelectionModel().clearSelection();
solicitarContratacion.setDisable(true);

limpiarPerfil();

// Actualizar las solicitudes del cliente.
mostrarContrataciones();

// Navegar directamente a "Mis solicitudes".
if (pestanasPrincipal != null
        && pestanaMisSolicitudes != null) {

    pestanasPrincipal.getSelectionModel()
            .select(pestanaMisSolicitudes);
}

// Confirmar el resultado al cliente.
mostrarMensaje(
        "Solicitud #" + idSolicitud
        + " registrada correctamente con estado PENDIENTE. "
        + "Se envió una notificación interna al organizador.");

            organizadorSeleccionado = null;

            listaOrganizadores.getSelectionModel()
                    .clearSelection();

            solicitarContratacion.setDisable(true);
            limpiarPerfil();

        } catch (RuntimeException e) {
            mostrarMensaje(
                    mensajeDeError(
                            e,
                            "No fue posible completar la solicitud. "
                            + "Inténtalo nuevamente."));
        }
    }

    // ---------------------------------------------------------
    // PESTAÑA DE SOLICITUDES
    // ---------------------------------------------------------

    private Parent crearVistaContrataciones() {

        VBox contenido = new VBox(20);
        contenido.getStyleClass().add("content");

        Label titulo = etiqueta(
                "Mis solicitudes de contratación", "page-title");

        Label ayuda = etiqueta(
                "Consulta las solicitudes enviadas y el estado "
                + "registrado para cada organizador.",
                "muted");

        ayuda.setWrapText(true);

        Button actualizar = new Button("Actualizar solicitudes");
        actualizar.getStyleClass().add("btn-secondary");
        actualizar.setOnAction(
                event -> mostrarContrataciones());

        listaContrataciones.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(listaContrataciones, Priority.ALWAYS);

        contenido.getChildren().addAll(
                titulo,
                ayuda,
                listaContrataciones,
                actualizar);

        return contenido;
    }

    private void configurarListaContrataciones() {

        listaContrataciones.setCellFactory(
                lista -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            Contratacion item, boolean empty) {

                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setText(null);
                            return;
                        }

                        setText(
                                "Solicitud #"
                                + item.getIdContratacion()
                                + "\nOrganizador #"
                                + item.getIdOrganizador()
                                + " · Estado: " + item.getEstado()
                                + "\nEvento: " + item.getTipoEvento()
                                + " · Ciudad: " + item.getCiudad()
                                + "\nFecha aproximada: "
                                + item.getFechaAproximada()
                                + "\nPresupuesto: "
                                + formatoMoneda.format(
                                        item.getPresupuestoEstimado()));
                    }
                });
    }

    /**
     * Consulta las solicitudes del cliente autenticado.
     * El controlador limita el resultado a su identificador.
     */
    public void mostrarContrataciones() {

        try {
            listaContrataciones.getItems().setAll(
                    controller.consultarContrataciones(null));

        } catch (RuntimeException e) {
            mostrarMensaje(
                    mensajeDeError(
                            e,
                            "No se pudieron consultar las solicitudes."));
        }
    }

    // ---------------------------------------------------------
    // COMPONENTES AUXILIARES
    // ---------------------------------------------------------

    private GridPane crearGrilla() {

        GridPane grilla = new GridPane();
        grilla.setHgap(23);
        grilla.setVgap(20);

        for (int i = 0; i < 2; i++) {
            ColumnConstraints columna =
                    new ColumnConstraints();

            columna.setPercentWidth(50);
            columna.setHgrow(Priority.ALWAYS);

            grilla.getColumnConstraints().add(columna);
        }

        return grilla;
    }

    private VBox grupo(
            String titulo, javafx.scene.Node control) {

        VBox grupo = new VBox(
                9,
                etiqueta(titulo, "field-label"),
                control);

        return grupo;
    }

    private Label etiqueta(
            String texto, String estilo) {

        Label label = new Label(texto);
        label.getStyleClass().add(estilo);

        return label;
    }

    private BigDecimal leerPresupuesto(String texto) {

        if (texto == null || texto.isBlank()) {
            throw new NumberFormatException(
                    "El presupuesto está vacío.");
        }

        String normalizado = texto
                .trim()
                .replace("$", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".");

        return new BigDecimal(normalizado);
    }

    private String texto(
            String valor, String predeterminado) {

        return valor == null || valor.isBlank()
                ? predeterminado
                : valor;
    }

    private String mensajeDeError(
            RuntimeException e, String predeterminado) {

        return e.getMessage() == null
                || e.getMessage().isBlank()
                ? predeterminado
                : e.getMessage();
    }

    private void limpiarPerfil() {

        detalleNombre.setText(
                "Selecciona un organizador");

        detalleEmpresa.setText("");
        detalleCiudad.setText("");
        detalleEspecialidad.setText("");
        detallePortafolio.setText("");
        detalleTarifa.setText("");
        detalleResenas.setText("");

        disponibilidad.setText(
                "Busca organizadores para consultar su disponibilidad.");

        solicitarContratacion.setDisable(true);
    }

    public void mostrarMensaje(String mensaje) {

        Alert alerta = new Alert(
                Alert.AlertType.INFORMATION);

        alerta.setTitle("Contratar organizador");
        alerta.setHeaderText(null);

        alerta.setContentText(
                mensaje == null || mensaje.isBlank()
                        ? "No fue posible completar la operación."
                        : mensaje);

        alerta.showAndWait();
    }
}
