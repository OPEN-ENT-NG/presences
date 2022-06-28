-- Delete old data due to the modification of the table
TRUNCATE TABLE presences.alerts;
TRUNCATE TABLE presences.alert_history;

ALTER TABLE presences.alerts DROP CONSTRAINT uniq_alert,
                             ADD CONSTRAINT uniq_alert UNIQUE (structure_id, student_id, type),
                             DROP COLUMN event_id,
                             ADD COLUMN exceed_date timestamp without time zone,
                             ADD COLUMN modified timestamp without time zone,
                             DROP COLUMN delete_at;
ALTER TABLE presences.alert_history ADD COLUMN exceed_date timestamp without time zone,
                                    ADD COLUMN modified timestamp without time zone,
                                    DROP COLUMN delete_at;