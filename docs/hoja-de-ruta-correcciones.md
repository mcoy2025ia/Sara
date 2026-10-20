# Hoja de ruta de correcciones — NextEventux

Origen: evaluación crítica del proyecto (base de datos primero). Cada commit tiene un responsable según la asignación vigente:

| Integrante | Casos de uso | Tablas |
| --- | --- | --- |
| Sara | CU-01 a CU-05, CU-30 | eventos, organizador, publicación, reseñas, mensajes |
| Valeria | CU-06, CU-08, CU-09, CU-28 | finanzas, usuarios y roles |
| Isabella | CU-11, CU-12, CU-13 | proveedores, servicios, contrataciones |
| Sofía | CU-16, CU-17, CU-20, CU-21, CU-23 | invitados, regalos, lugar, marketing |

**mcoy no realiza commits.** Ningún punto de este documento se le asigna.

Supuestos que el equipo debe confirmar: (a) CU-26 no aparece en la asignación; se deja provisionalmente con Sara. (b) Saúl y Juan Diego no figuran en la lista, por lo que no se les asigna nada.

---

## 1. Roadmap

Las fases se ejecutan en orden. Dentro de una fase, los commits se hacen en el orden indicado salvo que diga «paralelo».

| Fase | Objetivo | Commits |
| --- | --- | --- |
| 1. Fundación de la BD | Que la base sea estable, única y segura. Secuencial: todos tocan `DatabaseConnection` o el esquema. | C01 a C06 |
| 2. Autorización | Que ningún servicio funcione sin sesión. Paralelo. | C07 a C10 |
| 3. Estados | Un solo vocabulario de estados y `CHECK` en la base. | C11 a C14 |
| 4. Funcionalidad faltante | Escribir las tablas que hoy nadie llena, empezando por la contratación. | C15 a C22 |
| 5. Calidad | Consolidación, índices y deuda técnica. | C23 a C28 |

| Commit | Responsable | Resumen | Depende de |
| --- | --- | --- | --- |
| C01 | Sara | Ruta fija de la BD y registro de migraciones | — |
| C02 | Sara | Quitar datos de demostración de la migración 004_cu26 | C01 |
| C03 | Valeria | `Usuario.usuario` único | C01 |
| C04 | Sara | `Organizador.id_usuario` único | C01 |
| C05 | Isabella | `Proveedor.id_usuario` único | C01 |
| C06 | Valeria | Contraseñas con hash | C03 |
| C07 | Isabella | Sesión obligatoria en proveedores | C06 |
| C08 | Sofía | Sesión obligatoria en invitados (falla cerrado) | C06 |
| C09 | Sofía | Sesión y rol en marketing | C06 |
| C10 | Sara | Sesión en verificación de organizador y contratación de organizador | C06 |
| C11 | Isabella | Estados de solicitud, propuesta, contratación y servicio | C07 |
| C12 | Sara | Estados de evento y organizador | C11 |
| C13 | Sofía | Estados de invitación, invitado, regalo y reserva | C08 |
| C14 | Valeria | Estados de usuario, proveedor, movimiento y sanción | C11 |
| C15 | Valeria | Registrar movimientos financieros | C14 |
| C16 | Isabella | CU-13 Contratar servicio | C11, C15 |
| C17 | Valeria | CU-06 con datos reales | C16 |
| C18 | Valeria | CU-08 Paquetes de servicio | C14 |
| C19 | Valeria | CU-09 Sanciones | C16 |
| C20 | Sara | Reseñas al finalizar el evento (CU-05) | C16 |
| C21 | Sofía | Gestión de lugares (apoya CU-17) | C13 |
| C22 | Sofía | CU-23 Campañas de marketing | C13 |
| C23 | Valeria | Decidir `Permiso`, `Rol_permiso`, `Usuario_rol` | C14 |
| C24 | Valeria | Unificar notificaciones | C14, C16 |
| C25 | Sara, Valeria, Isabella, Sofía | Índices en claves foráneas (un commit cada una) | C14 |
| C26 | Isabella | `Servicio.precio` a `BigDecimal` | C11 |
| C27 | Sara | Quitar `System.out` y revisar `catch (Exception)` | — |
| C28 | Sara | Fotos del evento: decidir `BLOB` o ruta | C20 |

Revisión cruzada (CONTRIBUTING exige un revisor distinto del autor): Sara y Valeria se revisan entre sí; Isabella y Sofía se revisan entre sí.

---

## 2. Reglas para todos los commits

Esta carpeta no es un repositorio git; los pasos se hacen en el repositorio de GitHub del equipo.

```bash
git switch main
git pull
git switch -c <rama indicada en el commit>
# ... cambios ...
mvn clean test          # debe pasar completo antes del commit
git add <solo los archivos del commit>
git commit -m "<mensaje indicado>"
git push -u origin <rama>
```

- Un commit por tarea. No mezclar archivos de otras tareas.
- Abrir el Pull Request hacia `main` con título `[TIPO][MÓDULO] descripción`, plantilla completa y `Closes #N` si hay Issue.
- No fusionar sin la aprobación de la otra persona de la pareja de revisión.
- Después de fusionar, actualizar estado y horas en `Control_de_Alcance_NextEventux.xlsx`.
- **Migraciones nuevas:** usar el número asignado en este documento y agregarlas a la lista de `DatabaseConnection.inicializarEsquema()`. No basta con crear el archivo. Cuando exista C01, se registran en la tabla de versiones.
- Si una prueba existente deja de pasar por el cambio, se corrige la prueba en el mismo commit.
- Si `mvn` no está instalado en tu equipo, instálalo antes; sin pruebas ejecutadas no se hace el commit.

Números de migración reservados: 010 (Sara, C04), 011 (Valeria, C03), 012 (Isabella, C05), 013 (Isabella, C11), 014 (Sara, C12), 015 (Sofía, C13), 016 (Valeria, C14), 017 a 020 (índices, C25), 021 (Valeria, C24), 022 (Sofía, C22), 023 (Valeria, C18/C19).

---

## 3. Fase 1 — Fundación de la base de datos

### C01 — Ruta fija de la BD y registro de migraciones
- **Responsable:** Sara · **Rama:** `fix-database-ruta-y-version` · **Depende de:** —
- **Problema:** la URL `jdbc:h2:./data/nexteventux` depende del directorio de ejecución; lanzada desde otra carpeta crea una base vacía sin avisar. Además no se sabe qué migraciones corrieron.
- **Archivos:** `src/main/java/config/DatabaseConnection.java`, `README.md`, `src/test/java/MigracionesCU28Test.java` (si depende de la ruta).
- **Qué hacer:**
  1. Cambiar la URL por defecto a una ruta absoluta bajo `System.getProperty("user.home")` (por ejemplo `~/.nexteventux/nexteventux`), respetando `-Dnexteventux.db.url`.
  2. Crear la tabla `Esquema_version (nombre VARCHAR(150) PRIMARY KEY, aplicada TIMESTAMP DEFAULT CURRENT_TIMESTAMP)` en `inicializarEsquema()`.
  3. Dejar las migraciones 001 a 009 como están (siguen siendo repetibles). Las nuevas (010 en adelante) se ejecutan una sola vez: si su nombre ya está en `Esquema_version`, se omite; si no, se ejecuta y se registra en la misma transacción.
  4. Documentar en el README la nueva ubicación y cómo copiar la base antigua desde `data/`.
- **Pruebas:** añadir una prueba que abre la misma URL dos veces y comprueba que una migración nueva se registra una sola vez.
- **Commit:** `fix(database): fijar ruta de la base y registrar migraciones aplicadas`

### C02 — Quitar datos de demostración de la migración 004_cu26
- **Responsable:** Sara · **Rama:** `fix-database-migracion-cu26-sin-demo` · **Depende de:** C01
- **Problema:** `004_cu26_contratar_organizador.sql` actualiza organizadores por id fijo (1 a 5), los marca `VERIFICADO` e inserta reseñas en cada arranque.
- **Archivos:** `src/main/resources/migrations/004_cu26_contratar_organizador.sql`, `src/main/resources/seed.sql`.
- **Qué hacer:**
  1. Eliminar de la migración los cinco `UPDATE Organizador`, los dos `INSERT INTO Resena_Organizador` y comentarios asociados. Conservar los `ALTER`, `CREATE TABLE` y `CREATE INDEX`.
  2. Comprobar que `seed.sql` ya trae esos datos (organizadores 1 a 5 con ciudad, tarifa y `VERIFICADO`, y las reseñas). Si falta algo, añadirlo ahí.
  3. No intentar limpiar bases ya existentes.
- **Pruebas:** `DatosDemostracionH2Test`, `CU026ContratarOrganizadorTest`. Si alguna dependía de la migración, cargar el seed en la prueba.
- **Commit:** `fix(database): retirar datos de demostración de la migración CU-26`

### C03 — `Usuario.usuario` único
- **Responsable:** Valeria · **Rama:** `fix-database-usuario-unico` · **Depende de:** C01
- **Archivos:** nuevo `src/main/resources/migrations/011_usuario_unico.sql`, `DatabaseConnection.java` (lista), `ServiceAutenticacion`/`RepositoryAutenticacion` si crean usuarios.
- **Qué hacer:**
  1. La migración debe fallar con un mensaje claro si hay duplicados (consulta previa), sin borrar ni renombrar nada. Después, `CREATE UNIQUE INDEX IF NOT EXISTS uq_usuario_usuario ON Usuario(usuario)`.
  2. Decidir si la comparación ignora mayúsculas. Recomendado: normalizar a minúsculas al crear y al iniciar sesión.
  3. Revisar `CargarDatosEjemplo`, `CargarDemoPublicacion` y `CargarOrganizadoresPruebaCU30` para que no inserten duplicados.
- **Cómo registrarla:** C01 deja listos `ejecutarUnaVez(connection, "011_usuario_unico.sql", c -> verificarSinDuplicados(c, "Usuario", "usuario"))` y el helper; solo hay que añadir esa línea en `inicializarEsquema()` después de la de la 010.
- **Pruebas:** insertar dos usuarios iguales debe fallar; `ServiceAutenticacionH2Test` debe seguir pasando.
- **Commit:** `fix(database): exigir nombre de usuario único`

### C04 — `Organizador.id_usuario` único
- **Responsable:** Sara · **Rama:** `fix-database-organizador-usuario-unico` · **Depende de:** C01
- **Archivos:** nuevo `migrations/010_organizador_usuario_unico.sql`, `DatabaseConnection.java`.
- **Qué hacer:** igual que C03 pero sobre `Organizador(id_usuario)`: comprobar duplicados, fallar con mensaje claro, crear índice único.
- **Pruebas:** insertar dos organizadores para un mismo usuario debe fallar.
- **Commit:** `fix(database): impedir dos organizadores por usuario`

### C05 — `Proveedor.id_usuario` único
- **Responsable:** Isabella · **Rama:** `fix-database-proveedor-usuario-unico` · **Depende de:** C01
- **Archivos:** nuevo `migrations/012_proveedor_usuario_unico.sql`, `DatabaseConnection.java`, `CargarDatosEjemplo` (escenarios CU-28).
- **Qué hacer:** igual que C04 sobre `Proveedor(id_usuario)`. Verificar que los diez escenarios de CU-28 no crean dos proveedores por usuario.
- **Pruebas:** `RepositoryProveedorTest`, `ServiceProveedoresH2Test`.
- **Commit:** `fix(database): impedir dos proveedores por usuario`

### C06 — Contraseñas con hash
- **Responsable:** Valeria · **Rama:** `feat-administracion-hash-contrasenas` · **Depende de:** C03
- **Problema:** `RepositoryAutenticacion` compara `u.contraseña = ?` en texto plano; el seed trae `1234`, `clave123`, `claveadmin`.
- **Archivos:** `RepositoryAutenticacion.java`, `ServiceAutenticacion.java`, una clase nueva de utilidad (por ejemplo `services/Contrasenas.java`), `seed.sql`, `CargarDatosEjemplo`, `CargarDemoPublicacion`, `CargarOrganizadoresPruebaCU30`, pruebas de autenticación.
- **Qué hacer:**
  1. Usar PBKDF2 (`javax.crypto.SecretKeyFactory`, `PBKDF2WithHmacSHA256`) con sal aleatoria por usuario; guardar `pbkdf2$iteraciones$sal$hash` en la columna existente (cabe en `VARCHAR(255)`). No añadir dependencias.
  2. Cambiar el login: buscar al usuario por `usuario`, y verificar con comparación de tiempo constante.
  3. Compatibilidad: si el valor guardado no tiene el prefijo `pbkdf2$`, se compara en texto plano una última vez y, si coincide, se reescribe con hash (actualización al iniciar sesión). Así el seed puede seguir con claves simples sin romper nada.
  4. Los cargadores de ejemplo deben guardar con hash cuando sea posible.
  5. Nunca registrar contraseñas en logs (`CargarOrganizadoresPruebaCU30` imprime la clave común: dejarlo solo para entorno de pruebas y marcarlo).
- **Pruebas:** login correcto, incorrecto, usuario inexistente, y migración de una clave en texto plano a hash.
- **Commit:** `feat(administracion): almacenar contraseñas con hash PBKDF2`

---

## 4. Fase 2 — Autorización (paralelo)

Regla común: sin sesión, o con un rol que no corresponde, el servicio lanza excepción. Nunca se omite la validación porque la sesión sea `null`.

### C07 — Sesión obligatoria en proveedores
- **Responsable:** Isabella · **Rama:** `fix-proveedores-sesion-obligatoria` · **Depende de:** C06
- **Archivos:** `ServiceProveedores.java`, `ControllerProveedores.java`, `NextEventuxApp.java`, pruebas de proveedores.
- **Qué hacer:** el controlador recibe `SesionUsuario`. `crearServicio` ignora el `idProveedor` que llegue en los datos y usa el de la sesión. Todas las operaciones comprueban tipo `PROVEEDOR`, usuario activo y proveedor verificado. `listarServiciosDisponibles` (catálogo) queda para organizador.
- **Commit:** `fix(proveedores): exigir sesión y propiedad del servicio`

### C08 — Sesión obligatoria en invitados
- **Responsable:** Sofía · **Rama:** `fix-invitados-fallar-cerrado` · **Depende de:** C06
- **Problema:** en `ControllerInvitados.quitarInvitado`, si `organizadorEnSesion()` es `null` no se valida nada.
- **Archivos:** `ControllerInvitados.java`, `ServiceInvitados.java`, `LanzadorInvitados.java`, `LanzadorRespuestaInvitacion.java`.
- **Qué hacer:** las operaciones del organizador exigen sesión de organizador y propiedad del evento. Las operaciones públicas del invitado (consultar y responder por correo y teléfono) se separan explícitamente y no usan la sesión. Los lanzadores sueltos se limitan a pruebas manuales.
- **Commit:** `fix(invitados): rechazar operaciones sin sesión de organizador`

### C09 — Sesión y rol en marketing
- **Responsable:** Sofía · **Rama:** `fix-marketing-sesion-y-rol` · **Depende de:** C06
- **Archivos:** `ServiceMarketing.java`, `ControllerMarketing.java`, `ViewMarketing.java`, `MarketingLauncher.java`.
- **Qué hacer:** definir con el equipo qué rol usa marketing (propuesta: `ADMINISTRADOR`) y comprobarlo en el servicio.
- **Commit:** `fix(marketing): exigir sesión con rol autorizado`

### C10 — Sesión en verificación y contratación de organizador
- **Responsable:** Sara · **Rama:** `fix-administracion-sesion-organizador` · **Depende de:** C06
- **Archivos:** `ServiceVerificacionOrganizador.java` (hoy solo 1 referencia a sesión), `ServiceContratacionOrganizador.java` (0 referencias), sus controladores y vistas.
- **Qué hacer:** verificación (CU-30) solo para administrador activo, comprobado en el servicio como hace `ServiceAdministracion`. Contratación (CU-26) solo para cliente activo; la identidad sale de la sesión, no de parámetros.
- **Commit:** `fix(administracion): validar sesión en CU-26 y CU-30`

---

## 5. Fase 3 — Estados y restricciones

Cada persona unifica el vocabulario de **sus** tablas, deja constantes en una clase (por ejemplo `entities/EstadoContratacion.java`) y agrega `CHECK` en una migración que primero normaliza los valores antiguos.

### C11 — Estados de solicitud, propuesta, contratación y servicio
- **Responsable:** Isabella · **Rama:** `fix-proveedores-estados-unificados` · **Depende de:** C07
- **Archivos:** `migrations/013_estados_proveedores.sql`, `RepositoryContratacion`, `RepositorySolicitudEjecucion`, `RepositoryPropuestaServicio`, `RepositoryServicio`, `RepositoryCancelacion` y `RepositoryFinalizacion` (solo los literales de contratación).
- **Qué hacer:**
  1. Elegir una sola forma: `ACTIVA`, `CONFIRMADA`, `CANCELADA`, `FINALIZADA` para contrataciones (la mayoría del código ya la usa).
  2. En la migración, `UPDATE` de los valores antiguos y luego `ALTER TABLE ... ADD CONSTRAINT ... CHECK (estado IN (...))`.
  3. Sustituir literales por constantes.
  4. No tocar `Proveedor.estado`: lo hace Valeria en C14.
- **Pruebas:** `CancelacionTest`, `FinalizacionTest`, `ServiceSolicitudesH2Test`, `IntegracionCancelacionModulosTest`.
- **Commit:** `fix(proveedores): unificar estados de solicitudes y contrataciones`

### C12 — Estados de evento y organizador
- **Responsable:** Sara · **Rama:** `fix-eventos-estados-unificados` · **Depende de:** C11
- **Archivos:** `migrations/014_estados_eventos.sql`, `RepositoryEvento`, `RepositoryCancelacion`, `RepositoryFinalizacion`, `RepositoryOrganizador`.
- **Qué hacer:** `Evento.estado` ∈ `PLANEADO`, `CANCELADO`, `FINALIZADO` (confirmar la lista); `Organizador.estado` y `estado_verificacion` con `CHECK`. Constantes en lugar de literales. Misma migración con normalización previa.
- **Commit:** `fix(eventos): unificar estados de evento y organizador`

### C13 — Estados de invitados y regalos
- **Responsable:** Sofía · **Rama:** `fix-invitados-estados-unificados` · **Depende de:** C08
- **Archivos:** `migrations/015_estados_invitados.sql`, `RepositoryInvitacion`, `RepositoryInvitado`, `RepositoryRegalo`, `RepositoryReservaRegalo`.
- **Qué hacer:** `CHECK` en `Invitacion.estado_respuesta`, `Invitado.estado`, `Regalo.estado`, `Reserva_regalo.estado` tras normalizar.
- **Commit:** `fix(invitados): restringir estados de invitación, invitado y regalo`

### C14 — Estados de usuario, proveedor, movimiento y sanción
- **Responsable:** Valeria · **Rama:** `fix-finanzas-estados-unificados` · **Depende de:** C11
- **Archivos:** `migrations/016_estados_usuarios_finanzas.sql`, `ServiceAutenticacion`, `RepositoryProveedor`, `ServiceAdministracion`.
- **Qué hacer:** unificar `VERIFICADO`/`APROBADO` en `Proveedor.estado` (CU-28 aprueba como `VERIFICADO`; el login acepta ambos): normalizar a `VERIFICADO`, retirar la doble comprobación y poner `CHECK`. Lo mismo para `Usuario.estado`, `Movimiento_financiero.estado` y `Sancion.estado`. Avisar a Isabella antes de fusionar.
- **Commit:** `fix(administracion): unificar estados de usuario, proveedor y finanzas`

---

## 6. Fase 4 — Funcionalidad que hoy nadie escribe

Hoy ningún código inserta en `Contratacion`, `Sancion`, `Paquete_servicio`, `Campana`, `Envio_Campana`, `Puntos_fidelidad`, `Lugar`, `Resena`, `Respuesta_resena`, `Acompanante`, `Necesidad_Especial` ni `Movimiento_financiero`. Sin contratación, CU-04, CU-05 y CU-06 operan solo sobre datos de ejemplo.

### C15 — Registrar movimientos financieros
- **Responsable:** Valeria · **Rama:** `feat-finanzas-registrar-movimiento` · **Depende de:** C14
- **Archivos:** `RepositoryMovimientoFinanciero.java`, `ServiceFinanciera.java`, pruebas.
- **Qué hacer:** agregar `registrar(Connection, MovimientoFinanciero)` que reciba una conexión ya abierta, para que Isabella lo llame dentro de la transacción de C16. Validar que `valor` use `BigDecimal` y que tipo y estado sean los permitidos.
- **Commit:** `feat(finanzas): registrar movimientos financieros dentro de una transacción`

### C16 — CU-13 Contratar servicio
- **Responsable:** Isabella · **Rama:** `CU13-contratar-servicio` · **Depende de:** C11, C15
- **Archivos:** `RepositoryContratacion.java`, `ServiceProveedores` o un `ServiceContratacion` nuevo, `RepositoryPropuestaServicio`, `RepositoryDisponibilidadServicio`, una vista o acción en `ViewPropuestasOrganizador`.
- **Qué hacer:** a partir de una propuesta seleccionada, en **una transacción**: crear la `Contratacion` (con `id_propuesta`, `id_evento`, `valor_acordado`, fecha y horas del servicio), enlazar `id_disponibilidad`, marcar la disponibilidad como ocupada, registrar el movimiento (C15), reemplazar el `costo_comprometido` de la solicitud sin duplicarlo, y avisar al proveedor. Bloquear con `FOR UPDATE` el evento para respetar el presupuesto. Si cualquier paso falla, no se guarda nada.
- **Pruebas:** contratación exitosa, presupuesto insuficiente, horario ya ocupado, propuesta no vigente, reversión si falla el aviso.
- **Commit:** `feat(proveedores): implementar contratación de servicio CU-13`

### C17 — CU-06 con datos reales
- **Responsable:** Valeria · **Rama:** `CU06-estado-financiero-datos-reales` · **Depende de:** C16
- **Archivos:** `ServiceFinanciera`, `ViewFinanciera`, `ServiceFinancieraH2Test`.
- **Qué hacer:** probar el estado financiero con contrataciones creadas por C16 (no con el seed). Corregir lo que difiera.
- **Commit:** `fix(finanzas): validar CU-06 con contrataciones reales`

### C18 — CU-08 Paquetes de servicio
- **Responsable:** Valeria · **Rama:** `CU08-gestionar-paquetes` · **Depende de:** C14
- **Archivos:** nuevo `RepositoryPaquete`, `ServicePaquete`, controlador y vista; `migrations/023_paquetes.sql` solo si el diseño necesita columnas nuevas.
- **Qué hacer:** crear, editar y retirar paquetes de `Paquete_servicio` según el SRS. Confirmar con el SRS la relación paquete–servicios (hoy el esquema no la tiene) antes de programar.
- **Commit:** `feat(finanzas): implementar gestión de paquetes CU-08`

### C19 — CU-09 Sanciones
- **Responsable:** Valeria · **Rama:** `CU09-gestionar-sanciones` · **Depende de:** C16
- **Archivos:** nuevo `RepositorySancion`, `ServiceSancion`, controlador y vista.
- **Qué hacer:** registrar, consultar y resolver sanciones sobre `Sancion`, con `valor` en `BigDecimal` y reflejo en el movimiento financiero.
- **Commit:** `feat(finanzas): implementar sanciones por incumplimiento CU-09`

### C20 — Reseñas al finalizar el evento
- **Responsable:** Sara · **Rama:** `CU05-resenas-proveedores` · **Depende de:** C16
- **Problema:** CU-05 ofrece `proveedoresParaCalificar`, pero nada escribe en `Resena`.
- **Archivos:** `RepositoryFinalizacion`, `ServiceEventos`, `ViewFinalizacion`, nuevo `RepositoryResena`.
- **Qué hacer:** guardar la calificación (1 a 5) y comentario por contratación finalizada; impedir reseñas duplicadas y reseñas de contrataciones de otros eventos.
- **Commit:** `feat(eventos): guardar reseñas de proveedores al finalizar CU-05`

### C21 — Gestión de lugares
- **Responsable:** Sofía · **Rama:** `CU17-gestion-lugares` · **Depende de:** C13
- **Archivos:** `RepositoryLugar`, `ServiceInvitados`, `ViewInvitados`.
- **Qué hacer:** hoy `Lugar` solo se lee. Definir con el equipo quién da de alta lugares (propuesta: administrador) y agregar alta, edición y baja lógica. `Evento.id_lugar` debe guardarse al reservar.
- **Commit:** `feat(invitados): permitir alta y edición de lugares`

### C22 — CU-23 Campañas
- **Responsable:** Sofía · **Rama:** `CU23-crear-campana` · **Depende de:** C13
- **Archivos:** nuevo `RepositoryCampana`, `ServiceMarketing`, `ViewMarketing`, `migrations/022_campana_estado.sql` (añadir `estado` a `Campana` si el SRS lo exige).
- **Qué hacer:** crear campañas, enviar solo a clientes con `acepta_marketing = TRUE`, registrar `Envio_Campana`.
- **Commit:** `feat(marketing): implementar creación de campañas CU-23`

---

## 7. Fase 5 — Calidad

### C23 — Roles y permisos
- **Responsable:** Valeria · **Rama:** `fix-administracion-roles-permisos` · **Depende de:** C14
- `Permiso` y `Rol_permiso` no se leen en ningún lugar; los roles se resuelven con `tipo_usuario`. Proponer al equipo: eliminar las tablas sobrantes o usarlas de verdad. Registrar la decisión en `docs/` y aplicarla.
- **Commit:** `refactor(administracion): resolver uso de permisos y roles`

### C24 — Unificar notificaciones
- **Responsable:** Valeria (revisan Sara e Isabella) · **Rama:** `fix-database-notificaciones` · **Depende de:** C14, C16
- `Notificacion_interna` acumula `id_usuario`, `id_usuario_destinatario`, `id_invitado`, `id_proveedor` e `id_evento`, y existe además `Notificacion_Proveedor`. Definir un único modelo (destinatario usuario o invitado, tipo, evento opcional), migrar con `migrations/021_notificaciones.sql` sin perder avisos y actualizar `RepositoryProveedor`, `RepositoryOrganizador`, `RepositoryContratacion`, `RepositoryCancelacion` y `RepositoryAvisosProveedor`.
- **Commit:** `refactor(database): unificar el modelo de notificaciones internas`

### C25 — Índices en claves foráneas
- **Responsables:** uno por persona, cuatro commits, ramas `fix-database-indices-<nombre>`. Migraciones 017 (Sara), 018 (Valeria), 019 (Isabella), 020 (Sofía).
- Crear `CREATE INDEX IF NOT EXISTS` sobre las claves foráneas de sus propias tablas (por ejemplo `Evento(id_organizador)`, `Contratacion(id_evento)`, `Invitado(id_invitacion)`). Medir antes y después con una consulta frecuente.
- **Commit:** `fix(database): agregar índices en claves foráneas de <módulo>`

### C26 — `Servicio.precio` a `BigDecimal`
- **Responsable:** Isabella · **Rama:** `fix-proveedores-precio-bigdecimal` · **Depende de:** C11
- `entities/Servicio.java` y `RepositoryServicio.java` usan `double`; `ViewDetalleServicioProveedor` y `ViewListaServiciosProveedor` también. Cambiar a `BigDecimal` con dos decimales.
- **Commit:** `fix(proveedores): usar BigDecimal para el precio de servicios`

### C27 — Salida de consola y excepciones genéricas
- **Responsable:** Sara · **Rama:** `refactor-compartido-logs-y-excepciones`
- Reemplazar los `System.out.printf` de `Navegacion` y `NextEventuxApp` por `java.util.logging` o eliminarlos. Revisar los 22 `catch (Exception)`: capturar el tipo concreto o dejar de ocultar el error.
- **Commit:** `refactor(compartido): sustituir salida de consola y excepciones genéricas`

### C28 — Fotos del evento
- **Responsable:** Sara · **Rama:** `fix-eventos-fotos-almacenamiento` · **Depende de:** C20
- Hoy las fotos van como `BLOB` en H2 y los documentos por ruta. Decidir un criterio común con el equipo (recomendado: carpeta bajo `~/.nexteventux/` y solo la ruta en la base). Implementar la migración de las fotos existentes sin perderlas.
- **Commit:** `refactor(eventos): guardar fotografías por ruta en lugar de BLOB`

---

## 8. Deuda técnica sin commit asignado todavía

- Consultas a H2 en el hilo de JavaFX (ninguna usa `Task`): cada responsable las traslada a segundo plano en sus vistas, empezando por las que listan más datos.
- `RepositoryOrganizador` (1.614 líneas) y vistas de más de 1.000 líneas: dividir cuando las fases 1 a 4 estén fusionadas, para no generar conflictos.
- Acompañantes y necesidades especiales (`Acompanante`, `Necesidad_Especial`) no están en el compromiso actual de Sofía; incluirlos solo si el equipo los prioriza.

## 9. Lista de verificación antes de cada Pull Request

- [ ] `mvn clean test` pasa completo.
- [ ] La migración nueva está en la lista de `DatabaseConnection` y es repetible o está registrada.
- [ ] No hay datos de demostración en migraciones.
- [ ] No hay contraseñas, rutas locales ni secretos en el código.
- [ ] El mensaje de commit usa el formato `tipo(módulo): descripción`.
- [ ] La plantilla del PR está completa y la otra persona de la pareja lo revisó.
- [ ] Se actualizó `Control_de_Alcance_NextEventux.xlsx`.
