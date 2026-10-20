package controllers;

import entities.SesionUsuario;
import services.ServiceAutenticacion;

public class ControllerAutenticacion {

    private final ServiceAutenticacion service;

    public ControllerAutenticacion(
            ServiceAutenticacion service) {

        this.service = service;
    }

    public SesionUsuario iniciarSesion(
            String usuario,
            String contrasena) {

        return service.iniciarSesion(
                usuario,
                contrasena);
    }
}
