import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import entities.ClienteMarketing;
import entities.RangoEdad;
import entities.SegmentoCalculado;
import entities.SolicitudEvento;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import repositories.RepositoryMarketing;
import services.ServiceMarketing;

/** Verificación manual de la lógica de segmentación (CU21). */
public class PruebaMarketingSegmentacion {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 18);

    public static void main(String[] args) throws Exception {
        probarReglas();
        probarConDatosDelSeed();
        System.out.println("Segmentación verificada correctamente.");
    }

    private static void probarReglas() {
        List<ClienteMarketing> clientes = new ArrayList<>();
        LocalDate adulto = LocalDate.of(1996, 6, 20); // 30 años: rango 26-35

        // Bogotá 26-35: 5 clientes (justo el mínimo).
        for (int id = 1; id <= 5; id++) {
            clientes.add(new ClienteMarketing(id, "Cliente " + id, "Colombia", "Bogotá", adulto, true));
        }
        // Mismo grupo, pero la ciudad trae espacios: debe unirse al grupo de Bogotá.
        clientes.add(new ClienteMarketing(24, "Con espacios", "Colombia", " Bogotá ", adulto, true));
        // Ciudad con solo 4 clientes: no forma segmento.
        for (int id = 6; id <= 9; id++) {
            clientes.add(new ClienteMarketing(id, "Cliente " + id, "Colombia", "Cali", LocalDate.of(1985, 1, 10), true));
        }
        // Medellín 18-25: 5 clientes, pero sin solicitudes: no forma segmento.
        for (int id = 10; id <= 14; id++) {
            clientes.add(new ClienteMarketing(id, "Cliente " + id, "Colombia", "Medellín", LocalDate.of(2003, 3, 15), true));
        }
        // Cartagena 26-35: 5 clientes con empate entre tipos.
        for (int id = 15; id <= 19; id++) {
            clientes.add(new ClienteMarketing(id, "Cliente " + id, "Colombia", "Cartagena", adulto, true));
        }
        // Datos incompletos o fuera de rango: deben excluirse.
        clientes.add(new ClienteMarketing(20, "Sin ciudad", "Colombia", null, adulto, true));
        clientes.add(new ClienteMarketing(21, "Sin fecha", "Colombia", "Bogotá", null, true));
        clientes.add(new ClienteMarketing(22, "País en blanco", "  ", "Bogotá", adulto, true));
        clientes.add(new ClienteMarketing(23, "Menor de edad", "Colombia", "Bogotá", LocalDate.of(2015, 1, 1), true));

        List<SolicitudEvento> solicitudes = new ArrayList<>();
        for (int id = 1; id <= 5; id++) {
            solicitudes.add(new SolicitudEvento(id, "Social", 1));
        }
        solicitudes.add(new SolicitudEvento(1, "Corporativo", 3));
        // Cartagena: Cultural 2 y Corporativo 2 empatan; gana Corporativo (orden alfabético).
        solicitudes.add(new SolicitudEvento(15, "Cultural", 2));
        solicitudes.add(new SolicitudEvento(16, "Corporativo", 2));

        List<SegmentoCalculado> segmentos = new ServiceMarketing(new RepositoryMarketing()).calcularSegmentos(clientes, solicitudes, HOY);

        comprobar(segmentos.size() == 2, "Deben resultar 2 segmentos.");

        SegmentoCalculado bogota = segmentos.get(0);
        comprobar(bogota.getCiudad().equals("Bogotá"), "El primer segmento debe ser Bogotá.");
        comprobar(bogota.getRangoEdad() == RangoEdad.DE_26_A_35, "Bogotá debe ser del rango 26-35.");
        comprobar(bogota.getCantidadClientes() == 6, "Bogotá debe tener 6 clientes (5 + el de espacios).");
        comprobar(bogota.getTipoEvento().equals("Social"), "En Bogotá debe predominar Social (5 contra 3).");
        for (int excluido : new int[] {20, 21, 22, 23}) {
            comprobar(!bogota.getIdsClientes().contains(excluido), "El cliente " + excluido + " debe excluirse.");
        }

        SegmentoCalculado cartagena = segmentos.get(1);
        comprobar(cartagena.getCiudad().equals("Cartagena"), "El segundo segmento debe ser Cartagena.");
        comprobar(cartagena.getCantidadClientes() == 5, "Cartagena debe tener 5 clientes.");
        comprobar(cartagena.getTipoEvento().equals("Corporativo"), "En el empate debe ganar Corporativo.");

        try {
            new ServiceMarketing(new RepositoryMarketing()).calcularSegmentos(null, solicitudes, HOY);
            throw new AssertionError("Debió rechazar una lista de clientes nula.");
        } catch (IllegalArgumentException esperado) {
            // Es el comportamiento correcto.
        }
        System.out.println("Reglas verificadas con datos de ejemplo en memoria.");
    }

    private static void probarConDatosDelSeed() throws Exception {
        Path directorio = Files.createTempDirectory("marketing-segmentacion-");
        String url = "jdbc:h2:" + directorio.resolve("prueba");
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
        }

        RepositoryMarketing repositorio = new RepositoryMarketing(url);
        List<SegmentoCalculado> segmentos = new ServiceMarketing(repositorio).calcularSegmentos(
                repositorio.listarClientesActivos(), repositorio.contarSolicitudesPorTipo(), HOY);
        for (SegmentoCalculado segmento : segmentos) {
            System.out.println(segmento.getCiudad() + " | " + segmento.getRangoEdad().getEtiqueta()
                    + " | " + segmento.getTipoEvento() + " | " + segmento.getCantidadClientes() + " clientes");
        }

        comprobar(segmentos.size() == 3, "Con el seed deben resultar 3 segmentos.");
        comprobarSegmento(segmentos.get(0), "Bogotá", RangoEdad.DE_18_A_25, "Social", 6);
        comprobarSegmento(segmentos.get(1), "Bogotá", RangoEdad.DE_26_A_35, "Corporativo", 6);
        comprobarSegmento(segmentos.get(2), "Medellín", RangoEdad.DE_26_A_35, "Cultural", 5);
    }

    private static void comprobarSegmento(SegmentoCalculado segmento, String ciudad, RangoEdad rango,
                                          String tipo, int cantidad) {
        comprobar(segmento.getCiudad().equals(ciudad), "Ciudad esperada: " + ciudad);
        comprobar(segmento.getRangoEdad() == rango, "Rango esperado en " + ciudad + ": " + rango.getEtiqueta());
        comprobar(segmento.getTipoEvento().equals(tipo), "Tipo esperado en " + ciudad + ": " + tipo);
        comprobar(segmento.getCantidadClientes() == cantidad, "Cantidad esperada en " + ciudad + ": " + cantidad);
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}