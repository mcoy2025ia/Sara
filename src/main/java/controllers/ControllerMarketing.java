package controllers;

import entities.Segmentacion;
import java.util.List;
import services.ServiceMarketing;

public class ControllerMarketing {

    private final ServiceMarketing serviceMarketing;

    public ControllerMarketing(ServiceMarketing serviceMarketing) {
        this.serviceMarketing = serviceMarketing;
    }

    public List<Segmentacion> segmentarClientes(int idAdministrador) {
        return serviceMarketing.segmentarClientes(idAdministrador);
    }

    public List<Segmentacion> listarSegmentosActivos() {
        return serviceMarketing.listarSegmentosActivos();
    }
}