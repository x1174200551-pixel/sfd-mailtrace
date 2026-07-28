-- Backfill default flags for existing local databases that were created before
-- the "must keep one default" service guard was added.

UPDATE mt_sla_policy
SET is_default = 1,
    updated_by = 'SYSTEM'
WHERE id = (
    SELECT id
    FROM (
        SELECT id
        FROM mt_sla_policy
        WHERE is_deleted = 0
          AND is_enabled = 1
        ORDER BY id
        LIMIT 1
    ) candidate
)
  AND NOT EXISTS (
    SELECT 1
    FROM (
        SELECT id
        FROM mt_sla_policy
        WHERE is_deleted = 0
          AND is_enabled = 1
          AND is_default = 1
        LIMIT 1
    ) existing_default
);

UPDATE mt_work_calendar
SET is_default = 1,
    updated_by = 'SYSTEM'
WHERE id = (
    SELECT id
    FROM (
        SELECT id
        FROM mt_work_calendar
        WHERE is_deleted = 0
        ORDER BY id
        LIMIT 1
    ) candidate
)
  AND NOT EXISTS (
    SELECT 1
    FROM (
        SELECT id
        FROM mt_work_calendar
        WHERE is_deleted = 0
          AND is_default = 1
        LIMIT 1
    ) existing_default
);
