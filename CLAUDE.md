# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Requires JDK 17+ and Maven (no Maven Wrapper in the repo). JavaFX 21 and H2 are Maven dependencies.

```bash
mvn clean test                                  # full JUnit suite
mvn -Dtest=ServiceEventosTest test              # single test class
mvn -Dtest=ServiceEventosTest#metodo test       # single test method
mvn javafx:run                                  # launch the desktop app (needs a GUI session)
mvn test-compile dependency:copy-dependencies   # prepare target/ for manual runs
```

There is no lint or formatter configured.

Manual runners (`config.CargarDatosEjemplo`, `PruebaCU01`, `config.TestSchema`, ...) are not JUnit tests. Run them after the command above, on Windows with `;` as classpath separator:

```bash
java -cp "target/classes:target/dependency/*" config.CargarDatosEjemplo
java -cp "target/classes:target/test-classes:target/dependency/*" PruebaCU01
```

Surefire only picks up `*Test.java` classes. Files named `Prueba*.java` are manual mains and are skipped by `mvn test`.

To point the app or a run at a different database, pass `-Dnexteventux.db.url=jdbc:h2:...`. The default is `jdbc:h2:./data/nexteventux`, relative to the working directory.

## Architecture

**Layers.** Each use case (CU-01 to CU-30) is implemented as a vertical slice through the same layers under `src/main/java`:

`views` (JavaFX) → `controllers` → `services` → `repositories` (raw JDBC) → H2 (`entities` are plain data objects passed between layers).

Most files follow the `ControllerX` / `ServiceX` / `RepositoryX` naming. Controllers are thin delegates that hold the `SesionUsuario`; services enforce business rules and session checks against H2 before every operation. Services have several constructors that default to new repositories. Tests use the explicit constructors to inject H2 test databases.

**Entry point and navigation.** `app.NextEventuxApp` opens on `ViewLogin`. After login it builds the role-specific shell (organizador, proveedor, invitado, administrador, ...). Authenticated screens are registered as `Navegacion.Pantalla` enum values with `registrar(...)` (cached instance) or `registrarDinamica(...)` (rebuilt on each visit). Call `navegacion.invalidar(...)` after changing data that a cached screen shows. `docs/navegacion-compartida.md` explains how to add a screen and the shared menu (`MenuCompartido`). Follow it instead of building menus per view.

**Database and schema.** `config.DatabaseConnection.getConnection()` is the only way to get a connection. On first use per JVM and URL it runs `schema.sql` followed by a hard-coded, ordered list of scripts in `src/main/resources/migrations/`.

- A new migration must be added to that list in `inicializarEsquema()`. Dropping a file into the folder does nothing.
- Migration filenames repeat prefixes (`002_`, `003_`, `004_` appear several times). The Java list defines the execution order, not the filenames.
- The database is file-based (`data/nexteventux.mv.db`, gitignored). Do not delete it to reset state, since it holds user records; use the explicit seed loaders instead.
- `seed.sql` and the `config.Cargar*` classes are opt-in. They are never loaded automatically on startup, and `CargarDatosEjemplo` refuses to run if tables already contain data.

**Tests.** JUnit 5 tests cover services, repositories, and schema/migrations. Several tests create their own temporary H2 databases. `DatabaseConnection.reiniciarInicializacion()` resets the per-JVM init cache and is used by migration tests.

## Conventions

- Code, identifiers, and domain terms are in Spanish (`Evento`, `Organizador`, `SolicitudEvento`), and so are the docs in `docs/` and `README.md`. Keep new names consistent with the surrounding code.
- The specification (SRS, use-case annexes, UML) is not in this repo. `README.md` says code must not contradict it without an approved change request. If a behavior is unclear, flag it rather than inventing a rule.
- Commit format from `CONTRIBUTING.md`: `tipo(módulo): descripción`, for example `feat(eventos): implementar CU-01`. Branches follow `CUXX-descripcion-corta`. Work is done on a branch from `main`, never directly on `main`.
- This folder is not a git repository. The branch and PR process in `CONTRIBUTING.md` applies to the team's GitHub repo.

## Ownership (team assignment as of the current sprint)

Use-case ownership determines who reviews a change and which module's DB tables it touches. Coordinate with the owner before changing their tables or services.

| Owner | Use cases | DB tables (per team list) |
| --- | --- | --- |
| Sara | CU-01 to CU-05, CU-30 (verify organizer) | 10: eventos, organizador, publicación, reseñas, mensajes |
| Valeria | CU-06, CU-08, CU-09, CU-28 (approve suppliers) | 8: finanzas, usuarios/roles |
| Isabella | CU-11, CU-12, CU-13 | 9: proveedores, servicios, contrataciones |
| Sofía | CU-16, CU-17, CU-20, CU-21, CU-23 (marketing) | 11: invitados, regalos, lugar, marketing |

Note: `README.md` and `GUIA_IMPLEMENTACION_Y_VALIDACION.md` assign differently (Saúl for Marketing CU-21 to CU-25, Juan Diego for Administración CU-26 to CU-30, and CU-28 to Juan Diego). Treat the table above as current unless the team says otherwise; the docs are out of date on this point.
