package services;

import entities.Evento;
import entities.Invitacion;
import entities.InvitacionRecibida;
import entities.Invitado;
import entities.Lugar;
import entities.Regalo;
import entities.ReservaRegalo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import repositories.RepositoryEvento;
import repositories.RepositoryInvitacion;
import repositories.RepositoryInvitado;
import repositories.RepositoryLugar;
import repositories.RepositoryRegalo;
import repositories.RepositoryReservaRegalo;

/**
 * Reglas de negocio del módulo de invitados.
 * CU-17: crear, actualizar y revocar invitaciones y registrar sus invitados,
 * validando el aforo del lugar y que un invitado no tenga dos invitaciones activas
 * en el mismo evento (R40).
 * CU-16: el invitado consulta sus invitaciones (R37) y confirma o rechaza su
 * asistencia (R38); al rechazar libera su cupo (R39).
 * CU-20: el organizador arma la mesa de regalos de cada invitación y el invitado
 * reserva o cancela regalos; cada regalo tiene a lo sumo una reserva activa (R43).
 */
public class ServiceInvitados {
    public List<entities.Fotografia> consultarGaleria(String correo, String telefono) {
        String limpio=limpiar(correo);
        if(limpio==null || limpio.length()>150 || !CORREO.matcher(limpio).matches())
            throw new IllegalArgumentException("Ingresa un correo válido para consultar tu galería.");
        if(soloDigitos(telefono).isEmpty())
            throw new IllegalArgumentException("Ingresa el teléfono que registraste en la invitación.");
        return repositoryInvitado.listarGaleria(limpio,soloDigitos(telefono));
    }

    private static final String MENSAJE_EVENTO_AJENO =
            "Solo puedes gestionar las invitaciones de tus propios eventos.";
    private static final String MENSAJE_MESA_AJENA =
            "Solo el invitado puede usar su mesa de regalos. Verifica tu correo y tu teléfono.";
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final RepositoryInvitacion repositoryInvitacion;
    private final RepositoryInvitado repositoryInvitado;
    private final RepositoryLugar repositoryLugar;
    private final RepositoryEvento repositoryEvento;
    private final RepositoryRegalo repositoryRegalo;
    private final RepositoryReservaRegalo repositoryReservaRegalo;

    public ServiceInvitados() {
        this(new RepositoryInvitacion(), new RepositoryInvitado(), new RepositoryLugar(), new RepositoryEvento(),
                new RepositoryRegalo(), new RepositoryReservaRegalo());
    }

    public ServiceInvitados(RepositoryInvitacion repositoryInvitacion, RepositoryInvitado repositoryInvitado,
                            RepositoryLugar repositoryLugar, RepositoryEvento repositoryEvento) {
        this(repositoryInvitacion, repositoryInvitado, repositoryLugar, repositoryEvento,
                new RepositoryRegalo(), new RepositoryReservaRegalo());
    }

    public ServiceInvitados(RepositoryInvitacion repositoryInvitacion, RepositoryInvitado repositoryInvitado,
                            RepositoryLugar repositoryLugar, RepositoryEvento repositoryEvento,
                            RepositoryRegalo repositoryRegalo, RepositoryReservaRegalo repositoryReservaRegalo) {
        this.repositoryInvitacion = repositoryInvitacion;
        this.repositoryInvitado = repositoryInvitado;
        this.repositoryLugar = repositoryLugar;
        this.repositoryEvento = repositoryEvento;
        this.repositoryRegalo = repositoryRegalo;
        this.repositoryReservaRegalo = repositoryReservaRegalo;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------
    public List<Evento> listarEventos() {
        return repositoryEvento.listarEventos();
    }

    /** Eventos de un organizador: cada organizador solo gestiona las invitaciones de sus eventos. */
    public List<Evento> listarEventos(int idOrganizador) {
        return listarEventos().stream().filter(e -> e.getIdOrganizador() == idOrganizador).toList();
    }

    /** Comprueba que el evento pertenezca al organizador antes de consultarlo o modificarlo. */
    public void validarEventoDelOrganizador(int idOrganizador, int eventoId) {
        if (buscarEvento(eventoId).getIdOrganizador() != idOrganizador)
            throw new IllegalArgumentException(MENSAJE_EVENTO_AJENO);
    }

    public int eventoDeInvitacion(int idInvitacion) {
        Invitacion invitacion = repositoryInvitacion.buscarInvitacion(idInvitacion);
        if (invitacion == null) throw new IllegalArgumentException("La invitación no existe.");
        return invitacion.getIdEvento();
    }

    public int eventoDeInvitado(int idInvitado) {
        Invitado invitado = repositoryInvitado.buscarInvitado(idInvitado);
        if (invitado == null) throw new IllegalArgumentException("El invitado no existe.");
        return eventoDeInvitacion(invitado.getIdInvitacion());
    }

    public int eventoDeRegalo(int idRegalo) {
        return eventoDeInvitacion(buscarRegalo(idRegalo).getIdInvitacion());
    }

    public List<Lugar> listarLugares() {
        return repositoryLugar.listarLugares();
    }

    public List<Invitacion> listarInvitaciones(int eventoId) {
        return repositoryInvitacion.listarInvitaciones(eventoId);
    }

    public List<Invitado> listarInvitados(int idInvitacion) {
        return repositoryInvitado.listarInvitados(idInvitacion);
    }

    public Map<Integer, Integer> contarAsistentesPorInvitacion(int eventoId) {
        return repositoryInvitado.contarAsistentesPorInvitacion(eventoId);
    }

    public int contarAsistentes(int eventoId, int idLugar) {
        return repositoryInvitado.contarAsistentes(eventoId, idLugar);
    }

    // ------------------------------------------------------------------
    // Invitaciones
    // ------------------------------------------------------------------
    public void crearInvitacion(int eventoId, Invitacion datos) {
        if (datos == null) throw new IllegalArgumentException("Completa los datos de la invitación.");
        Evento evento = buscarEventoAbierto(eventoId);
        buscarLugar(datos.getIdLugar());
        validarMensaje(datos.getMensaje());
        datos.setIdEvento(evento.getIdEvento());
        datos.setMensaje(datos.getMensaje().trim());
        datos.setFechaEnvio(LocalDateTime.now());
        datos.setEstadoRespuesta(Invitacion.ESTADO_ENVIADA);
        repositoryInvitacion.guardarInvitacion(datos);
    }

    public void actualizarInvitacion(int idInvitacion, Invitacion datos) {
        if (datos == null) throw new IllegalArgumentException("Completa los datos de la invitación.");
        Invitacion actual = buscarInvitacionActiva(idInvitacion);
        validarEventoModificable(actual.getIdEvento());
        Lugar lugar = buscarLugar(datos.getIdLugar());
        validarMensaje(datos.getMensaje());
        if (lugar.getIdLugar() != actual.getIdLugar()) {
            int deEstaInvitacion = repositoryInvitado.contarAsistentesPorInvitacion(actual.getIdEvento())
                    .getOrDefault(idInvitacion, 0);
            int enNuevoLugar = repositoryInvitado.contarAsistentes(actual.getIdEvento(), lugar.getIdLugar());
            if (deEstaInvitacion + enNuevoLugar > lugar.getCapacidad()) {
                throw new IllegalArgumentException("No se puede cambiar a " + lugar.getNombre() + ": con "
                        + (deEstaInvitacion + enNuevoLugar) + " asistentes superaría su aforo de "
                        + lugar.getCapacidad() + ".");
            }
        }
        actual.setIdLugar(lugar.getIdLugar());
        actual.setMensaje(datos.getMensaje().trim());
        repositoryInvitacion.actualizarInvitacion(actual);
    }

    public void revocarInvitacion(int idInvitacion) {
        Invitacion actual = buscarInvitacionActiva(idInvitacion);
        validarEventoModificable(actual.getIdEvento());
        actual.setEstadoRespuesta(Invitacion.ESTADO_REVOCADA);
        repositoryInvitacion.actualizarInvitacion(actual);
    }

    // ------------------------------------------------------------------
    // Invitados de una invitación
    // ------------------------------------------------------------------
    public void agregarInvitado(int idInvitacion, Invitado datos) {
        if (datos == null) throw new IllegalArgumentException("Completa los datos del invitado.");
        Invitacion invitacion = buscarInvitacionActiva(idInvitacion);
        validarEventoModificable(invitacion.getIdEvento());
        String nombre = limpiar(datos.getNombre());
        String correo = limpiar(datos.getCorreo());
        String telefono = limpiar(datos.getTelefono());
        String direccion = limpiar(datos.getDireccion());
        if (nombre == null || nombre.length() > 100) throw new IllegalArgumentException("Ingresa el nombre del invitado (hasta 100 caracteres).");
        if (correo == null || correo.length() > 150 || !CORREO.matcher(correo).matches())
            throw new IllegalArgumentException("Ingresa un correo válido para el invitado (hasta 150 caracteres).");
        if (telefono != null && (telefono.length() > 30 || !telefono.matches("[0-9+()\\s-]+")))
            throw new IllegalArgumentException("El teléfono solo puede tener números, espacios y los signos + - ( ), hasta 30 caracteres.");
        if (direccion != null && direccion.length() > 255) throw new IllegalArgumentException("La dirección debe tener hasta 255 caracteres.");
        if (repositoryInvitado.existeCorreoEnEvento(invitacion.getIdEvento(), correo))
            throw new IllegalArgumentException("El invitado con correo " + correo
                    + " ya tiene una invitación activa para este evento.");
        Lugar lugar = buscarLugar(invitacion.getIdLugar());
        int asistentes = repositoryInvitado.contarAsistentes(invitacion.getIdEvento(), lugar.getIdLugar());
        if (asistentes + 1 > lugar.getCapacidad()) {
            throw new IllegalArgumentException("Se alcanzó el aforo de " + lugar.getNombre() + " ("
                    + lugar.getCapacidad() + " personas). Ajusta la lista de invitados o cambia el lugar.");
        }
        datos.setIdInvitacion(idInvitacion);
        datos.setNombre(nombre);
        datos.setCorreo(correo.toLowerCase());
        datos.setTelefono(telefono);
        datos.setDireccion(direccion);
        datos.setEstado(Invitado.ESTADO_PENDIENTE);
        repositoryInvitado.guardarInvitado(datos);
    }

    public void quitarInvitado(int idInvitado) {
        Invitado invitado = repositoryInvitado.buscarInvitado(idInvitado);
        if (invitado == null) throw new IllegalArgumentException("El invitado no existe.");
        Invitacion invitacion = repositoryInvitacion.buscarInvitacion(invitado.getIdInvitacion());
        if (invitacion != null) validarEventoModificable(invitacion.getIdEvento());
        if (repositoryInvitado.tieneRegistrosAsociados(idInvitado)) {
            throw new IllegalArgumentException(invitado.getNombre() + " tiene acompañantes, necesidades especiales "
                    + "o regalos registrados; no se puede quitar de la invitación.");
        }
        repositoryInvitado.eliminarInvitado(idInvitado);
    }

    // ------------------------------------------------------------------
    // CU-16: respuesta del invitado
    // ------------------------------------------------------------------
    /**
     * R37: invitaciones activas del invitado. Como los invitados no tienen cuenta, se identifican
     * con su correo y el teléfono que registró el organizador; así nadie consulta ni responde
     * por otra persona (cada invitado solo tiene permisos sobre su propia participación).
     */
    public List<InvitacionRecibida> consultarInvitacionesPorCorreo(String correo, String telefono) {
        String limpio = limpiar(correo);
        if (limpio == null || limpio.length() > 150 || !CORREO.matcher(limpio).matches())
            throw new IllegalArgumentException("Ingresa un correo válido para consultar tus invitaciones.");
        if (soloDigitos(telefono).isEmpty())
            throw new IllegalArgumentException("Ingresa el teléfono que registraste en la invitación.");
        List<InvitacionRecibida> invitaciones = new ArrayList<>();
        for (InvitacionRecibida inv : repositoryInvitado.listarInvitacionesPorCorreo(limpio)) {
            Invitado invitado = repositoryInvitado.buscarInvitado(inv.getIdInvitado());
            if (invitado == null || !mismoTelefono(invitado.getTelefono(), telefono)) continue;
            inv.setOcupados(repositoryInvitado.contarAsistentes(inv.getIdEvento(), inv.getIdLugar()));
            invitaciones.add(inv);
        }
        return invitaciones;
    }

    public List<String> consultarAvisosPorCorreo(String correo) {
        String limpio = limpiar(correo);
        if (limpio == null || limpio.length() > 150 || !CORREO.matcher(limpio).matches())
            throw new IllegalArgumentException("Ingresa un correo válido para consultar tus avisos.");
        return repositoryInvitado.listarAvisosPorCorreo(limpio);
    }

    /**
     * R38 y R39: el invitado confirma (CONFIRMADO) o rechaza (RECHAZADO) su asistencia.
     * Puede cambiar su respuesta mientras el evento no haya comenzado; al confirmar de nuevo
     * se valida que todavía haya cupo para él y sus acompañantes.
     */
    public void gestionarRespuestaInvitacion(int idInvitado, String correo, String telefono, String respuesta) {
        String nueva = respuesta == null ? "" : respuesta.trim().toUpperCase();
        if (!nueva.equals(Invitado.ESTADO_CONFIRMADO) && !nueva.equals(Invitado.ESTADO_RECHAZADO))
            throw new IllegalArgumentException("Elige si asistirás o no al evento.");
        Invitado invitado = repositoryInvitado.buscarInvitado(idInvitado);
        if (invitado == null) throw new IllegalArgumentException("El invitado no existe.");
        validarIdentidad(invitado, correo, telefono);
        Invitacion invitacion = buscarInvitacionActiva(invitado.getIdInvitacion());
        Evento evento = buscarEventoAbierto(invitacion.getIdEvento());
        if (!evento.getFechaInicio().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("El evento ya comenzó; no se puede cambiar la respuesta.");
        String actual = invitado.getEstado() == null ? Invitado.ESTADO_PENDIENTE : invitado.getEstado().toUpperCase();
        if (actual.equals(nueva)) {
            throw new IllegalArgumentException(nueva.equals(Invitado.ESTADO_CONFIRMADO)
                    ? "Ya habías confirmado tu asistencia." : "Ya habías indicado que no asistirás.");
        }
        if (nueva.equals(Invitado.ESTADO_CONFIRMADO) && actual.equals(Invitado.ESTADO_RECHAZADO)) {
            // Al rechazar liberó su cupo: para volver a confirmar debe haber espacio para él y sus acompañantes.
            Lugar lugar = buscarLugar(invitacion.getIdLugar());
            int necesarios = 1 + repositoryInvitado.contarAcompanantes(idInvitado);
            int ocupados = repositoryInvitado.contarAsistentes(invitacion.getIdEvento(), lugar.getIdLugar());
            if (ocupados + necesarios > lugar.getCapacidad()) {
                throw new IllegalArgumentException("Ya no hay cupo disponible en " + lugar.getNombre()
                        + " para confirmar tu asistencia.");
            }
        }
        repositoryInvitado.actualizarEstado(idInvitado, nueva);
        actualizarEstadoInvitacion(invitacion);
    }

    /** La invitación queda ACEPTADA si alguien confirmó, RECHAZADA si todos rechazaron, y si no, ENVIADA. */
    private void actualizarEstadoInvitacion(Invitacion invitacion) {
        List<Invitado> invitados = repositoryInvitado.listarInvitados(invitacion.getIdInvitacion());
        boolean alguienConfirma = invitados.stream().anyMatch(i -> Invitado.ESTADO_CONFIRMADO.equalsIgnoreCase(i.getEstado()));
        boolean todosRechazan = !invitados.isEmpty()
                && invitados.stream().allMatch(i -> Invitado.ESTADO_RECHAZADO.equalsIgnoreCase(i.getEstado()));
        String estado = alguienConfirma ? Invitacion.ESTADO_ACEPTADA
                : todosRechazan ? Invitacion.ESTADO_RECHAZADA : Invitacion.ESTADO_ENVIADA;
        if (!estado.equals(invitacion.getEstadoRespuesta())) {
            invitacion.setEstadoRespuesta(estado);
            repositoryInvitacion.actualizarInvitacion(invitacion);
        }
    }

    // ------------------------------------------------------------------
    // CU-20: mesa de regalos (organizador)
    // ------------------------------------------------------------------
    /** Mesa de regalos de una invitación; solo la consulta el organizador dueño del evento. */
    public List<Regalo> listarRegalos(int idOrganizador, int idInvitacion) {
        validarEventoDelOrganizador(idOrganizador, eventoDeInvitacion(idInvitacion));
        return repositoryRegalo.listarRegalos(idInvitacion);
    }

    public void agregarRegalo(int idOrganizador, int idInvitacion, Regalo datos) {
        if (datos == null) throw new IllegalArgumentException("Completa los datos del regalo.");
        Invitacion invitacion = buscarInvitacionActiva(idInvitacion);
        validarEventoDelOrganizador(idOrganizador, invitacion.getIdEvento());
        validarEventoParaRegalos(invitacion.getIdEvento(), false);
        String nombre = limpiar(datos.getNombre());
        String descripcion = limpiar(datos.getDescripcion());
        if (nombre == null || nombre.length() > 150)
            throw new IllegalArgumentException("Ingresa el nombre del regalo (hasta 150 caracteres).");
        if (descripcion != null && descripcion.length() > 255)
            throw new IllegalArgumentException("La descripción del regalo debe tener hasta 255 caracteres.");
        BigDecimal precio = datos.getPrecioEstimado();
        if (precio == null || precio.signum() <= 0)
            throw new IllegalArgumentException("Ingresa un precio estimado mayor que cero.");
        // Se valida antes de redondear: 0.001 no puede guardarse como 0.00.
        if (precio.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("El precio estimado admite máximo dos decimales.");
        precio = precio.setScale(2, RoundingMode.UNNECESSARY);
        if (precio.precision() > 12)
            throw new IllegalArgumentException("El precio estimado es demasiado alto.");
        datos.setIdInvitacion(idInvitacion);
        datos.setNombre(nombre);
        datos.setDescripcion(descripcion);
        datos.setPrecioEstimado(precio);
        datos.setEstado(Regalo.ESTADO_DISPONIBLE);
        repositoryRegalo.guardarRegalo(datos);
    }

    /** Un regalo reservado no se puede quitar: el invitado ya lo eligió. */
    public void quitarRegalo(int idOrganizador, int idRegalo) {
        Regalo regalo = buscarRegalo(idRegalo);
        Invitacion invitacion = buscarInvitacionActiva(regalo.getIdInvitacion());
        validarEventoDelOrganizador(idOrganizador, invitacion.getIdEvento());
        validarEventoParaRegalos(invitacion.getIdEvento(), false);
        if (regalo.estaReservado() || repositoryReservaRegalo.contarReservasActivas(idRegalo) > 0)
            throw new IllegalArgumentException("\"" + regalo.getNombre() + "\" ya fue reservado por un invitado; no se puede quitar.");
        repositoryRegalo.eliminarRegalo(idRegalo);
    }

    // ------------------------------------------------------------------
    // CU-20: mesa de regalos (invitado)
    // ------------------------------------------------------------------
    /**
     * Mesa de regalos de la invitación del invitado. Cada regalo indica si está disponible,
     * si lo reservó otro invitado o si lo reservó él mismo. Igual que en CU-16, el invitado se
     * identifica con su correo y su teléfono: nadie ve ni reserva en la mesa de otra persona.
     */
    public List<Regalo> consultarRegalosDisponibles(int idInvitado, String correo, String telefono) {
        Invitado invitado = buscarInvitado(idInvitado);
        validarIdentidad(invitado, correo, telefono, MENSAJE_MESA_AJENA);
        Invitacion invitacion = buscarInvitacionActiva(invitado.getIdInvitacion());
        return repositoryRegalo.listarRegalos(invitacion.getIdInvitacion(), idInvitado);
    }

    /** R43: un regalo está disponible si nadie lo tiene reservado. */
    public boolean validarDisponibilidadRegalo(int idRegalo) {
        Regalo regalo = buscarRegalo(idRegalo);
        return !regalo.estaReservado() && repositoryReservaRegalo.contarReservasActivas(idRegalo) == 0;
    }

    public void reservarRegalo(int idInvitado, String correo, String telefono, int idRegalo) {
        Invitado invitado = buscarInvitado(idInvitado);
        validarIdentidad(invitado, correo, telefono, MENSAJE_MESA_AJENA);
        Invitacion invitacion = buscarInvitacionActiva(invitado.getIdInvitacion());
        validarEventoParaRegalos(invitacion.getIdEvento(), true);
        Regalo regalo = buscarRegalo(idRegalo);
        if (regalo.getIdInvitacion() != invitacion.getIdInvitacion())
            throw new IllegalArgumentException("El regalo no pertenece a la mesa de regalos de tu invitación.");
        if (regalo.estaReservadoPor(idInvitado))
            throw new IllegalArgumentException("Ya tienes reservado \"" + regalo.getNombre() + "\".");
        if (!validarDisponibilidadRegalo(idRegalo))
            throw new IllegalArgumentException("\"" + regalo.getNombre() + "\" ya fue reservado por otro invitado. Elige otro regalo.");
        ReservaRegalo reserva = new ReservaRegalo(0, invitacion.getIdEvento(), idInvitado, idRegalo,
                LocalDateTime.now(), ReservaRegalo.ESTADO_RESERVADO);
        if (!repositoryReservaRegalo.registrarReserva(reserva))
            throw new IllegalArgumentException("\"" + regalo.getNombre() + "\" ya fue reservado por otro invitado. Elige otro regalo.");
    }

    public void cancelarReservaRegalo(int idInvitado, String correo, String telefono, int idRegalo) {
        Invitado invitado = buscarInvitado(idInvitado);
        validarIdentidad(invitado, correo, telefono, MENSAJE_MESA_AJENA);
        Invitacion invitacion = buscarInvitacionActiva(invitado.getIdInvitacion());
        validarEventoParaRegalos(invitacion.getIdEvento(), true);
        Regalo regalo = buscarRegalo(idRegalo);
        ReservaRegalo reserva = repositoryReservaRegalo.buscarReservaActiva(idRegalo, idInvitado);
        if (reserva == null)
            throw new IllegalArgumentException("No tienes una reserva activa de \"" + regalo.getNombre() + "\".");
        repositoryReservaRegalo.cancelarReserva(reserva);
    }

    // ------------------------------------------------------------------
    // Validaciones auxiliares
    // ------------------------------------------------------------------
    /** La mesa no se modifica en eventos cancelados o finalizados; el invitado tampoco reserva si ya empezó. */
    private void validarEventoParaRegalos(int eventoId, boolean comoInvitado) {
        Evento evento = repositoryEvento.listarEventos().stream()
                .filter(e -> e.getIdEvento() != null && e.getIdEvento() == eventoId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El evento no está registrado."));
        String estado = evento.getEstado() == null ? "" : evento.getEstado().toUpperCase();
        if (estado.equals("CANCELADO") || estado.equals("FINALIZADO"))
            throw new IllegalArgumentException("El evento está " + estado.toLowerCase() + "; su mesa de regalos ya no se puede modificar.");
        if (comoInvitado && evento.getFechaInicio() != null && !evento.getFechaInicio().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("El evento ya comenzó; la mesa de regalos está cerrada.");
    }

    private Invitado buscarInvitado(int idInvitado) {
        Invitado invitado = repositoryInvitado.buscarInvitado(idInvitado);
        if (invitado == null) throw new IllegalArgumentException("El invitado no existe.");
        return invitado;
    }

    private Regalo buscarRegalo(int idRegalo) {
        Regalo regalo = repositoryRegalo.buscarRegalo(idRegalo);
        if (regalo == null) throw new IllegalArgumentException("El regalo no existe.");
        return regalo;
    }

    /** Solo el destinatario (su correo y su teléfono registrado) puede responder la invitación. */
    private void validarIdentidad(Invitado invitado, String correo, String telefono) {
        validarIdentidad(invitado, correo, telefono, "Solo el invitado puede responder su invitación. "
                + "Verifica tu correo y tu teléfono.");
    }

    private void validarIdentidad(Invitado invitado, String correo, String telefono, String mensaje) {
        String limpio = limpiar(correo);
        boolean mismoCorreo = limpio != null && invitado.getCorreo() != null
                && invitado.getCorreo().trim().equalsIgnoreCase(limpio);
        if (!mismoCorreo || !mismoTelefono(invitado.getTelefono(), telefono))
            throw new IllegalArgumentException(mensaje);
    }

    /** Compara solo los dígitos, para aceptar "300 555 4444" o "300-555-4444". */
    private static boolean mismoTelefono(String registrado, String escrito) {
        String a = soloDigitos(registrado);
        return !a.isEmpty() && a.equals(soloDigitos(escrito));
    }

    private static String soloDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    private Evento buscarEventoAbierto(int eventoId) {
        Evento evento = buscarEvento(eventoId);
        if (estaCerrado(evento)) {
            throw new IllegalArgumentException("No se pueden enviar invitaciones a un evento "
                    + evento.getEstado().trim().toLowerCase() + ".");
        }
        return evento;
    }

    /** En un evento cancelado o finalizado ya no se modifican invitaciones ni invitados. */
    private void validarEventoModificable(int eventoId) {
        Evento evento = buscarEvento(eventoId);
        if (estaCerrado(evento)) {
            throw new IllegalArgumentException("El evento está " + evento.getEstado().trim().toLowerCase()
                    + "; ya no se pueden modificar sus invitaciones ni sus invitados.");
        }
    }

    private Evento buscarEvento(int eventoId) {
        return repositoryEvento.listarEventos().stream()
                .filter(e -> e.getIdEvento() != null && e.getIdEvento() == eventoId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un evento registrado."));
    }

    private static boolean estaCerrado(Evento evento) {
        String estado = evento.getEstado() == null ? "" : evento.getEstado().trim().toUpperCase();
        return estado.equals("CANCELADO") || estado.equals("FINALIZADO");
    }

    private Invitacion buscarInvitacionActiva(int idInvitacion) {
        Invitacion invitacion = repositoryInvitacion.buscarInvitacion(idInvitacion);
        if (invitacion == null) throw new IllegalArgumentException("La invitación no existe.");
        if (!invitacion.estaActiva()) throw new IllegalArgumentException("La invitación #" + idInvitacion + " fue revocada.");
        return invitacion;
    }

    private Lugar buscarLugar(int idLugar) {
        Lugar lugar = repositoryLugar.buscarLugar(idLugar);
        if (lugar == null) throw new IllegalArgumentException("Selecciona un lugar registrado.");
        return lugar;
    }

    private void validarMensaje(String mensaje) {
        if (mensaje == null || mensaje.isBlank() || mensaje.trim().length() > 500)
            throw new IllegalArgumentException("Escribe el mensaje de la invitación (hasta 500 caracteres).");
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
