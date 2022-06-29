-- Add column for incidents params
ALTER TABLE incidents.seriousness ADD COLUMN exclude_alert_seriousness boolean NOT NULL DEFAULT FALSE;