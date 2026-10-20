import java.util.Date;

import controllers.ControllerProveedores;
import entities.DisponibilidadServicio;
import entities.Servicio;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryServicio;
import services.ServiceProveedores;

// Comprueba el flujo desde el controller hasta el repositorio
public class PruebaControllerProveedores {

    public static void main(String[] args) {

        // Creamos las capas que necesita el flujo
        RepositoryServicio repository = new RepositoryServicio();
        RepositoryDisponibilidadServicio repositoryDisponibilidad = new RepositoryDisponibilidadServicio();
        ServiceProveedores service = new ServiceProveedores(repository, repositoryDisponibilidad);
        ControllerProveedores controller = new ControllerProveedores(service);

        Servicio servicio = new Servicio();
        servicio.setIdServicio(3);
        servicio.setIdProveedor(10);
        servicio.setNombre("Catering para eventos");
        servicio.setDescripcion("Servicio de alimentacion para el evento");
        servicio.setPrecio(1200000);
        servicio.setCategoria("Alimentacion");
        servicio.setEstado("Activo");
        servicio.setTipo("Catering");
        servicio.setCantidadMaximaPersonas(150);
        servicio.setCiudadZona("Bogota");
        servicio.setAnticipacionMinima(7);

        // El servicio entra por el controller y sigue por las demas capas
        controller.crearServicio(servicio);

        // Revisamos al final que haya llegado al repositorio
        Servicio servicioEncontrado = repository.buscarServicio(3);

        if (servicioEncontrado != null) {
            System.out.println("El controller completo el flujo correctamente");
            System.out.println("Nombre: " + servicioEncontrado.getNombre());
        } else {
            System.out.println("El servicio no completo el flujo");
        }
        // Creamos los nuevos datos para modificar el servicio
        Servicio servicioModificado = new Servicio();
        servicioModificado.setIdProveedor(10);
        servicioModificado.setNombre("Catering premium para eventos");
        servicioModificado.setDescripcion("Servicio de alimentacion actualizado");
        servicioModificado.setPrecio(1500000);
        servicioModificado.setCategoria("Alimentacion");
        servicioModificado.setEstado("Activo");
        servicioModificado.setTipo("Catering");
        servicioModificado.setCantidadMaximaPersonas(200);
        servicioModificado.setCiudadZona("Bogota");
        servicioModificado.setAnticipacionMinima(7);

        // La modificacion entra por el controller
        controller.modificarServicio(10, 3, servicioModificado);

        // Revisamos que los cambios hayan llegado al repositorio
        Servicio servicioActualizado = repository.buscarServicio(3);

        if (servicioActualizado != null
                && servicioActualizado.getNombre().equals("Catering premium para eventos")
                && servicioActualizado.getPrecio() == 1500000) {

            System.out.println("El controller modifico el servicio correctamente");
            System.out.println("Nuevo nombre: " + servicioActualizado.getNombre());
            System.out.println("Nuevo precio: " + servicioActualizado.getPrecio());
        } else {
            System.out.println("El controller no modifico el servicio correctamente");
        }

        Date fecha = new Date();

        // Registramos una disponibilidad entrando por el controller
        controller.registrarDisponibilidadServicio(
        10,
        3,
        fecha,
        "14:00 - 18:00");

        // Comprobamos que haya llegado al repositorio
        DisponibilidadServicio disponibilidadEncontrada = repositoryDisponibilidad.buscarDisponibilidad(3, fecha, "14:00 - 18:00");

        if (disponibilidadEncontrada != null) {
            System.out.println("El controller registro la disponibilidad correctamente");
            System.out.println("Horario: " + disponibilidadEncontrada.getHorario());
        } else {
            System.out.println("La disponibilidad no completo el flujo");
        }
    }
}