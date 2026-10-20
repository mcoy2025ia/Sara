
package entities;

import java.time.LocalDateTime;

/**
 * Representa la evaluacion realizada a un organizador
 * durante el caso de uso CU-030.
 *
 * Registra el puntaje, la categoria propuesta, la decision
 * administrativa y la validacion adicional de presupuesto.
 */
public class EvaluacionOrganizador {

    public static final String CATEGORIA_JUNIOR = "JUNIOR";
    public static final String CATEGORIA_SENIOR = "SENIOR";

    public static final String DECISION_APROBADO = "APROBADO";
    public static final String DECISION_RECHAZADO = "RECHAZADO";
    public static final String DECISION_CORRECCION_SOLICITADA =
            "CORRECCION_SOLICITADA";
    public static final String DECISION_PENDIENTE_PRESUPUESTO =
            "PENDIENTE_PRESUPUESTO";

    private int idEvaluacion;
    private int idOrganizador;
    private int idAdministrador;

    private int puntaje;
    private String categoriaSugerida;
    private String categoriaFinal;
    private String justificacionCambio;

    private String decision;
    private String motivoRechazo;

    private boolean requiereRevisionPresupuesto;
    private boolean presupuestoValidado;

    private int versionDatosRevisada;
    private LocalDateTime fechaEvaluacion;

    public EvaluacionOrganizador() {
        this.categoriaSugerida = CATEGORIA_JUNIOR;
        this.decision = DECISION_PENDIENTE_PRESUPUESTO;
        this.requiereRevisionPresupuesto = false;
        this.presupuestoValidado = false;
    }

    public EvaluacionOrganizador(
            int idOrganizador,
            int idAdministrador,
            int puntaje,
            String categoriaSugerida,
            int versionDatosRevisada) {

        this.idOrganizador = idOrganizador;
        this.idAdministrador = idAdministrador;
        this.puntaje = puntaje;
        this.categoriaSugerida = categoriaSugerida;
        this.categoriaFinal = categoriaSugerida;
        this.versionDatosRevisada = versionDatosRevisada;

        this.decision = DECISION_PENDIENTE_PRESUPUESTO;
        this.requiereRevisionPresupuesto = false;
        this.presupuestoValidado = false;
    }

    public int getIdEvaluacion() {
        return idEvaluacion;
    }

    public void setIdEvaluacion(int idEvaluacion) {
        this.idEvaluacion = idEvaluacion;
    }

    public int getIdOrganizador() {
        return idOrganizador;
    }

    public void setIdOrganizador(int idOrganizador) {
        this.idOrganizador = idOrganizador;
    }

    public int getIdAdministrador() {
        return idAdministrador;
    }

    public void setIdAdministrador(int idAdministrador) {
        this.idAdministrador = idAdministrador;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(int puntaje) {
        this.puntaje = puntaje;
    }

    public String getCategoriaSugerida() {
        return categoriaSugerida;
    }

    public void setCategoriaSugerida(String categoriaSugerida) {
        this.categoriaSugerida = categoriaSugerida;
    }

    public String getCategoriaFinal() {
        return categoriaFinal;
    }

    public void setCategoriaFinal(String categoriaFinal) {
        this.categoriaFinal = categoriaFinal;
    }

    public String getJustificacionCambio() {
        return justificacionCambio;
    }

    public void setJustificacionCambio(String justificacionCambio) {
        this.justificacionCambio = justificacionCambio;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public boolean isRequiereRevisionPresupuesto() {
        return requiereRevisionPresupuesto;
    }

    public void setRequiereRevisionPresupuesto(
            boolean requiereRevisionPresupuesto) {
        this.requiereRevisionPresupuesto =
                requiereRevisionPresupuesto;
    }

    public boolean isPresupuestoValidado() {
        return presupuestoValidado;
    }

    public void setPresupuestoValidado(boolean presupuestoValidado) {
        this.presupuestoValidado = presupuestoValidado;
    }

    public int getVersionDatosRevisada() {
        return versionDatosRevisada;
    }

    public void setVersionDatosRevisada(
            int versionDatosRevisada) {
        this.versionDatosRevisada = versionDatosRevisada;
    }

    public LocalDateTime getFechaEvaluacion() {
        return fechaEvaluacion;
    }

    public void setFechaEvaluacion(
            LocalDateTime fechaEvaluacion) {
        this.fechaEvaluacion = fechaEvaluacion;
    }

    /**
     * Indica si se modifico manualmente la categoria sugerida.
     */
    public boolean tieneCambioManualDeCategoria() {
        return categoriaFinal != null
                && categoriaSugerida != null
                && !categoriaFinal.equalsIgnoreCase(categoriaSugerida);
    }

    /**
     * Indica si la evaluacion puede continuar a una decision final.
     */
    public boolean puedeTomarDecisionFinal() {
        return !requiereRevisionPresupuesto || presupuestoValidado;
    }

    /**
     * Comprueba que la categoria solicitada sea una de las permitidas.
     */
    public static boolean esCategoriaValida(String categoria) {
        return CATEGORIA_JUNIOR.equalsIgnoreCase(categoria)
                || CATEGORIA_SENIOR.equalsIgnoreCase(categoria);
    }

    /**
     * Comprueba si la decision corresponde a una aprobacion o rechazo.
     */
    public boolean tieneDecisionFinal() {
        return DECISION_APROBADO.equalsIgnoreCase(decision)
                || DECISION_RECHAZADO.equalsIgnoreCase(decision);
    }
}
