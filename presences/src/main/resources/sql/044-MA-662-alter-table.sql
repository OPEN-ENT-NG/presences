-- Delete old data due to the modification of the table
DELETE FROM presences.alerts WHERE 1 = 1;
DELETE FROM presences.alert_history WHERE 1 = 1;

-- Override unique CONSTRAINT
ALTER TABLE presences.alerts ADD COLUMN event_id bigint NOT NULL;
ALTER TABLE presences.alerts DROP CONSTRAINT uniq_alert;
ALTER TABLE presences.alerts ADD CONSTRAINT uniq_alert UNIQUE (event_id, structure_id, student_id, type);


-- Delete column no longer used
ALTER TABLE presences.alerts DROP COLUMN exceed_date;
ALTER TABLE presences.alert_history DROP COLUMN exceed_date;

-- Add column for viesco params
ALTER TABLE presences.settings ADD COLUMN exclude_absence_no_reason boolean NOT NULL DEFAULT FALSE;
ALTER TABLE presences.settings ADD COLUMN exclude_lateness_no_reason boolean NOT NULL DEFAULT FALSE;
ALTER TABLE presences.settings ADD COLUMN exclude_absence_regularized boolean NOT NULL DEFAULT TRUE;
ALTER TABLE presences.settings ADD COLUMN exclude_absence_no_regularized boolean NOT NULL DEFAULT TRUE;
ALTER TABLE presences.settings ADD COLUMN exclude_forgotten_notebook boolean NOT NULL DEFAULT FALSE;

-- Add column for reason params
ALTER TABLE presences.reason ADD COLUMN exclude_reason boolean NOT NULL DEFAULT TRUE;

-- Add column for incidents params
ALTER TABLE incidents.seriousness ADD COLUMN exclude_seriousness boolean NOT NULL DEFAULT FALSE;