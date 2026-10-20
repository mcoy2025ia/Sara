import config.DatabaseConnection;
import entities.Proveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryProveedor;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryProveedorTest {
    @TempDir Path directorio;
    private RepositoryProveedor repository;
    private String url;

    @BeforeEach
    void prepararBase() throws Exception {
        url = "jdbc:h2:" + directorio.resolve("proveedores_" + UUID.randomUUID());
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                    + "fecha_registro, acepta_marketing, tipo_usuario) VALUES "
                    + "(1, 'Proveedor pendiente', 'proveedor1', 'solo-prueba', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR'), "
                    + "(2, 'Proveedor aprobado', 'proveedor2', 'solo-prueba', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR')");
            statement.executeUpdate("INSERT INTO Proveedor "
                    + "(id_proveedor, id_usuario, nombre, empresa, correo, telefono, direccion, estado) VALUES "
                    + "(1, 1, 'Ana Proveedor', 'Flores Ana', 'ana@flores.test', '3001234567', 'Bogotá', 'PENDIENTE_VERIFICACION'), "
                    + "(2, 2, 'Carlos Proveedor', 'Sonido Carlos', 'carlos@sonido.test', '3101234567', 'Bogotá', 'VERIFICADO')");
        }
        repository = new RepositoryProveedor(url);
    }

    @Test
    void listaSoloProveedoresPendientes() {
        List<Proveedor> pendientes = repository.listarProveedoresPendientes();
        assertEquals(1, pendientes.size());
        assertEquals(1, pendientes.get(0).getIdProveedor());
        assertEquals("Ana Proveedor", pendientes.get(0).getNombre());
    }

    @Test
    void consultaProveedorPorIdentificador() {
        Proveedor proveedor = repository.buscarProveedor(1);
        assertNotNull(proveedor);
        assertEquals("Flores Ana", proveedor.getEmpresa());
        assertEquals("ana@flores.test", proveedor.getCorreo());
        assertNull(proveedor.getMotivoRechazo());
    }

    @Test
    void aprobarActualizaEstadoYLoRetiraDePendientes() {
        repository.actualizarEstadoProveedor(1, Proveedor.ESTADO_VERIFICADO);
        assertEquals(Proveedor.ESTADO_VERIFICADO, repository.buscarProveedor(1).getEstado());
        assertTrue(repository.listarProveedoresPendientes().isEmpty());
    }

    @Test
    void rechazoGuardaElMotivoYElEstadoEnLaMismaOperacion() {
        repository.rechazarProveedor(1, "La información registrada no coincide.");
        Proveedor rechazado = repository.buscarProveedor(1);
        assertEquals(Proveedor.ESTADO_RECHAZADO, rechazado.getEstado());
        assertEquals("La información registrada no coincide.", rechazado.getMotivoRechazo());
        assertTrue(repository.listarProveedoresPendientes().isEmpty());
    }

    @Test
    void noModificaUnProveedorQueYaNoEstaPendiente() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.actualizarEstadoProveedor(2, Proveedor.ESTADO_VERIFICADO));
        assertEquals("VERIFICADO", repository.buscarProveedor(2).getEstado());
    }
}
