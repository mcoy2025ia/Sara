import java.util.Date;

import entities.DisponibilidadServicio;
import repositories.RepositoryDisponibilidadServicio;

// Prueba sencilla para comprobar el guardado de disponibilidades
public class PruebaRepositoryDisponibilidadServicio {

    public static void main(String[] args) {

        RepositoryDisponibilidadServicio repository =
                new RepositoryDisponibilidadServicio();

        Date fecha = new Date();

        DisponibilidadServicio disponibilidad =
                new DisponibilidadServicio();

        disponibilidad.setIdDisponibilidad(1);
        disponibilidad.setIdServicio(3);
        disponibilidad.setFecha(fecha);
        disponibilidad.setHorario("14:00 - 18:00");

        repository.guardarDisponibilidad(disponibilidad);

        DisponibilidadServicio encontrada =
                repository.buscarDisponibilidad(
                        3,
                        fecha,
                        "14:00 - 18:00");

        if (encontrada != null) {
            System.out.println("Disponibilidad encontrada correctamente");
            System.out.println("Horario: " + encontrada.getHorario());
        } else {
            System.out.println("No se encontro la disponibilidad");
        }

        System.out.println(
                "Disponibilidades del servicio: "
                        + repository.listarDisponibilidadesServicio(3).size());
    }
}