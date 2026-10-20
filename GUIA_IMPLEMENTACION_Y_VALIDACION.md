# Guía de implementación y validación — NextEventux

## 1. Propósito

Esta guía define el flujo de trabajo que seguirá el equipo durante la etapa de implementación y validación de NextEventux.

Los diagramas desarrollados hasta este momento representan una primera iteración del diseño del sistema. La implementación de los casos de uso permitirá comprobar si las clases, métodos, relaciones, responsabilidades y estructuras de persistencia propuestas son suficientes para representar el comportamiento definido en los requisitos.

Por lo tanto, en esta etapa no se busca únicamente escribir código.

La implementación también debe utilizarse para validar el diseño.

El proceso general será:

```text
Caso de uso
    ↓
Revisión del requisito
    ↓
Revisión del diseño actual
    ↓
Implementación
    ↓
Pruebas
    ↓
Análisis de resultados
    ↓
Identificación de hallazgos
    ↓
Corrección de código o ajuste justificado del diseño
    ↓
Nueva validación
    ↓
Integración
```

El diseño no debe modificarse automáticamente ante cualquier error.

Primero debe identificarse si el problema corresponde al código, configuración, persistencia, integración entre módulos, requisitos o al diseño propiamente dicho.

---

# 2. Alcance de esta etapa

La documentación del sistema continúa describiendo el sistema completo.

Sin embargo, la implementación comprometida actualmente se concentra en los casos de uso definidos en el Product Backlog.

Los casos de uso comprometidos actualmente son:

## Gestión de Eventos — Sara Coy

- CU01 — Crear evento
- CU03 — Subir evento a la plataforma
- CU05 — Finalizar evento

## Gestión Financiera — Valeria Salgado

- CU06 — Consultar estado financiero
- CU08 — Gestionar paquetes de servicio
- CU09 — Gestionar sanciones por incumplimiento

## Gestión de Proveedores — Isabella Hermosa

- CU11 — Gestionar servicio
- CU12 — Aceptar/Rechazar solicitud de ejecución
- CU13 — Contratar servicio

## Gestión de Invitados — Sofía Cortés

- CU17 — Gestionar invitaciones del evento
- CU20 — Interactuar con la mesa de regalos virtual

## Marketing — Saúl Cruz

- CU21 — Segmentar clientes automáticamente
- CU23 — Crear campaña de marketing

## Administración — Juan Diego Rojas

- CU28 — Aprobar proveedores
- CU30 — Verificar, categorizar y aprobar organizador

Los demás casos de uso pueden seguir apareciendo en los modelos y en la documentación general del sistema aunque no formen parte del compromiso actual de implementación.

No deben eliminarse del diseño únicamente porque no estén siendo programados en esta etapa.

---

# 3. Arquitectura utilizada

La organización inicial del código está basada en las siguientes responsabilidades:

```text
View
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
Entity
  ↓
Persistencia
```

La estructura inicial del proyecto es:

```text
src/
├── main/
│   ├── java/
│   │   ├── controllers/
│   │   ├── entities/
│   │   ├── repositories/
│   │   ├── services/
│   │   └── views/
│   └── resources/
└── test/
    └── java/
```

Esta estructura representa una primera organización del código y podrá ajustarse si la implementación demuestra que existe una necesidad real.

No se deben crear carpetas, capas o componentes únicamente por conveniencia o porque sean comunes en otros proyectos.

Todo elemento nuevo debe tener una responsabilidad identificable dentro de NextEventux.

---

# 4. Qué revisar antes de programar un caso de uso

Antes de comenzar la implementación, cada integrante debe revisar el caso de uso que va a desarrollar.

No se debe programar únicamente utilizando el nombre del CU.

Se deben consultar, cuando corresponda:

1. SRS.
2. Especificación detallada del caso de uso.
3. Diagramas de clases.
4. Diagramas de comportamiento.
5. Diseño de persistencia.
6. Interfaces relacionadas.
7. Dependencias con otros módulos.

Antes de programar se debe identificar:

## Actor

- ¿Quién inicia el caso de uso?
- ¿Existen otros actores involucrados?

## Precondiciones

- ¿Qué información debe existir?
- ¿Qué estado debe cumplirse antes de iniciar?

## Entradas

- ¿Qué información recibe el sistema?

## Flujo básico

- ¿Cuál es el comportamiento esperado cuando todo ocurre normalmente?

## Flujos alternativos

- ¿Qué otras decisiones válidas pueden ocurrir?

## Excepciones

- ¿Qué situaciones deben impedir o modificar la operación?

## Salidas

- ¿Qué debe mostrar o producir el sistema?

## Postcondiciones

- ¿Qué información debe quedar almacenada o modificada después de la ejecución?

Si un elemento necesario no se encuentra definido en las fuentes del proyecto, no debe inventarse silenciosamente.

Debe registrarse como duda o hallazgo.

---

# 5. Estrategia inicial de implementación

No se recomienda comenzar implementando simultáneamente todos los casos de uso asignados a un módulo.

Cada integrante comenzará validando un primer caso de uso.

Primera iteración seleccionada:

| Responsable | Caso de uso inicial |
|---|---|
| Sara Coy | CU01 — Crear evento |
| Valeria Salgado | CU06 — Consultar estado financiero |
| Isabella Hermosa | CU11 — Gestionar servicio |
| Sofía Cortés | CU17 — Gestionar invitaciones del evento |
| Saúl Cruz | CU21 — Segmentar clientes automáticamente |
| Juan Diego Rojas | CU28 — Aprobar proveedores |

El objetivo de esta primera implementación es recorrer un flujo suficientemente representativo para comprobar el diseño.

Completar una parte del flujo no significa automáticamente que el caso de uso completo se encuentre terminado.

---

# 6. Flujo de trabajo con Git

## 6.1. Rama principal

La rama:

```text
main
```

representa la versión estable del proyecto.

No se debe desarrollar directamente sobre `main`.

Antes de iniciar un nuevo trabajo:

```bash
git switch main
git pull
```

Después se crea una rama temporal para el requerimiento.

---

## 6.2. Ramas por requerimiento

Cada desarrollador debe trabajar en una rama separada asociada al requerimiento que está implementando.

La convención utilizada por el equipo será:

```text
CUXX-descripcion-corta
```

Ejemplos:

```text
CU01-crear-evento
CU06-consultar-estado-financiero
CU11-gestionar-servicio
CU17-gestionar-invitaciones
CU21-segmentar-clientes
CU28-aprobar-proveedores
```

Reglas:

- utilizar el código del caso de uso;
- utilizar una descripción corta y representativa;
- no utilizar espacios;
- crear la rama antes de comenzar a realizar cambios;
- una rama debe tener un objetivo identificable;
- evitar mezclar requerimientos diferentes dentro de la misma rama.

Ejemplo:

```bash
git switch main
git pull
git switch -c CU11-gestionar-servicio
```

Para tareas que no pertenecen directamente a un CU puede utilizarse un nombre descriptivo.

Ejemplo:

```text
documentar-guia-implementacion-validacion
```

---

## 6.3. Commits

Antes de realizar un commit debe revisarse qué archivos serán incluidos.

Comandos básicos:

```bash
git status
git add <archivo>
git commit -m "mensaje"
```

Los mensajes deben explicar qué se realizó.

Ejemplos:

```text
feat: crear entidad Servicio
feat: implementar creación de servicio
fix: validar datos obligatorios del servicio
docs: agregar guía de implementación y validación
```

No deben mezclarse cambios sin relación en un mismo commit cuando puedan separarse de forma razonable.

---

## 6.4. Actualización antes de integrar

Antes de integrar el trabajo se debe comprobar que la rama sigue siendo compatible con los cambios recientes del proyecto.

También debe verificarse:

```bash
git status
```

y ejecutar las pruebas relacionadas con la funcionalidad.

Si existen conflictos, deben resolverse antes de integrar.

No debe elegirse automáticamente una versión durante un conflicto sin entender qué cambio debe conservarse.

---

## 6.5. Pull Request

Cuando el trabajo esté listo:

1. realizar las pruebas;
2. revisar los cambios;
3. realizar los commits necesarios;
4. hacer push de la rama;
5. abrir un Pull Request;
6. solicitar revisión de otro integrante;
7. atender observaciones;
8. integrar el trabajo cuando se encuentre validado.

La revisión por Pull Request es el mecanismo utilizado por el equipo para revisar el trabajo antes de integrarlo a `main`.

La rama puede eliminarse después de que su trabajo haya sido integrado correctamente.

---

# 7. Qué significa validar un caso de uso

Un caso de uso no se considera validado únicamente porque:

- compile;
- no produzca errores;
- muestre una pantalla;
- ejecute el flujo principal una vez.

La validación debe comprobar si el diseño actual permite representar correctamente el comportamiento especificado.

Deben revisarse los siguientes elementos.

---

## 7.1. Flujo básico

Debe comprobarse que el camino principal definido en la especificación puede ejecutarse.

Ejemplo general:

```text
Usuario realiza acción
        ↓
Sistema valida información
        ↓
Sistema procesa operación
        ↓
Sistema almacena cambios
        ↓
Sistema muestra resultado
```

---

## 7.2. Flujos alternativos

Si el caso de uso define alternativas relevantes, también deben probarse.

Ejemplo:

```text
Si se cumple la condición:
    realizar operación A

Si no se cumple:
    realizar operación B
```

Probar únicamente A no valida completamente ese comportamiento.

---

## 7.3. Excepciones

Las situaciones que deben impedir una operación también deben comprobarse.

Ejemplo:

```text
Si faltan datos obligatorios:
    no guardar
    mostrar mensaje
```

La prueba debe verificar tanto que la operación no se realice como que el sistema responda de forma consistente.

---

# 8. Qué revisar del diseño durante la implementación

## 8.1. Clases

Preguntas:

- ¿existe la clase necesaria?
- ¿su responsabilidad es clara?
- ¿está en la capa adecuada?
- ¿está realizando tareas que deberían pertenecer a otra clase?

---

## 8.2. Métodos

Preguntas:

- ¿existe el método necesario?
- ¿recibe la información suficiente?
- ¿retorna lo que necesita el flujo?
- ¿pertenece realmente a esa clase?
- ¿la firma definida en el diagrama permite implementar el CU?

---

## 8.3. Relaciones

Preguntas:

- ¿las clases que necesitan comunicarse tienen una relación coherente?
- ¿falta alguna dependencia?
- ¿la cardinalidad definida representa lo que sucede realmente?
- ¿una relación existente resulta innecesaria?

---

## 8.4. Entidades

Preguntas:

- ¿existe la entidad necesaria?
- ¿contiene los datos requeridos?
- ¿permite representar los estados mencionados en el CU?
- ¿se encuentra relacionada con las entidades correctas?

---

## 8.5. Persistencia

Cuando el caso de uso requiera almacenar información, comprobar:

- si existe una estructura donde guardarla;
- si se puede recuperar posteriormente;
- si se pueden representar sus relaciones;
- si se conserva historial cuando el requisito lo exige;
- si se pueden representar los estados necesarios;
- si existe una identificación adecuada de los registros.

---

# 9. Cómo clasificar un problema

Cuando algo falle durante la implementación, primero debe clasificarse.

## A. Error de implementación

Ejemplos:

- condición incorrecta;
- variable mal utilizada;
- llamada incorrecta;
- lógica equivocada;
- objeto mal inicializado.

### Acción

Corregir código.

No modificar automáticamente los diagramas.

---

## B. Problema técnico o de configuración

Ejemplos:

- error de H2;
- librería no encontrada;
- configuración del entorno;
- problema de Java;
- configuración de JavaFX;
- ruta incorrecta.

### Acción

Corregir configuración.

No justificar un cambio del diseño con un problema de entorno.

---

## C. Problema de diseño

Ejemplos:

- falta una clase;
- falta un método;
- una responsabilidad está ubicada en una clase incorrecta;
- falta una relación;
- una firma de método no permite realizar la operación;
- la cardinalidad no representa el comportamiento;
- una clase necesita datos que el modelo no contempla.

### Acción

Registrar el hallazgo.

Proponer el cambio.

Actualizar código y documentación de manera coherente.

---

## D. Problema de persistencia

Ejemplos:

- falta guardar un dato requerido;
- falta una relación entre tablas;
- no puede mantenerse historial;
- falta representar un estado;
- no puede identificarse correctamente un registro.

### Acción

Registrar el hallazgo.

Revisar el modelo de persistencia y las entidades relacionadas.

---

## E. Dependencia entre módulos

Ejemplo:

```text
Proveedores necesita consultar información financiera.
```

Esto no significa que el módulo de Proveedores deba duplicar la lógica del módulo Financiero.

### Acción

Identificar qué módulo tiene la responsabilidad y coordinar la integración.

---

## F. Inconsistencia de requisitos

Ejemplo:

El caso de uso exige un comportamiento que contradice otro documento.

### Acción

No modificar silenciosamente el requisito.

Registrar:

- fuentes involucradas;
- contradicción encontrada;
- impacto;
- posible decisión requerida.

Si es necesario, consultar al profesor.

---

# 10. Cuándo modificar un diagrama

Un diagrama puede modificarse cuando la implementación proporcione evidencia de que el diseño actual no permite representar correctamente el requisito.

Ejemplos de evidencia válida:

- falta una clase necesaria;
- falta un método;
- falta un atributo;
- falta una relación;
- una responsabilidad pertenece a otra clase;
- una firma es insuficiente;
- una entidad no representa la información requerida;
- una relación de persistencia no permite guardar lo solicitado.

No debe modificarse un diagrama solamente porque:

- existe un error de sintaxis;
- un objeto fue inicializado incorrectamente;
- una condición quedó mal escrita;
- H2 no conecta;
- un import falló;
- una librería está mal configurada;
- una prueba fue escrita incorrectamente.

---

# 11. Documentos que pueden verse afectados

Según el tipo de hallazgo puede ser necesario actualizar distintas partes de la documentación.

## Estructura de clases

Si cambia:

- clase;
- método;
- atributo;
- asociación;
- responsabilidad;

revisar:

```text
SDD 3.2 — Estructura
```

Responsable de integración:

```text
Isabella Hermosa
```

---

## Comportamiento

Si cambia una interacción relevante entre componentes o la secuencia necesaria para ejecutar una funcionalidad, revisar:

```text
SDD 3.3 — Comportamiento
```

Responsable de integración:

```text
Saúl Cruz
```

---

## Persistencia

Si cambia:

- tabla;
- relación;
- atributo persistente;
- identificación;
- estado almacenado;

revisar:

```text
SDD 3.4 — Persistencia
```

Responsable de integración:

```text
Sofía Cortés
```

---

## Interfaces

Si el hallazgo afecta:

- navegación;
- pantalla;
- interacción;
- entrada o salida visible;

revisar:

```text
SDD 3.1 — Diseño de interfaz
```

La revisión debe coordinarse con los responsables de interfaz.

---

## Requisitos

Si la implementación revela una contradicción o necesidad de modificar un requisito:

No modificar directamente el SRS como si fuera un error de código.

La modificación debe registrarse y validarse según el proceso de control de cambios definido por el proyecto.

---

# 12. Dependencias entre módulos

Los módulos no deben convertirse en sistemas completamente aislados.

Cuando un caso de uso necesite una responsabilidad perteneciente a otro módulo, debe registrarse como dependencia.

Ejemplo:

```text
Proveedor
    ↓
Propuesta
    ↓
Contratación
    ↓
Información financiera
    ↓
Evento
```

No se deben duplicar responsabilidades para evitar coordinar con otro integrante.

Cuando aparezca una dependencia:

1. identificar el módulo responsable;
2. informar al integrante correspondiente;
3. acordar qué información se necesita;
4. definir la interacción;
5. registrar cualquier cambio de diseño necesario.

---

# 13. Repositorios experimentales

El repositorio oficial contiene la implementación que formará parte de NextEventux.

Los repositorios experimentales pueden utilizarse únicamente cuando exista una prueba técnica aislada.

Ejemplo existente:

```text
prueba-h2
```

Su propósito fue validar técnicamente:

- conexión Java con H2;
- creación de base de datos;
- creación de tablas;
- inserción;
- consulta;
- persistencia entre ejecuciones.

No es necesario crear un repositorio separado para cada integrante o módulo.

El trabajo funcional debe desarrollarse en ramas del repositorio oficial.

---

# 14. Pruebas

Cada primera iteración debe incluir pruebas suficientes para conocer si el flujo implementado cumple el comportamiento esperado.

Las pruebas pueden incluir:

- flujo principal;
- entradas válidas;
- entradas inválidas;
- condiciones límite;
- alternativas;
- excepciones;
- persistencia;
- interacción con otros componentes.

El proyecto contempla el uso de JUnit para pruebas automatizadas.

Sin embargo, mientras no exista una configuración común de dependencias/build dentro del repositorio oficial, no se debe inventar un comando Maven o Gradle que el equipo todavía no haya definido.

Las primeras validaciones pueden apoyarse también en ejecución controlada y evidencia manual cuando sea necesario.

---

# 15. Reporte de validación

Después de implementar una primera iteración, cada integrante debe poder registrar:

```text
Caso de uso:
Responsable:
Módulo:

Rama:

Flujo implementado:

Escenarios probados:

Resultado esperado:

Resultado obtenido:

Clases utilizadas:

Métodos utilizados:

Entidades utilizadas:

Dependencias con otros módulos:

¿El diseño permitió implementar el flujo?
Sí / Parcialmente / No

Problemas encontrados:

Clasificación:
- Implementación
- Configuración
- Diseño
- Persistencia
- Dependencia
- Requisito

¿Requiere modificar diagramas?
Sí / No

Cambio propuesto:

Documentos afectados:

Pruebas realizadas:

Evidencia:

Bloqueantes:

Siguiente paso:
```

Si no se encontraron problemas de diseño puede indicarse:

```text
No se identificaron cambios estructurales necesarios durante esta validación.
```

---

# 16. Sprint Backlog

Cada integrante debe mantener actualizadas las tareas que tenga asignadas.

Debe registrarse:

- tarea;
- responsable;
- estado;
- fecha de inicio;
- fecha fin planificada;
- entregable;
- horas estimadas;
- horas faltantes en cada corte semanal.

Los estados utilizados son:

```text
No comenzada
En progreso
Completada
Bloqueada
```

---

## Horas faltantes

Las horas faltantes representan:

```text
cuánto tiempo se estima que todavía falta para terminar la tarea
```

No representan las horas ya trabajadas.

Ejemplo:

```text
Horas estimadas inicialmente: 5
Horas trabajadas hasta ahora: 3
Horas que se calcula que todavía faltan: 2

Horas faltantes = 2
```

Cuando una tarea está completamente terminada:

```text
Horas faltantes = 0
```

No deben reconstruirse valores históricos inventados para semanas anteriores.

---

# 17. Product Backlog

Un caso de uso no debe considerarse completado por implementar únicamente una parte de su flujo.

Para que un CU cuente como completado dentro del Product Backlog debe cumplir las condiciones definidas allí:

```text
Programado
Probado
Integrado a main
```

Una primera iteración parcial puede aparecer como avance dentro del Sprint Backlog sin marcar todavía el CU completo en Product Backlog.

---

# 18. Evidencias

Cada integrante debe conservar evidencia suficiente para demostrar el trabajo realizado.

Puede incluir:

- rama;
- commits;
- Pull Request;
- pruebas;
- capturas relevantes;
- resultados de ejecución;
- cambios en diagramas;
- reporte de validación.

La evidencia debe permitir responder:

1. ¿Qué se intentó implementar?
2. ¿Qué se probó?
3. ¿Qué ocurrió?
4. ¿Qué problema apareció?
5. ¿Qué decisión se tomó?
6. ¿Qué cambió como consecuencia?

No se deben generar capturas sin propósito únicamente para aumentar la cantidad de evidencias.

---

# 19. Integración

La validación individual de cada módulo es solamente una primera etapa.

Posteriormente será necesario comprobar la integración entre módulos.

Una funcionalidad puede funcionar correctamente de manera aislada y fallar al depender de otro componente.

Por esta razón, las dependencias identificadas durante la primera implementación deben quedar registradas.

No es necesario implementar todas las integraciones inmediatamente si dependen de trabajo que todavía no existe.

---

# 20. Checklist antes de solicitar integración

## Requisitos

- [ ] Revisé la especificación detallada del CU.
- [ ] Implementé el flujo correspondiente a la tarea.
- [ ] Revisé alternativas relevantes.
- [ ] Revisé excepciones relevantes.

## Diseño

- [ ] El código respeta las responsabilidades de las capas.
- [ ] Las clases utilizadas corresponden al diseño.
- [ ] Identifiqué cualquier cambio necesario.
- [ ] Reporté los diagramas afectados.

## Persistencia

- [ ] Los datos necesarios pueden almacenarse.
- [ ] Los datos pueden recuperarse cuando corresponde.
- [ ] No dupliqué responsabilidades de otro módulo.

## Pruebas

- [ ] Ejecuté el flujo principal.
- [ ] Probé los escenarios necesarios.
- [ ] Registré el resultado.
- [ ] Conservé evidencia suficiente.

## Git

- [ ] No trabajé directamente en `main`.
- [ ] La rama corresponde al requerimiento.
- [ ] Revisé `git status`.
- [ ] Los commits describen claramente los cambios.
- [ ] No incluí archivos ajenos a la tarea.

## Seguimiento

- [ ] Actualicé la tarea en Sprint Backlog.
- [ ] Actualicé las horas faltantes.
- [ ] Registré bloqueantes si existen.

---

# 21. Qué debe poder explicar cada integrante

Cada integrante debe poder responder:

1. ¿Qué caso de uso estoy implementando?
2. ¿Qué comportamiento define?
3. ¿Qué clases participan?
4. ¿Qué responsabilidad tiene cada clase?
5. ¿Qué flujo implementé?
6. ¿Qué escenarios probé?
7. ¿Qué resultado esperaba?
8. ¿Qué resultado obtuve?
9. ¿Qué problemas encontré?
10. ¿El problema era de código o de diseño?
11. ¿Fue necesario modificar algún diagrama?
12. ¿Qué evidencia respalda esa modificación?
13. ¿Qué dependencias encontré con otros módulos?

---

# 22. Decisiones pendientes

No deben asumirse sin coordinación decisiones que todavía no hayan sido definidas por el equipo o por el profesor.

Entre ellas pueden encontrarse:

- gestor de construcción o dependencias;
- paquete base definitivo;
- cambios arquitectónicos que afecten múltiples módulos;
- modificaciones importantes del modelo de persistencia;
- comportamientos no definidos por los requisitos.

Cuando una decisión pendiente bloquee realmente la implementación, debe registrarse como bloqueante.

---

# 23. Regla general de esta etapa

El flujo de trabajo debe mantenerse así:

```text
Requisito
   ↓
Diseño
   ↓
Rama de trabajo
   ↓
Implementación
   ↓
Pruebas
   ↓
Hallazgos
   ↓
Clasificación del problema
   ↓
Corrección
   ↓
Actualización de documentación si corresponde
   ↓
Nueva prueba
   ↓
Revisión
   ↓
Integración a main
```

Los diagramas no deben considerarse intocables.

Pero tampoco deben modificarse ante cualquier error de programación.

La implementación debe utilizarse como evidencia para determinar qué elementos del diseño funcionan correctamente y cuáles necesitan una nueva iteración.
```
