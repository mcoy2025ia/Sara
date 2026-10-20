package app;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Navegacion {

    public enum Pantalla {
        EVENTOS("NextEventux | Crear evento"),
        INVITADOS("NextEventux | Invitaciones"),
        CATALOGO_SERVICIOS("NextEventux | Proveedores"),
        DETALLE_SERVICIO("NextEventux | Detalle del servicio"),
        MIS_SERVICIOS("NextEventux | Mis servicios"),
        AVISOS_PROVEEDOR("NextEventux | Mis avisos"),
        DETALLE_SERVICIO_PROVEEDOR("NextEventux | Administración del servicio"),
        MIS_EVENTOS("NextEventux | Mis eventos"),
        EDICION_EVENTO("NextEventux | Editar evento"),
        CANCELACION_EVENTO("NextEventux | Cancelar evento"),
        FINALIZACION_EVENTO("NextEventux | Finalizar evento"),
        PUBLICACION_EVENTOS("NextEventux | Publicación de eventos"),
        MIS_INVITACIONES("NextEventux | Mis invitaciones"),
        MESA_REGALOS("NextEventux | Mesa de regalos"),
        SOLICITUDES_PROVEEDOR("NextEventux | Solicitudes recibidas"),
        PROPUESTAS_ORGANIZADOR("NextEventux | Propuestas recibidas"),
        ESTADO_FINANCIERO("NextEventux | Estado financiero");

        private final String titulo;

        Pantalla(String titulo) {
            this.titulo = titulo;
        }
    }

    private final Map<Pantalla, Supplier<Parent>> fabricas = new EnumMap<>(Pantalla.class);
    private final Map<Pantalla, Parent> creadas = new EnumMap<>(Pantalla.class);
    private final Map<Pantalla, Boolean> dinamicas = new EnumMap<>(Pantalla.class);
    private Scene scene;
    private BorderPane contenedor;

    public Navegacion registrar(Pantalla pantalla, Supplier<Parent> fabrica) {
        registrar(pantalla, fabrica, false);
        return this;
    }

    public Navegacion registrarDinamica(
            Pantalla pantalla,
            Supplier<Parent> fabrica) {

        registrar(pantalla, fabrica, true);
        return this;
    }

    public void invalidar(Pantalla pantalla) {
        creadas.remove(Objects.requireNonNull(pantalla, "pantalla"));
    }

    private void registrar(
            Pantalla pantalla,
            Supplier<Parent> fabrica,
            boolean dinamica) {

        fabricas.put(
                Objects.requireNonNull(pantalla, "pantalla"),
                Objects.requireNonNull(fabrica, "fabrica"));
        dinamicas.put(pantalla, dinamica);
        creadas.remove(pantalla);
    }

    public Scene crearEscena(Pantalla inicial, double ancho, double alto) {
        scene = new Scene(obtener(inicial), ancho, alto);
        return scene;
    }

    public void mostrarEn(BorderPane contenedor, Pantalla inicial) {
        this.contenedor = Objects.requireNonNull(contenedor, "contenedor");
        ir(inicial);
    }

    public void ir(Pantalla pantalla) {
        if (!fabricas.containsKey(pantalla)) {
            throw new IllegalArgumentException(
                    "No se registró la pantalla " + pantalla);
        }
        Scene actual;
        if (contenedor != null) {
            contenedor.setCenter(obtener(pantalla));
            actual = contenedor.getScene();
        } else if (scene != null) {
            scene.setRoot(obtener(pantalla));
            actual = scene;
        } else {
            return;
        }
        if (actual != null && actual.getWindow() instanceof Stage stage) {
            stage.setTitle(pantalla.titulo);
        }
    }

    private Parent obtener(Pantalla pantalla) {
        if (Boolean.TRUE.equals(dinamicas.get(pantalla))) {
            return crear(pantalla);
        }
        return creadas.computeIfAbsent(pantalla, p -> {
            return crear(p);
        });
    }

    private Parent crear(Pantalla pantalla) {
        return Objects.requireNonNull(
                fabricas.get(pantalla).get(),
                "La fábrica de " + pantalla + " devolvió una pantalla nula");
    }
}
