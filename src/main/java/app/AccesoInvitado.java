package app;

import controllers.ControllerInvitados;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import views.ViewMesaRegalos;
import views.ViewRespuestaInvitacion;

/**
 * Pantallas del invitado sin cuenta (CU-16 y CU-20) sobre la navegación
 * compartida. El invitado se identifica con su correo y su teléfono; al
 * verificarse en una pantalla, la otra los usa sin pedirlos de nuevo.
 */
public final class AccesoInvitado {

    private final Navegacion navegacion = new Navegacion();
    private ViewRespuestaInvitacion vistaRespuesta;
    private ViewMesaRegalos vistaMesaRegalos;
    private String correo;
    private String telefono;

    public AccesoInvitado(ControllerInvitados controller, Runnable salir) {
        navegacion
                .registrar(Navegacion.Pantalla.MIS_INVITACIONES, () -> {
                    vistaRespuesta = new ViewRespuestaInvitacion(controller,
                            this::irMisInvitaciones, this::irMesaRegalos, salir, this::recordar);
                    return vistaRespuesta.crearPantalla();
                })
                .registrar(Navegacion.Pantalla.MESA_REGALOS, () -> {
                    vistaMesaRegalos = new ViewMesaRegalos(controller,
                            this::irMisInvitaciones, this::irMesaRegalos, salir, this::recordar);
                    return vistaMesaRegalos.crearPantalla();
                });
    }

    /** Muestra el acceso dentro del contenedor principal de la aplicación. */
    public void mostrarEn(BorderPane contenedor, Navegacion.Pantalla inicial) {
        navegacion.mostrarEn(contenedor, inicial);
    }

    /** Crea una escena propia, para abrir el acceso desde un lanzador. */
    public Scene crearEscena(Navegacion.Pantalla inicial, double ancho, double alto) {
        return navegacion.crearEscena(inicial, ancho, alto);
    }

    public void irMisInvitaciones() {
        navegacion.ir(Navegacion.Pantalla.MIS_INVITACIONES);
        vistaRespuesta.usarDatos(correo, telefono);
    }

    public void irMesaRegalos() {
        navegacion.ir(Navegacion.Pantalla.MESA_REGALOS);
        vistaMesaRegalos.usarDatos(correo, telefono);
    }

    private void recordar(String correoVerificado, String telefonoVerificado) {
        correo = correoVerificado;
        telefono = telefonoVerificado;
    }
}
