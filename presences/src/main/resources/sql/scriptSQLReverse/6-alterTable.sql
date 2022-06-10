ALTER TABLE presences.alerts DROP CONSTRAINT uniq_alert;
ALTER TABLE presences.alerts ADD CONSTRAINT uniq_alert UNIQUE (structure_id, student_id, type);
ALTER TABLE presences.alerts DROP COLUMN event_id; --check old data
ALTER TABLE presences.alerts ADD COLUMN exceed_date timestamp without time zone;
ALTER TABLE presences.alert_history ADD COLUMN exceed_date timestamp without time zone;
ALTER TABLE presences.settings DROP COLUMN exclude_absence_no_reason;
ALTER TABLE presences.settings DROP COLUMN exclude_lateness_no_reason;
ALTER TABLE presences.settings DROP COLUMN exclude_absence_regularized;
ALTER TABLE presences.settings DROP COLUMN exclude_absence_no_regularized;
ALTER TABLE presences.settings DROP COLUMN exclude_forgotten_notebook;
ALTER TABLE presences.reason DROP COLUMN exclude_reason;
ALTER TABLE incidents.seriousness DROP COLUMN exclude_seriousness;