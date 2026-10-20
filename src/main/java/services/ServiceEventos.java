package services;

import entities.Evento;
import entities.EdicionEvento;
import entities.Organizador;
import entities.SesionUsuario;
import java.util.List;
import repositories.RepositoryEvento;
import repositories.RepositoryCancelacion;
import repositories.RepositoryFinalizacion;
import entities.Fotografia;
import entities.ServicioSolicitable;
import entities.PresupuestoEvento;
import entities.SolicitudServicioEvento;
import java.math.BigDecimal;
import repositories.RepositorySolicitudServicio;

public class ServiceEventos {
    private final RepositoryEvento repositoryEvento;
    private final RepositoryCancelacion cancelaciones;
    private final RepositoryFinalizacion finalizaciones;
    private final RepositorySolicitudServicio solicitudes;

    public ServiceEventos(RepositoryEvento repositoryEvento) {
        this(repositoryEvento, new RepositoryCancelacion());
    }

    public ServiceEventos(RepositoryEvento repositoryEvento, RepositoryCancelacion cancelaciones) {
        this(repositoryEvento, cancelaciones, new RepositoryFinalizacion());
    }

    public ServiceEventos(RepositoryEvento repositoryEvento, RepositoryCancelacion cancelaciones, RepositoryFinalizacion finalizaciones) {
        this(repositoryEvento,cancelaciones,finalizaciones,new RepositorySolicitudServicio());
    }
    public ServiceEventos(RepositoryEvento repositoryEvento, RepositoryCancelacion cancelaciones,
            RepositoryFinalizacion finalizaciones, RepositorySolicitudServicio solicitudes) {
        this.repositoryEvento = repositoryEvento;
        this.cancelaciones = cancelaciones;
        this.finalizaciones = finalizaciones;
        this.solicitudes = solicitudes;
    }

    public void crearEvento(Evento datos, SesionUsuario sesion) {
        int organizador=validarSesionOrganizador(sesion);
        if(datos!=null && datos.getIdOrganizador()!=organizador)
            throw new IllegalArgumentException("El evento debe pertenecer a tu cuenta.");
        if(!solicitudes.hayProveedorVerificado())
            throw new IllegalArgumentException("Debe existir al menos un proveedor verificado en el directorio antes de crear el evento.");
        crearEvento(datos);
    }

    public List<ServicioSolicitable> listarServiciosSolicitables(SesionUsuario sesion) {
        validarSesionOrganizador(sesion); return solicitudes.listarServicios();
    }
    public PresupuestoEvento consultarPresupuesto(int evento, SesionUsuario sesion) {
        return solicitudes.presupuesto(evento,validarSesionOrganizador(sesion));
    }
    public List<SolicitudServicioEvento> listarSolicitudesServicio(int evento, SesionUsuario sesion) {
        return solicitudes.listarSolicitudes(evento,validarSesionOrganizador(sesion));
    }
    public int solicitarServicio(int evento, int servicio, SesionUsuario sesion) {
        return solicitudes.solicitar(evento,validarSesionOrganizador(sesion),servicio);
    }
    public void retirarSolicitudServicio(int evento, int solicitud, SesionUsuario sesion) {
        solicitudes.retirar(evento,validarSesionOrganizador(sesion),solicitud);
    }
    public void ajustarPresupuesto(int evento, BigDecimal total, SesionUsuario sesion) {
        int organizador=validarSesionOrganizador(sesion);
        if(total==null || total.signum()<0 || total.compareTo(new BigDecimal("9999999999.99"))>0)
            throw new IllegalArgumentException("Ingresa un presupuesto entre 0 y 9999999999.99 COP.");
        try { total.setScale(2,java.math.RoundingMode.UNNECESSARY); }
        catch(ArithmeticException e) { throw new IllegalArgumentException("El presupuesto debe tener como máximo dos decimales."); }
        solicitudes.ajustarPresupuesto(evento,organizador,total);
    }

    public List<Evento> listarEventosParaFinalizar(SesionUsuario sesion) {
        return repositoryEvento.listarEventosPorOrganizador(validarSesionOrganizador(sesion));
    }

    public void finalizarEvento(int evento, SesionUsuario sesion, List<Fotografia> fotos) {
        int organizador=validarSesionOrganizador(sesion);
        if(evento<=0) throw new IllegalArgumentException("Selecciona un evento de tu cuenta.");
        validarFotografias(fotos);
        finalizaciones.finalizar(evento,organizador,fotos);
    }

    public static void validarFotografias(List<Fotografia> fotos) {
        if(fotos==null || fotos.size()>10) throw new IllegalArgumentException("Selecciona hasta 10 fotografías PNG o JPG, de hasta 5 MB cada una.");
        for(Fotografia foto:fotos) {
            if(foto==null || foto.nombre()==null || foto.nombre().isBlank() || foto.nombre().length()>150
                    || foto.contenido()==null || foto.contenido().length==0 || foto.contenido().length>5*1024*1024)
                throw new IllegalArgumentException("Cada fotografía debe tener un nombre de hasta 150 caracteres y pesar hasta 5 MB.");
            try (var entrada=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(foto.contenido()))) {
                var lectores=javax.imageio.ImageIO.getImageReaders(entrada);
                if(!lectores.hasNext()) throw new IllegalArgumentException("Selecciona fotografías PNG o JPG válidas.");
                var lector=lectores.next();
                try {
                    lector.setInput(entrada);
                    String formato=lector.getFormatName();
                    if(!(formato.equalsIgnoreCase("png") || formato.equalsIgnoreCase("jpeg"))
                            || (long)lector.getWidth(0)*lector.getHeight(0)>20_000_000)
                        throw new IllegalArgumentException("Usa fotografías PNG o JPG de hasta 20 megapíxeles.");
                    lector.read(0);
                } finally { lector.dispose(); }
            } catch(java.io.IOException e) { throw new IllegalArgumentException("No se pudo leer una fotografía. Selecciona un archivo PNG o JPG válido.",e); }
        }
    }

    public List<String> proveedoresParaCalificar(int evento, SesionUsuario sesion) {
        return finalizaciones.proveedoresParaCalificar(evento,validarSesionOrganizador(sesion));
    }

    public boolean galeriaPendiente(int evento, SesionUsuario sesion) {
        return finalizaciones.galeriaPendiente(evento,validarSesionOrganizador(sesion));
    }

    public List<Organizador> listarOrganizadores() {
        return repositoryEvento.listarOrganizadores();
    }

    public List<Evento> listarEventosParaCancelar(SesionUsuario sesion) {
        return repositoryEvento.listarEventosPorOrganizador(validarSesionOrganizador(sesion));
    }

    public void cancelarEvento(int evento, SesionUsuario sesion, boolean confirmado) {
        int organizador = validarSesionOrganizador(sesion);
        if (evento <= 0) throw new IllegalArgumentException("Selecciona un evento de tu cuenta.");
        if (!confirmado) throw new IllegalArgumentException("Confirma explícitamente que deseas cancelar el evento.");
        cancelaciones.cancelar(evento, organizador);
    }

    public List<String> listarAvisosCancelacion(int evento, SesionUsuario sesion) {
        return cancelaciones.listarAvisos(evento, validarSesionOrganizador(sesion));
    }

    public List<Evento> listarEventosParaEditar(SesionUsuario sesion) {
        return repositoryEvento.listarEventosPorOrganizador(validarSesionOrganizador(sesion));
    }

    public String nombreOrganizador(SesionUsuario sesion) {
        validarSesionOrganizador(sesion);
        return sesion.getNombre();
    }

    private int validarSesionOrganizador(SesionUsuario sesion) {
        if(sesion==null || sesion.getIdUsuario()<=0 || sesion.getIdActor()<=0
                || !"ORGANIZADOR".equalsIgnoreCase(sesion.getTipoUsuario())
                || !"ACTIVO".equalsIgnoreCase(sesion.getEstadoUsuario())
                || !"ACTIVO".equalsIgnoreCase(sesion.getEstadoActor())
                || !repositoryEvento.organizadorActivo(sesion.getIdUsuario(),sesion.getIdActor()))
            throw new IllegalStateException("Inicia sesión como organizador activo para gestionar tus eventos.");
        return sesion.getIdActor();
    }

    public Evento consultarEventoParaEditar(int evento, SesionUsuario sesion) {
        int organizador=validarSesionOrganizador(sesion);
        Evento actual=repositoryEvento.buscarEvento(evento,organizador);
        if(actual==null) throw new IllegalArgumentException("Selecciona un evento existente de tu cuenta.");
        if(!Evento.ESTADO_INICIAL.equalsIgnoreCase(actual.getEstado()))
            throw new IllegalArgumentException("No se puede editar: el evento está en estado " + actual.getEstado() + ".");
        return actual;
    }

    public void editarEvento(int evento, SesionUsuario sesion, EdicionEvento datos) {
        int organizador=validarSesionOrganizador(sesion);
        if(evento<=0) throw new IllegalArgumentException("Selecciona un evento de tu cuenta.");
        if(datos==null) throw new IllegalArgumentException("Completa los datos del evento.");
        String error=errorDatosGenerales(datos.nombre(),datos.tipo(),datos.inicio(),datos.fin(),datos.lugar());
        if(error!=null) throw new IllegalArgumentException(error);
        repositoryEvento.editarEvento(evento,organizador,datos);
    }

    public void crearEvento(Evento datos) {
        String error = obtenerError(datos);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        datos.setEstado(Evento.ESTADO_INICIAL);
        repositoryEvento.guardarEvento(datos);
    }

    public boolean validarInformacionEvento(Evento datos) {
        return obtenerError(datos) == null;
    }

    private String obtenerError(Evento datos) {
        if (datos == null) return "Completa los datos del evento.";
        if (datos.getIdOrganizador() <= 0) return "Ingresa un identificador de organizador mayor que cero.";
        String general=errorDatosGenerales(datos.getNombre(),datos.getTipo(),datos.getFechaInicio(),datos.getFechaFin(),datos.getUbicacion());
        if(general!=null) return general;
        if (datos.getDescripcion() != null && datos.getDescripcion().length() > 255)
            return "La descripción debe tener hasta 255 caracteres.";
        if (datos.getPresupuesto() == null || datos.getPresupuesto().signum() < 0)
            return "Ingresa un presupuesto mayor o igual a cero.";
        if (datos.getPresupuesto().compareTo(new java.math.BigDecimal("9999999999.99")) > 0)
            return "El presupuesto no puede superar 9999999999.99 COP.";
        try {
            datos.getPresupuesto().setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            return "El presupuesto debe tener como máximo dos decimales.";
        }
        if (datos.getInvitadosIniciales() == null || datos.getInvitadosIniciales() < 0) return "Los invitados iniciales deben ser mayores o iguales a cero.";
        return null;
    }

    private String errorDatosGenerales(String nombre,String tipo,java.time.LocalDateTime inicio,java.time.LocalDateTime fin,String lugar) {
        if(!textoValido(nombre,150)) return "Ingresa un nombre de hasta 150 caracteres.";
        if(!textoValido(tipo,100)) return "Selecciona el tipo de evento.";
        if(!textoValido(lugar,255)) return "Ingresa un lugar de hasta 255 caracteres.";
        if(inicio==null || fin==null) return "Selecciona las fechas de inicio y fin.";
        if(fin.isBefore(inicio)) return "El fin del evento no puede ser anterior al inicio.";
        return null;
    }

    private boolean textoValido(String texto, int longitudMaxima) {
        return texto != null && !texto.isBlank() && texto.length() <= longitudMaxima;
    }
}
