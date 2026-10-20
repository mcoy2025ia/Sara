-- CU-028: conserva el motivo indicado cuando un proveedor es rechazado.
-- IF NOT EXISTS permite aplicar la migración en bases existentes sin perder datos.
ALTER TABLE Proveedor ADD COLUMN IF NOT EXISTS motivo_rechazo VARCHAR(500);
