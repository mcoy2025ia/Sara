package controllers;

import java.util.Map;
import entities.SesionUsuario;
import services.ServiceFinanciera;

public class ControllerFinanciera {

    private final ServiceFinanciera serviceFinanciera;
    private final SesionUsuario sesion;

    public ControllerFinanciera(ServiceFinanciera serviceFinanciera, SesionUsuario sesion) {
        this.serviceFinanciera = serviceFinanciera;
        this.sesion = sesion;
    }

    public Map<String, Object> consultarEstadoFinanciero(int idEvento) {
        return serviceFinanciera.consultarEstadoFinanciero(idEvento, sesion);
    }
}
