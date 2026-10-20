
package services;

import entities.BitacoraOrganizador;
import entities.DocumentoOrganizador;
import entities.EvaluacionOrganizador;
import entities.MensajeOrganizador;
import entities.Organizador;
import repositories.RepositoryOrganizador;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servicio de administracion para CU-030.
 *
 * Coordina la consulta de organizadores, la evaluacion del perfil,
 * la revision de documentos, la categoria y la decision administrativa.
 */
public class ServiceVerificacionOrganizador {

    private final RepositoryOrganizador repositoryOrganizador;
    private final int idAdministrador;

    public ServiceVerificacionOrganizador(
            RepositoryOrganizador repositoryOrganizador) {
        this(repositoryOrganizador, 0);
    }

    public ServiceVerificacionOrganizador(
            RepositoryOrganizador repositoryOrganizador,
            int idAdministrador) {

        if (repositoryOrganizador == null) {
            throw new IllegalArgumentException(
                    "El repositorio de organizadores es obligatorio.");
        }

        this.repositoryOrganizador = repositoryOrganizador;
        this.idAdministrador = idAdministrador;
    }

    // ---------------------------------------------------------
    // CONSULTAS
    // ---------------------------------------------------------

    /**
     * Consulta los organizadores pendientes de verificacion.
     */
    public List<Organizador> consultarOrganizadoresPendientes() {
        return repositoryOrganizador.listarOrganizadoresPendientes();
    }

    /**
     * Recupera el detalle del organizador seleccionado.
     */
    public Map<String, Object> consultarDetalleOrganizador(
            int idOrganizador) {

        validarIdOrganizador(idOrganizador);

        Map<String, Object> detalle =
                repositoryOrganizador.obtenerDetalleOrganizador(
                        idOrganizador);

        if (detalle == null) {
            throw new IllegalArgumentException(
                    "No se encontro el organizador seleccionado.");
        }

        return detalle;
    }

    /**
     * Consulta los documentos presentados por el organizador.
     */
    public List<DocumentoOrganizador> consultarDocumentosOrganizador(
            int idOrganizador) {

        validarIdOrganizador(idOrganizador);

        return repositoryOrganizador.listarDocumentosPorOrganizador(
                idOrganizador);
    }

    /**
     * Consulta el historial del canal interno.
     */
    public List<MensajeOrganizador> consultarMensajes(
            int idOrganizador) {

        validarIdOrganizador(idOrganizador);

        return repositoryOrganizador.listarMensajes(idOrganizador);
    }

    /**
     * Comprueba posibles duplicidades del documento de identidad
     * frente a las cuentas de Cliente y Proveedor.
     *
     * Devuelve true cuando se detecta una duplicidad.
     */
    public boolean verificarIdentidadDuplicada(
            int idOrganizador) {

        validarIdOrganizador(idOrganizador);

        return repositoryOrganizador.verificarIdentidadDuplicada(
                idOrganizador);
    }

    // ---------------------------------------------------------
    // VERIFICACION DE INFORMACION
    // ---------------------------------------------------------

    /**
     * Verifica que el perfil persistido tenga sus datos esenciales.
     */
    public boolean verificarInformacionOrganizador(
            int idOrganizador) {

        Map<String, Object> datos =
                consultarDetalleOrganizador(idOrganizador);

        return campoCompleto(datos, "nombre")
                && campoCompleto(datos, "documentoIdentidad")
                && campoCompleto(datos, "empresa")
                && campoCompleto(datos, "telefono")
                && campoCompleto(datos, "especialidad")
                && campoCompleto(datos, "portafolio")
                && numeroPositivo(datos.get("experienciaAnios"))
                && decimalPositivo(datos.get("tarifaBase"))
                && decimalPositivo(datos.get("presupuestoMaximo"));
    }

    /**
     * Variante compatible con la firma del diagrama de clases.
     * Comprueba que el objeto corresponda al organizador solicitado
     * y que la informacion persistida este completa.
     */
    public boolean verificarInformacionOrganizador(
            int idOrganizador, Organizador datos) {

        return datos != null
                && datos.id() == idOrganizador
                && datos.nombre() != null
                && !datos.nombre().isBlank()
                && verificarInformacionOrganizador(idOrganizador);
    }

    /**
     * Verifica que existan documentos, que hayan sido validados
     * y que ninguno este vencido.
     */
    public boolean verificarDocumentacionOrganizador(
            int idOrganizador,
            Map<String, Object> archivos) {

        validarIdOrganizador(idOrganizador);

        List<DocumentoOrganizador> documentos =
                consultarDocumentosOrganizador(idOrganizador);

        if (documentos.isEmpty()) {
            return false;
        }

        // Si la vista entrega archivos adicionales, deben estar
        // identificados antes de continuar con la verificacion.
        if (archivos != null) {
            for (Map.Entry<String, Object> archivo
                    : archivos.entrySet()) {

                if (archivo.getKey() == null
                        || archivo.getKey().isBlank()
                        || archivo.getValue() == null) {
                    return false;
                }
            }
        }

        return documentos.stream()
                .allMatch(DocumentoOrganizador::estaValidado);
    }

    /**
     * Comprueba si el perfil cumple los requisitos basicos
     * para continuar a una posible aprobacion.
     */
    public boolean verificarPerfilParaAprobacion(
            int idOrganizador) {

        return verificarInformacionOrganizador(idOrganizador)
                && verificarDocumentacionOrganizador(
                        idOrganizador, null)
                && !verificarIdentidadDuplicada(idOrganizador);
    }

    // ---------------------------------------------------------
    // EVALUACION DEL ORGANIZADOR
    // ---------------------------------------------------------

    /**
     * Calcula el puntaje y propone una categoria Junior o Senior.
     *
     * Los pesos y el umbral se consultan en la configuracion
     * persistida. Los valores iniciales de la migracion son
     * configurables y deben confirmarse con el equipo del proyecto.
     */
    public EvaluacionOrganizador evaluarOrganizador(
            int idOrganizador) {

        validarIdOrganizador(idOrganizador);
        exigirAdministrador();

        Map<String, Object> datos =
                consultarDetalleOrganizador(idOrganizador);

        Map<String, Object> configuracion =
                repositoryOrganizador.obtenerConfiguracionEvaluacion();

        List<DocumentoOrganizador> documentos =
                consultarDocumentosOrganizador(idOrganizador);

        int puntajeDocumentos =
                calcularPuntajeDocumentos(documentos);

        int puntajePortafolio =
                calcularPuntajePortafolio(datos);

        int puntajeExperiencia =
                calcularPuntajeExperiencia(datos);

        int puntajePerfil =
                calcularPuntajePerfil(datos);

        int pesoDocumentos =
                enteroConfiguracion(configuracion, "pesoDocumentos");
        int pesoPortafolio =
                enteroConfiguracion(configuracion, "pesoPortafolio");
        int pesoExperiencia =
                enteroConfiguracion(configuracion, "pesoExperiencia");
        int pesoPerfil =
                enteroConfiguracion(configuracion, "pesoPerfil");

        int puntaje = Math.round(
                (puntajeDocumentos * pesoDocumentos
                + puntajePortafolio * pesoPortafolio
                + puntajeExperiencia * pesoExperiencia
                + puntajePerfil * pesoPerfil) / 100.0f);

        int umbralSenior =
                enteroConfiguracion(
                        configuracion, "puntajeMinimoSenior");

        String categoria = puntaje >= umbralSenior
                ? EvaluacionOrganizador.CATEGORIA_SENIOR
                : EvaluacionOrganizador.CATEGORIA_JUNIOR;

        Object versionObj = datos.get("versionDatos");

        int version = versionObj instanceof Number
                ? ((Number) versionObj).intValue()
                : 0;

        EvaluacionOrganizador evaluacion =
                new EvaluacionOrganizador(
                        idOrganizador,
                        idAdministrador,
                        puntaje,
                        categoria,
                        version);

        BigDecimal presupuestoMaximo =
                decimalConfiguracion(
                        datos.get("presupuestoMaximo"));

        BigDecimal limitePresupuesto =
                decimalConfiguracion(
                        configuracion.get(
                                "presupuestoMaximoSinRevision"));

        boolean requiereRevision =
                presupuestoMaximo != null
                && limitePresupuesto != null
                && presupuestoMaximo.compareTo(limitePresupuesto) > 0;

        evaluacion.setRequiereRevisionPresupuesto(
                requiereRevision);

        evaluacion.setPresupuestoValidado(!requiereRevision);

        return evaluacion;
    }

    /**
     * Calcula el puntaje documental según la proporcion de
     * documentos validados y no vencidos.
     *
     * Un perfil sin documentos obtiene cero en este componente.
     */
    private int calcularPuntajeDocumentos(
            List<DocumentoOrganizador> documentos) {

        if (documentos == null || documentos.isEmpty()) {
            return 0;
        }

        long validados = documentos.stream()
                .filter(DocumentoOrganizador::estaValidado)
                .count();

        return (int) Math.round(
                validados * 100.0 / documentos.size());
    }

    /**
     * Puntaje inicial de portafolio.
     * Se usa un criterio sencillo mientras el equipo define
     * criterios de calidad mas especificos.
     */
    private int calcularPuntajePortafolio(
            Map<String, Object> datos) {

        Object valor = datos.get("portafolio");

        if (valor == null || valor.toString().isBlank()) {
            return 0;
        }

        int longitud = valor.toString().trim().length();

        if (longitud >= 80) {
            return 100;
        }

        if (longitud >= 30) {
            return 80;
        }

        return 50;
    }

    /**
     * Puntaje de experiencia: hasta diez años equivalen
     * a cien puntos en este componente.
     */
    private int calcularPuntajeExperiencia(
            Map<String, Object> datos) {

        Object experiencia = datos.get("experienciaAnios");

        if (!(experiencia instanceof Number)) {
            return 0;
        }

        int anios = ((Number) experiencia).intValue();

        if (anios <= 0) {
            return 0;
        }

        return Math.min(anios, 10) * 10;
    }

    /**
     * Puntaje de completitud del perfil profesional.
     */
    private int calcularPuntajePerfil(
            Map<String, Object> datos) {

        String[] campos = {
                "nombre",
                "documentoIdentidad",
                "empresa",
                "telefono",
                "especialidad",
                "tarifaBase",
                "presupuestoMaximo"
        };

        int completos = 0;

        for (String campo : campos) {
            Object valor = datos.get(campo);

            if (valor != null
                    && !valor.toString().isBlank()
                    && !"0".equals(valor.toString())) {
                completos++;
            }
        }

        return Math.round(
                completos * 100.0f / campos.length);
    }

    // ---------------------------------------------------------
    // CATEGORIA Y VALIDACION DE PRESUPUESTO
    // ---------------------------------------------------------

    /**
     * Permite ajustar la categoria calculada.
     * Cuando difiere de la sugerida, exige una justificacion.
     */
    public void categorizarOrganizador(
            EvaluacionOrganizador evaluacion,
            String categoria,
            String justificacion) {

        if (evaluacion == null) {
            throw new IllegalArgumentException(
                    "La evaluacion es obligatoria.");
        }

        if (!EvaluacionOrganizador.esCategoriaValida(categoria)) {
            throw new IllegalArgumentException(
                    "La categoria debe ser JUNIOR o SENIOR.");
        }

        boolean cambioManual =
                !categoria.equalsIgnoreCase(
                        evaluacion.getCategoriaSugerida());

        if (cambioManual
                && (justificacion == null
                || justificacion.isBlank())) {

            throw new IllegalArgumentException(
                    "Justifica el cambio manual de categoria.");
        }

        evaluacion.setCategoriaFinal(
                categoria.toUpperCase());

        evaluacion.setJustificacionCambio(
                cambioManual ? justificacion.trim() : null);
    }

    /**
     * Registra el resultado de la revision adicional de presupuesto.
     * Solo se permite aprobar si el presupuesto fue validado.
     */
    public void validarPresupuesto(
            EvaluacionOrganizador evaluacion,
            boolean cumpleCondiciones) {

        if (evaluacion == null) {
            throw new IllegalArgumentException(
                    "La evaluacion es obligatoria.");
        }

        if (!evaluacion.isRequiereRevisionPresupuesto()) {
            evaluacion.setPresupuestoValidado(true);
            return;
        }

        evaluacion.setPresupuestoValidado(
                cumpleCondiciones);

        if (!cumpleCondiciones) {
            throw new IllegalStateException(
                    "El presupuesto no cumple las condiciones. "
                    + "El organizador debe permanecer pendiente "
                    + "hasta completar la revision.");
        }
    }

    // ---------------------------------------------------------
    // APROBACION Y RECHAZO
    // ---------------------------------------------------------

    /**
     * Aprueba una evaluacion previamente calculada.
     * Bloquea la aprobacion cuando faltan datos, documentos,
     * existe duplicidad de identidad o falta validar presupuesto.
     */
    public void aprobarOrganizador(
            EvaluacionOrganizador evaluacion) {

        if (evaluacion == null) {
            throw new IllegalArgumentException(
                    "La evaluacion es obligatoria.");
        }

        exigirAdministrador();

        if (!verificarInformacionOrganizador(
                evaluacion.getIdOrganizador())) {

            throw new IllegalStateException(
                    "El perfil esta incompleto. Solicita las correcciones "
                    + "necesarias antes de aprobarlo.");
        }

        if (!verificarDocumentacionOrganizador(
                evaluacion.getIdOrganizador(), null)) {

            throw new IllegalStateException(
                    "Hay documentos ausentes, sin validar o vencidos.");
        }

        if (verificarIdentidadDuplicada(
                evaluacion.getIdOrganizador())) {

            throw new IllegalStateException(
                    "Existe una posible duplicidad de identidad. "
                    + "Revisa la informacion antes de aprobar.");
        }

        if (!evaluacion.puedeTomarDecisionFinal()
                || !evaluacion.isPresupuestoValidado()) {

            throw new IllegalStateException(
                    "Debes completar la validacion adicional del presupuesto.");
        }

        evaluacion.setDecision(
                EvaluacionOrganizador.DECISION_APROBADO);

        repositoryOrganizador.guardarEvaluacion(evaluacion);
    }

    /**
     * Variante compatible con el diagrama.
     */
    public void aprobarOrganizador(int idOrganizador) {
        EvaluacionOrganizador evaluacion =
                evaluarOrganizador(idOrganizador);

        aprobarOrganizador(evaluacion);
    }

    /**
     * Rechaza un organizador y registra el motivo.
     */
    public void rechazarOrganizador(
            EvaluacionOrganizador evaluacion,
            String motivo) {

        if (evaluacion == null) {
            throw new IllegalArgumentException(
                    "La evaluacion es obligatoria.");
        }

        exigirAdministrador();

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes indicar el motivo de rechazo.");
        }

        if (motivo.trim().length() > 500) {
            throw new IllegalArgumentException(
                    "El motivo de rechazo no puede superar 500 caracteres.");
        }

        evaluacion.setMotivoRechazo(motivo.trim());
        evaluacion.setDecision(
                EvaluacionOrganizador.DECISION_RECHAZADO);

        repositoryOrganizador.guardarEvaluacion(evaluacion);
    }

    /**
     * Variante compatible con el diagrama.
     */
    public void rechazarOrganizador(
            int idOrganizador, String motivo) {

        EvaluacionOrganizador evaluacion =
                evaluarOrganizador(idOrganizador);

        rechazarOrganizador(evaluacion, motivo);
    }

    // ---------------------------------------------------------
    // DOCUMENTOS Y MENSAJES
    // ---------------------------------------------------------

    public void guardarDocumentoOrganizador(
            DocumentoOrganizador documento) {

        exigirAdministrador();

        if (documento == null) {
            throw new IllegalArgumentException(
                    "El documento es obligatorio.");
        }

        repositoryOrganizador.guardarDocumento(documento);
    }

    public void actualizarEstadoDocumento(
            int idDocumento,
            String estado,
            String observacion) {

        exigirAdministrador();

        try {
            repositoryOrganizador.actualizarEstadoDocumento(
                    idDocumento, estado, observacion);

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo actualizar el estado del documento.", e);
        }
    }

    /**
     * Solicita informacion o correcciones mediante el canal interno.
     * El organizador permanece pendiente de verificacion.
     */
    public void solicitarCorrecciones(
            int idOrganizador, String mensaje) {

        exigirAdministrador();
        validarIdOrganizador(idOrganizador);

        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException(
                    "Describe la informacion o correccion solicitada.");
        }

        if (mensaje.trim().length() > 1000) {
            throw new IllegalArgumentException(
                    "El mensaje no puede superar 1000 caracteres.");
        }

        Map<String, Object> detalle =
                consultarDetalleOrganizador(idOrganizador);

        int idUsuarioOrganizador =
                ((Number) detalle.get("idUsuario")).intValue();

        MensajeOrganizador nuevoMensaje =
                new MensajeOrganizador(
                        idOrganizador,
                        idAdministrador,
                        idUsuarioOrganizador,
                        mensaje.trim(),
                        MensajeOrganizador.TIPO_SOLICITUD_CORRECCION);

        repositoryOrganizador.guardarMensaje(nuevoMensaje);
    }

    /**
     * Envia un mensaje general entre el administrador y el organizador.
     */
    public void enviarMensaje(
            int idOrganizador,
            int idRemitente,
            int idDestinatario,
            String contenido,
            String tipoMensaje) {

        if (idOrganizador <= 0
                || idRemitente <= 0
                || idDestinatario <= 0) {
            throw new IllegalArgumentException(
                    "Los participantes del mensaje no son validos.");
        }

        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException(
                    "El contenido del mensaje es obligatorio.");
        }

        MensajeOrganizador mensaje =
                new MensajeOrganizador(
                        idOrganizador,
                        idRemitente,
                        idDestinatario,
                        contenido.trim(),
                        tipoMensaje);

        if (!mensaje.esValido()) {
            throw new IllegalArgumentException(
                    "Los datos del mensaje no son validos.");
        }

        repositoryOrganizador.guardarMensaje(mensaje);
    }

    /**
     * El organizador responde al canal interno.
     */
    public void responderMensajeOrganizador(
            int idOrganizador, int idUsuarioOrganizador,
            String respuesta) {

        exigirUsuarioOrganizador(idOrganizador, idUsuarioOrganizador);

        if (respuesta == null || respuesta.isBlank()) {
            throw new IllegalArgumentException(
                    "La respuesta no puede estar vacia.");
        }

        if (respuesta.trim().length() > 1000) {
            throw new IllegalArgumentException(
                    "La respuesta no puede superar 1000 caracteres.");
        }

        MensajeOrganizador mensaje =
                new MensajeOrganizador(
                        idOrganizador,
                        idUsuarioOrganizador,
                        idAdministrador,
                        respuesta.trim(),
                        MensajeOrganizador.TIPO_RESPUESTA);

        repositoryOrganizador.guardarMensaje(mensaje);
    }

    private void exigirUsuarioOrganizador(
            int idOrganizador, int idUsuarioOrganizador) {

        Map<String, Object> detalle =
                consultarDetalleOrganizador(idOrganizador);

        int propietario =
                ((Number) detalle.get("idUsuario")).intValue();

        if (propietario != idUsuarioOrganizador) {
            throw new IllegalArgumentException(
                    "El usuario no corresponde al organizador seleccionado.");
        }
    }

    // ---------------------------------------------------------
    // UTILIDADES
    // ---------------------------------------------------------

    private void validarIdOrganizador(int idOrganizador) {
        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del organizador no es valido.");
        }
    }

    private void exigirAdministrador() {
        if (idAdministrador <= 0) {
            throw new IllegalStateException(
                    "Se necesita una sesion administrativa valida.");
        }
    }

    private boolean campoCompleto(
            Map<String, Object> datos, String campo) {

        Object valor = datos.get(campo);

        return valor != null
                && !valor.toString().isBlank();
    }

    private boolean numeroPositivo(Object valor) {
        return valor instanceof Number
                && ((Number) valor).intValue() > 0;
    }

    private boolean decimalPositivo(Object valor) {
        BigDecimal decimal = decimalConfiguracion(valor);

        return decimal != null
                && decimal.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal decimalConfiguracion(Object valor) {

        if (valor == null) {
            return null;
        }

        try {
            return valor instanceof BigDecimal
                    ? (BigDecimal) valor
                    : new BigDecimal(valor.toString());

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int enteroConfiguracion(
            Map<String, Object> configuracion, String clave) {

        Object valor = configuracion.get(clave);

        if (!(valor instanceof Number)) {
            throw new IllegalStateException(
                    "La configuracion de evaluacion no contiene "
                    + "un valor numerico valido para " + clave + ".");
        }

        return ((Number) valor).intValue();
    }
}
