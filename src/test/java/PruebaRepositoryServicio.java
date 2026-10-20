import entities.Servicio;
import repositories.RepositoryServicio;

// Prueba sencilla para comprobar que podemos guardar y volver a buscar un servicio
public class PruebaRepositoryServicio {

    public static void main(String[] args) {

        RepositoryServicio repository = new RepositoryServicio();

        // Creamos un servicio de prueba con algunos datos
        Servicio servicio = new Servicio();
        servicio.setIdServicio(1);
        servicio.setIdProveedor(10);
        servicio.setNombre("Fotografía para eventos");
        servicio.setDescripcion("Servicio de fotografía para el evento");
        servicio.setPrecio(500000);
        servicio.setCategoria("Fotografía");
        servicio.setEstado("Activo");

        // Lo guardamos en el repositorio
        repository.guardarServicio(servicio);

        // Intentamos encontrar el mismo servicio usando su id
        Servicio servicioEncontrado = repository.buscarServicio(1);

        if (servicioEncontrado != null) {
            System.out.println("Servicio encontrado correctamente");
            System.out.println("Nombre: " + servicioEncontrado.getNombre());
            System.out.println("Precio: " + servicioEncontrado.getPrecio());
        } else {
            System.out.println("No se encontró el servicio");
        }
    // Cambiamos algunos datos del mismo servicio
    Servicio servicioModificado = new Servicio();
    servicioModificado.setIdServicio(1);
    servicioModificado.setIdProveedor(10);
    servicioModificado.setNombre("Fotografia premium para eventos");
    servicioModificado.setDescripcion("Servicio de fotografia actualizado");
    servicioModificado.setPrecio(750000);
    servicioModificado.setCategoria("Fotografia");
    servicioModificado.setEstado("Activo");
    servicioModificado.setTipo("Fotografia");
    servicioModificado.setCantidadMaximaPersonas(200);
    servicioModificado.setCiudadZona("Bogota");
    servicioModificado.setAnticipacionMinima(5);

    // Pedimos al repositorio que actualice el servicio existente
    repository.actualizarServicio(servicioModificado);

    Servicio servicioActualizado = repository.buscarServicio(1);

    if (servicioActualizado != null
            && servicioActualizado.getNombre().equals("Fotografia premium para eventos")
            && servicioActualizado.getPrecio() == 750000) {

        System.out.println("Servicio actualizado correctamente");
        System.out.println("Nuevo nombre: " + servicioActualizado.getNombre());
        System.out.println("Nuevo precio: " + servicioActualizado.getPrecio());
    } else {
        System.out.println("El servicio no fue actualizado correctamente");
    }
    }
}