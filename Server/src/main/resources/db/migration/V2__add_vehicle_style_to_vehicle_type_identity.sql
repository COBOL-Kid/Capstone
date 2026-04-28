ALTER TABLE vehicle_type
    ADD COLUMN IF NOT EXISTS vehicle_style varchar(160);

UPDATE vehicle_type
SET vehicle_style = 'UNKNOWN'
WHERE vehicle_style IS NULL OR btrim(vehicle_style) = '';

ALTER TABLE vehicle_type
    ALTER COLUMN vehicle_style SET DEFAULT 'UNKNOWN',
    ALTER COLUMN vehicle_style SET NOT NULL;

DO $$
DECLARE
    old_constraint_name text;
BEGIN
    SELECT constraint_name
    INTO old_constraint_name
    FROM (
        SELECT c.conname AS constraint_name,
               array_agg(a.attname::text ORDER BY a.attname::text) AS column_names
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY (c.conkey)
        WHERE t.relname = 'vehicle_type'
          AND c.contype = 'u'
        GROUP BY c.conname
    ) constraints
    WHERE column_names = ARRAY['vehicle_make', 'vehicle_model', 'vehicle_trim', 'vehicle_year']
    LIMIT 1;

    IF old_constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE vehicle_type DROP CONSTRAINT %I', old_constraint_name);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'vehicle_type'
          AND c.conname = 'uk_vehicle_type_identity'
    ) THEN
        ALTER TABLE vehicle_type
            ADD CONSTRAINT uk_vehicle_type_identity UNIQUE (
                vehicle_year,
                vehicle_make,
                vehicle_model,
                vehicle_trim,
                vehicle_style
            );
    END IF;
END $$;
