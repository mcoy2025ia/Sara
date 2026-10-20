
package services;

import entities.Contratacion;
import entities.Organizador;
import repositories.RepositoryContratacion;
import repositories.RepositoryOrganizador;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Servicio de administracion.
 * Implementa la logica de negocio del caso de uso CU-026.
 */
public class ServiceContratacionOrganizador {

    private final RepositoryContratacion repositoryContratacion;
    private final RepositoryOrganizador repositoryOrganizador;

    public ServiceContratacionOrganizador(
            RepositoryContratacion repositoryContratacion,
            RepositoryOrganizador repositoryOrganizador) {

        if (repositoryContratacion == null
                || repositoryOrganizador == null) {
            throw new IllegalArgumentException(
                    "Los repositorios de administracion son obligatorios.");
        }

        this.repositoryContratacion = repositoryContratacion;
        this.repositoryOrganizador = repositoryOrganizador;
    }

    /**
     * Consulta organizadores activos y verificados que coincidan
     * con la ciudad, el tipo de evento, el presupuesto y la fecha.
     *
     * Filtros requeridos:
     * ciudad, tipoEvento, presupuestoEstimado y fechaAproximada.
     */
    public List<Organizador> consultarOrganizadores(
            Map<String, Object> filtros) {

        validarFiltrosBusqueda(filtros);

        return repositoryOrganizador.consultarOrganizadoresDisponibles(
                filtros);
    }

    /**
     * Recupera el perfil seleccionado y comprueba que el
     * organizador continue activo y verificado.
     */
    public Organizador seleccionarOrganizador(int idOrganizador) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un organizador valido.");
        }

        if (!repositoryOrganizador.organizadorActivoVerificado(
                idOrganizador)) {
            throw new IllegalStateException(
                    "El organizador no esta activo o no se encuentra verificado.");
        }

        Organizador organizador =
                repositoryOrganizador.buscarOrganizador(idOrganizador);

        if (organizador == null) {
            throw new IllegalStateException(
                    "No se encontro el perfil del organizador seleccionado.");
        }

        return organizador;
    }

    /**
     * Comprueba la disponibilidad del organizador para la fecha
     * aproximada del evento.
     */
    public boolean verificarDisponibilidadOrganizador(
            int idOrganizador, LocalDate fecha) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "El organizador seleccionado no es valido.");
        }

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "Ingresa la fecha aproximada del evento.");
        }

        return repositoryOrganizador
                .verificarDisponibilidadOrganizador(idOrganizador, fecha);
    }

    /**
     * Registra la solicitud de contratacion.
     *
     * La solicitud queda PENDIENTE y el repositorio se encarga
     * de guardarla y notificar internamente al organizador.
     */
    public void contratarOrganizador(
            int idOrganizador, Contratacion datos) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un organizador valido.");
        }

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos de la solicitud son obligatorios.");
        }

        // Verificar el estado actual antes de continuar.
        seleccionarOrganizador(idOrganizador);

        // La disponibilidad se comprueba nuevamente al enviar.
        if (!verificarDisponibilidadOrganizador(
                idOrganizador, datos.getFechaAproximada())) {
            throw new IllegalStateException(
                    "El organizador seleccionado no esta disponible "
                    + "para la fecha indicada. Busca otra opcion.");
        }

        datos.setIdOrganizador(idOrganizador);
        datos.setEstado("PENDIENTE");

        repositoryContratacion.guardarContratacion(datos);
    }

    /**
     * Consulta las solicitudes de contratacion mediante filtros.
     */
    public List<Contratacion> consultarContrataciones(
            Map<String, Object> filtros) {

        return repositoryContratacion.listarContrataciones(filtros);
    }

    private void validarFiltrosBusqueda(
            Map<String, Object> filtros) {

        if (filtros == null) {
            throw new IllegalArgumentException(
                    "Ingresa los criterios de busqueda.");
        }

        Object ciudad = filtros.get("ciudad");
        Object tipoEvento = filtros.get("tipoEvento");
        Object presupuesto = filtros.get("presupuestoEstimado");
        Object fecha = filtros.get("fechaAproximada");

        if (ciudad == null || ciudad.toString().isBlank()
                || tipoEvento == null || tipoEvento.toString().isBlank()
                || presupuesto == null || fecha == null) {

            throw new IllegalArgumentException(
                    "Completa el tipo de evento, la ciudad, "
                    + "la fecha aproximada y el presupuesto.");
        }

        if (!(presupuesto instanceof Number)) {
            throw new IllegalArgumentException(
                    "El presupuesto debe ser numerico.");
        }

        BigDecimal valorPresupuesto =
                new BigDecimal(presupuesto.toString());

        if (valorPresupuesto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El presupuesto no puede ser negativo.");
        }

        if (!(fecha instanceof LocalDate)) {
            throw new IllegalArgumentException(
                    "La fecha aproximada no tiene un formato valido.");
        }

        if (ciudad.toString().trim().length() > 150
                || tipoEvento.toString().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "La ciudad o el tipo de evento son demasiado largos.");
        }
    }
}
