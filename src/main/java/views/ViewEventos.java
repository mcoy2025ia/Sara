package views;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import controllers.ControllerEventos;
import entities.Evento;
import entities.Organizador;
import entities.SesionUsuario;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import views.components.MenuCompartido;

public class ViewEventos {

    private final ControllerEventos controller;
    private final SesionUsuario sesion;
    private final Runnable abrirEventos;
    private final Runnable abrirCatalogo;
    private final Runnable abrirInvitados;
    private final Runnable cerrarSesion;
    private final Runnable abrirMisEventos;
    private final Runnable abrirPropuestas;
    private final TextField nombre = campo("nombre", "Ej. Festival NextEventux");
    private final ComboBox<String> tipo = new ComboBox<>();
    private final ComboBox<Organizador> organizador = new ComboBox<>();
    private final DatePicker fechaInicio = new DatePicker();
    private final DatePicker fechaFin = new DatePicker();
    private final TextField horaInicio = campo("horaInicio", "09:00");
    private final TextField horaFin = campo("horaFin", "18:00");
    private final TextField lugar = campo("lugar", "Ej. Bogotá, Colombia");
    private final TextField descripcion = campo("descripcion", "Descripción opcional del evento");
    private final TextField presupuesto = campo("presupuesto", "Ej. 15000000");
    private final TextField invitados = campo("invitados", "Ej. 100");
    private final Label resumenPresupuesto = new Label("$0 COP");
    private final Label resumenInvitados = new Label("0");
    private final Label mensaje = new Label();
    private final NumberFormat formatoCOP = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));

    private java.util.function.Consumer<Evento> abrirSolicitudes;
    private final Button solicitar = new Button("Servicios y presupuesto");
    private Evento eventoCreado;

    public ViewEventos conSolicitudes(java.util.function.Consumer<Evento> accion) {
        abrirSolicitudes = accion;
        return this;
    }

    public ViewEventos(ControllerEventos controller) {
        this(controller, null, null, null, null, null, null, null);
    }

    public ViewEventos(
            ControllerEventos controller,
            Runnable abrirCatalogo,
            Runnable cerrarSesion) {

        this(controller, null, abrirCatalogo, null, cerrarSesion, null, null, null);
    }

    public ViewEventos(
            ControllerEventos controller,
            Runnable abrirEventos,
            Runnable abrirCatalogo,
            Runnable abrirInvitados,
            Runnable cerrarSesion,
            SesionUsuario sesion) {

        this(controller, abrirEventos, abrirCatalogo, abrirInvitados, cerrarSesion, sesion, null, null);
    }

    public ViewEventos(
            ControllerEventos controller,
            Runnable abrirEventos,
            Runnable abrirCatalogo,
            Runnable abrirInvitados,
            Runnable cerrarSesion,
            SesionUsuario sesion,
            Runnable abrirMisEventos) {

        this(controller, abrirEventos, abrirCatalogo, abrirInvitados,
                cerrarSesion, sesion, abrirMisEventos, null);
    }

    public ViewEventos(
            ControllerEventos controller,
            Runnable abrirEventos,
            Runnable abrirCatalogo,
            Runnable abrirInvitados,
            Runnable cerrarSesion,
            SesionUsuario sesion,
            Runnable abrirMisEventos,
            Runnable abrirPropuestas) {

        this.controller = controller;
        this.abrirEventos = abrirEventos;
        this.abrirCatalogo = abrirCatalogo;
        this.abrirInvitados = abrirInvitados;
        this.cerrarSesion = cerrarSesion;
        this.sesion = sesion;
        this.abrirMisEventos = abrirMisEventos;
        this.abrirPropuestas = abrirPropuestas;

        formatoCOP.setMaximumFractionDigits(2);
        organizador.setId("organizador");
        organizador.setPromptText("Seleccionar organizador");
        organizador.setMaxWidth(Double.MAX_VALUE);
        organizador.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Organizador item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? organizador.getPromptText() : item.nombre());
            }
        });
        tipo.setId("tipo");
        tipo.setPromptText("Seleccionar tipo");
        tipo.getItems().addAll("Corporativo", "Social", "Cultural", "Otro");
        tipo.setMaxWidth(Double.MAX_VALUE);
        tipo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "Seleccionar tipo" : item);
            }
        });
        fechaInicio.setId("fechaInicio");
        fechaFin.setId("fechaFin");
        fechaInicio.setEditable(false);
        fechaFin.setEditable(false);
        fechaInicio.setMaxWidth(Double.MAX_VALUE);
        fechaFin.setMaxWidth(Double.MAX_VALUE);
        mensaje.setId("mensaje");
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);
        resumenPresupuesto.setId("resumenPresupuesto");
        resumenInvitados.setId("resumenInvitados");
        presupuesto.textProperty().addListener((observable, anterior, actual) -> actualizarResumen());
        invitados.textProperty().addListener((observable, anterior, actual) -> actualizarResumen());
    }

    public Parent crearPantalla() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");
        VBox sidebar = crearMenu();
        raiz.setLeft(sidebar);
        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());
        VBox contenido = new VBox(30);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1320);
        VBox titulo = new VBox(8, etiqueta("Crear nuevo evento", "page-title"),
                etiqueta("Completa la información para registrar tu próximo evento.", "muted"));
        HBox encabezado = new HBox(20, titulo, espacioHorizontal(), etiqueta("● En planeación", "status"));
        encabezado.setAlignment(Pos.CENTER_LEFT);
        VBox formulario = crearFormulario();
        VBox panel = crearPanelDerecho();
        GridPane columnas = new GridPane();
        columnas.setHgap(25);
        columnas.setVgap(25);
        ColumnConstraints izquierda = new ColumnConstraints();
        izquierda.setHgrow(Priority.ALWAYS);
        ColumnConstraints derecha = new ColumnConstraints(290);
        columnas.getColumnConstraints().addAll(izquierda, derecha);
        columnas.add(formulario, 0, 0);
        columnas.add(panel, 1, 0);
        GridPane.setHgrow(formulario, Priority.ALWAYS);
        formulario.setMinWidth(0);
        contenido.getChildren().addAll(encabezado, columnas);
        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        principal.setCenter(scroll);
        raiz.setCenter(principal);
        // En ventanas pequeñas el panel baja y el menú deja espacio al formulario.
        raiz.widthProperty().addListener((observable, anterior, actual) -> {
            boolean ancho = actual.doubleValue() >= 1200;
            if (ancho != (GridPane.getColumnIndex(panel) == 1)) {
                columnas.getChildren().clear();
                columnas.getColumnConstraints().setAll(izquierda);
                if (ancho) {
                    columnas.getColumnConstraints().add(derecha);
                }
                columnas.add(formulario, 0, 0);
                columnas.add(panel, ancho ? 1 : 0, ancho ? 0 : 1);
                panel.setMaxWidth(Double.MAX_VALUE);
                GridPane.setHgrow(panel, Priority.ALWAYS);
            }
            sidebar.setVisible(actual.doubleValue() >= 1000);
            sidebar.setManaged(sidebar.isVisible());
        });
        cargarOrganizadores();
        return raiz;
    }

    private void cargarOrganizadores() {
        try {
            if (sesion == null) {
                organizador.getItems().setAll(controller.listarOrganizadores());
            } else {
                organizador.getItems().setAll(new Organizador(sesion.getIdActor(), controller.nombreOrganizadorEdicion()));
                organizador.getSelectionModel().selectFirst();
                organizador.setDisable(true);
            }
            if (organizador.getItems().isEmpty()) {
                mostrarMensaje("No hay organizadores registrados. Registra un organizador antes de crear el evento.", false);
            }
        } catch (IllegalStateException e) {
            mostrarMensaje(e.getMessage(), false);
        }
    }

    private VBox crearMenu() {
        return MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.CREAR_EVENTO,
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
        return MenuCompartido.crearBarraSuperior(sesion, "Crear evento");
    }

    private VBox crearFormulario() {
        VBox tarjeta = new VBox(22);
        tarjeta.getStyleClass().add("card");
        HBox titulo = new HBox(13, etiqueta("▤", "heading-icon"), etiqueta("Información del evento", "card-title"));
        titulo.setAlignment(Pos.CENTER_LEFT);
        Label ayuda = etiqueta("Ingresa los datos generales de tu evento. Los campos marcados con * son obligatorios.", "muted");
        ayuda.setWrapText(true);
        GridPane datos = grilla();
        datos.add(grupo("Nombre del evento *", nombre, null), 0, 0, 2, 1);
        datos.add(grupo("Tipo de evento *", tipo, null), 0, 1);
        datos.add(grupo("Organizador *", organizador, null), 1, 1);
        datos.add(grupo("Fecha de inicio *", fechaInicio, null), 0, 2);
        datos.add(grupo("Hora de inicio *", horaInicio, "Formato de 24 horas: HH:mm"), 1, 2);
        datos.add(grupo("Fecha de fin *", fechaFin, null), 0, 3);
        datos.add(grupo("Hora de fin *", horaFin, "Formato de 24 horas: HH:mm"), 1, 3);
        datos.add(grupo("Lugar del evento *", lugar, null), 0, 4, 2, 1);
        datos.add(grupo("Descripción", descripcion, null), 0, 5, 2, 1);
        GridPane recursos = grilla();
        recursos.add(grupo("Presupuesto total (COP) *", presupuesto, "Ingresa el presupuesto asignado."), 0, 0);
        recursos.add(grupo("Invitados iniciales *", invitados, "Número inicial de invitados."), 1, 0);
        Button cancelar = new Button("Cancelar");
        cancelar.setId("cancelar");
        cancelar.getStyleClass().add("btn-secondary");
        cancelar.setOnAction(event -> limpiarFormulario());
        Button crear = new Button("+ Crear evento");
        crear.setId("crear");
        crear.getStyleClass().add("btn-primary");
        crear.setDefaultButton(true);
        crear.setOnAction(event -> crearDesdeFormulario());
        HBox acciones = new HBox(12, cancelar, crear);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        tarjeta.getChildren().addAll(titulo, ayuda, datos, new Separator(),
                etiqueta("Presupuesto y asistentes", "section-title"), recursos, mensaje, acciones);
        solicitar.setId("serviciosEventoCreado");
        solicitar.getStyleClass().add("btn-secondary");
        solicitar.setDisable(eventoCreado == null || abrirSolicitudes == null);
        solicitar.setOnAction(e -> abrirSolicitudes.accept(eventoCreado));
        tarjeta.getChildren().add(solicitar);
        return tarjeta;
    }

    private VBox crearPanelDerecho() {
        VBox panel = new VBox(20);
        panel.setPrefWidth(290);
        panel.setMinWidth(260);
        VBox inicio = informacion("✦ Tu evento comienza aquí", "Registra la información inicial de tu evento para comenzar su planeación.");
        inicio.getStyleClass().add("highlight");
        VBox resumen = new VBox(14, etiqueta("Resumen del evento", "card-title"),
                filaResumen("Estado", new Label("En planeación")), new Separator(),
                filaResumen("Presupuesto", resumenPresupuesto), new Separator(),
                filaResumen("Invitados", resumenInvitados));
        resumen.getStyleClass().add("info-card");
        panel.getChildren().addAll(inicio, resumen, informacion("ⓘ Información importante",
                "Tu evento comenzará en estado En planeación. Sus datos se guardarán y estarán disponibles después de cerrar la aplicación."));
        return panel;
    }

    private void crearDesdeFormulario() {
        try {
            if (organizador.getValue() == null) {
                throw new IllegalArgumentException("Selecciona un organizador.");
            }
            if (fechaInicio.getValue() == null || fechaFin.getValue() == null) {
                throw new IllegalArgumentException("Selecciona las fechas de inicio y fin.");
            }
            LocalTime inicio = leerHora(horaInicio.getText(), "inicio");
            LocalTime fin = leerHora(horaFin.getText(), "fin");
            BigDecimal valor;
            try {
                valor = new BigDecimal(presupuesto.getText().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Ingresa un presupuesto numérico; usa punto para los decimales.");
            }
            Evento datos = new Evento(organizador.getValue().id(), nombre.getText().trim(),
                    tipo.getValue(), descripcion.getText().trim(), LocalDateTime.of(fechaInicio.getValue(), inicio),
                    LocalDateTime.of(fechaFin.getValue(), fin), lugar.getText().trim(), valor,
                    leerEntero(invitados.getText(), "invitados iniciales"));
            controller.crearEvento(datos);
            eventoCreado = datos;
            solicitar.setDisable(abrirSolicitudes == null);
            mostrarMensaje("Evento creado correctamente. Número de evento: " + datos.getIdEvento() + ".", true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarMensaje(e.getMessage(), false);
        }
    }

    private LocalTime leerHora(String texto, String campo) {
        try {
            return LocalTime.parse(texto.trim(), DateTimeFormatter.ofPattern("HH:mm")
                    .withResolverStyle(java.time.format.ResolverStyle.STRICT));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ingresa una hora de " + campo + " válida en formato HH:mm.");
        }
    }

    private int leerEntero(String texto, String campo) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ingresa un número entero válido para " + campo + ".");
        }
    }

    private void actualizarResumen() {
        try {
            BigDecimal valor = new BigDecimal(presupuesto.getText().trim());
            resumenPresupuesto.setText(valor.signum() < 0 ? "$0 COP" : "$" + formatoCOP.format(valor) + " COP");
        } catch (NumberFormatException e) {
            resumenPresupuesto.setText("$0 COP");
        }
        try {
            int cantidad = Integer.parseInt(invitados.getText().trim());
            resumenInvitados.setText(Integer.toString(Math.max(0, cantidad)));
        } catch (NumberFormatException e) {
            resumenInvitados.setText("0");
        }
    }

    private void limpiarFormulario() {
        eventoCreado = null;
        solicitar.setDisable(true);
        for (TextField campo : new TextField[]{nombre, horaInicio, horaFin, lugar, descripcion, presupuesto, invitados}) {
            campo.clear();
        }
        if (sesion == null) organizador.getSelectionModel().clearSelection();
        tipo.getSelectionModel().clearSelection();
        fechaInicio.setValue(null);
        fechaFin.setValue(null);
        mensaje.setVisible(false);
        mensaje.setManaged(false);
        nombre.requestFocus();
    }

    private void mostrarMensaje(String texto, boolean exito) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("message", exito ? "success" : "error");
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private static TextField campo(String id, String ayuda) {
        TextField campo = new TextField();
        campo.setId(id);
        campo.setPromptText(ayuda);
        return campo;
    }

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

    private GridPane grilla() {
        GridPane grid = new GridPane();
        grid.setHgap(23);
        grid.setVgap(23);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(50);
            columna.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(columna);
        }
        return grid;
    }

    private VBox grupo(String titulo, javafx.scene.Node control, String ayuda) {
        VBox grupo = new VBox(9, etiqueta(titulo, "field-label"), control);
        if (ayuda != null) {
            grupo.getChildren().add(etiqueta(ayuda, "hint"));
        }
        return grupo;
    }

    private VBox informacion(String titulo, String texto) {
        Label descripcion = etiqueta(texto, "muted");
        descripcion.setWrapText(true);
        VBox tarjeta = new VBox(14, etiqueta(titulo, "card-title"), descripcion);
        tarjeta.getStyleClass().add("info-card");
        return tarjeta;
    }

    private HBox filaResumen(String titulo, Label valor) {
        valor.getStyleClass().add("summary-value");
        HBox fila = new HBox(10, etiqueta(titulo, "hint"), espacioHorizontal(), valor);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }
}
