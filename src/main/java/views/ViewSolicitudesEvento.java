package views;

import controllers.ControllerEventos;
import entities.Evento;
import entities.ServicioSolicitable;
import entities.SolicitudServicioEvento;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Continuación de CU01: compromisos y presupuesto del evento seleccionado. */
public class ViewSolicitudesEvento {
    private final ControllerEventos controller;
    private final Evento evento;
    private final Label resumen = new Label();
    private final Label mensaje = new Label();
    private final ComboBox<ServicioSolicitable> servicios = new ComboBox<>();
    private final ListView<SolicitudServicioEvento> solicitudes = new ListView<>();
    private final TextField presupuesto = new TextField();
    private final NumberFormat pesos = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    public ViewSolicitudesEvento(ControllerEventos controller, Evento evento) {
        this.controller = controller;
        this.evento = evento;
    }

    public void mostrar(Window owner) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Servicios y presupuesto · " + evento.getNombre());
        if (owner != null) {
            dialogo.initOwner(owner);
            dialogo.getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        resumen.setId("presupuestoDisponibleEvento");
        resumen.setWrapText(true);
        resumen.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        mensaje.setId("mensajeSolicitudEvento");
        mensaje.setWrapText(true);
        servicios.setId("servicioSolicitableEvento");
        servicios.setPromptText("Selecciona un servicio y su proveedor");
        servicios.setMaxWidth(Double.MAX_VALUE);
        solicitudes.setId("solicitudesEvento");
        solicitudes.setPrefHeight(180);
        solicitudes.setPlaceholder(new Label("Todavía no has solicitado servicios."));
        presupuesto.setId("ajustePresupuestoEvento");
        presupuesto.setPromptText("Presupuesto total, ejemplo: 1500000.00");

        Button enviar = boton("Solicitar servicio", "solicitarServicioEvento", () -> {
            if (servicios.getValue() == null) throw new IllegalArgumentException("Selecciona un servicio.");
            controller.solicitarServicio(evento.getIdEvento(), servicios.getValue().id());
        });
        Button retirar = boton("Retirar solicitud", "retirarSolicitudEvento", () -> {
            if (solicitudes.getSelectionModel().getSelectedItem() == null)
                throw new IllegalArgumentException("Selecciona una solicitud pendiente.");
            controller.retirarSolicitudServicio(evento.getIdEvento(), solicitudes.getSelectionModel().getSelectedItem().id());
        });
        Button ajustar = boton("Guardar presupuesto", "guardarPresupuestoEvento", () -> {
            BigDecimal valor;
            try { valor = new BigDecimal(presupuesto.getText().trim()); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Ingresa un presupuesto numérico; usa punto para los decimales."); }
            controller.ajustarPresupuesto(evento.getIdEvento(), valor);
        });
        boolean editable = Evento.ESTADO_INICIAL.equalsIgnoreCase(evento.getEstado());
        for (Control control : new Control[]{servicios, enviar, retirar, presupuesto, ajustar}) control.setDisable(!editable);
        Label ayuda = new Label("El servicio se solicita al precio publicado. Si el presupuesto no alcanza, puedes aumentarlo o retirar una solicitud pendiente sin propuestas.");
        ayuda.setWrapText(true);
        ayuda.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label estado = new Label(editable ? "Evento en planeación" : "Consulta del evento · " + evento.getEstado());
        VBox contenido = new VBox(14, new Label(evento.getNombre()), estado, resumen, ayuda, servicios,
                enviar, solicitudes, retirar, new Label("Ajustar presupuesto total"), new HBox(10, presupuesto, ajustar), mensaje);
        contenido.getStyleClass().add("card");
        contenido.setPrefWidth(680);
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        recargar();
        dialogo.showAndWait();
    }

    private Button boton(String texto, String id, Runnable accion) {
        Button boton = new Button(texto);
        boton.setId(id);
        boton.getStyleClass().add("btn-secondary");
        boton.setOnAction(e -> {
            try {
                accion.run();
                mensaje.setText("Cambio guardado correctamente.");
                mensaje.getStyleClass().setAll("message", "success");
                recargar();
            } catch (IllegalArgumentException | IllegalStateException ex) { error(ex); }
        });
        return boton;
    }

    private void recargar() {
        try {
            solicitudes.getItems().setAll(controller.listarSolicitudesServicio(evento.getIdEvento()));
            servicios.getItems().setAll(controller.listarServiciosSolicitables());
            var valores = controller.consultarPresupuesto(evento.getIdEvento());
            resumen.setText("Total: " + pesos.format(valores.total()) + " · Comprometido: " + pesos.format(valores.comprometido())
                    + " · Disponible: " + pesos.format(valores.disponible()));
            presupuesto.setText(valores.total().toPlainString());
        } catch (IllegalArgumentException | IllegalStateException ex) { error(ex); }
    }

    private void error(RuntimeException error) {
        mensaje.setText(error.getMessage());
        mensaje.getStyleClass().setAll("message", "error");
    }
}
