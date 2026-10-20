# nextEventux

NextEventux es una aplicación de escritorio para centralizar la planeación y el seguimiento de eventos. Reúne en un mismo sistema la información que normalmente queda repartida entre conversaciones, hojas de cálculo y notas independientes, y facilita la colaboración entre clientes, organizadores, proveedores e invitados.

## Navegación

| Sección | Contenido |
| --- | --- |
| [Visión](#visión-del-producto) | Problema que resuelve y usuarios principales. |
| [Alcance](#alcance) | Capacidades incluidas y límites del producto. |
| [Módulos](#módulos-funcionales) | Organización de los 30 casos de uso. |
| [Tecnologías](#tecnologías) | Herramientas de implementación y calidad. |
| [Estructura](#estructura-recomendada-del-repositorio) | Distribución prevista del código y la documentación técnica. |
| [Desarrollo](#flujo-de-desarrollo) | Ramas, Issues, Pull Requests y control de avance. |
| [Ejecución](#ejecución-local) | Preparación del entorno local. |
| [Contribución](#contribución) | Reglas que debe seguir el equipo. |

## Visión del producto

La aplicación permitirá que un cliente compare organizadores verificados y solicite la contratación de uno. El organizador podrá administrar la planeación, el presupuesto, las propuestas de proveedores, los servicios contratados y las invitaciones. Los proveedores gestionarán sus servicios y solicitudes, mientras que los invitados podrán consultar y responder información relacionada con su participación.

NextEventux busca ofrecer información actualizada para comparar alternativas, controlar recursos y detectar situaciones como compromisos superiores al presupuesto, cruces de horario de proveedores o una cantidad de asistentes mayor al aforo permitido.

## Alcance

El alcance contempla la gestión de eventos, información financiera, proveedores, invitados, actividades de marketing y funciones administrativas. También incluye autenticación, soporte multiusuario, pagos registrados, reseñas, servicios, campañas y mecanismos de fidelización.

NextEventux no presta directamente los servicios contratados ni ejecuta pagos bancarios. Es una aplicación de escritorio, no una aplicación web o móvil. El sistema contempla notificaciones internas; el posible envío de correos en procesos concretos debe quedar definido por el equipo antes de implementar esas funciones.

## Módulos funcionales

El producto se especifica mediante 30 casos de uso distribuidos de forma consecutiva en seis módulos.

| Módulo | Casos de uso | Responsable | Propósito general |
| --- | --- | --- | --- |
| Gestión de Eventos | CU-01 a CU-05 | Sara Alejandra Coy Calderón | Crear, actualizar, publicar, cancelar y finalizar eventos. |
| Gestión Financiera | CU-06 a CU-10 | Valeria Salgado Cortés | Consultar y controlar planificación, paquetes, sanciones y movimientos. |
| Gestión de Proveedores | CU-11 a CU-15 | Isabella Hermosa Losada | Administrar servicios, solicitudes, contrataciones, reseñas y portafolios. |
| Gestión de Invitados | CU-16 a CU-20 | Sofía Cortés Salazar | Administrar respuestas, invitaciones, necesidades, acompañantes y regalos. |
| Marketing | CU-21 a CU-25 | Saúl Leonardo Cruz Romero | Gestionar segmentación, fidelidad, campañas, consentimientos y canjes. |
| Administración | CU-26 a CU-30 | Juan Diego Rojas Zárate | Contratar organizadores y administrar registros, aprobaciones y suspensiones. |

La especificación detallada de cada comportamiento pertenece al SRS y a los anexos de casos de uso. El código no debe introducir reglas que contradigan esos documentos sin registrar y aprobar previamente una solicitud de cambio.

Consulta la [entrega de Gestión de Eventos CU01–CU05](docs/cu01-solicitudes-presupuesto.md)
para ver el flujo de solicitudes, presupuesto, pruebas y dependencias pendientes.

## Tecnologías

| Área | Tecnología | Uso |
| --- | --- | --- |
| Lenguaje | Java | Implementación de funcionalidades y reglas de negocio. |
| Interfaz | JavaFX y CSS | JavaFX construye las pantallas de escritorio y CSS define sus colores, fuentes, bordes y espacios. |
| Persistencia | H2 | Base de datos relacional embebida obligatoria para el proyecto. |
| Pruebas | JUnit | Pruebas automatizadas de clases, validaciones y reglas críticas. |
| Modelado | UML y draw.io | Representación visual de casos de uso y conceptos del sistema. |
| Desarrollo | Visual Studio Code | Edición, ejecución y depuración del código. |
| Versionamiento | Git y GitHub | Historial, colaboración, revisión e integración del código. |

## Organización del repositorio

```text
NextEventux/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   └── pull_request_template.md
├── docs/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       └── java/
├── .gitignore
├── CONTRIBUTING.md
└── README.md
```

Los scripts de creación o migración de la base de datos que sean necesarios para reproducir el sistema sí deben versionarse. Los archivos locales generados por H2, las compilaciones y los secretos no deben incluirse en el repositorio.

## Flujo de desarrollo

`main` conserva la versión estable del proyecto. Cada requerimiento o tarea se desarrolla en una rama temporal independiente creada a partir de `main`. Cuando el trabajo está listo, probado y revisado, se integra nuevamente a `main` mediante Pull Request.

Para los casos de uso, el equipo utilizará nombres de rama con el formato:

`CUXX-descripcion-corta`

Por ejemplo:

`CU11-gestionar-servicio`

Para trabajos que no correspondan directamente a un caso de uso se utilizará un nombre breve y representativo de la tarea.

El proyecto combina Scrum con seguimiento Kanban. GitHub Issues y GitHub Project permiten visualizar el flujo técnico, mientras que `Control_de_Alcance_NextEventux.xlsx` continúa siendo el registro oficial de tareas, estados, horas faltantes, métricas y Burndown Chart. El responsable debe mantener consistencia entre ambos recursos.

## Ejecución local

Se requiere un JDK compatible con la versión que declare el archivo de construcción del proyecto, JavaFX, Git y Visual Studio Code con soporte para Java. H2 y JUnit deben incorporarse como dependencias administradas por el proyecto, no mediante archivos JAR personales sin documentar.

```bash
git clone <URL_DEL_REPOSITORIO>
cd NextEventux
git switch main
git pull
```

El proyecto utiliza Maven y Java 17 o superior. Para probar y abrir la aplicación JavaFX:

```bash
mvn clean test
mvn javafx:run
```

Se requiere una sesión gráfica de escritorio para abrir la ventana. Las
bibliotecas nativas de JavaFX se descargan mediante Maven para el sistema actual.
El repositorio todavía no incluye Maven Wrapper.

## Documentación del proyecto

El PMP, el SRS, el reporte gerencial, los diagramas y las matrices de seguimiento se almacenan en la carpeta institucional definida por el equipo. GitHub se utiliza principalmente para el código, las pruebas y la documentación técnica necesaria para comprender o ejecutar el repositorio.

## Equipo

NextEventux es desarrollado por:

| Integrante | Módulo principal | Github |
| --- | --- | --- |
| Sara Alejandra Coy Calderón | Gestión de Eventos |https://github.com/sarasscx|
| Valeria Salgado Cortés | Gestión Financiera |https://github.com/vsalgadoo |
| Isabella Hermosa Losada | Gestión de Proveedores |https://github.com/Isa21087 |
| Sofía Cortés Salazar | Gestión de Invitados | https://github.com/cortesssofia |
| Saúl Leonardo Cruz Romero | Marketing | https://github.com/Conecruz |
| Juan Diego Rojas Zárate | Administración | https://github.com/MaizenaDura |


## Contribución

Antes de realizar cambios, consulte [CONTRIBUTING.md](CONTRIBUTING.md). Allí se explican la convención de commits, el uso de ramas, las plantillas de Issues, la política de Pull Requests, la aprobación requerida y el cierre trazable de las tareas.

Todas las contribuciones deben realizarse mediante una rama independiente y un Pull Request. Las ramas de trabajo se crean a partir de `main` y deben representar claramente el requerimiento o tarea que se está desarrollando. Cada Pull Request debe dirigirse a `main` y requiere la revisión de una persona diferente al autor antes de integrarse.

Para el procedimiento completo de implementación, validación, pruebas y registro de hallazgos consulte [GUIA_IMPLEMENTACION_Y_VALIDACION.md](GUIA_IMPLEMENTACION_Y_VALIDACION.md).

## Estado

NextEventux se encuentra en desarrollo académico incremental. Las funcionalidades se priorizan e implementan por sprint, y cada incremento debe quedar probado, revisado e integrado con los módulos relacionados antes de considerarse terminado.

# Uso de la carpeta `.github`

GitHub reconoce automáticamente los archivos de esta carpeta y los utiliza para estandarizar Issues y Pull Requests.

## Carpeta `ISSUE_TEMPLATE`

Cada archivo `.yml` de `ISSUE_TEMPLATE` define un formulario diferente. Cuando una persona seleccione **New issue**, GitHub mostrará las plantillas disponibles y solicitará la información configurada en cada una.

| Archivo | Función |
| --- | --- |
| `tarea-kanban.yml` | Crea tareas concretas para el tablero y el Sprint Backlog. |
| `reporte-error.yml` | Registra errores con pasos de reproducción y evidencia. |
| `mejora.yml` | Presenta mejoras con su impacto y criterios de aceptación. |
| `caso-de-uso.yml` | Controla la implementación o revisión de un caso de uso completo. |
| `asunto-general.yml` | Registra consultas, decisiones o asuntos no cubiertos por otra plantilla. |
| `config.yml` | Impide Issues vacíos para que siempre se utilice una plantilla. |

### Cómo utilizar correctamente una plantilla

1. Seleccionar la plantilla que corresponda al trabajo real.
2. Escribir un título concreto sin borrar el prefijo sugerido.
3. Completar todos los campos obligatorios.
4. Asignar responsable, prioridad y módulo.
5. Agregar el Issue al tablero Kanban.
6. Moverlo de estado en el tablero a medida que avance.
7. Relacionar el Pull Request mediante `Closes #número`.
8. Verificar la evidencia antes de cerrar el Issue.

Las plantillas no deben utilizarse para simular avance. Un Issue abierto representa trabajo pendiente y uno cerrado debe tener un resultado verificable.

## Plantilla de Pull Request

El archivo `PULL_REQUEST_TEMPLATE.md` se carga automáticamente al crear un Pull Request. El autor debe completar todas las secciones y la lista de comprobación.

Los Pull Requests de las ramas de trabajo se dirigen hacia `main`. Cada Pull Request requiere la aprobación de una persona diferente al autor antes de integrarse.

Cada Pull Request requiere una aprobación de una persona diferente al autor. La persona revisora debe comprobar la rama de destino, el alcance, las pruebas, las reglas de negocio y la ausencia de información sensible antes de aprobar.

## Labels

GitHub no crea automáticamente todos los labels personalizados solo por mencionarlos en las plantillas. Primero deben crearse en la configuración del repositorio utilizando los nombres, colores y descripciones indicados en [`LABELS.md`](LABELS.md).

El estado del trabajo se administra en el tablero Kanban. Los labels sirven para clasificar tipo, módulo, prioridad y bloqueos.

## Inicialización de H2

La conexión `config.DatabaseConnection.getConnection()` crea las tablas de
`src/main/resources/schema.sql` si todavía no existen. Al abrir nuevas conexiones
se conservan los registros existentes. Después aplica la migración repetible
`migrations/001_evento_cu01.sql` para añadir los campos de CU01 a bases existentes.
Los futuros cambios de estructura también necesitan una migración explícita.

Las migraciones `001` a `009` son repetibles y se ejecutan en cada inicialización. Desde la `010` cada migración se ejecuta una sola vez y queda registrada en la tabla `Esquema_version`; se agregan a la lista de `DatabaseConnection.inicializarEsquema()` con `ejecutarUnaVez(...)`. Si una migración exige unicidad, `verificarSinDuplicados(...)` detiene la apertura con un mensaje claro cuando hay datos repetidos, sin modificar nada.

Los datos de ejemplo están separados en `src/main/resources/seed.sql`. Su carga es
opcional y debe realizarse una sola vez sobre una base recién inicializada; no se
cargan al abrir la aplicación ni deben ejecutarse nuevamente sobre datos existentes.

Con JDK 17 o superior y Maven instalado, ejecutar desde la raíz del repositorio:

```bash
mvn clean test-compile dependency:copy-dependencies
java -cp "target/classes:target/test-classes:target/dependency/*" config.TestSchema
```

En Windows se utiliza `;` como separador del classpath. `TestSchema` puede ejecutarse manualmente y también se verifica mediante
`SchemaPersistenciaTest` al ejecutar `mvn test`.
Comprueba creación del esquema, carga de ejemplos, reinicialización sin duplicados
y conservación de datos después de cerrar y volver a abrir la base. Utiliza una
base temporal y no modifica la base de la aplicación (`~/.nexteventux/nexteventux`).

### Conciliación de Evento con CU01

La entidad conserva nombre, tipo, presupuesto e invitados iniciales y representa
los campos requeridos por la tabla compartida. El constructor ahora recibe un
organizador explícito, descripción opcional y fechas de inicio y fin con hora.

| Entidad Java | Columna H2 |
| --- | --- |
| `idEvento` (`Integer`, nulo hasta guardar) | `id_evento` autogenerado |
| `idOrganizador` | `id_organizador`, clave foránea |
| `nombre`, `tipo`, `descripcion` | `nombre`, `tipo`, `descripcion` |
| `fechaInicio`, `fechaFin` (`LocalDateTime`) | `fecha_inicio`, `fecha_fin` (`TIMESTAMP`) |
| `ubicacion` (antes `lugar`) | `ubicacion` |
| `presupuesto` (`BigDecimal`) | `presupuesto` (`DECIMAL(12,2)`) |
| `invitadosIniciales` | `invitados_iniciales` |
| `estado` | `estado`, valor inicial `PLANEADO` |
| `fechaCreacion` (`LocalDateTime`) | `fecha_creacion` |

El presupuesto admite cero y hasta 9999999999.99 con precisión de centavos, sin
redondeo silencioso. Los invitados admiten cero. Las fechas son obligatorias y el
fin no puede ser anterior al inicio; no se añade una restricción sobre fechas
pasadas ni se exige duración mínima. Las longitudes se validan según las columnas.

Las columnas nuevas permiten `NULL` para conservar registros anteriores cuyos
datos reales no se conocen. La creación de nuevos eventos mediante el servicio
requiere los tres campos. La migración no rellena datos históricos. Los ejemplos
incluyen estos campos y adelantan sus contadores automáticos; al inicializar se
recupera también el contador de eventos de bases antiguas si quedó por debajo de
los identificadores existentes, sin reducir un contador ya adelantado.

Validaciones adicionales, después de compilar las clases de prueba:

```bash
java -cp "target/classes:target/test-classes:target/dependency/*" PruebaEventoSchema
java -cp "target/classes:target/test-classes:target/dependency/*" PruebaCU01
```

`PruebaEventoSchema` comprueba que H2 genera un identificador y conserva los
campos del evento. `TestSchema` verifica además la migración de tablas existentes.
Los ejemplos pueden ejecutarse manualmente. `ServiceEventosTest` cubre las
validaciones y el flujo del controlador con JUnit; `SchemaPersistenciaTest`
ejecuta las verificaciones H2 existentes. Ambos se ejecutan con `mvn test`.

`RepositoryEvento` guarda y consulta mediante JDBC. Antes de insertar verifica
que el organizador exista; la clave foránea mantiene la relación en H2. El evento
recibe el identificador generado después de confirmar la transacción. La consulta
recupera estado, fecha de creación y campos históricos nulos sin inventar valores.
Los invitados históricos usan `Integer` para distinguir desconocido (`NULL`) de cero.

Esta conciliación cambia atributos y tipos respecto de la primera iteración.
Debe reflejarse en el diseño de estructura (SDD 3.2), persistencia (SDD 3.4) e
interfaz (SDD 3.1) de los documentos institucionales, que no están en el repositorio.

## Pantalla JavaFX de creación de eventos

`app.NextEventuxApp` abre la pantalla de `ViewEventos`, construida con controles
JavaFX y un único archivo CSS en `src/main/resources/styles/eventos.css`. Sigue el
mockup aportado: menú lateral, barra superior, formulario, tarjetas informativas y
resumen del presupuesto en COP y de los invitados. El panel baja en ventanas
pequeñas y el contenido se puede desplazar para acceder a los botones.

El formulario añade los campos requeridos por la entidad conciliada: organizador
seleccionado por nombre desde H2, inicio y fin con hora y descripción opcional. Las horas usan
`HH:mm`, los invitados son enteros y los decimales del presupuesto usan punto.
El selector de tipo contiene Corporativo, Social, Cultural y Otro. La interfaz
muestra «En planeación» y el servicio conserva el valor `PLANEADO`.

«Crear evento» construye la entidad y llama al controlador. Los errores aparecen
en el formulario y conservan lo ingresado. «Cancelar» limpia los campos, el
resumen y el mensaje, sin eliminar eventos ya creados. Después de crear se
conservan los campos y se muestra una confirmación, siguiendo el mockup.

La aplicación guarda eventos de forma persistente en `~/.nexteventux/nexteventux.mv.db`
(carpeta del usuario del sistema; se puede cambiar con `-Dnexteventux.home=<carpeta>`).
La ruta es absoluta y no depende del directorio desde el que se ejecute. Una base creada
antes en `data/nexteventux.mv.db` no se usa automáticamente: copia ese archivo a la nueva
carpeta con el nombre `nexteventux.mv.db` si necesitas conservarla.
No elimines ese archivo si necesitas conservar los registros. El organizador debe
estar previamente registrado. Los errores de validación o base de datos aparecen
en el formulario, sin mostrar confirmación de éxito ni borrar los campos.

No hay sesión de usuario. El menú de otros módulos y la cuenta son elementos
visuales sin navegación ni autenticación implementadas.

### Preparar una demostración en una base vacía

Para probar CU01 antes de disponer del módulo de registro de organizadores:

```bash
mvn test-compile dependency:copy-dependencies
java -cp "target/classes:target/dependency/*" config.CargarDatosEjemplo
mvn javafx:run
```

En Windows usa `;` como separador del classpath. La carga es explícita y se rechaza
si hay registros en cualquier tabla, para proteger los datos existentes. El desplegable muestra los nombres de los
organizadores de ejemplo. La aplicación no carga
estos ejemplos automáticamente. Si ya existen datos reales, usa un organizador
registrado y omite la carga de ejemplos.

### Comprobación del formulario

1. Abrir con `mvn javafx:run` y verificar la distribución y el desplazamiento.
2. Escribir presupuesto e invitados y comprobar el resumen en vivo.
3. Crear con nombre, tipo, organizador seleccionado, fechas y horas válidas, lugar,
   presupuesto no negativo con hasta dos decimales e invitados enteros no negativos.
4. Probar un campo obligatorio vacío, `NaN`, invitados decimales, hora `24:00` y
   fin anterior al inicio: deben mostrarse errores y conservarse los campos.
5. Pulsar Cancelar: deben limpiarse los campos y el mensaje, y el resumen volver a cero.
6. Reducir y ampliar la ventana: el formulario y sus acciones deben seguir accesibles.

Verificación de esta implementación: 16 pruebas JUnit sin fallos que cubren
validaciones, creación mediante controlador y repositorio JDBC, identificador
automático, organizador inexistente, recuperación tras cerrar y reabrir H2,
registros históricos nulos, error de conexión, migración y carga segura de ejemplos.
Las bases de prueba son temporales y no modifican los datos de la aplicación.
La interfaz fue validada visualmente por la usuaria y mediante una ejecución
controlada JavaFX con aserciones sobre resumen, rechazo de entradas inválidas,
creación, persistencia, cancelación y redimensionamiento.

Esta es una primera iteración de CU01; el caso de uso completo sigue pendiente.
Según la especificación original confirmada por la usuaria («Especificación Casos
de Uso-33.pdf», CU-001), faltan:

- Organizador autenticado con rol ORG y proveedor en estado Verificado como precondiciones.
- Envío de una solicitud de servicio dentro del flujo principal.
- Validación de que el costo comprometido no supere el presupuesto.
- Actualización del presupuesto disponible en el flujo exitoso.

La existencia del organizador en H2 no equivale a autenticación ni autorización.
La persistencia del evento no sustituye estas reglas. El PR se mantiene como
primera iteración para revisión; no debe registrarse CU01 como completado en el
Control de Alcance hasta implementar, probar e integrar los requisitos pendientes.

El campo Organizador consulta H2 y muestra únicamente los nombres de los usuarios
asociados a organizadores, ordenados por nombre. El identificador permanece
interno y se utiliza al guardar. No se selecciona un organizador automáticamente;
Cancelar borra la selección. Si no hay organizadores o falla la consulta, se
muestra un mensaje y no se permite crear sin seleccionar uno.

## CU03 — Subir evento al repositorio

La rama `CU03-subir-evento-repositorio` incorpora la base autenticada de
`CU02-editar-evento`. Esa dependencia debe conservarse al preparar su PR.
Esta integración no añade tablas ni dependencias.

Iniciar sesión y entrar por **Publicaciones** o **Eventos → Subir al repositorio**.
El administrador entra directamente a la revisión al iniciar sesión.

- **Organizador:** selecciona uno de sus eventos y solicita su publicación. Solo
  se aceptan eventos finalizados. Consulta sus solicitudes y avisos internos.
- **Administrador:** consulta solicitudes pendientes. Aprobar publica la plantilla;
  rechazar exige un motivo de hasta 250 caracteres. Ambas decisiones notifican al
  organizador. Esta sección solo se muestra al administrador.
- **Plantillas publicadas:** catálogo disponible a ambos perfiles con estructuras
  aprobadas para futura reutilización. No implementa una operación de clonación.

### Alcance y privacidad

La identidad se obtiene del login, sin selectores de organizador o administrador.
El controlador conserva la sesión y el servicio comprueba su tipo, estado y
relación con H2 antes de cada consulta u operación. Para el administrador también
se comprueba el rol ADMINISTRADOR activo. Cerrar sesión invalida los controladores
anteriores. El organizador no puede consultar solicitudes ni avisos de otras
cuentas, ni aprobar publicaciones. El administrador no puede solicitar en nombre
de un organizador. Los proveedores no tienen acceso a CU03.

La publicación copia únicamente el tipo y la duración en minutos. Genera un título
genérico a partir del tipo; no copia el nombre original, descripción, lugar,
fechas exactas, invitados, identificadores de personas ni presupuesto. La tabla
pública de plantillas no contiene esas columnas. La referencia interna a la
solicitud se utiliza para asegurar una sola plantilla por aprobación, y no se
expone en la vista pública. El evento original no cambia de estado ni pierde datos.

No se permiten solicitudes duplicadas mientras haya una pendiente o aprobada.
Después de un rechazo, el organizador puede solicitar de nuevo expresamente; se
conservan la solicitud anterior y su motivo. Las notificaciones se guardan en H2
por usuario y se consultan dentro de la aplicación; no se envían correos.

### Persistencia y errores

La migración repetible `migrations/002_publicacion_eventos.sql` añade:

- `Solicitud_publicacion`, con estado, motivo y administrador revisor.
- `Plantilla_evento`, con los datos estructurales de la plantilla.
- `Notificacion_interna`, con destinatario, mensaje y fecha.

La comprobación del evento, registro de solicitud y notificaciones forman una
transacción. La aprobación también confirma conjuntamente la plantilla, decisión
y notificación. Ante un fallo se revierten los cambios, se muestra un mensaje
claro y se conserva el formulario para reintentar. Se bloquea el evento al procesar
la solicitud y se vuelve a comprobar su estado antes de aprobar. Las consultas
usan parámetros JDBC.

Si no hay administradores activos con el rol correspondiente, la solicitud se
bloquea con un mensaje; no se simula un envío ni una aprobación automática.
El botón Actualizar recarga las solicitudes, el catálogo y los avisos permitidos
para la sesión actual. Si falla una operación, se conserva el motivo escrito para
reintentar.

### Demostración en una base vacía

Con Maven instalado:

```bash
mvn test-compile dependency:copy-dependencies
java -cp "target/classes:target/dependency/*" config.CargarDemoPublicacion
mvn javafx:run
```

En Windows, sustituir `:` por `;` en el classpath. No ejecutar previamente
`CargarDatosEjemplo`: la demostración CU03 ya lo llama y exige una base vacía.
El comando no borra datos existentes; rechaza la carga si alguna tabla contiene
registros. Los ejemplos no se cargan al abrir la aplicación.

Iniciar sesión con la cuenta de prueba `laura.gomez` / `clave123`, abrir
Publicaciones y elegir «Evento finalizado de demostración». Enviar la solicitud,
cerrar sesión e ingresar como `admin.demo` / `solo-demostracion` para revisarla.
Probar rechazo con motivo o aprobación; volver a la cuenta del organizador para
consultar Mis solicitudes, Notificaciones y Plantillas.
El evento finalizado es una fixture de demostración; este comando no implementa
ni ejecuta el proceso de finalización CU05. Si ya hay datos reales, usar los
organizadores, eventos finalizados y administradores registrados, sin cargar
la demostración ni eliminar la base de datos.

### Verificación de CU03

- 91 pruebas JUnit en total después de incorporar main. Las 14 pruebas anteriores de CU03 ahora usan login
  real; se añadieron 9 de sesión, aislamiento entre cuentas, cierre de sesión,
  permisos de cada perfil y revocación del rol o desactivación después del login.
- Cobertura nueva: solicitud pendiente, propiedad del evento, estados, administrador
  con rol activo, motivo obligatorio, duplicados, publicación sin datos sensibles,
  conservación del original, notificaciones, rechazo y nuevo envío, persistencia,
  reversión por fallo y reintento.
- Prueba JavaFX con H2 temporal: solicitud bloqueada por estado, envío, rechazo sin
  motivo, rechazo válido, notificación, reenvío, fallo durante aprobación con
  conservación del formulario, reintento exitoso y consulta de plantilla.
- Inspección de capturas de las tres secciones de JavaFX.

En este equipo no hay Maven instalado: la verificación se ejecutó compilando con
`javac --release 17` y usando JUnit Platform Console 1.10.2, con las dependencias
JavaFX/H2 ya disponibles. En un entorno con Maven, usar `mvn clean test`.
Las pruebas usan bases temporales; no modifican los registros de la aplicación.
La comprobación visual se realizó en macOS; no se ha validado esta pantalla en Windows.

Se verificó además el recorrido JavaFX con sesiones reales: solicitud, bloqueo de
duplicados, rechazo con motivo, aviso al organizador, reenvío, aprobación y catálogo.
Queda pendiente la revisión del equipo de las tablas e interacciones que afectan
a Administración y a las notificaciones. El cierre institucional requiere esa
revisión y la integración del PR.

## CU04 — Cancelar evento

CU04 reutiliza `ControllerEventos`, `ServiceEventos` y los estilos CSS actuales.
La rama `CU04-cancelar-evento` incorpora la base autenticada de CU02 y CU03;
esa dependencia debe conservarse al preparar su PR. La primera iteración añadió
el repositorio, la pantalla y la migración de cancelación; esta integración con
la sesión no agrega tablas ni dependencias.

### Uso

Iniciar sesión como organizador y elegir **Cancelar evento** en la barra de
sesión, el menú lateral o el menú **Eventos**. El formulario muestra el nombre del
organizador autenticado, sin selector de identidad. Seleccionar uno de sus eventos
por nombre y marcar la confirmación explícita.
El botón permanece deshabilitado hasta confirmar. Cambiar de evento borra esa
confirmación para evitar cancelar otra selección por accidente.

Se bloquean eventos cancelados, finalizados, inexistentes o de otro organizador.
Al cancelar se liberan los horarios de servicios vinculados, se cancelan las
contrataciones activas, se libera el lugar cuando corresponde, se revocan las
invitaciones enviadas y se registran los avisos internos para proveedores e
invitados. No se modifica la aprobación del proveedor ni se eliminan invitados.
Los borradores y las invitaciones ya revocadas se conservan.

Todos los cambios se confirman juntos. Si falla una liberación o notificación,
se revierte la operación completa, se mantiene la selección del formulario y se
permite reintentar. La confirmación de éxito aparece solo después del commit.

### Vínculos de reservas e integración

La migración `003_cancelacion_eventos.sql` agrega:

- `Contratacion.id_disponibilidad`, para identificar el horario exacto que se libera.
- `Evento.id_lugar`, para identificar el lugar reservado mediante su clave real.
- La tabla `Disponibilidad_Servicio` con la estructura ya definida en la rama CU11.
  Si esa tabla existe, no se recrean sus datos.
- Destinatario `id_invitado` y referencia `id_evento` en `Notificacion_interna`.
  La restricción exige un destinatario: usuario o invitado, nunca ambos.

El módulo de contratación debe guardar `id_disponibilidad`; la reserva de lugar
debe guardar `id_lugar`. No se infieren esos vínculos de los textos ni se rellenan
identificadores arbitrarios en datos existentes. Cuando falta el vínculo directo,
CU04 admite la coincidencia exacta del servicio, fecha y horas que utiliza CU11;
el vínculo se guarda dentro de la misma transacción de cancelación. Se contemplan
contrataciones ACTIVA y CONFIRMADA. Si una contratación activa carece
de una reserva válida o una invitación apunta a un lugar reservado sin vínculo
coherente con el evento, se bloquea la cancelación sin modificar datos.

Una reserva compartida no se libera mientras otro evento mantenga un compromiso
activo, aunque ese contrato solo identifique el servicio, la fecha y las horas.
El estado global de Lugar permanece reservado si otro evento activo lo
utiliza; el modelo actual no contiene un calendario independiente de reservas de
lugares. Ese comportamiento debe coordinarse con el módulo responsable.

Los avisos para proveedores se dirigen a su Usuario. Invitado no tiene una cuenta
Usuario en el esquema actual: su aviso se guarda mediante `id_invitado`, sin
buscar cuentas por nombre o correo. La pantalla muestra el resumen de avisos del
evento. El proveedor los consulta en **Mis avisos** después de iniciar sesión.
El invitado entra por **Consultar mis invitaciones** en el login: CU16 consulta
los avisos por correo aunque la invitación haya sido revocada. Esa consulta por
correo es el mecanismo existente de CU16 y no autentica al invitado.

El controlador recibe la sesión del login. El servicio comprueba que el usuario
y el organizador sigan activos y vinculados en H2 antes de listar, cancelar o
consultar avisos. Las consultas filtran por el identificador de organizador de la
sesión, que puede diferir del identificador de usuario. Administradores,
proveedores y sesiones cerradas no tienen acceso a CU04. La cancelación vuelve a
comprobar la propiedad y el estado del evento dentro de la transacción.

Se incorporaron las ramas de CU11 y CU16 y se conectó la consulta de avisos.
Falta identificar la implementación de CU12/CU13 de Proveedores y el flujo de
reserva del lugar para verificar sus escrituras reales. La revisión de los
módulos y la definición del
acceso de invitados siguen pendientes. Detalles y contrato de datos en
[Integración de CU04](docs/integracion-cu04.md).

### Pruebas

La integración de proveedores e invitados amplía la suite a 117 pruebas JUnit,
incluidas 10 que cruzan los módulos. Las tres últimas verifican las reservas
compartidas sin vínculo directo y que una contratación de otra fecha no retenga
la disponibilidad cancelada. Se mantienen las 11 de cancelación adaptadas
al login real y 8 nuevas de integración con la sesión. Se comprueban confirmación, propiedad,
estados, reservas válidas, liberación sin afectar otros eventos, reserva compartida,
revocación, destinatarios, persistencia y rollback si falla un servicio o un aviso.
La prueba de liberación compartida comprueba que el último evento cancelado deja
el recurso disponible. Las nuevas pruebas cubren aislamiento de eventos y avisos,
identificadores distintos de usuario y organizador, cierre de sesión, roles no
autorizados, desactivación después del login, cambio de estado y confirmación
obligatoria. Las pruebas utilizan exclusivamente bases temporales.

La ventana JavaFX también se comprobó con una base temporal: login, selección de
eventos propios, confirmación obligatoria, cambio de selección que borra la
confirmación, fallo simulado, conservación del formulario, reintento, estado
actualizado y seis avisos registrados para los afectados de los ejemplos. Se
verificaron además el cambio de cuenta y la ausencia de acceso para el
administrador. La barra de sesión permite ajustar sus botones en varias filas.
Se inspeccionaron capturas de la pantalla antes y después de cancelar.

Con Maven instalado:

```bash
mvn clean test
mvn javafx:run
```

Como Maven no está instalado en este equipo, se verificó compilando con
`javac --release 17` y ejecutando JUnit Platform Console 1.10.2 con H2 2.3.232.
Las pruebas utilizan el login integrado y no tocan la base de la aplicación.

## CU02 — Editar evento

Implementación sencilla sobre el controlador, servicio y repositorio de eventos
existentes. La rama `CU02-editar-evento` parte del avance local de CU04 para mantener
la navegación disponible; conserva esa dependencia al preparar sus PRs.
No añade tablas ni dependencias.

Desde Crear evento, usar **Eventos → Editar evento** o el botón del menú lateral.
Iniciar sesión como organizador y seleccionar un evento propio por nombre. El
organizador se obtiene de la sesión y se muestra como texto, sin selector. Solo los eventos en estado de
planeación (`PLANEADO`) habilitan el formulario. El estado y la pertenencia se
consultan otra vez al precargar, y también se comprueban en el UPDATE al guardar.
Si otro proceso cambia el estado después de cargar el formulario, la edición se
bloquea sin sobrescribir los datos.

Campos permitidos: nombre, tipo, fecha/hora de inicio, fecha/hora de fin y lugar.
La edición de inicio y fin con hora fue confirmada por la usuaria para adaptar
la «fecha» de la especificación al modelo actual. Las horas admiten HH:mm; si un
registro previo contiene segundos, se precargan y conservan sin truncarlos.
Se aplican las mismas validaciones generales de CU01, sin duplicar sus reglas.

El objeto `EdicionEvento` contiene solo estos campos. El UPDATE no modifica
organizador, descripción, presupuesto, invitados, estado, fecha de creación ni
vínculos de reservas. Los valores históricos NULL de presupuesto e invitados se
conservan: no es necesario inventarlos para editar los datos generales. La
coordinación de horarios y reservas contratadas con los otros módulos deberá
validarse según las reglas que defina el equipo; este flujo no cambia contratos.

**Guardar cambios** confirma la edición; **Restablecer** vuelve a cargar los datos
actuales. Si falla el guardado, el UPDATE se revierte como una única operación
atómica, la base conserva sus datos anteriores y la pantalla mantiene lo escrito
para reintentar. Los mensajes son claros y no muestran errores SQL al usuario.

Se integró `feature/login-h2-compartido` (254d348). El controlador recibe la sesión
validada por el login; las consultas filtran en H2 por su identificador de
organizador, que puede ser distinto del identificador de usuario. Al listar,
precargar y guardar se comprueban el rol y los estados activos del usuario y del
organizador, además de su vínculo en la base. Cerrar sesión invalida el objeto de
sesión y vuelve al login. La aplicación no permite elegir otra identidad.

Desde la sesión de organizador están disponibles Crear y Editar evento. Crear
conserva el organizador de la sesión. CU03 también está disponible mediante Publicaciones, con permisos por sesión.
CU04 está disponible para el organizador desde Cancelar evento, con permisos por sesión.
La cuenta de administrador abre CU03; el proveedor accede a sus servicios y avisos.

### Pruebas de CU02

82 pruebas JUnit correctas tras integrar main, incluidas 8 de edición y 7 nuevas de
integración con la sesión. Las pruebas de edición
comprueban precarga y propiedad, campos permitidos, conservación de los demás
datos, estados bloqueados, cambio de estado después de cargar, entradas inválidas,
fallo técnico con reintento e históricos NULL. Las pruebas de sesión comprueban
login real, identificadores distintos, filtrado por cuenta, rechazo de eventos
ajenos, ausencia de sesión, roles no autorizados, cierre de sesión y desactivación
del usuario u organizador después del login. Las bases de prueba son temporales.

Se comprobó además la ventana JavaFX: precarga, rechazo de hora 24:00, conservación
de los datos ingresados ante un fallo simulado, guardado exitoso, preservación de
segundos en las horas y bloqueo de un evento que pasó a Confirmado. Se revisaron
capturas antes y después del guardado.

Con Maven instalado: `mvn clean test` y `mvn javafx:run`. En este equipo se compiló
con `javac --release 17` y se ejecutaron las pruebas mediante JUnit Platform Console
1.10.2, sin modificar la base de datos de la aplicación.

La comprobación JavaFX con H2 temporal también recorre login inválido y válido,
navegación a CU02, selección exclusiva de eventos propios, validación, guardado,
cierre de sesión y entrada con una segunda cuenta sin mezclar sus eventos.

La conciliación de CU03 con CU02 y main conserva el catálogo de proveedores, la
pantalla de gestión de servicios y la navegación a Invitados. Se mantienen las
migraciones de los módulos y el manejo de errores de autenticación de main.

### CU05 — finalizar evento

Se agregó la finalización de eventos propios confirmados que ya terminaron, con
fotografías guardadas en H2, liberación de reservas y entrega de la galería a los
invitados confirmados. Si falla la entrega interna se puede reintentar sin cargar
las fotos de nuevo ni duplicar avisos. Mis invitaciones permite consultar las fotos
con el correo y teléfono registrados.

Se accede desde **Mis eventos**, seleccionando un evento confirmado que ya terminó
y pulsando **Finalizar evento**. Si quedó una entrega pendiente, aparece
**Reintentar envío de galería**. La pantalla conserva el menú compartido, el evento
seleccionado y el cierre de sesión. El formulario de reseñas depende de CU14 y no
se implementa en esta rama. Las decisiones, dependencias y evidencias están en
[la documentación de CU05](docs/cu05-finalizar-evento.md).
