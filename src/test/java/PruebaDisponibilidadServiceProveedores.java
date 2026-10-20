
import java.util.Date;

import entities.DisponibilidadServicio;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryServicio;
import services.ServiceProveedores;

// Comprueba que el service pueda registrar una disponibilidad
public class PruebaDisponibilidadServiceProveedores {

    public static void main(String[] args) {

        RepositoryServicio repositoryServicio
                = new RepositoryServicio();

        RepositoryDisponibilidadServicio repositoryDisponibilidad
                = new RepositoryDisponibilidadServicio();

        ServiceProveedores service
                = new ServiceProveedores(
                        repositoryServicio,
                        repositoryDisponibilidad);

        Date fecha = new Date();

        // La disponibilidad entra por el service
        service.registrarDisponibilidadServicio(
                10,
                3,
                fecha,
                "14:00 - 18:00");

        // Revisamos que haya llegado al repositorio
        DisponibilidadServicio encontrada
                = repositoryDisponibilidad.buscarDisponibilidad(
                        3,
                        fecha,
                        "14:00 - 18:00");

        if (encontrada != null) {
            System.out.println("El service registro la disponibilidad correctamente");
            System.out.println("Horario: " + encontrada.getHorario());
        } else {
            System.out.println("La disponibilidad no fue registrada");
        }
    }
}
