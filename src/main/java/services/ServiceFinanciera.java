package services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import entities.CompromisoFinanciero;
import entities.Evento;
import entities.MovimientoFinanciero;
import entities.Planificacion;
import entities.SesionUsuario;
import repositories.RepositoryEvento;
import repositories.RepositoryMovimientoFinanciero;
import repositories.RepositoryPlanificacion;

/**
 * CU06: consultar el estado financiero de un evento del organizador en sesión.
 *
 * Regla del presupuesto disponible (también la usa CU13 al contratar servicios):
 *
 *   disponible = presupuesto del evento (Evento.presupuesto)
 *              − Σ valor de las contrataciones del evento que NO están CANCELADA
 *
 * - El valor de cada contratación es su valor acordado; si no lo tiene, el de la propuesta aceptada.
 * - Una contratación FINALIZADA sigue contando: ya se pagó.
 * - Una contratación sin ningún valor no se suma; se informa en "Contrataciones_Sin_Valor"
 *   para que el organizador lo corrija (en ese caso el disponible real puede ser menor).
 * - Todos los importes se calculan con BigDecimal.
 */
public class ServiceFinanciera {

    private final RepositoryMovimientoFinanciero repoMovimientos;
    private final RepositoryPlanificacion repoPlanificacion;
    private final RepositoryEvento repoEventos;

    public ServiceFinanciera(RepositoryMovimientoFinanciero repoMovimientos,
                             RepositoryPlanificacion repoPlanificacion,
                             RepositoryEvento repoEventos) {
        this.repoMovimientos = repoMovimientos;
        this.repoPlanificacion = repoPlanificacion;
        this.repoEventos = repoEventos;
    }

    /** Estado financiero completo para la pantalla del CU06. */
    public Map<String, Object> consultarEstadoFinanciero(int idEvento, SesionUsuario sesion) {
        Evento evento = buscarEventoDelOrganizador(idEvento, sesion);

        List<MovimientoFinanciero> movimientos;
        Planificacion planificacion;
        List<CompromisoFinanciero> compromisos;
        try {
            movimientos = repoMovimientos.listarMovimientosPorEvento(idEvento);
            planificacion = repoPlanificacion.buscarPlanificacionPorEvento(idEvento);
            compromisos = repoMovimientos.listarCompromisosPorEvento(idEvento);
        } catch (RuntimeException e) {
            throw new IllegalStateException("No se pudo consultar el estado financiero. Vuelve a intentarlo.", e);
        }

        return calcularEstado(evento.getPresupuesto(), movimientos, planificacion, compromisos);
    }

    /**
     * Presupuesto disponible del evento según la regla documentada en la clase.
     * Devuelve null si el evento no tiene presupuesto registrado.
     */
    public BigDecimal consultarPresupuestoDisponible(int idEvento, SesionUsuario sesion) {
        Evento evento = buscarEventoDelOrganizador(idEvento, sesion);
        if (evento.getPresupuesto() == null) {
            return null;
        }
        try {
            return evento.getPresupuesto().subtract(sumarComprometido(repoMovimientos.listarCompromisosPorEvento(idEvento)));
        } catch (RuntimeException e) {
            throw new IllegalStateException("No se pudo consultar el presupuesto disponible. Vuelve a intentarlo.", e);
        }
    }

    private Evento buscarEventoDelOrganizador(int idEvento, SesionUsuario sesion) {
        int organizador = validarSesion(sesion);
        if (idEvento <= 0) {
            throw new IllegalArgumentException("Selecciona uno de tus eventos para consultar sus finanzas.");
        }
        // Solo se consultan eventos del organizador en sesión.
        Evento evento = repoEventos.buscarEvento(idEvento, organizador);
        if (evento == null) {
            throw new IllegalArgumentException("Selecciona un evento existente de tu cuenta.");
        }
        return evento;
    }

    private int validarSesion(SesionUsuario sesion) {
        if (sesion == null
                || !"ORGANIZADOR".equalsIgnoreCase(sesion.getTipoUsuario())
                || !repoEventos.organizadorActivo(sesion.getIdUsuario(), sesion.getIdActor())) {
            throw new IllegalStateException("Inicia sesión como organizador activo para consultar tus finanzas.");
        }
        return sesion.getIdActor();
    }

    private static BigDecimal sumarComprometido(List<CompromisoFinanciero> compromisos) {
        BigDecimal total = BigDecimal.ZERO;
        for (CompromisoFinanciero compromiso : compromisos) {
            if (compromiso.tieneValor()) {
                total = total.add(compromiso.getValor());
            }
        }
        return total;
    }

    private static Map<String, Object> calcularEstado(BigDecimal presupuestoEvento,
                                                      List<MovimientoFinanciero> movimientos,
                                                      Planificacion planificacion,
                                                      List<CompromisoFinanciero> compromisos) {
        // Balance de movimientos, tolerando mayúsculas y minúsculas en el tipo.
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal egresos = BigDecimal.ZERO;
        for (MovimientoFinanciero mov : movimientos) {
            if (mov.getTipo() == null || mov.getValor() == null) {
                continue;
            }
            String tipo = mov.getTipo().trim().toUpperCase();
            if ("INGRESO".equals(tipo)) {
                ingresos = ingresos.add(mov.getValor());
            } else if ("EGRESO".equals(tipo)) {
                egresos = egresos.add(mov.getValor());
            }
        }

        boolean hayPresupuesto = presupuestoEvento != null;
        BigDecimal presupuesto = hayPresupuesto ? presupuestoEvento : BigDecimal.ZERO;
        BigDecimal comprometido = sumarComprometido(compromisos);
        BigDecimal disponible = presupuesto.subtract(comprometido);
        int sinValor = 0;
        for (CompromisoFinanciero compromiso : compromisos) {
            if (!compromiso.tieneValor()) {
                sinValor++;
            }
        }
        // Proporción usada (0.0 – 1.0+) solo para mostrar la barra; no se usa en cálculos.
        double porcentajeUsado = presupuesto.signum() > 0
                ? comprometido.divide(presupuesto, 4, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        Map<String, Object> estado = new HashMap<>();
        estado.put("Ingresos", ingresos);
        estado.put("Egresos", egresos);
        estado.put("Balance", ingresos.subtract(egresos));

        estado.put("Presupuesto_Registrado", hayPresupuesto);
        estado.put("Presupuesto", presupuesto);
        estado.put("Comprometido", comprometido);
        estado.put("Disponible", disponible);
        estado.put("Porcentaje_Usado", porcentajeUsado);
        estado.put("Excede_Presupuesto", hayPresupuesto && comprometido.compareTo(presupuesto) > 0);
        estado.put("Contrataciones_Sin_Valor", sinValor);

        if (planificacion != null) {
            estado.put("Planificacion_Descripcion", planificacion.getDescripcion());
            estado.put("Planificacion_Version", planificacion.getVersion());
            estado.put("Planificacion_Estado", planificacion.getEstado());
        } else {
            estado.put("Planificacion_Descripcion", "No hay planificación registrada para este evento.");
            estado.put("Planificacion_Estado", "Desconocido");
        }

        estado.put("Movimientos", movimientos);
        estado.put("Compromisos", compromisos);
        return estado;
    }
}
