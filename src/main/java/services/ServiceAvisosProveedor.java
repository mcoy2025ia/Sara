package services;

import entities.SesionUsuario;
import repositories.RepositoryAvisosProveedor;
import java.util.List;

public class ServiceAvisosProveedor {
    private final RepositoryAvisosProveedor repository;
    public ServiceAvisosProveedor(RepositoryAvisosProveedor repository) { this.repository = repository; }

    public List<String> listar(SesionUsuario sesion) {
        if (sesion == null || sesion.getIdUsuario() <= 0 || sesion.getIdActor() <= 0
                || !"PROVEEDOR".equalsIgnoreCase(sesion.getTipoUsuario())
                || !"ACTIVO".equalsIgnoreCase(sesion.getEstadoUsuario())
                || !("APROBADO".equalsIgnoreCase(sesion.getEstadoActor()) || "VERIFICADO".equalsIgnoreCase(sesion.getEstadoActor())))
            throw new IllegalStateException("Inicia sesión como proveedor aprobado para consultar tus avisos.");
        return repository.listar(sesion);
    }
}
