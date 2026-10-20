package views;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.Consumer;

import controllers.ControllerProveedores;
import entities.Servicio;
import entities.SesionUsuario;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import views.components.MenuCompartido;

public class ViewCatalogoServicios {

    private final ControllerProveedores controller;
    private final SesionUsuario sesion;
    private final Runnable abrirCatalogo;
    private final Runnable abrirEventos;
    private final Runnable abrirInvitados;
    private final Runnable abrirMisEventos;
    private final Runnable cerrarSesion;
    private final Consumer<Servicio> abrirDetalle;

    private final TextField busqueda
            = new TextField();

    private final ComboBox<String> categoriaFiltro
            = new ComboBox<>();

    private final TextField precioMinimo
            = new TextField();

    private final TextField precioMaximo
            = new TextField();

    private final ListView<Servicio> listaServicios
            = new ListView<>();

    private final List<Servicio> serviciosDisponibles
            = new ArrayList<>();

    private final NumberFormat formatoCOP
            = NumberFormat.getNumberInstance(
                    Locale.forLanguageTag("es-CO"));

    public ViewCatalogoServicios(
            ControllerProveedores controller,
            SesionUsuario sesion,
            Runnable abrirEventos,
            Runnable cerrarSesion,
            Consumer<Servicio> abrirDetalle) {

        this(controller, sesion, abrirEventos, null, null,
                cerrarSesion, abrirDetalle);
    }

    public ViewCatalogoServicios(
            ControllerProveedores controller,
            SesionUsuario sesion,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable cerrarSesion,
            Consumer<Servicio> abrirDetalle) {

        this(controller, sesion, abrirEventos, abrirInvitados, null,
                cerrarSesion, abrirDetalle);
    }

    public ViewCatalogoServicios(
            ControllerProveedores controller,
            SesionUsuario sesion,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable cerrarSesion,
            Consumer<Servicio> abrirDetalle) {

        this(controller, sesion, abrirEventos, abrirInvitados, abrirCatalogo,
                cerrarSesion, abrirDetalle, null);
    }

    public ViewCatalogoServicios(
            ControllerProveedores controller,
            SesionUsuario sesion,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable cerrarSesion,
            Consumer<Servicio> abrirDetalle,
            Runnable abrirMisEventos) {

        this.controller = controller;
        this.sesion = sesion;
        this.abrirCatalogo = abrirCatalogo;
        this.abrirEventos = abrirEventos;
        this.abrirInvitados = abrirInvitados;
        this.abrirMisEventos = abrirMisEventos;
        this.cerrarSesion = cerrarSesion;
        this.abrirDetalle = abrirDetalle;

        formatoCOP.setMaximumFractionDigits(0);

        busqueda.setPromptText(
                "Buscar por nombre, categoría, ciudad...");

        precioMinimo.setPromptText(
                "Mínimo");

        precioMaximo.setPromptText(
                "Máximo");

        categoriaFiltro
                .getItems()
                .add("Todas");

        categoriaFiltro
                .getSelectionModel()
                .selectFirst();

        configurarFiltros();
        configurarLista();
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
                                "Proveedores y servicios",
                                "page-title"),
                        etiqueta(
                                "Explora los servicios publicados y encuentra el que mejor se adapte a tu evento.",
                                "muted"));

        HBox encabezado
                = new HBox(
                        20,
                        titulo,
                        espacioHorizontal(),
                        etiqueta(
                                "● Servicios disponibles",
                                "status"));

        encabezado.setAlignment(
                Pos.CENTER_LEFT);

        VBox catalogo
                = crearCatalogo();

        contenido
                .getChildren()
                .addAll(
                        encabezado,
                        catalogo);

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

                    sidebar.setVisible(
                            actual.doubleValue() >= 1000);

                    sidebar.setManaged(
                            sidebar.isVisible());
                });

        recargarServicios();

        return raiz;
    }

    private VBox crearMenu() {
        return MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.CATALOGO,
                abrirEventos,
                abrirInvitados,
                abrirCatalogo,
                null,
                cerrarSesion,
                abrirMisEventos);
    }

    private HBox crearBarraSuperior() {
        return MenuCompartido.crearBarraSuperior(sesion, "Proveedores");
    }

    private VBox crearCatalogo() {

        VBox tarjeta
                = new VBox(24);

        tarjeta.getStyleClass().add(
                "card");

        HBox titulo
                = new HBox(
                        13,
                        etiqueta(
                                "♧",
                                "heading-icon"),
                        etiqueta(
                                "Servicios publicados",
                                "card-title"));

        titulo.setAlignment(
                Pos.CENTER_LEFT);

        Label ayuda
                = etiqueta(
                        "Utiliza los filtros para encontrar rápidamente el servicio que necesitas.",
                        "muted");

        ayuda.setWrapText(true);

        FlowPane filtros
                = new FlowPane();

        filtros.setHgap(16);
        filtros.setVgap(14);

        filtros.getChildren().addAll(
                grupoFiltro(
                        "Buscar servicio",
                        busqueda,
                        350),
                grupoFiltro(
                        "Categoría",
                        categoriaFiltro,
                        190),
                grupoFiltro(
                        "Precio mínimo",
                        precioMinimo,
                        145),
                grupoFiltro(
                        "Precio máximo",
                        precioMaximo,
                        145));

        listaServicios
                .getStyleClass()
                .add("catalog-service-list");

        listaServicios.setPrefHeight(
                590);

        tarjeta
                .getChildren()
                .addAll(
                        titulo,
                        ayuda,
                        filtros,
                        new Separator(),
                        listaServicios);

        return tarjeta;
    }

    private VBox grupoFiltro(
            String titulo,
            Control control,
            double ancho) {

        Label label
                = etiqueta(
                        titulo,
                        "field-label");

        control.setPrefWidth(ancho);
        control.setMinWidth(ancho);

        return new VBox(
                7,
                label,
                control);
    }

    private void configurarFiltros() {

        busqueda
                .textProperty()
                .addListener(
                        (observable,
                                anterior,
                                actual)
                        -> aplicarFiltros());

        precioMinimo
                .textProperty()
                .addListener(
                        (observable,
                                anterior,
                                actual)
                        -> aplicarFiltros());

        precioMaximo
                .textProperty()
                .addListener(
                        (observable,
                                anterior,
                                actual)
                        -> aplicarFiltros());

        categoriaFiltro.setOnAction(
                event
                -> aplicarFiltros());
    }

    private void configurarLista() {

        listaServicios.setCellFactory(
                lista
                -> new ListCell<>() {

            {
                getStyleClass().add(
                        "catalog-service-cell");

                setOnMouseClicked(
                        event -> {

                            Servicio servicio
                            = getItem();

                            if (!isEmpty()
                            && servicio != null
                            && abrirDetalle != null) {

                                abrirDetalle.accept(
                                        servicio);
                            }
                        });
            }

            @Override
            protected void updateItem(
                    Servicio servicio,
                    boolean empty) {

                super.updateItem(
                        servicio,
                        empty);

                if (empty
                        || servicio == null) {

                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label icono
                        = etiqueta(
                                iconoCategoria(
                                        servicio.getCategoria()),
                                "catalog-category-icon");

                Label nombre
                        = etiqueta(
                                servicio.getNombre(),
                                "catalog-service-name");

                Label detalle
                        = etiqueta(
                                seguro(
                                        servicio.getCategoria())
                                + " · "
                                + seguro(
                                        servicio.getCiudadZona()),
                                "muted");

                VBox informacion
                        = new VBox(
                                5,
                                nombre,
                                detalle);

                Label precio
                        = etiqueta(
                                "$"
                                + formatoCOP.format(
                                        servicio.getPrecio())
                                + " COP",
                                "summary-value");

                Region espacio
                        = new Region();

                HBox.setHgrow(
                        espacio,
                        Priority.ALWAYS);

                HBox fila
                        = new HBox(
                                15,
                                icono,
                                informacion,
                                espacio,
                                precio);

                fila.setAlignment(
                        Pos.CENTER_LEFT);

                setText(null);
                setGraphic(fila);
            }
        });
    }

    private void recargarServicios() {

        try {

            serviciosDisponibles.clear();

            serviciosDisponibles.addAll(
                    controller
                            .listarServiciosDisponibles());

            TreeSet<String> categorias
                    = new TreeSet<>(
                            String.CASE_INSENSITIVE_ORDER);

            for (Servicio servicio
                    : serviciosDisponibles) {

                if (servicio.getCategoria() != null
                        && !servicio
                                .getCategoria()
                                .isBlank()) {

                    categorias.add(
                            servicio.getCategoria());
                }
            }

            categoriaFiltro
                    .getItems()
                    .setAll("Todas");

            categoriaFiltro
                    .getItems()
                    .addAll(categorias);

            categoriaFiltro
                    .getSelectionModel()
                    .selectFirst();

            aplicarFiltros();

        } catch (Exception e) {

            mostrarError(
                    e.getMessage());
        }
    }

    private void aplicarFiltros() {

        String texto
                = busqueda
                        .getText()
                        .trim()
                        .toLowerCase();

        String categoria
                = categoriaFiltro
                        .getValue();

        Double minimo
                = leerPrecio(
                        precioMinimo.getText());

        Double maximo
                = leerPrecio(
                        precioMaximo.getText());

        List<Servicio> resultado
                = new ArrayList<>();

        for (Servicio servicio
                : serviciosDisponibles) {

            String contenido
                    = (seguro(servicio.getNombre())
                            + " "
                            + seguro(servicio.getDescripcion())
                            + " "
                            + seguro(servicio.getCategoria())
                            + " "
                            + seguro(servicio.getCiudadZona())
                            + " "
                            + seguro(servicio.getTipo()))
                            .toLowerCase();

            boolean coincideTexto
                    = texto.isBlank()
                    || contenido.contains(
                            texto);

            boolean coincideCategoria
                    = categoria == null
                    || "Todas".equals(
                            categoria)
                    || categoria.equalsIgnoreCase(
                            servicio.getCategoria());

            boolean coincideMinimo
                    = minimo == null
                    || servicio.getPrecio()
                    >= minimo;

            boolean coincideMaximo
                    = maximo == null
                    || servicio.getPrecio()
                    <= maximo;

            if (coincideTexto
                    && coincideCategoria
                    && coincideMinimo
                    && coincideMaximo) {

                resultado.add(
                        servicio);
            }
        }

        listaServicios
                .getItems()
                .setAll(resultado);
    }

    private Double leerPrecio(
            String texto) {

        if (texto == null
                || texto.isBlank()) {

            return null;
        }

        try {

            return Double.parseDouble(
                    texto.trim());

        } catch (NumberFormatException e) {

            return null;
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
