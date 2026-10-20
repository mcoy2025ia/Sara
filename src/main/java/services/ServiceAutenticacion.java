package services;

import entities.SesionUsuario;
import repositories.RepositoryAutenticacion;

public class ServiceAutenticacion {

    private final RepositoryAutenticacion repository;

    public ServiceAutenticacion(
            RepositoryAutenticacion repository) {

        this.repository = repository;
    }

    public SesionUsuario iniciarSesion(
            String usuario,
            String contrasena) {

        if (usuario == null
                || usuario.isBlank()
                || contrasena == null
                || contrasena.isBlank()) {

            throw new IllegalArgumentException(
                    "Ingresa usuario y contrasena.");
        }

        SesionUsuario sesion
                = repository.autenticar(
                        usuario.trim(),
                        contrasena);

        if (sesion == null) {
            throw new IllegalArgumentException(
                    "Usuario o contrasena incorrectos.");
        }

        if (!"ACTIVO".equalsIgnoreCase(
                sesion.getEstadoUsuario())) {

            throw new IllegalStateException(
                    "El usuario no esta activo.");
        }

        if ("PROVEEDOR".equalsIgnoreCase(
                sesion.getTipoUsuario())) {

            if (sesion.getIdActor() == 0
                    || !("APROBADO".equalsIgnoreCase(sesion.getEstadoActor())
                         || "VERIFICADO".equalsIgnoreCase(sesion.getEstadoActor()))) {

                throw new IllegalStateException(
                        "El proveedor aun no esta aprobado.");
            }

            return sesion;
        }

        if ("ORGANIZADOR".equalsIgnoreCase(
                sesion.getTipoUsuario())) {

            if (sesion.getIdActor() == 0
                    || !"ACTIVO".equalsIgnoreCase(
                            sesion.getEstadoActor())) {

                throw new IllegalStateException(
                        "El organizador no esta activo.");
            }

            return sesion;
        }
        if ("CLIENTE".equalsIgnoreCase(
        sesion.getTipoUsuario())) {

        return sesion;
        }
        
        if ("ADMINISTRADOR".equalsIgnoreCase(
                sesion.getTipoUsuario())) {

            return sesion;
        }

        throw new IllegalStateException(
                "Este usuario no tiene acceso a estas funciones.");
    }
}
