package controllers;

import entities.Evento;
import entities.EdicionEvento;
import entities.Organizador;
import entities.SesionUsuario;
import entities.Fotografia;
import java.util.List;
import services.ServiceEventos;

public class ControllerEventos {

    private final ServiceEventos serviceEventos;
    private final SesionUsuario sesion;

    public ControllerEventos(ServiceEventos serviceEventos) {
        this(serviceEventos,null);
    }

    public ControllerEventos(ServiceEventos serviceEventos, SesionUsuario sesion) {
        this.serviceEventos = serviceEventos;
        this.sesion = sesion;
    }

    public List<Organizador> listarOrganizadores() {
        return serviceEventos.listarOrganizadores();
    }

    public List<Evento> listarEventosParaFinalizar() { return serviceEventos.listarEventosParaFinalizar(sesion); }
    public List<Evento> listarMisEventos() { return serviceEventos.listarEventosParaFinalizar(sesion); }
    public String nombreOrganizadorFinalizacion() { return serviceEventos.nombreOrganizador(sesion); }
    public void finalizarEvento(int evento, List<Fotografia> fotos) { serviceEventos.finalizarEvento(evento,sesion,fotos); }
    public List<String> proveedoresParaCalificar(int evento) { return serviceEventos.proveedoresParaCalificar(evento,sesion); }
    public boolean galeriaPendiente(int evento) { return serviceEventos.galeriaPendiente(evento,sesion); }
    public List<entities.ServicioSolicitable> listarServiciosSolicitables() { return serviceEventos.listarServiciosSolicitables(sesion); }
    public entities.PresupuestoEvento consultarPresupuesto(int evento) { return serviceEventos.consultarPresupuesto(evento,sesion); }
    public List<entities.SolicitudServicioEvento> listarSolicitudesServicio(int evento) { return serviceEventos.listarSolicitudesServicio(evento,sesion); }
    public int solicitarServicio(int evento, int servicio) { return serviceEventos.solicitarServicio(evento,servicio,sesion); }
    public void retirarSolicitudServicio(int evento, int solicitud) { serviceEventos.retirarSolicitudServicio(evento,solicitud,sesion); }
    public void ajustarPresupuesto(int evento, java.math.BigDecimal total) { serviceEventos.ajustarPresupuesto(evento,total,sesion); }

    public List<Evento> listarEventosParaCancelar() { return serviceEventos.listarEventosParaCancelar(sesion); }
    public String nombreOrganizadorCancelacion() { return serviceEventos.nombreOrganizador(sesion); }
    public void cancelarEvento(int evento, boolean confirmado) { serviceEventos.cancelarEvento(evento, sesion, confirmado); }
    public List<String> listarAvisosCancelacion(int evento) { return serviceEventos.listarAvisosCancelacion(evento, sesion); }

    public List<Evento> listarEventosParaEditar() { return serviceEventos.listarEventosParaEditar(sesion); }
    public String nombreOrganizadorEdicion() { return serviceEventos.nombreOrganizador(sesion); }
    public Evento consultarEventoParaEditar(int evento) { return serviceEventos.consultarEventoParaEditar(evento,sesion); }
    public void editarEvento(int evento,EdicionEvento datos) { serviceEventos.editarEvento(evento,sesion,datos); }

    public void crearEvento(Evento datos) {
        serviceEventos.crearEvento(datos,sesion);
    }
}
