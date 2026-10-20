# Guía de contribución de NextEventux

Esta guía define cómo registrar, desarrollar, revisar e integrar cambios en NextEventux. Su objetivo es mantener trazabilidad entre los casos de uso, el código, las pruebas y los documentos del proyecto.

## 1. Fuentes de información y trazabilidad

Cada herramienta cumple una función diferente:

| Recurso | Uso |
| --- | --- |
| SRS | Define los requisitos, los casos de uso y el comportamiento esperado del producto. |
| PMP | Define el proceso de trabajo, las responsabilidades y los controles de calidad. |
| GitHub Issues y Project | Funcionan como Kanban operativo para organizar y relacionar el trabajo técnico. |
| `Control_de_Alcance_NextEventux.xlsx` | Es la fuente oficial de estados, horas faltantes, métricas y Burndown Chart. |
| Git y GitHub | Conservan el historial del código, las pruebas y los cambios revisados. |

Los Issues de GitHub no sustituyen la actualización del Excel. Cuando una tarea cambie de estado o de horas faltantes, el responsable debe reflejarlo también en el archivo de Control de Alcance.

## 2. Flujo de trabajo

## 2. Flujo de trabajo

1. Identificar el requerimiento o tarea que se va a desarrollar.
2. Revisar sus requisitos, criterios y documentación relacionada.
3. Actualizar la copia local de `main`.
4. Crear una rama temporal específica para ese trabajo.
5. Realizar commits pequeños y relacionados con un solo objetivo.
6. Ejecutar las pruebas aplicables y conservar evidencia.
7. Publicar la rama y abrir un Pull Request hacia `main`.
8. Atender los comentarios de revisión.
9. Obtener la aprobación de un compañero diferente al autor.
10. Fusionar el Pull Request cuando el cambio esté validado.
11. Eliminar la rama temporal cuando ya no sea necesaria.
12. Actualizar el estado y las horas faltantes en el archivo de Control de Alcance.

No se deben realizar cambios de desarrollo directamente sobre `main`.

```bash
git switch main
git pull
git switch -c CU12-aceptar-rechazar-solicitud
```

## 3. Ramas

| Rama | Propósito | Ejemplo |
| --- | --- | --- |
| `main` | Versión estable y oficial del proyecto. | `main` |
| Rama de caso de uso | Implementación o validación de un caso de uso. | `CU11-gestionar-servicio` |
| Rama de tarea | Trabajo técnico o documental que no corresponde directamente a un CU. | `actualizar-flujo-git-documentacion` |

### Convención de nombres

Para los casos de uso:

`CUXX-descripcion-corta`

Ejemplos:

- `CU01-crear-evento`
- `CU11-gestionar-servicio`
- `CU28-aprobar-proveedores`

El nombre debe:

- representar el trabajo que se realiza;
- evitar espacios;
- ser corto pero identificable;
- utilizar el código del caso de uso cuando corresponda.

Las ramas son temporales. Una vez que el trabajo ha sido revisado e integrado a `main`, pueden eliminarse.

## 4. Commits

### 4.1 Formato

Cada commit debe seguir esta estructura:

```text
tipo(módulo): descripción breve
```

Si el cambio corresponde a un caso de uso, se agrega el identificador al final:

```text
feat(proveedores): implementar respuesta a solicitud CU-12
fix(finanzas): impedir contratación sobre el presupuesto CU-09
docs(srs): precisar flujo alternativo del CU-12
test(eventos): cubrir cancelación de evento CU-04
```

### 4.2 Etiquetas de commit

| Etiqueta | Cuándo usarla |
| --- | --- |
| `feat` | Nueva funcionalidad visible o regla de negocio. |
| `fix` | Corrección de un comportamiento defectuoso. |
| `docs` | Cambio exclusivo de documentación. |
| `test` | Creación o modificación de pruebas. |
| `refactor` | Reorganización del código sin cambiar su comportamiento esperado. |
| `build` | Cambios en dependencias o configuración de construcción. |
| `chore` | Mantenimiento que no modifica la funcionalidad del producto. |

### 4.3 Módulos permitidos

`eventos`, `finanzas`, `proveedores`, `invitados`, `marketing`, `administracion`, `compartido`, `database`, `ui`, `docs`, `repo`.

La descripción se escribe en minúscula, en infinitivo, sin punto final y debe expresar un solo cambio. No se aceptan mensajes vagos como `cambios`, `arreglos`, `avance` o `commit final`.

## 5. Pull Requests

### 5.1 Contenido obligatorio

Cada Pull Request debe:

1. Tener un título con el formato `[TIPO][MÓDULO] descripción`, por ejemplo: `[FEATURE][PROVEEDORES] Implementar CU-12`.
2. Relacionar el Issue correspondiente mediante `Closes #número` cuando deba cerrarse al fusionar.
3. Indicar el módulo y el caso de uso afectados.
4. Explicar el problema u objetivo y resumir los cambios realizados.
5. Describir las pruebas ejecutadas y su resultado.
6. Adjuntar evidencia cuando exista una interfaz, un error corregido o una validación importante.
7. Informar efectos sobre base de datos, requisitos, documentación u otros módulos.
8. Completar la lista de verificación de `.github/pull_request_template.md`.

Un Pull Request debe atender un solo objetivo principal. No debe mezclar funcionalidades independientes, correcciones ajenas o cambios masivos de formato.

### 5.2 Aprobación y fusión

- Se requiere **una aprobación de un compañero distinto del autor** antes de fusionar a `main`.
- El autor no puede contar como su propio revisor.
- Las conversaciones de revisión deben quedar resueltas o justificadas antes de la fusión.
- Si el cambio afecta otro módulo, se debe avisar a su responsable y dejar constancia en el Pull Request.
- Deben ejecutarse las pruebas aplicables. Si una prueba no puede ejecutarse, el motivo y el riesgo deben quedar escritos.
- No se fusionará código con errores conocidos que impidan cumplir los criterios de aceptación.

**Condiciones para integrar a main**

1. Tiene al menos una aprobación válida.
2. No presenta conflictos con `main`.
3. Las pruebas aplicables pasan correctamente.
4. La plantilla del Pull Request está completa.
5. El cambio tiene evidencia suficiente.

## 6. Issues y tablero Kanban

### 6.1 Dónde están las plantillas

Las plantillas están en `.github/ISSUE_TEMPLATE/` y deben estar presentes en la rama predeterminada del repositorio para que GitHub las muestre al seleccionar **New issue**.

| Plantilla | Uso |
| --- | --- |
| `kanban_task.yml` | Tarea planificada de un sprint o actividad asociada a un caso de uso. |
| `general_issue.yml` | Situación válida que no encaja en las demás plantillas. |
| `bug_report.yml` | Comportamiento incorrecto y reproducible del software. |
| `change_request.yml` | Cambio propuesto en alcance, requisito, regla de negocio o diseño. |
| `documentation.yml` | Corrección o actualización de documentación. |
| `config.yml` | Configura el selector de plantillas y evita Issues vacíos sin estructura. |

Los archivos `.yml` son formularios: GitHub convierte las respuestas en el cuerpo del Issue. Los campos obligatorios evitan abrir tareas sin objetivo, evidencia o criterios de aceptación. Los labels automáticos solo se aplican si esos labels ya fueron creados en el repositorio.

**Reglas para crear un issue**

El título debe describir una sola necesidad. El cuerpo debe incluir objetivo, responsable, módulo, criterios de aceptación, dependencias y evidencia esperada. Para tareas de programación, relacione el caso de uso correspondiente.

Cada Issue debe tener:

- Un label de tipo.
- Un label de módulo cuando corresponda.
- Un label de prioridad.
- Responsable asignado.
- Ubicación en el tablero del proyecto.

El estado del trabajo se administra principalmente mediante las columnas o el campo `Status` del tablero Kanban. No se deben crear labels duplicados para `Por hacer`, `En progreso` y `Terminado`. El label `estado:bloqueado` se utiliza únicamente cuando existe un impedimento real.

### 6.2 Uso correcto de una tarea Kanban

Una tarjeta debe representar trabajo verificable y suficientemente pequeño para avanzar dentro de un sprint. Debe contener responsable, módulo, caso de uso cuando aplique, objetivo, criterios de aceptación, prioridad, horas estimadas, dependencias y evidencia esperada.

El tablero puede usar estas columnas:

| Estado en GitHub Project | Equivalencia en Control de Alcance |
| --- | --- |
| Backlog / Por hacer | No comenzado |
| En progreso | En progreso |
| En revisión | En progreso |
| Bloqueado | Bloqueado |
| Terminado | Completado, únicamente si cumple la Definition of Done |

La columna `En revisión` permite mostrar que la implementación terminó, pero todavía no ha sido aprobada o integrada. Por eso aún corresponde a `En progreso` en el control oficial.

### 6.3 Cierre de Issues

El método preferido es escribir `Closes #número` en el Pull Request. El Issue se cerrará automáticamente cuando el PR se fusione en la rama configurada por GitHub. No debe cerrarse solo porque se escribió el código o se abrió el PR.

Antes de cerrar una tarea se debe comprobar que:

- cumple sus criterios de aceptación;
- cuenta con las pruebas y evidencias aplicables;
- fue revisada por un compañero;
- está integrada en la rama correspondiente;
- no deja documentación necesaria sin actualizar;
- su estado y sus horas faltantes fueron actualizados en Excel.

Comentario de cierre por implementación:

```text
Se cierra porque el trabajo quedó implementado en el PR #___, revisado por @___ y validado mediante ___. Se actualizaron el estado y las horas faltantes en Control_de_Alcance_NextEventux.xlsx.
```

Comentario de cierre sin implementación:

```text
Se cierra sin implementar porque ___. La decisión quedó acordada en ___. El trabajo fue reemplazado por #___ / no hace parte del alcance vigente. Se actualizó el control correspondiente.
```

No se deben borrar Issues para ocultar errores o decisiones descartadas: cerrarlos con una explicación conserva la trazabilidad.

## 7. Labels personalizados

La lista inicial y sus colores está documentada en [`.github/LABELS.md`](.github/LABELS.md). Para evitar confusiones, cada Issue debe utilizar como máximo un label de tipo, uno de módulo y uno de prioridad, además de `estado:bloqueado` cuando corresponda.

Los labels no reemplazan la asignación de responsables ni el estado del tablero Kanban.

## 8. Calidad y definición de terminado

Una funcionalidad puede considerarse terminada dentro de la iteración cuando cumple los criterios de aceptación, fue probada, recibió revisión de otra persona, quedó integrada en `main` y cuenta con evidencia. Para un caso de uso también deben estar controlados el flujo principal, los flujos alternativos, las excepciones y las reglas de negocio correspondientes.

Las pruebas automáticas se realizarán con JUnit. El término correcto para comprobar la interacción entre componentes es **pruebas de integración**, no “pruebas integrales”.

## 9. Comunicación y archivos del proyecto

Las decisiones formales deben quedar registradas en Issues, Pull Requests, actas o documentos institucionales. WhatsApp puede utilizarse para avisos rápidos, pero no debe ser la única evidencia de una decisión.

GitHub almacenará el código fuente y su historial. Los documentos académicos en Word, Excel o PowerPoint continuarán en la carpeta institucional acordada por el equipo, salvo que el grupo decida versionar alguna copia expresamente.
 