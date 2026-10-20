
package views.components;

import java.util.Locale;
import java.util.Objects;

import entities.SesionUsuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class MenuCompartido {

    public enum Pagina {
        MIS_EVENTOS,
        CREAR_EVENTO,
        INVITADOS,
        CATALOGO,
        MIS_SERVICIOS,
        MIS_INVITACIONES,
        MESA_REGALOS,
        SOLICITUDES_PROVEEDOR,
        PROPUESTAS_ORGANIZADOR
    }

    private MenuCompartido() {
    }

    public static VBox crear(
            SesionUsuario sesion,
            Pagina paginaActiva,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirServicios,
            Runnable cerrarSesion) {

        return crear(
                sesion,
                paginaActiva,
                abrirEventos,
                abrirInvitados,
                abrirCatalogo,
                abrirServicios,
                cerrarSesion,
                null);
    }

    public static VBox crear(
            SesionUsuario sesion,
            Pagina paginaActiva,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirServicios,
            Runnable cerrarSesion,
            Runnable abrirMisEventos) {
        return crear(
                sesion,
                paginaActiva,
                abrirEventos,
                abrirInvitados,
                abrirCatalogo,
                abrirServicios,
                cerrarSesion,
                abrirMisEventos,
                null,
                null);
    }

    public static VBox crear(
            SesionUsuario sesion,
            Pagina paginaActiva,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirServicios,
            Runnable cerrarSesion,
            Runnable abrirMisEventos,
            Runnable abrirSolicitudes,
            Runnable abrirPropuestas) {
        VBox menu = new VBox(7);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);

        // Todas las pantallas utilizan exactamente la misma imagen
        ImageView logo = crearLogo();

        VBox marca = new VBox(10, logo,
                etiqueta("GESTIÓN DE EVENTOS", "brand-caption"));
        marca.setAlignment(Pos.CENTER);
        VBox.setMargin(marca, new Insets(0, 0, 38, 0));

        menu.getChildren().addAll(
                marca,
                etiqueta("MENÚ PRINCIPAL", "menu-title"));

        boolean proveedor = sesion != null
                && "PROVEEDOR".equalsIgnoreCase(sesion.getTipoUsuario());

        if (proveedor) {
            agregarOpcion(menu, "♧  Mis servicios",
                    paginaActiva == Pagina.MIS_SERVICIOS,
                    abrirServicios);
            agregarOpcion(menu, "✉  Solicitudes recibidas",
                    paginaActiva == Pagina.SOLICITUDES_PROVEEDOR,
                    abrirSolicitudes);
        } else {
            if (abrirMisEventos != null) {
                agregarOpcion(menu, "▦  Mis eventos",
                        paginaActiva == Pagina.MIS_EVENTOS,
                        abrirMisEventos);
            }

            agregarOpcion(menu, "＋  Crear evento",
                    paginaActiva == Pagina.CREAR_EVENTO,
                    abrirEventos);

            agregarOpcion(menu, "♧  Proveedores",
                    paginaActiva == Pagina.CATALOGO,
                    abrirCatalogo);

            agregarOpcion(menu, "♙  Invitados",
                    paginaActiva == Pagina.INVITADOS,
                    abrirInvitados);
            agregarOpcion(menu, "✉  Propuestas recibidas",
                    paginaActiva == Pagina.PROPUESTAS_ORGANIZADOR,
                    abrirPropuestas);
        }

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        menu.getChildren().add(espacio);
        if (sesion != null) {
            String nombre = nombreSesion(sesion);
            Label nombrePerfil = etiqueta(nombre, "profile-name");
            nombrePerfil.setMaxWidth(Double.MAX_VALUE);
            nombrePerfil.setTextOverrun(OverrunStyle.ELLIPSIS);
            VBox perfil = new VBox(4,
                    nombrePerfil,
                    etiqueta(rolSesion(sesion), "hint"));
            perfil.setMinWidth(0);
            HBox.setHgrow(perfil, Priority.ALWAYS);
            if (cerrarSesion != null) {
                Label salir = etiqueta("Cerrar sesión", "logout-link");
                salir.setOnMouseClicked(event -> cerrarSesion.run());
                salir.setStyle("-fx-cursor: hand;");
                perfil.getChildren().add(salir);
            }

            Label avatar = etiqueta(iniciales(nombre), "avatar");
            avatar.setMinSize(42, 42);
            avatar.setPrefSize(42, 42);
            avatar.setMaxSize(42, 42);

            HBox cuenta = new HBox(12, avatar, perfil);
            cuenta.setAlignment(Pos.CENTER_LEFT);
            cuenta.setMaxWidth(Double.MAX_VALUE);
            cuenta.getStyleClass().add("sidebar-bottom");
            menu.getChildren().add(cuenta);
        }
        return menu;
    }

    /**
     * Menú del invitado (CU-16 y CU-20). El invitado no tiene cuenta: entra desde
     * "Soy invitado" en el login y se identifica con su correo y teléfono.
     */
    public static VBox crearInvitado(
            Pagina paginaActiva,
            Runnable abrirInvitaciones,
            Runnable abrirMesaRegalos,
            Runnable salir) {

        VBox menu = new VBox(7);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);

        VBox marca = new VBox(10, crearLogo(),
                etiqueta("GESTIÓN DE EVENTOS", "brand-caption"));
        marca.setAlignment(Pos.CENTER);
        VBox.setMargin(marca, new Insets(0, 0, 38, 0));

        menu.getChildren().addAll(
                marca,
                etiqueta("MENÚ DEL INVITADO", "menu-title"));

        agregarOpcion(menu, "✉  Mis invitaciones",
                paginaActiva == Pagina.MIS_INVITACIONES,
                abrirInvitaciones);
        agregarOpcion(menu, "♡  Mesa de regalos",
                paginaActiva == Pagina.MESA_REGALOS,
                abrirMesaRegalos);

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);
        menu.getChildren().add(espacio);

        VBox perfil = new VBox(4,
                etiqueta("Invitado", "profile-name"),
                etiqueta("Sin cuenta", "hint"));
        perfil.setMinWidth(0);
        HBox.setHgrow(perfil, Priority.ALWAYS);
        if (salir != null) {
            Label enlace = etiqueta("Cerrar sesión", "logout-link");
            enlace.setOnMouseClicked(event -> salir.run());
            enlace.setStyle("-fx-cursor: hand;");
            perfil.getChildren().add(enlace);
        }
        Label avatar = etiqueta("IN", "avatar");
        avatar.setMinSize(42, 42);
        avatar.setPrefSize(42, 42);
        avatar.setMaxSize(42, 42);
        HBox cuenta = new HBox(12, avatar, perfil);
        cuenta.setAlignment(Pos.CENTER_LEFT);
        cuenta.setMaxWidth(Double.MAX_VALUE);
        cuenta.getStyleClass().add("sidebar-bottom");
        menu.getChildren().add(cuenta);
        return menu;
    }

    public static ImageView crearLogo() {
        ImageView logo = new ImageView(new Image(
                Objects.requireNonNull(
                        MenuCompartido.class.getResource("/images/logo-nexteventux.png"),
                        "No se encontró /images/logo-nexteventux.png")
                        .toExternalForm()));
        logo.setFitWidth(190);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        return logo;
    }

    public static HBox crearBarraSuperior(
            SesionUsuario sesion,
            String titulo) {

        return crearBarraSuperior(sesion, titulo, null, null);
    }

    public static HBox crearBarraSuperior(
            SesionUsuario sesion,
            String titulo,
            String textoRegreso,
            Runnable alRegresar) {

        HBox barra = new HBox(18);
        if (alRegresar != null) {
            Button volver = new Button("← " + textoRegreso);
            volver.getStyleClass().add("btn-secondary");
            volver.setOnAction(evento -> alRegresar.run());
            barra.getChildren().add(volver);
        }
        barra.getChildren().addAll(
                etiqueta("Inicio /", "breadcrumb"),
                etiqueta(titulo, "breadcrumb-current"));
        barra.getChildren().add(espacioHorizontal());
        if (sesion != null) {
            barra.getChildren().add(
                    etiqueta(nombreSesion(sesion), "user-badge"));
        }
        configurarBarraSuperior(barra);
        return barra;
    }

    public static HBox crearBarraSuperior(
            String titulo,
            String nombreUsuario) {

        HBox barra = new HBox(
                18,
                etiqueta("Inicio /", "breadcrumb"),
                etiqueta(titulo, "breadcrumb-current"));
        barra.getChildren().add(espacioHorizontal());
        if (nombreUsuario != null && !nombreUsuario.isBlank()) {
            barra.getChildren().add(etiqueta(nombreUsuario, "user-badge"));
        }
        configurarBarraSuperior(barra);
        return barra;
    }

    private static void configurarBarraSuperior(HBox barra) {
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPrefHeight(78);
        barra.setMinHeight(78);
        barra.getStyleClass().add("topbar");
    }

    private static void agregarOpcion(
            VBox menu,
            String texto,
            boolean activa,
            Runnable accion) {

        if (accion == null) {
            return;
        }

        Label item = etiqueta(texto, "menu-item");
        item.setMaxWidth(Double.MAX_VALUE);

        if (activa) {
            item.getStyleClass().add("active");
        }

        item.setOnMouseClicked(event -> accion.run());
        item.setStyle("-fx-cursor: hand;");

        menu.getChildren().add(item);
    }

    private static Label etiqueta(String texto, String estilo) {
        Label label = new Label(texto);
        label.getStyleClass().add(estilo);
        return label;
    }

    private static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "U";
        }

        String[] partes = nombre.trim().split("\\s+");

        String resultado = partes[0].substring(0, 1);

        if (partes.length > 1) {
            resultado += partes[partes.length - 1].substring(0, 1);
        }

        return resultado.toUpperCase(Locale.ROOT);
    }

    private static String nombreSesion(SesionUsuario sesion) {
        if (sesion == null || sesion.getNombre() == null
                || sesion.getNombre().isBlank()) {
            return "Usuario";
        }
        return sesion.getNombre().trim();
    }

    private static String rolSesion(SesionUsuario sesion) {
        if (sesion == null || sesion.getTipoUsuario() == null
                || sesion.getTipoUsuario().isBlank()) {
            return "Usuario";
        }

        String rol = sesion.getTipoUsuario().trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(rol.charAt(0)) + rol.substring(1);
    }

    private static Region espacioHorizontal() {
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        return espacio;
    }
}
