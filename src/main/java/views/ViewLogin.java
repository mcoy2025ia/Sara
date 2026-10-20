package views;

import controllers.ControllerAutenticacion;
import entities.SesionUsuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import views.components.MenuCompartido;

import java.util.function.Consumer;

public class ViewLogin {

    private final ControllerAutenticacion controller;
    private final Consumer<SesionUsuario> alIngresar;

    // Acceso sin cuenta para invitados (CU-16 y CU-20). Puede ser null.
    private final Runnable alEntrarComoInvitado;

    private final TextField usuario =
            new TextField();

    private final PasswordField contrasena =
            new PasswordField();

    private final TextField contrasenaVisible =
            new TextField();

    private final Label mensaje =
            new Label();

    private boolean mostrarContrasena;

    public ViewLogin(
            ControllerAutenticacion controller,
            Consumer<SesionUsuario> alIngresar) {

        this(controller, alIngresar, null);
    }

    public ViewLogin(
            ControllerAutenticacion controller,
            Consumer<SesionUsuario> alIngresar,
            Runnable alEntrarComoInvitado) {

        this.controller = controller;
        this.alIngresar = alIngresar;
        this.alEntrarComoInvitado = alEntrarComoInvitado;

        usuario.setId("usuarioLogin");
        usuario.setPromptText("Usuario");

        contrasena.setId("contrasenaLogin");
        contrasena.setPromptText("Contraseña");
        contrasenaVisible.setPromptText("Contraseña");

        usuario.setMaxWidth(Double.MAX_VALUE);
        contrasena.setMaxWidth(Double.MAX_VALUE);
        contrasenaVisible.setMaxWidth(Double.MAX_VALUE);

        contrasenaVisible
                .textProperty()
                .bindBidirectional(
                        contrasena.textProperty());

        contrasenaVisible.setVisible(false);
        contrasenaVisible.setManaged(false);

        contrasena
                .getStyleClass()
                .add("login-password-field");

        contrasenaVisible
                .getStyleClass()
                .add("login-password-field");

        mensaje.getStyleClass().add(
                "login-error");

        mensaje.setId("mensajeLogin");
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);
    }

    public Parent crearPantalla() {

        ImageView logo = MenuCompartido.crearLogo();

        Label subtitulo =
                new Label("GESTIÓN DE EVENTOS");

        subtitulo.getStyleClass().add(
                "login-caption");

        VBox cabecera =
                new VBox(
                        10,
                        logo,
                        subtitulo);

        cabecera.setAlignment(
                Pos.CENTER);

        Label titulo =
                new Label("Iniciar sesión");

        titulo.getStyleClass().add(
                "login-title");

        Label ayuda =
                new Label(
                        "Ingresa con tu cuenta para continuar.");

        ayuda.getStyleClass().add(
                "login-muted");

        Button ingresar =
                new Button("Ingresar");
        ingresar.setId("ingresarLogin");

        ingresar.getStyleClass().add(
                "login-button");

        ingresar.setMaxWidth(
                Double.MAX_VALUE);

        ingresar.setOnAction(
                event -> iniciarSesion());

        contrasena.setOnAction(
                event -> iniciarSesion());

        contrasenaVisible.setOnAction(
                event -> iniciarSesion());

        VBox formulario =
                new VBox(
                        14,
                        titulo,
                        ayuda,
                        espacio(),
                        etiqueta("Usuario"),
                        usuario,
                        etiqueta("Contraseña"),
                        crearCampoContrasena(),
                        mensaje,
                        ingresar);

        formulario.getStyleClass().add(
                "login-card");

        if (alEntrarComoInvitado != null) {

            Label invitado =
                    new Label(
                            "¿Recibiste una invitación? Entra con tu correo, sin cuenta.");

            invitado.getStyleClass().add(
                    "login-muted");

            invitado.setWrapText(true);

            Button soyInvitado =
                    new Button("Soy invitado");

            soyInvitado.setId(
                    "soyInvitado");

            soyInvitado.getStyleClass().add(
                    "login-guest-button");

            soyInvitado.setMaxWidth(
                    Double.MAX_VALUE);

            soyInvitado.setOnAction(
                    event -> alEntrarComoInvitado.run());

            formulario.getChildren().addAll(
                    new Separator(),
                    invitado,
                    soyInvitado);
        }

        VBox contenido =
                new VBox(
                        30,
                        cabecera,
                        formulario);

        contenido.setAlignment(
                Pos.CENTER);

        VBox raiz =
                new VBox(contenido);

        raiz.setAlignment(
                Pos.CENTER);

        raiz.getStyleClass().add(
                "login-root");

        return raiz;
    }

    private StackPane crearCampoContrasena() {

        Button ver =
                new Button("👁");

        ver.getStyleClass().add(
                "login-eye-button");

        ver.setFocusTraversable(false);

        StackPane.setAlignment(
                ver,
                Pos.CENTER_RIGHT);

        StackPane.setMargin(
                ver,
                new Insets(
                        0,
                        8,
                        0,
                        0));

        ver.setOnAction(
                event -> alternarContrasena());

        StackPane campo =
                new StackPane(
                        contrasena,
                        contrasenaVisible,
                        ver);

        campo.getStyleClass().add(
                "login-password-box");

        return campo;
    }

    private void alternarContrasena() {

        mostrarContrasena =
                !mostrarContrasena;

        contrasenaVisible.setVisible(
                mostrarContrasena);

        contrasenaVisible.setManaged(
                mostrarContrasena);

        contrasena.setVisible(
                !mostrarContrasena);

        contrasena.setManaged(
                !mostrarContrasena);

        if (mostrarContrasena) {

            contrasenaVisible.requestFocus();

            contrasenaVisible.positionCaret(
                    contrasenaVisible
                            .getText()
                            .length());

        } else {

            contrasena.requestFocus();

            contrasena.positionCaret(
                    contrasena
                            .getText()
                            .length());
        }
    }

    private void iniciarSesion() {

        try {
            SesionUsuario sesion =
                    controller.iniciarSesion(
                            usuario.getText(),
                            contrasena.getText());

            mensaje.setVisible(false);
            mensaje.setManaged(false);

            alIngresar.accept(sesion);

        } catch (Exception e) {
            mensaje.setText(
                    e.getMessage() == null
                            ? "No se pudo iniciar sesión."
                            : e.getMessage());

            mensaje.setVisible(true);
            mensaje.setManaged(true);
        }
    }

    private Label etiqueta(
            String texto) {

        Label label =
                new Label(texto);

        label.getStyleClass().add(
                "login-label");

        return label;
    }

    private Region espacio() {

        Region region =
                new Region();

        region.setPrefHeight(3);

        return region;
    }
}
