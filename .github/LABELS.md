# Labels recomendados para NextEventux

Los labels permiten clasificar los Issues y Pull Requests sin reemplazar el tablero Kanban. Deben crearse manualmente en `Settings > Issues > Labels` antes de comenzar a usar las plantillas.

## Labels de tipo

Cada Issue debe tener solo uno de estos labels.

| Nombre | Color | Uso |
| --- | --- | --- |
| `tipo:funcionalidad` | `#1D76DB` | Implementación o revisión de un caso de uso. |
| `tipo:error` | `#D73A4A` | Comportamiento incorrecto o fallo reproducible. |
| `tipo:tarea` | `#5319E7` | Actividad concreta del Sprint Backlog. |
| `tipo:mejora` | `#A2EEEF` | Mejora de una función existente. |
| `tipo:documentación` | `#0075CA` | Cambios en documentos o explicaciones técnicas. |
| `tipo:pruebas` | `#BFDADC` | Creación o corrección de pruebas. |
| `tipo:consulta` | `#D4C5F9` | Pregunta o decisión que debe registrarse. |

## Labels de módulo

Use uno cuando el trabajo pertenezca principalmente a un módulo. Si afecta a varios, utilice `módulo:compartido`.

| Nombre | Color | Uso |
| --- | --- | --- |
| `módulo:eventos` | `#0E8A16` | CU-01 a CU-05. |
| `módulo:finanzas` | `#FBCA04` | CU-06 a CU-10. |
| `módulo:proveedores` | `#006B75` | CU-11 a CU-15. |
| `módulo:invitados` | `#C2E0C6` | CU-16 a CU-20. |
| `módulo:marketing` | `#D876E3` | CU-21 a CU-25. |
| `módulo:administración` | `#B60205` | CU-26 a CU-30. |
| `módulo:compartido` | `#5EBEFF` | Cambio transversal o compartido. |

## Labels de prioridad

La prioridad debe definirse con relación al alcance y a la iteración actual.

| Nombre | Color | Uso |
| --- | --- | --- |
| `prioridad:alta` | `#B60205` | Impide una entrega, bloquea otro trabajo o corrige un riesgo grave. |
| `prioridad:media` | `#FBCA04` | Necesario para la iteración, pero no bloquea inmediatamente. |
| `prioridad:baja` | `#C5DEF5` | Puede aplazarse sin comprometer la entrega actual. |

## Labels adicionales

| Nombre | Color | Uso |
| --- | --- | --- |
| `estado:bloqueado` | `#000000` | Existe un impedimento real que requiere intervención. |
| `requiere-decisión` | `#E99695` | El equipo o el profesor debe aprobar una opción. |
| `buena-primera-tarea` | `#7057FF` | Actividad pequeña y adecuada para familiarizarse con el repositorio. |

## Relación con el Kanban

El tablero debe administrar estados como `Por hacer`, `En progreso`, `En revisión` y `Terminado`. Esos estados no necesitan labels porque duplicarlos genera inconsistencias. El label `estado:bloqueado` sí se conserva para que los impedimentos sean visibles en búsquedas, Issues y Pull Requests.

## Regla de uso

Una combinación típica sería:

```text
tipo:funcionalidad + módulo:proveedores + prioridad:alta
```

Si la actividad queda impedida, se agrega temporalmente `estado:bloqueado` y se explica el bloqueo en un comentario. El label se retira cuando el impedimento se resuelve.
