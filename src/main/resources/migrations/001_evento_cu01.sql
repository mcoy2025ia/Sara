-- Los registros anteriores conservan NULL hasta disponer de sus datos reales.
ALTER TABLE Evento ADD COLUMN IF NOT EXISTS tipo VARCHAR(100);
ALTER TABLE Evento ADD COLUMN IF NOT EXISTS presupuesto DECIMAL(12,2) CHECK (presupuesto >= 0);
ALTER TABLE Evento ADD COLUMN IF NOT EXISTS invitados_iniciales INT CHECK (invitados_iniciales >= 0);
