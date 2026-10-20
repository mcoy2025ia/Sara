# Navegación e interfaz compartidas

Las vistas autenticadas se muestran dentro de `NextEventuxApp` y las rutas se
registran en `Navegacion`. Cada pantalla principal se registra con
`registrar(...)`, por lo que su instancia y estado se conservan al volver.
Para cambiar el elemento mostrado en una ruta registrada, invalide esa pantalla
con `navegacion.invalidar(...)` antes de navegar. Use `registrarDinamica(...)`
solo si la pantalla debe crearse de nuevo en cada visita.

En una vista autenticada:

- Construya el menú con `MenuCompartido.crear(...)`, indicando el rol, la
  página activa y las acciones recibidas por constructor.
- Use `MenuCompartido.crearBarraSuperior(sesion, titulo)` para mostrar la
  misma ruta, altura y nombre de sesión.
- Use `MenuCompartido.crearLogo()` para reutilizar
  `/images/logo-nexteventux.png`.
- No cree opciones de menú sin una acción. Añada la ruta y su fábrica a
  `Navegacion.Pantalla` y al registro de rutas del rol correspondiente en
  `NextEventuxApp`; conecte las acciones con `navegacion.ir(...)`.
- Pase `SesionUsuario` a todos los componentes que muestran nombre, iniciales
  o rol. El cierre de sesión debe delegar en la acción común de la aplicación.

«Proveedores» es la ruta de catálogo del organizador. «Mis servicios» es la
ruta de gestión del proveedor; deben registrarse y navegarse por separado.
