package views;

import controllers.ControllerAdministracion;
import entities.DocumentoProveedor;
import entities.NotificacionInterna;
import entities.Proveedor;
import entities.SesionUsuario;
import views.components.MenuCompartido;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ViewAdministracion {

    private final ControllerAdministracion controller;
    private final SesionUsuario sesion;
    private final Runnable cerrarSesion;

    private final TableView<Proveedor> tablaProveedores = new TableView<>();
    private final TableView<DocumentoProveedor> tablaDocumentos = new TableView<>();
    private final ListView<String> listaServicios = new ListView<>();

    private final Label nombre = new Label("Selecciona un proveedor");
    private final Label empresa = new Label("—");
    private final Label correo = new Label("—");
    private final Label telefono = new Label("—");
    private final Label direccion = new Label("—");
    private final Label estado = new Label("Sin selección");
    private final Label cantidadPendientes = new Label("0");
    private final Label mensaje = new Label();

    private final Button verificar = new Button("Verificar información");
    private final Button aprobar = new Button("Aprobar proveedor");
    private final Button rechazar = new Button("Rechazar proveedor");
    private final Button pedirCorrecciones = new Button("Solicitar correcciones");

    private final Button registrarDocumento =
            new Button("Registrar documento recibido");
    private final Button actualizarDocumento =
            new Button("Registrar documento corregido");
    private final Button validarDocumento =
            new Button("Marcar documento validado");
    private final Button corregirDocumento =
            new Button("Marcar documento por corregir");
    private final Button consultarNotificaciones =
            new Button("Ver notificaciones del proveedor");

    private boolean informacionValidada = false;

    public ViewAdministracion(ControllerAdministracion controller) {
        this(controller,null,null);
    }
    public ViewAdministracion(ControllerAdministracion controller, SesionUsuario sesion, Runnable cerrarSesion) {
        if (controller == null) {
            throw new IllegalArgumentException(
                    "El controlador de administración es obligatorio.");
        }

        this.controller = controller;
        this.sesion=sesion; this.cerrarSesion=cerrarSesion;

        configurarTablaProveedores();
        configurarTablaDocumentos();
        configurarAcciones();
    }

    // CU-030: acceso opcional a Verificar organizadores desde el mismo menú.
    private Runnable abrirVerificacionOrganizadores;

    public ViewAdministracion conVerificacionOrganizadores(Runnable accion) {
        abrirVerificacionOrganizadores = accion;
        return this;
    }

    public Parent crearPantalla() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");
        raiz.setLeft(crearMenu());

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());

        VBox contenido = new VBox(
                20,
                crearEncabezado(),
                crearResumen(),
                crearZonaTrabajo(),
                crearNotaDiseno()
        );

        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1450);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");

        principal.setCenter(scroll);
        raiz.setCenter(principal);

        cargarProveedores();

        return raiz;
    }

    // ---------------------------------------------------------
    // MENU E INFORMACION GENERAL
    // ---------------------------------------------------------

    private VBox crearMenu() {
        VBox menu = new VBox(7);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);

        VBox marca = new VBox(
                5,
                MenuCompartido.crearLogo(),
                etiqueta("GESTIÓN DE EVENTOS", "brand-caption")
        );
        marca.setAlignment(Pos.CENTER);
        VBox.setMargin(marca, new Insets(0, 0, 38, 0));

        menu.getChildren().addAll(
                marca,
                etiqueta("MENÚ PRINCIPAL", "menu-title"),
                itemMenu("✓  Aprobar proveedores", true)
        );

        if (abrirVerificacionOrganizadores != null) {
            Label verificar = itemMenu("✓  Verificar organizadores", false);
            verificar.setId("abrirVerificacionOrganizadores");
            verificar.setOnMouseClicked(event -> abrirVerificacionOrganizadores.run());
            menu.getChildren().add(verificar);
        }

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        Button volver = new Button("← Volver a publicaciones");
        volver.getStyleClass().addAll("menu-item", "menu-action");
        volver.setMaxWidth(Double.MAX_VALUE);
        volver.setOnAction(event ->
                ((Stage) volver.getScene().getWindow()).close());

        VBox perfil = new VBox(
                4,
                etiqueta(sesion==null ? "Administración" : sesion.getNombre(), "profile-name"),
                etiqueta("Revisión de proveedores", "hint")
        );

        if(cerrarSesion!=null) {
            Button salir=new Button("Cerrar sesión"); salir.setId("cerrarSesionAdministracion");
            salir.getStyleClass().add("btn-secondary"); salir.setOnAction(e->cerrarSesion.run());
            perfil.getChildren().add(salir);
        }
        HBox cuenta = new HBox(12, etiqueta("AD", "avatar"), perfil);
        cuenta.setAlignment(Pos.CENTER_LEFT);
        cuenta.getStyleClass().add("sidebar-bottom");

        menu.getChildren().addAll(espacio, volver, cuenta);

        return menu;
    }

    private Label itemMenu(String texto, boolean activo) {
        Label item = etiqueta(texto, "menu-item");
        item.setMaxWidth(Double.MAX_VALUE);

        if (activo) {
            item.getStyleClass().add("active");
        }

        return item;
    }

    private HBox crearBarraSuperior() {
        return MenuCompartido.crearBarraSuperior(sesion,"Aprobar proveedores");
    }

    private HBox crearEncabezado() {
        VBox textos = new VBox(
                8,
                etiqueta("Aprobación de proveedores", "page-title"),
                etiqueta(
                        "Revisa datos, documentos y servicios antes de decidir.",
                        "muted"
                )
        );

        HBox.setHgrow(textos, Priority.ALWAYS);

        HBox encabezado = new HBox(
                20,
                textos,
                etiqueta("● Revisión pendiente", "status")
        );

        encabezado.setAlignment(Pos.CENTER_LEFT);

        return encabezado;
    }

    private HBox crearResumen() {
        VBox pendientes = new VBox(
                8,
                etiqueta("Proveedores pendientes", "hint"),
                cantidadPendientes,
                etiqueta("Registros que requieren revisión", "muted")
        );

        pendientes.getStyleClass().add("admin-stat-card");
        cantidadPendientes.getStyleClass().add("admin-stat-value");

        VBox proceso = new VBox(
                8,
                etiqueta("Proceso de revisión", "hint"),
                etiqueta(
                        "Consultar → Verificar → Decidir",
                        "admin-stat-value-small"
                ),
                etiqueta(
                        "Las decisiones y notificaciones se guardan en H2.",
                        "muted"
                )
        );

        proceso.getStyleClass().add("admin-stat-card");

        HBox resumen = new HBox(16, pendientes, proceso);

        HBox.setHgrow(pendientes, Priority.ALWAYS);
        HBox.setHgrow(proceso, Priority.ALWAYS);

        pendientes.setMaxWidth(Double.MAX_VALUE);
        proceso.setMaxWidth(Double.MAX_VALUE);

        return resumen;
    }

    // ---------------------------------------------------------
    // ZONA DE TRABAJO
    // ---------------------------------------------------------

    private GridPane crearZonaTrabajo() {
        VBox tarjetaLista = new VBox(16);
        tarjetaLista.getStyleClass().add("card");

        HBox tituloLista = new HBox(
                12,
                etiqueta("▤", "heading-icon"),
                etiqueta("Solicitudes pendientes", "card-title"),
                espacioHorizontal()
        );

        Button actualizar = new Button("↻ Actualizar");
        actualizar.getStyleClass().add("btn-secondary");
        actualizar.setOnAction(event -> cargarProveedores());

        tituloLista.getChildren().add(actualizar);

        tablaProveedores.setPrefHeight(450);
        VBox.setVgrow(tablaProveedores, Priority.ALWAYS);

        tarjetaLista.getChildren().addAll(
                tituloLista,
                etiqueta(
                        "Selecciona un proveedor para comenzar la revisión.",
                        "muted"
                ),
                tablaProveedores
        );

        VBox tarjetaDetalle = crearDetalle();

        GridPane zona = new GridPane();
        zona.setHgap(20);

        ColumnConstraints izquierda = new ColumnConstraints();
        izquierda.setPercentWidth(45);
        izquierda.setHgrow(Priority.ALWAYS);

        ColumnConstraints derecha = new ColumnConstraints();
        derecha.setPercentWidth(55);
        derecha.setHgrow(Priority.ALWAYS);

        zona.getColumnConstraints().addAll(izquierda, derecha);
        zona.add(tarjetaLista, 0, 0);
        zona.add(tarjetaDetalle, 1, 0);

        GridPane.setHgrow(tarjetaLista, Priority.ALWAYS);
        GridPane.setHgrow(tarjetaDetalle, Priority.ALWAYS);

        return zona;
    }

    private VBox crearDetalle() {
        VBox tarjeta = new VBox(14);
        tarjeta.getStyleClass().add("card");

        nombre.getStyleClass().add("admin-provider-name");
        nombre.setWrapText(true);
        estado.getStyleClass().add("admin-state");

        GridPane campos = new GridPane();
        campos.setHgap(12);
        campos.setVgap(10);

        ColumnConstraints etiquetas = new ColumnConstraints();
        etiquetas.setMinWidth(85);

        ColumnConstraints valores = new ColumnConstraints();
        valores.setHgrow(Priority.ALWAYS);

        campos.getColumnConstraints().addAll(etiquetas, valores);

        agregarCampo(campos, 0, "Empresa", empresa);
        agregarCampo(campos, 1, "Correo", correo);
        agregarCampo(campos, 2, "Teléfono", telefono);
        agregarCampo(campos, 3, "Dirección", direccion);
        agregarCampo(campos, 4, "Estado", estado);

        listaServicios.setPrefHeight(100);
        listaServicios.setPlaceholder(
                etiqueta("No hay servicios registrados.", "muted"));

        tablaDocumentos.setPrefHeight(155);

        FlowPane accionesDocumentos = new FlowPane(8, 8);
        accionesDocumentos.getChildren().addAll(
                registrarDocumento,
                actualizarDocumento,
                validarDocumento,
                corregirDocumento
        );

        FlowPane accionesProveedor = new FlowPane(8, 8);
        accionesProveedor.getChildren().addAll(
                verificar,
                aprobar,
                rechazar,
                pedirCorrecciones,
                consultarNotificaciones
        );

        verificar.getStyleClass().add("btn-secondary");
        aprobar.getStyleClass().add("btn-primary");
        rechazar.getStyleClass().add("btn-danger");
        pedirCorrecciones.getStyleClass().add("btn-secondary");

        mensaje.setWrapText(true);
        mensaje.setMaxWidth(Double.MAX_VALUE);
        mensaje.setVisible(false);
        mensaje.setManaged(false);

        tarjeta.getChildren().addAll(
                etiqueta("Información del proveedor", "card-title"),
                nombre,
                new Separator(),
                campos,
                new Separator(),
                etiqueta("Servicios ofrecidos", "card-title"),
                listaServicios,
                new Separator(),
                etiqueta("Documentos de verificación", "card-title"),
                tablaDocumentos,
                accionesDocumentos,
                new Separator(),
                etiqueta("Decisión administrativa", "card-title"),
                accionesProveedor,
                mensaje
        );

        return tarjeta;
    }

    private VBox crearNotaDiseno() {
        VBox nota = new VBox(
                9,
                etiqueta("ⓘ Información importante", "card-title"),
                etiqueta(
                        "Revisa la información del proveedor y sus documentos antes de aprobar. "
                        + "Si falta información, solicita correcciones. Las decisiones quedan "
                        + "registradas y generan una notificación para el proveedor.",
                        "muted"
                )
        );

        nota.getStyleClass().addAll("info-card", "highlight");

        return nota;
    }

    // ---------------------------------------------------------
    // CONFIGURACION DE TABLAS
    // ---------------------------------------------------------

    private void configurarTablaProveedores() {
        tablaProveedores.setId("tablaProveedoresPendientes");
        tablaProveedores.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        tablaProveedores.setPlaceholder(
                etiqueta(
                        "No hay proveedores pendientes de verificación.",
                        "muted"
                )
        );

        TableColumn<Proveedor, Number> id = new TableColumn<>("ID");
        id.setCellValueFactory(datos ->
                new ReadOnlyIntegerWrapper(
                        datos.getValue().getIdProveedor()));

        TableColumn<Proveedor, String> proveedor =
                new TableColumn<>("Proveedor");
        proveedor.setCellValueFactory(datos ->
                new SimpleStringProperty(datos.getValue().getNombre()));

        TableColumn<Proveedor, String> empresaColumna =
                new TableColumn<>("Empresa");
        empresaColumna.setCellValueFactory(datos ->
                new SimpleStringProperty(
                        valor(datos.getValue().getEmpresa())));

        TableColumn<Proveedor, String> estadoColumna =
                new TableColumn<>("Estado");
        estadoColumna.setCellValueFactory(datos ->
                new SimpleStringProperty(datos.getValue().getEstado()));

        tablaProveedores.getColumns().addAll(
                id, proveedor, empresaColumna, estadoColumna
        );

        tablaProveedores.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, seleccionado) ->
                        mostrarDetalle(seleccionado));
    }

    private void configurarTablaDocumentos() {
        tablaDocumentos.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        tablaDocumentos.setPlaceholder(
                etiqueta("No hay documentos registrados.", "muted")
        );

        TableColumn<DocumentoProveedor, Number> id =
                new TableColumn<>("ID");
        id.setCellValueFactory(datos ->
                new ReadOnlyIntegerWrapper(
                        datos.getValue().getIdDocumento()));

        TableColumn<DocumentoProveedor, String> tipo =
                new TableColumn<>("Tipo");
        tipo.setCellValueFactory(datos ->
                new SimpleStringProperty(
                        datos.getValue().getTipoDocumento()));

        TableColumn<DocumentoProveedor, String> archivo =
                new TableColumn<>("Archivo");
        archivo.setCellValueFactory(datos ->
                new SimpleStringProperty(
                        datos.getValue().getNombreArchivo()));

        TableColumn<DocumentoProveedor, String> vencimiento =
                new TableColumn<>("Vence");
        vencimiento.setCellValueFactory(datos ->
                new SimpleStringProperty(
                        fecha(datos.getValue().getFechaVencimiento())));

        TableColumn<DocumentoProveedor, String> estadoDocumento =
                new TableColumn<>("Estado");
        estadoDocumento.setCellValueFactory(datos ->
                new SimpleStringProperty(datos.getValue().getEstado()));

        tablaDocumentos.getColumns().addAll(
                id, tipo, archivo, vencimiento, estadoDocumento
        );
    }

    private void configurarAcciones() {
        verificar.setOnAction(event -> validarSeleccionado());
        aprobar.setOnAction(event -> aprobarSeleccionado());
        rechazar.setOnAction(event -> rechazarSeleccionado());

        pedirCorrecciones.setOnAction(
                event -> solicitarCorrecciones());

        registrarDocumento.setOnAction(
                event -> registrarDocumentoRecibido());

        actualizarDocumento.setOnAction(
                event -> actualizarDocumentoRecibido());

        validarDocumento.setOnAction(
                event -> marcarDocumentoValidado());

        corregirDocumento.setOnAction(
                event -> marcarDocumentoPorCorregir());

        consultarNotificaciones.setOnAction(
                event -> mostrarNotificaciones());
    }

    // ---------------------------------------------------------
    // CARGA Y SELECCION
    // ---------------------------------------------------------

    private void cargarProveedores() {
        try {
            List<Proveedor> proveedores =
                    controller.consultarProveedoresPendientes();

            tablaProveedores.getItems().setAll(proveedores);

            cantidadPendientes.setText(
                    Integer.toString(proveedores.size()));

            if (proveedores.isEmpty()) {
                mostrarDetalle(null);
                mostrarMensaje(
                        "No hay proveedores pendientes de verificación.",
                        true
                );
            } else {
                tablaProveedores.getSelectionModel().selectFirst();
                mostrarMensaje(
                        "Selecciona un proveedor para comenzar la revisión.",
                        true
                );
            }

        } catch (RuntimeException e) {
            cantidadPendientes.setText("—");
            mostrarDetalle(null);
            mostrarMensaje(
                    "No se pudieron consultar los proveedores: "
                    + e.getMessage(),
                    false
            );
        }
    }

    private void mostrarDetalle(Proveedor proveedor) {
        informacionValidada = false;

        boolean seleccionado = proveedor != null;

        verificar.setDisable(!seleccionado);
        aprobar.setDisable(true);
        rechazar.setDisable(!seleccionado);
        pedirCorrecciones.setDisable(!seleccionado);
        registrarDocumento.setDisable(!seleccionado);
        actualizarDocumento.setDisable(!seleccionado);
        validarDocumento.setDisable(!seleccionado);
        corregirDocumento.setDisable(!seleccionado);
        consultarNotificaciones.setDisable(!seleccionado);

        if (!seleccionado) {
            nombre.setText("Selecciona un proveedor");
            empresa.setText("—");
            correo.setText("—");
            telefono.setText("—");
            direccion.setText("—");
            estado.setText("Sin selección");

            tablaDocumentos.getItems().clear();
            listaServicios.getItems().clear();
            return;
        }

        nombre.setText(valor(proveedor.getNombre()));
        empresa.setText(valor(proveedor.getEmpresa()));
        correo.setText(valor(proveedor.getCorreo()));
        telefono.setText(valor(proveedor.getTelefono()));
        direccion.setText(valor(proveedor.getDireccion()));
        estado.setText(valor(proveedor.getEstado()));

        cargarDocumentosYServicios(proveedor);

        mostrarMensaje(
                "Revisa los datos, servicios y documentos antes de decidir.",
                true
        );
    }

    private Proveedor proveedorSeleccionado() {
        return tablaProveedores.getSelectionModel().getSelectedItem();
    }

    private DocumentoProveedor documentoSeleccionado() {
        return tablaDocumentos.getSelectionModel().getSelectedItem();
    }

    private void cargarDocumentosYServicios(Proveedor proveedor) {
        try {
            tablaDocumentos.getItems().setAll(
                    controller.consultarDocumentosProveedor(
                            proveedor.getIdProveedor()));

            listaServicios.getItems().setAll(
                    controller.consultarServiciosProveedor(
                            proveedor.getIdProveedor()));

        } catch (RuntimeException e) {
            tablaDocumentos.getItems().clear();
            listaServicios.getItems().clear();

            mostrarMensaje(
                    "No fue posible cargar documentos o servicios: "
                    + e.getMessage(),
                    false
            );
        }
    }

    // ---------------------------------------------------------
    // VALIDAR, APROBAR Y RECHAZAR
    // ---------------------------------------------------------

    private void validarSeleccionado() {
        Proveedor proveedor = proveedorSeleccionado();

        if (proveedor == null) {
            return;
        }

        try {
            informacionValidada =
                    controller.verificarInformacionProveedor(
                            proveedor.getIdProveedor());

            aprobar.setDisable(!informacionValidada);

            if (informacionValidada) {
                mostrarMensaje(
                        "Los datos y documentos disponibles pasan las "
                        + "validaciones. Puedes continuar con la aprobación.",
                        true
                );
            } else {
                mostrarMensaje(
                        "Faltan datos válidos o documentos validados y "
                        + "vigentes. El proveedor debe continuar pendiente.",
                        false
                );
            }

            cargarDocumentosYServicios(proveedor);

        } catch (RuntimeException e) {
            informacionValidada = false;
            aprobar.setDisable(true);

            mostrarMensaje(
                    "No se pudo verificar la información: "
                    + e.getMessage(),
                    false
            );
        }
    }

    private void aprobarSeleccionado() {
        Proveedor proveedor = proveedorSeleccionado();

        if (proveedor == null || !informacionValidada) {
            mostrarMensaje(
                    "Selecciona y verifica un proveedor antes de aprobarlo.",
                    false
            );
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Confirmas la aprobación de " + proveedor.getNombre()
                        + "? Su estado cambiará a VERIFICADO.",
                ButtonType.CANCEL,
                ButtonType.OK
        );

        confirmacion.setTitle("Confirmar aprobación");
        confirmacion.setHeaderText("Aprobar proveedor");

        if (confirmacion.showAndWait()
                .filter(ButtonType.OK::equals).isEmpty()) {
            return;
        }

        try {
            controller.aprobarProveedor(proveedor.getIdProveedor());

            cargarProveedores();

            mostrarMensaje(
                    "Proveedor aprobado y notificación registrada.",
                    true
            );

        } catch (RuntimeException e) {
            cargarProveedores();

            mostrarMensaje(
                    "No se pudo completar la aprobación: " + e.getMessage(),
                    false
            );
        }
    }

    private void rechazarSeleccionado() {
        Proveedor proveedor = proveedorSeleccionado();

        if (proveedor == null) {
            return;
        }

        TextInputDialog dialogo = new TextInputDialog();

        dialogo.setTitle("Rechazar proveedor");
        dialogo.setHeaderText("Indica el motivo del rechazo");
        dialogo.setContentText("Motivo (máximo 500 caracteres):");

        dialogo.showAndWait().ifPresent(motivo -> {
            try {
                controller.rechazarProveedor(
                        proveedor.getIdProveedor(), motivo);

                cargarProveedores();

                mostrarMensaje(
                        "Proveedor rechazado. Motivo y notificación guardados.",
                        true
                );

            } catch (RuntimeException e) {
                mostrarMensaje(
                        "No se pudo rechazar el proveedor: " + e.getMessage(),
                        false
                );
            }
        });
    }

    private void solicitarCorrecciones() {
        Proveedor proveedor = proveedorSeleccionado();

        if (proveedor == null) {
            return;
        }

        TextInputDialog dialogo = new TextInputDialog();

        dialogo.setTitle("Solicitar correcciones");
        dialogo.setHeaderText(
                "Indica qué información o documentos debe corregir el proveedor.");
        dialogo.setContentText("Observación (máximo 500 caracteres):");

        dialogo.showAndWait().ifPresent(observacion -> {
            try {
                controller.solicitarCorrecciones(
                        proveedor.getIdProveedor(), observacion);

                mostrarMensaje(
                        "Corrección registrada. Se guardó una notificación "
                        + "para el usuario del proveedor.",
                        true
                );

            } catch (RuntimeException e) {
                mostrarMensaje(
                        "No se pudo solicitar la corrección: " + e.getMessage(),
                        false
                );
            }
        });
    }

    // ---------------------------------------------------------
    // REGISTRO Y CORRECCION DE DOCUMENTOS
    // ---------------------------------------------------------

    private void registrarDocumentoRecibido() {
        Proveedor proveedor = proveedorSeleccionado();

        if (proveedor == null) {
            return;
        }

        File archivo = seleccionarArchivo();

        if (archivo == null) {
            return;
        }

        Optional<String> tipo = pedirTexto(
                "Tipo de documento",
                "Registrar documento recibido",
                "Ejemplo: IDENTIFICACION"
        );

        if (tipo.isEmpty() || tipo.get().isBlank()) {
            return;
        }

        Optional<String> fechaTexto = pedirTexto(
                "Fecha de vencimiento",
                "Fecha opcional",
                "Usa AAAA-MM-DD o deja vacío si no vence."
        );

        if (fechaTexto.isEmpty()) {
            return;
        }

        try {
            LocalDate fechaVencimiento =
                    parsearFechaOpcional(fechaTexto.get());

            Path copia = copiarArchivo(archivo, proveedor.getIdProveedor());

            try {
                DocumentoProveedor documento = new DocumentoProveedor(
                        proveedor.getIdProveedor(),
                        tipo.get().trim(),
                        archivo.getName(),
                        copia.toAbsolutePath().toString(),
                        fechaVencimiento
                );

                controller.registrarDocumento(documento);

            } catch (RuntimeException e) {
                eliminarArchivo(copia);
                throw e;
            }

            cargarDocumentosYServicios(proveedor);
            informacionValidada = false;
            aprobar.setDisable(true);

            mostrarMensaje(
                    "Documento recibido y registrado. Ahora debe revisarse "
                    + "y marcarse como validado.",
                    true
            );

        } catch (RuntimeException | IOException e) {
            mostrarMensaje(
                    "No se pudo registrar el documento: " + e.getMessage(),
                    false
            );
        }
    }

    private void actualizarDocumentoRecibido() {
        Proveedor proveedor = proveedorSeleccionado();
        DocumentoProveedor documento = documentoSeleccionado();

        if (proveedor == null || documento == null) {
            mostrarMensaje(
                    "Selecciona el documento que deseas actualizar.",
                    false
            );
            return;
        }

        File archivo = seleccionarArchivo();

        if (archivo == null) {
            return;
        }

        Optional<String> fechaTexto = pedirTexto(
                "Fecha de vencimiento",
                "Actualizar documento",
                "Usa AAAA-MM-DD o deja vacío si no vence."
        );

        if (fechaTexto.isEmpty()) {
            return;
        }

        try {
            LocalDate fechaVencimiento =
                    parsearFechaOpcional(fechaTexto.get());

            Path copia = copiarArchivo(archivo, proveedor.getIdProveedor());

            try {
                controller.actualizarDocumentoCorregido(
                        proveedor.getIdProveedor(),
                        documento.getIdDocumento(),
                        archivo.getName(),
                        copia.toAbsolutePath().toString(),
                        fechaVencimiento
                );

            } catch (RuntimeException e) {
                eliminarArchivo(copia);
                throw e;
            }

            cargarDocumentosYServicios(proveedor);
            informacionValidada = false;
            aprobar.setDisable(true);

            mostrarMensaje(
                    "Documento actualizado. Debe revisarse antes de aprobar "
                    + "al proveedor.",
                    true
            );

        } catch (RuntimeException | IOException e) {
            mostrarMensaje(
                    "No se pudo actualizar el documento: " + e.getMessage(),
                    false
            );
        }
    }

    private void marcarDocumentoValidado() {
        Proveedor proveedor = proveedorSeleccionado();
        DocumentoProveedor documento = documentoSeleccionado();

        if (proveedor == null || documento == null) {
            mostrarMensaje(
                    "Selecciona un documento para validarlo.",
                    false
            );
            return;
        }

        try {
            controller.marcarDocumentoValidado(
                    proveedor.getIdProveedor(),
                    documento.getIdDocumento()
            );

            cargarDocumentosYServicios(proveedor);

            informacionValidada = false;
            aprobar.setDisable(true);

            mostrarMensaje(
                    "Documento marcado como VALIDADO. Verifica nuevamente "
                    + "toda la información del proveedor.",
                    true
            );

        } catch (RuntimeException e) {
            cargarDocumentosYServicios(proveedor);

            mostrarMensaje(
                    "No se pudo validar el documento: " + e.getMessage(),
                    false
            );
        }
    }

    private void marcarDocumentoPorCorregir() {
        Proveedor proveedor = proveedorSeleccionado();
        DocumentoProveedor documento = documentoSeleccionado();

        if (proveedor == null || documento == null) {
            mostrarMensaje(
                    "Selecciona un documento para registrar la corrección.",
                    false
            );
            return;
        }

        Optional<String> observacion = pedirTexto(
                "Observación del documento",
                "Marcar documento por corregir",
                "Indica qué debe corregirse."
        );

        if (observacion.isEmpty() || observacion.get().isBlank()) {
            return;
        }

        try {
            controller.marcarDocumentoPorCorregir(
                    proveedor.getIdProveedor(),
                    documento.getIdDocumento(),
                    observacion.get()
            );

            /*
             * Se registra también la solicitud de corrección para conservar
             * la observación y enviar la notificación interna.
             */
            controller.solicitarCorrecciones(
                    proveedor.getIdProveedor(),
                    "Documento " + documento.getTipoDocumento()
                            + ": " + observacion.get().trim()
            );

            cargarDocumentosYServicios(proveedor);
            informacionValidada = false;
            aprobar.setDisable(true);

            mostrarMensaje(
                    "Documento marcado por corregir y solicitud registrada "
                    + "para el proveedor.",
                    true
            );

        } catch (RuntimeException e) {
            mostrarMensaje(
                    "No se pudo registrar la corrección: " + e.getMessage(),
                    false
            );
        }
    }

    // ---------------------------------------------------------
    // NOTIFICACIONES
    // ---------------------------------------------------------

    private void mostrarNotificaciones() {
        Proveedor proveedor = proveedorSeleccionado();

        if (proveedor == null) {
            return;
        }

        try {
            List<NotificacionInterna> notificaciones =
                    controller.consultarNotificacionesPorUsuario(
                            proveedor.getIdUsuario());

            if (notificaciones.isEmpty()) {
                mostrarMensaje(
                        "Este usuario todavía no tiene notificaciones internas.",
                        true
                );
                return;
            }

            StringBuilder texto = new StringBuilder();

            for (NotificacionInterna notificacion : notificaciones) {
                texto.append(notificacion.getTitulo())
                        .append("\n")
                        .append(notificacion.getFechaCreacion())
                        .append("\n")
                        .append(notificacion.getMensaje())
                        .append("\n\n");
            }

            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("Notificaciones internas");
            alerta.setHeaderText(
                    "Notificaciones de " + proveedor.getNombre());

            TextArea contenido = new TextArea(texto.toString());
            contenido.setEditable(false);
            contenido.setWrapText(true);
            contenido.setPrefColumnCount(48);
            contenido.setPrefRowCount(12);

            alerta.getDialogPane().setContent(contenido);
            alerta.showAndWait();

        } catch (RuntimeException e) {
            mostrarMensaje(
                    "No se pudieron consultar las notificaciones: "
                    + e.getMessage(),
                    false
            );
        }
    }

    // ---------------------------------------------------------
    // UTILIDADES DE ARCHIVOS Y DATOS
    // ---------------------------------------------------------

    private File seleccionarArchivo() {
        FileChooser selector = new FileChooser();

        selector.setTitle("Seleccionar documento del proveedor");

        selector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "Documentos habituales",
                        "*.pdf", "*.png", "*.jpg", "*.jpeg",
                        "*.doc", "*.docx"
                ),
                new FileChooser.ExtensionFilter(
                        "Todos los archivos", "*.*")
        );

        return selector.showOpenDialog(
                tablaProveedores.getScene().getWindow());
    }

    private Path copiarArchivo(File origen, int idProveedor)
            throws IOException {

        Path carpeta = Paths.get(
                "data",
                "documentos-proveedores",
                Integer.toString(idProveedor)
        );

        Files.createDirectories(carpeta);

        String nombreDestino = UUID.randomUUID()
                + "_" + origen.getName();

        Path destino = carpeta.resolve(nombreDestino);

        Files.copy(
                origen.toPath(),
                destino,
                StandardCopyOption.COPY_ATTRIBUTES
        );

        return destino;
    }

    private void eliminarArchivo(Path archivo) {
        if (archivo == null) {
            return;
        }

        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignored) {
            // La operación principal ya informó el error.
        }
    }

    private Optional<String> pedirTexto(
            String titulo,
            String encabezado,
            String contenido) {

        TextInputDialog dialogo = new TextInputDialog();

        dialogo.setTitle(titulo);
        dialogo.setHeaderText(encabezado);
        dialogo.setContentText(contenido);

        return dialogo.showAndWait();
    }

    private LocalDate parsearFechaOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return LocalDate.parse(texto.trim());
    }

    private void agregarCampo(
            GridPane grilla, int fila, String titulo, Label valor) {

        valor.setWrapText(true);

        grilla.add(etiqueta(titulo, "hint"), 0, fila);
        grilla.add(valor, 1, fila);
    }

    private void mostrarMensaje(String texto, boolean exito) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll(
                "message",
                exito ? "success" : "error"
        );

        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private String valor(String texto) {
        return texto == null || texto.isBlank()
                ? "No registrado"
                : texto;
    }

    private String fecha(LocalDate fecha) {
        return fecha == null ? "No indicada" : fecha.toString();
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
}