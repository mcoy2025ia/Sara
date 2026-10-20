import entities.Servicio;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryServicio;
import services.ServiceProveedores;

// Comprueba que el service pueda enviar un servicio al repositorio
public class PruebaServiceProveedores {

    public static void main(String[] args) {

        // Primero creamos el repositorio que va a guardar los servicios
        RepositoryServicio repository = new RepositoryServicio();
        RepositoryDisponibilidadServicio repositoryDisponibilidad =
        new RepositoryDisponibilidadServicio();

        ServiceProveedores service = new ServiceProveedores(repository, repositoryDisponibilidad);

        Servicio servicio = new Servicio();
        servicio.setIdServicio(2);
        servicio.setIdProveedor(10);
        servicio.setNombre("Decoracion para eventos");
        servicio.setDescripcion("Decoracion del lugar del evento");
        servicio.setPrecio(800000);
        servicio.setCategoria("Decoracion");
        servicio.setEstado("Activo");

        // En vez de guardar directamente desde la prueba, pasamos por el service
        service.crearServicio(servicio);

        // Buscamos en el repositorio para comprobar que el service si lo envio
        Servicio servicioEncontrado = repository.buscarServicio(2);

        // Creamos los nuevos datos para modificar el mismo servicio
        Servicio servicioModificado = new Servicio();
        servicioModificado.setIdProveedor(10);
        servicioModificado.setNombre("Decoracion premium para eventos");
        servicioModificado.setDescripcion("Servicio de decoracion actualizado");
        servicioModificado.setPrecio(950000);
        servicioModificado.setCategoria("Decoracion");
        servicioModificado.setEstado("Activo");
        servicioModificado.setTipo("Decoracion");
        servicioModificado.setCantidadMaximaPersonas(180);
        servicioModificado.setCiudadZona("Bogota");
        servicioModificado.setAnticipacionMinima(6);

        // Modificamos el servicio usando su id
        service.modificarServicio(10, 2, servicioModificado);

        Servicio servicioActualizado = repository.buscarServicio(2);

        if (servicioActualizado != null
                && servicioActualizado.getNombre().equals("Decoracion premium para eventos")
                && servicioActualizado.getPrecio() == 950000) {

            System.out.println("El service modifico el servicio correctamente");
            System.out.println("Nuevo nombre: " + servicioActualizado.getNombre());
            System.out.println("Nuevo precio: " + servicioActualizado.getPrecio());
        } else {
            System.out.println("El service no modifico el servicio correctamente");
        }

        if (servicioEncontrado != null) {
            System.out.println("El service creo el servicio correctamente");
            System.out.println("Nombre: " + servicioEncontrado.getNombre());
        } else {
            System.out.println("El servicio no fue guardado");
        }
    }
}