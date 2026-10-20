
package entities;

import java.math.BigDecimal;

/**
 * Representa un organizador y la informacion que el cliente
 * necesita consultar para comparar perfiles en el CU-026.
 *
 * El constructor de dos parametros se conserva para mantener
 * compatibilidad con el modulo de eventos existente.
 */
public record Organizador(
        int id,
        String nombre,
        String empresa,
        String ciudad,
        String especialidad,
        String portafolio,
        BigDecimal tarifaBase,
        BigDecimal calificacionPromedio,
        int totalResenas) {

    /**
     * Constructor compatible con las pantallas existentes
     * que solo necesitan el identificador y el nombre.
     */
    public Organizador(int id, String nombre) {
        this(
                id,
                nombre,
                "",
                "",
                "",
                "",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0
        );
    }

    @Override
    public String toString() {
        return nombre;
    }
}
