package services;

import entities.ClienteMarketing;
import entities.RangoEdad;
import entities.SegmentoCalculado;
import entities.SolicitudEvento;
import entities.Segmentacion;
import repositories.RepositoryMarketing;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ServiceMarketing {
    public static final int MINIMO_CLIENTES_POR_SEGMENTO = 5;

    private final RepositoryMarketing repositorio;

    public ServiceMarketing(RepositoryMarketing repositorio) {
        this.repositorio = repositorio;
    }

    // Clientes que comparten país, ciudad y rango de edad.
    private record GrupoClientes(String pais, String ciudad, RangoEdad rangoEdad, List<ClienteMarketing> miembros) {

        boolean coincideCon(String otroPais, String otraCiudad, RangoEdad otroRango) {
            return pais.equals(otroPais) && ciudad.equals(otraCiudad) && rangoEdad == otroRango;
        }
    }

    public List<Segmentacion> listarSegmentosActivos() {
        return repositorio.listarSegmentosActivos();
    }
    
    public List<Segmentacion> segmentarClientes(int idAdministrador) {
        return segmentarClientes(idAdministrador, LocalDate.now());
    }

    public List<Segmentacion> segmentarClientes(int idAdministrador, LocalDate hoy) {
        if (!repositorio.existeAdministradorActivo(idAdministrador)) {
            throw new IllegalArgumentException("Solo un administrador activo puede segmentar clientes.");
        }

        List<SegmentoCalculado> calculados = calcularSegmentos(
                repositorio.listarClientesActivos(), repositorio.contarSolicitudesPorTipo(), hoy);
        if (calculados.isEmpty()) {
            return new ArrayList<>();
        }

        List<Segmentacion> segmentos = new ArrayList<>();
        for (SegmentoCalculado calculado : calculados) {
            segmentos.add(convertirEnSegmentacion(idAdministrador, calculado));
        }
        return repositorio.reemplazarSegmentosAutomaticos(segmentos);
    }

    private Segmentacion convertirEnSegmentacion(int idAdministrador, SegmentoCalculado calculado) {
        String edad = calculado.getRangoEdad().getEtiqueta();
        String nombre = calculado.getCiudad() + " " + edad + " - " + calculado.getTipoEvento();
        String criterios = Segmentacion.PREFIJO_CRITERIOS_AUTOMATICOS
                + "pais=" + calculado.getPais()
                + "|ciudad=" + calculado.getCiudad()
                + "|edad=" + edad
                + "|tipo=" + calculado.getTipoEvento();
        String descripcion = "Clientes de " + calculado.getCiudad() + " (" + calculado.getPais() + ") de "
                + edad + " años que más solicitan eventos de tipo " + calculado.getTipoEvento();
        return new Segmentacion(idAdministrador, nombre, criterios, descripcion,
                Segmentacion.ESTADO_ACTIVA, calculado.getIdsClientes());
    }

    public List<SegmentoCalculado> calcularSegmentos(List<ClienteMarketing> clientes, List<SolicitudEvento> solicitudes, LocalDate hoy) {
        if (clientes == null || solicitudes == null || hoy == null) {
            throw new IllegalArgumentException("Se requieren los clientes, las solicitudes y la fecha actual.");
        }

        List<GrupoClientes> grupos = agruparClientes(clientes, hoy);

        List<SegmentoCalculado> segmentos = new ArrayList<>();
        for (GrupoClientes grupo : grupos) {
            if (grupo.miembros().size() < MINIMO_CLIENTES_POR_SEGMENTO) {
                continue;
            }

            List<Integer> idsClientes = new ArrayList<>();
            for (ClienteMarketing cliente : grupo.miembros()) {
                idsClientes.add(cliente.getIdUsuario());
            }

            String tipoPredominante = obtenerTipoPredominante(idsClientes, solicitudes);
            if (tipoPredominante == null) {
                continue;
            }

            segmentos.add(new SegmentoCalculado(grupo.pais(), grupo.ciudad(), grupo.rangoEdad(),
                    tipoPredominante, idsClientes));
        }

        segmentos.sort(Comparator.comparing(SegmentoCalculado::getPais)
                .thenComparing(SegmentoCalculado::getCiudad)
                .thenComparing(SegmentoCalculado::getRangoEdad));
        return segmentos;
    }

    private List<GrupoClientes> agruparClientes(List<ClienteMarketing> clientes, LocalDate hoy) {
        List<GrupoClientes> grupos = new ArrayList<>();
        for (ClienteMarketing cliente : clientes) {
            if (!hayTexto(cliente.getPais()) || !hayTexto(cliente.getCiudad())
                    || cliente.getFechaNacimiento() == null) {
                continue;
            }
            int edad = Period.between(cliente.getFechaNacimiento(), hoy).getYears();
            RangoEdad rango = RangoEdad.paraEdad(edad);
            if (rango == null) {
                continue;
            }

            String pais = cliente.getPais().trim();
            String ciudad = cliente.getCiudad().trim();
            GrupoClientes grupo = buscarGrupo(grupos, pais, ciudad, rango);
            if (grupo == null) {
                grupo = new GrupoClientes(pais, ciudad, rango, new ArrayList<>());
                grupos.add(grupo);
            }
            grupo.miembros().add(cliente);
        }
        return grupos;
    }

    // Devuelve el grupo que coincide con los datos indicados, o null si todavía no existe.
    private GrupoClientes buscarGrupo(List<GrupoClientes> grupos, String pais, String ciudad,
                                      RangoEdad rango) {
        for (GrupoClientes grupo : grupos) {
            if (grupo.coincideCon(pais, ciudad, rango)) {
                return grupo;
            }
        }
        return null;
    }

    // Devuelve el tipo de evento más solicitado por los clientes indicados, o null si no hay solicitudes.
    // Si dos tipos empatan, gana el primero en orden alfabético para que el resultado sea estable.
    private String obtenerTipoPredominante(List<Integer> idsClientes, List<SolicitudEvento> solicitudes) {
        List<String> tiposRevisados = new ArrayList<>();
        String mejorTipo = null;
        int mejorTotal = 0;

        for (SolicitudEvento solicitud : solicitudes) {
            String tipo = solicitud.getTipo();
            if (tipo == null || !idsClientes.contains(solicitud.getIdCliente())
                    || tiposRevisados.contains(tipo)) {
                continue;
            }
            tiposRevisados.add(tipo);

            int total = sumarSolicitudes(tipo, idsClientes, solicitudes);
            boolean supera = total > mejorTotal;
            boolean empataYVaAntes = total == mejorTotal && mejorTipo != null && tipo.compareTo(mejorTipo) < 0;
            if (supera || empataYVaAntes) {
                mejorTipo = tipo;
                mejorTotal = total;
            }
        }
        return mejorTipo;
    }

    // Suma cuántas veces los clientes indicados solicitaron un tipo de evento.
    private int sumarSolicitudes(String tipo, List<Integer> idsClientes, List<SolicitudEvento> solicitudes) {
        int total = 0;
        for (SolicitudEvento solicitud : solicitudes) {
            if (tipo.equals(solicitud.getTipo()) && idsClientes.contains(solicitud.getIdCliente())) {
                total += solicitud.getCantidad();
            }
        }
        return total;
    }

    private static boolean hayTexto(String texto) {
        return texto != null && !texto.isBlank();
    }
}