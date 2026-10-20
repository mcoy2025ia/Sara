-- Importe acordado al solicitar, independiente de futuros cambios del catálogo.
ALTER TABLE Solicitud_Ejecucion ADD COLUMN IF NOT EXISTS costo_comprometido DECIMAL(12,2)
    CHECK (costo_comprometido >= 0);
