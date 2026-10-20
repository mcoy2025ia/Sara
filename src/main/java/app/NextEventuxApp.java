package app;

import controllers.ControllerAutenticacion;
import controllers.ControllerEventos;
import controllers.ControllerFinanciera;
import controllers.ControllerPublicacion;
import controllers.ControllerInvitados;
import controllers.ControllerProveedores;
import controllers.ControllerSolicitudes;
import controllers.ControllerAdministracion;
import controllers.ControllerContratacionOrganizador;
import controllers.ControllerVerificacionOrganizador;
import entities.Evento;
import entities.Servicio;
import entities.SesionUsuario;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryEvento;
import repositories.RepositoryMovimientoFinanciero;
import repositories.RepositoryPlanificacion;
import repositories.RepositoryPublicacion;
import repositories.RepositoryPropuestaServicio;
import repositories.RepositoryServicio;
import repositories.RepositorySolicitudEjecucion;
import repositories.RepositoryProveedor;
import services.ServiceAutenticacion;
import services.ServiceEventos;
import services.ServiceFinanciera;
import services.ServicePublicacion;
import services.ServiceInvitados;
import services.ServiceProveedores;
import services.ServiceSolicitudes;
import services.ServiceAdministracion;
import services.ServiceContratacionOrganizador;
import services.ServiceVerificacionOrganizador;
import repositories.RepositoryContratacion;
import repositories.RepositoryOrganizador;
import views.ViewAdministracion;
import views.ViewContratarOrganizador;
import views.ViewVerificacionOrganizador;
import views.ViewCatalogoServicios;
import views.ViewDetalleServicio;
import views.ViewDetalleServicioProveedor;
import views.ViewEventos;
import views.ViewFinanciera;
import views.ViewPublicacion;
import views.ViewEdicion;
import views.ViewInvitados;
import views.ViewListaServiciosProveedor;
import views.ViewLogin;
import views.ViewPropuestasOrganizador;
import views.ViewSolicitudesProveedor;
import views.ViewMisEventos;
import views.ViewCancelacion;
import views.ViewFinalizacion;
import views.ViewRespuestaInvitacion;
import views.ViewAvisosProveedor;
import services.ServiceAvisosProveedor;
import repositories.RepositoryAvisosProveedor;
import javafx.scene.control.Button;
import java.util.logging.Logger;

public class NextEventuxApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(NextEventuxApp.class.getName());

    private BorderPane contenedor;
    private ControllerEventos controllerEventos;
    private ControllerAutenticacion controllerAutenticacion;
    private ControllerProveedores controllerProveedores;
    private ControllerSolicitudes controllerSolicitudes;

    private SesionUsuario sesionActual;
    private Navegacion navegacion;
    private Servicio servicioSeleccionado;
    private Integer servicioProveedorSeleccionado;
    private Integer eventoSeleccionado;
    private Stage stage;
    private Stage ventanaAdministracion;
    private ControllerPublicacion publicaciones;
    private ControllerFinanciera controllerFinanciera;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setOnHidden(event -> cerrarAdministracion());

        controllerAutenticacion = new ControllerAutenticacion(
                new ServiceAutenticacion(new RepositoryAutenticacion()));

        controllerProveedores = new ControllerProveedores(
                new ServiceProveedores(
                        new RepositoryServicio(),
                        new RepositoryDisponibilidadServicio()));

        contenedor = new BorderPane();

        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();

        double ancho = Math.min(1440, pantalla.getWidth());
        double alto = Math.min(900, pantalla.getHeight());

        Scene scene = new Scene(contenedor, ancho, alto);

        scene.getStylesheets().add(
                getClass().getResource("/styles/eventos.css").toExternalForm());

        scene.getStylesheets().add(
                getClass().getResource("/styles/proveedores.css").toExternalForm());

        scene.getStylesheets().add(
                getClass().getResource("/styles/login.css").toExternalForm());

        mostrarLogin();

        stage.setTitle("NextEventux");
        stage.setMinWidth(Math.min(700, pantalla.getWidth()));
        stage.setMinHeight(Math.min(500, pantalla.getHeight()));
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
    }

    private void cerrarAdministracion() {
        if(ventanaAdministracion!=null) {
            ventanaAdministracion.close();
            ventanaAdministracion=null;
        }
    }

    private void mostrarLogin() {
        cerrarAdministracion();

        if (sesionActual != null) sesionActual.cerrar();
        sesionActual = null;
        navegacion = null;
        servicioSeleccionado = null;
        servicioProveedorSeleccionado = null;
        eventoSeleccionado = null;
        controllerEventos = null;
        controllerSolicitudes = null;
        publicaciones = null;
        contenedor.setTop(null);
        stage.setTitle("NextEventux | Iniciar sesión");

        contenedor.setCenter(
                new ViewLogin(
                        controllerAutenticacion,
                        this::procesarSesion,
                        this::mostrarInvitado)
                        .crearPantalla());
    }

    // ------------------------------------------------------------------
    // Acceso del invitado (CU-16 y CU-20), sin cuenta: se identifica con
    // su correo y su teléfono en cada pantalla.
    // ------------------------------------------------------------------
    private void mostrarInvitado() {

        sesionActual = null;
        contenedor.setTop(null);
        navegacion = null;
        new AccesoInvitado(new ControllerInvitados(new ServiceInvitados()), this::mostrarLogin)
                .mostrarEn(contenedor, Navegacion.Pantalla.MIS_INVITACIONES);
    }

    private void procesarSesion(SesionUsuario sesion) {

        sesionActual = sesion;
        publicaciones = new ControllerPublicacion(new ServicePublicacion(
                new RepositoryPublicacion(), new RepositoryEvento()), sesion);
        controllerEventos = new ControllerEventos(new ServiceEventos(new RepositoryEvento()), sesion);
        controllerFinanciera = new ControllerFinanciera(new ServiceFinanciera(new RepositoryMovimientoFinanciero(),
            new RepositoryPlanificacion(), new RepositoryEvento()), sesion);

        String tipo = sesion.getTipoUsuario();

        if (!"ORGANIZADOR".equalsIgnoreCase(tipo)
                && !"PROVEEDOR".equalsIgnoreCase(tipo)
                && !"ADMINISTRADOR".equalsIgnoreCase(tipo)
                && !"CLIENTE".equalsIgnoreCase(tipo)) {

            LOGGER.warning("Rol no soportado en la sesión: " + tipo);
            mostrarLogin();
            return;
        }

        controllerSolicitudes = new ControllerSolicitudes(
                new ServiceSolicitudes(
                        new RepositorySolicitudEjecucion(),
                        new RepositoryPropuestaServicio(),
                        sesionActual));

        contenedor.setTop(null);
        navegacion = new Navegacion();

        if ("ORGANIZADOR".equalsIgnoreCase(tipo) || "ADMINISTRADOR".equalsIgnoreCase(tipo)) {
            registrarRutasOrganizador();
            if ("ORGANIZADOR".equalsIgnoreCase(tipo)) {
                mostrarEventos();
            } else {
                mostrarPublicacion();
            }
        } else if ("PROVEEDOR".equalsIgnoreCase(tipo)) {
            registrarRutasProveedor();
            mostrarProveedores();
        } else if ("CLIENTE".equalsIgnoreCase(tipo)) {
            // CU-026: el cliente busca y contrata un organizador verificado.
            mostrarContratarOrganizador();
        }
    }

    /** CU-026: abre la pantalla Contratar organizador para el cliente autenticado. */
    private void mostrarContratarOrganizador() {
        if (sesionActual == null || !"CLIENTE".equalsIgnoreCase(sesionActual.getTipoUsuario())) {
            mostrarLogin();
            return;
        }
        ControllerContratacionOrganizador controlador = new ControllerContratacionOrganizador(
                new ServiceContratacionOrganizador(new RepositoryContratacion(), new RepositoryOrganizador()),
                sesionActual);
        stage.setTitle("NextEventux | Contratar organizador");
        contenedor.setCenter(new ViewContratarOrganizador(controlador, sesionActual)
                .conCerrarSesion(this::mostrarLogin)
                .crearPantalla());
    }

    private boolean esOrganizador() {

        return sesionActual != null
                && "ORGANIZADOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario());
    }

    private void mostrarEventos() {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        navegacion.mostrarEn(
                contenedor,
                Navegacion.Pantalla.EVENTOS);
    }

    private void mostrarInvitados() {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        navegacion.ir(Navegacion.Pantalla.INVITADOS);
    }

    private void mostrarEstadoFinanciero(Evento evento) {
        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }
        eventoSeleccionado = evento.getIdEvento();
        navegacion.ir(Navegacion.Pantalla.ESTADO_FINANCIERO);
     }

    private void registrarRutasOrganizador() {
        navegacion
                .registrar(
                        Navegacion.Pantalla.EVENTOS,
                        () -> new ViewEventos(
                                controllerEventos,
                                this::mostrarEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin,
                                sesionActual,
                                this::mostrarMisEventos,
                                this::mostrarPropuestasOrganizador).conSolicitudes(this::mostrarSolicitudesEvento).crearPantalla())
                .registrar(
                        Navegacion.Pantalla.INVITADOS,
                        () -> new ViewInvitados(
                                new ControllerInvitados(
                                        new ServiceInvitados(), sesionActual),
                                this::mostrarEventos,
                                this::mostrarInvitados,
                                this::mostrarCatalogoServicios,
                                this::mostrarLogin,
                                sesionActual,
                                this::mostrarMisEventos).crearPantalla())
                .registrar(
                        Navegacion.Pantalla.CATALOGO_SERVICIOS,
                        () -> new ViewCatalogoServicios(
                                controllerProveedores,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarInvitados,
                                this::mostrarCatalogoServicios,
                                this::mostrarLogin,
                                this::mostrarDetalleServicio,
                                this::mostrarMisEventos).crearPantalla())
                .registrar(
                        Navegacion.Pantalla.DETALLE_SERVICIO,
                        this::crearVistaDetalleServicio)
                .registrarDinamica(
                        Navegacion.Pantalla.PROPUESTAS_ORGANIZADOR,
                        this::crearVistaPropuestasOrganizador)
                .registrarDinamica(
                        Navegacion.Pantalla.MIS_EVENTOS,
                        () -> new ViewMisEventos(
                                controllerEventos,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarMisEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin,
                                this::mostrarEdicion,
                                this::mostrarPublicacion,
                                this::mostrarCancelacion,
                                this::mostrarFinalizacion)
                                .conSolicitudes(this::mostrarSolicitudesEvento)
                                .conFinanzas(this::mostrarEstadoFinanciero)
                                .crearPantalla())
                .registrarDinamica(
                        Navegacion.Pantalla.EDICION_EVENTO,
                        () -> new ViewEdicion(
                                controllerEventos,
                                this::mostrarMisEventos,
                                eventoSeleccionado,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarMisEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin).crearPantalla())
                .registrarDinamica(
                        Navegacion.Pantalla.CANCELACION_EVENTO,
                        () -> new ViewCancelacion(
                                controllerEventos,
                                this::mostrarMisEventos,
                                eventoSeleccionado,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarMisEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin).crearPantalla())
                .registrarDinamica(
                        Navegacion.Pantalla.FINALIZACION_EVENTO,
                        () -> new ViewFinalizacion(controllerEventos, this::mostrarMisEventos,
                                null, eventoSeleccionado, sesionActual, this::mostrarEventos,
                                this::mostrarMisEventos, this::mostrarCatalogoServicios,
                                this::mostrarInvitados, this::mostrarLogin).crearPantalla())
                .registrarDinamica(
                        Navegacion.Pantalla.PUBLICACION_EVENTOS,
                        () -> new ViewPublicacion(
                                publicaciones,
                                this::volverDesdePublicacion,
                                eventoSeleccionado,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarMisEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin,
                                this::mostrarAdministracion).crearPantalla())
                .registrarDinamica(
                        Navegacion.Pantalla.ESTADO_FINANCIERO,
                        () -> new ViewFinanciera(
                                controllerFinanciera,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarMisEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin)
                                .crearPantalla(eventoSeleccionado != null ? eventoSeleccionado : 0));
    }

    private void mostrarFinalizacion(Evento evento) {
        if(!esOrganizador()) { mostrarLogin(); return; }
        eventoSeleccionado=evento.getIdEvento();
        navegacion.ir(Navegacion.Pantalla.FINALIZACION_EVENTO);
    }

    private void mostrarSolicitudesEvento(Evento evento) {
        if (!esOrganizador()) { mostrarLogin(); return; }
        new views.ViewSolicitudesEvento(controllerEventos, evento).mostrar(stage);
    }

    private void mostrarMisEventos() {
        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }
        navegacion.ir(Navegacion.Pantalla.MIS_EVENTOS);
    }

    private void mostrarEdicion(Evento evento) {
        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }
        eventoSeleccionado = evento.getIdEvento();
        navegacion.ir(Navegacion.Pantalla.EDICION_EVENTO);
    }

    private void mostrarPublicacion() {
        if (!esOrganizador()
                && (sesionActual == null
                || !"ADMINISTRADOR".equalsIgnoreCase(sesionActual.getTipoUsuario()))) {
            mostrarLogin();
            return;
        }
        eventoSeleccionado = null;
        navegacion.mostrarEn(contenedor, Navegacion.Pantalla.PUBLICACION_EVENTOS);
    }

    private void mostrarPublicacion(Evento evento) {
        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }
        eventoSeleccionado = evento.getIdEvento();
        navegacion.ir(Navegacion.Pantalla.PUBLICACION_EVENTOS);
    }

    private void mostrarCancelacion(Evento evento) {
        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }
        eventoSeleccionado = evento.getIdEvento();
        navegacion.ir(Navegacion.Pantalla.CANCELACION_EVENTO);
    }

    private void volverDesdePublicacion() {
        if (eventoSeleccionado != null && esOrganizador()) {
            mostrarMisEventos();
        } else if (esOrganizador()) {
            mostrarEventos();
        } else {
            contenedor.setCenter(new Label(
                    "Selecciona Publicaciones para revisar las solicitudes."));
        }
    }

    private void mostrarCatalogoServicios() {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        navegacion.ir(Navegacion.Pantalla.CATALOGO_SERVICIOS);
    }

    private void mostrarDetalleServicio(Servicio servicio) {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        if (servicio == null) {
            mostrarCatalogoServicios();
            return;
        }

        if (servicioSeleccionado == null
                || servicioSeleccionado.getIdServicio()
                        != servicio.getIdServicio()) {
            navegacion.invalidar(Navegacion.Pantalla.DETALLE_SERVICIO);
        }
        servicioSeleccionado = servicio;
        navegacion.ir(Navegacion.Pantalla.DETALLE_SERVICIO);
    }

    private javafx.scene.Parent crearVistaDetalleServicio() {
        if (servicioSeleccionado == null) {
            throw new IllegalStateException(
                    "No hay un servicio seleccionado para mostrar");
        }
        return new ViewDetalleServicio(
                controllerProveedores,
                controllerSolicitudes,
                servicioSeleccionado,
                sesionActual.getIdActor(),
                sesionActual,
                this::mostrarCatalogoServicios,
                this::mostrarEventos,
                this::mostrarInvitados,
                this::mostrarLogin,
                this::mostrarMisEventos)
                .crearPantalla();
    }

    private void mostrarPropuestasOrganizador() {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        navegacion.ir(Navegacion.Pantalla.PROPUESTAS_ORGANIZADOR);
    }

    private javafx.scene.Parent crearVistaPropuestasOrganizador() {
        return new ViewPropuestasOrganizador(
                controllerSolicitudes,
                sesionActual.getIdActor(),
                sesionActual.getIdUsuario(),
                this::mostrarEventos,
                sesionActual,
                this::mostrarEventos,
                this::mostrarInvitados,
                this::mostrarCatalogoServicios,
                this::mostrarMisEventos,
                this::mostrarPropuestasOrganizador,
                this::mostrarLogin)
                .crearPantalla();
    }

    private void mostrarProveedores() {
        if (sesionActual == null
                || !"PROVEEDOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario())) {

            mostrarLogin();
            return;
        }

        navegacion.invalidar(Navegacion.Pantalla.MIS_SERVICIOS);
        navegacion.mostrarEn(
                contenedor,
                Navegacion.Pantalla.MIS_SERVICIOS);
    }

    private void registrarRutasProveedor() {
        navegacion
                .registrar(
                        Navegacion.Pantalla.MIS_SERVICIOS,
                        () -> new ViewListaServiciosProveedor(
                                controllerProveedores,
                                sesionActual.getIdActor(),
                                sesionActual,
                                this::mostrarLogin,
                                this::mostrarDetalleServicioProveedor,
                                () -> mostrarDetalleServicioProveedor(null),
                                this::mostrarProveedores,
                                this::mostrarSolicitudesProveedor,
                                this::mostrarAvisosProveedor)
                                .crearPantalla())
                .registrarDinamica(
                        Navegacion.Pantalla.AVISOS_PROVEEDOR,
                        () -> new ViewAvisosProveedor(new ServiceAvisosProveedor(new RepositoryAvisosProveedor()),
                                sesionActual,this::mostrarProveedores,this::mostrarLogin).crearPantalla())
                .registrar(
                        Navegacion.Pantalla.DETALLE_SERVICIO_PROVEEDOR,
                        this::crearVistaDetalleServicioProveedor)
                .registrarDinamica(
                        Navegacion.Pantalla.SOLICITUDES_PROVEEDOR,
                        this::crearVistaSolicitudesProveedor);
    }

    private void mostrarDetalleServicioProveedor(
            Integer idServicio) {

        if (sesionActual == null
                || !"PROVEEDOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario())) {

            mostrarLogin();
            return;
        }

        if (!java.util.Objects.equals(
                servicioProveedorSeleccionado,
                idServicio)) {
            navegacion.invalidar(
                    Navegacion.Pantalla.DETALLE_SERVICIO_PROVEEDOR);
        }
        servicioProveedorSeleccionado = idServicio;
        navegacion.ir(
                Navegacion.Pantalla.DETALLE_SERVICIO_PROVEEDOR);
    }

    private void mostrarAvisosProveedor() {
        if(sesionActual==null || !"PROVEEDOR".equalsIgnoreCase(sesionActual.getTipoUsuario())) {
            mostrarLogin(); return;
        }
        navegacion.ir(Navegacion.Pantalla.AVISOS_PROVEEDOR);
    }

    private javafx.scene.Parent crearVistaDetalleServicioProveedor() {
        return new ViewDetalleServicioProveedor(
                controllerProveedores,
                sesionActual.getIdActor(),
                sesionActual,
                this::mostrarLogin,
                servicioProveedorSeleccionado,
                this::mostrarProveedores)
                .crearPantalla();
    }

    private void mostrarSolicitudesProveedor() {

        if (sesionActual == null
                || !"PROVEEDOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario())) {

            mostrarLogin();
            return;
        }

        navegacion.ir(Navegacion.Pantalla.SOLICITUDES_PROVEEDOR);
    }

    private javafx.scene.Parent crearVistaSolicitudesProveedor() {
        return new ViewSolicitudesProveedor(
                controllerSolicitudes,
                sesionActual.getIdActor(),
                sesionActual.getIdUsuario(),
                this::mostrarProveedores,
                sesionActual,
                this::mostrarProveedores,
                this::mostrarSolicitudesProveedor,
                this::mostrarLogin)
                .crearPantalla();
    }

    /** CU-028: abre la ventana de Administración en «Aprobar proveedores». */
    private void mostrarAdministracion() {
        if (!esAdministrador()) {
            mostrarLogin();
            return;
        }
        ControllerAdministracion controller = new ControllerAdministracion(
                new ServiceAdministracion(new RepositoryProveedor(), sesionActual));
        mostrarEnVentanaAdministracion(
                new ViewAdministracion(controller, sesionActual, this::mostrarLogin)
                        .conVerificacionOrganizadores(this::mostrarVerificacionOrganizadores)
                        .crearPantalla(),
                "NextEventux | Aprobar proveedores");
    }

    /** CU-030: verificar, categorizar y aprobar organizadores en la misma ventana. */
    private void mostrarVerificacionOrganizadores() {
        if (!esAdministrador() || !"ACTIVO".equalsIgnoreCase(sesionActual.getEstadoUsuario())) {
            mostrarLogin();
            return;
        }
        ControllerVerificacionOrganizador controller = new ControllerVerificacionOrganizador(
                new ServiceVerificacionOrganizador(new RepositoryOrganizador(), sesionActual.getIdUsuario()),
                sesionActual);
        mostrarEnVentanaAdministracion(
                new ViewVerificacionOrganizador(controller, sesionActual)
                        .conNavegacion(this::mostrarAdministracion, this::mostrarLogin)
                        .crearPantalla(),
                "NextEventux | Verificar y categorizar organizadores");
    }

    private boolean esAdministrador() {
        return sesionActual != null
                && "ADMINISTRADOR".equalsIgnoreCase(sesionActual.getTipoUsuario());
    }

    private void mostrarEnVentanaAdministracion(javafx.scene.Parent pantalla, String titulo) {
        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        if (ventanaAdministracion == null || !ventanaAdministracion.isShowing()) {
            ventanaAdministracion = new Stage();
            ventanaAdministracion.initOwner(stage);
            ventanaAdministracion.setMinWidth(Math.min(1000, area.getWidth()));
            ventanaAdministracion.setMinHeight(Math.min(650, area.getHeight()));
            Scene escena = new Scene(pantalla, Math.min(1440, area.getWidth()), Math.min(900, area.getHeight()));
            escena.getStylesheets().add(getClass().getResource("/styles/eventos.css").toExternalForm());
            escena.getStylesheets().add(getClass().getResource("/styles/administracion.css").toExternalForm());
            ventanaAdministracion.setScene(escena);
            ventanaAdministracion.show();
            ventanaAdministracion.centerOnScreen();
        } else {
            ventanaAdministracion.getScene().setRoot(pantalla);
            ventanaAdministracion.toFront();
        }
        ventanaAdministracion.setTitle(titulo);
    }
}
