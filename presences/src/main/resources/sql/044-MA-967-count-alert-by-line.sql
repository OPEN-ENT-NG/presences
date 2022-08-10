-- Delete old data due to the modification of the table
TRUNCATE TABLE presences.alerts;
TRUNCATE TABLE presences.alert_history;

-- Override unique CONSTRAINT
ALTER TABLE presences.alerts ADD COLUMN event_id bigint NOT NULL,
                             DROP CONSTRAINT uniq_alert,
                             ADD CONSTRAINT uniq_alert UNIQUE (event_id, structure_id, student_id, type),
                             -- Delete column no longer used
                             DROP COLUMN exceed_date,
                             DROP COLUMN modified,
                             -- Column used in trigger
                             ADD COLUMN date timestamp without time zone NOT NULL,
                             ADD COLUMN delete_at timestamp without time zone;

ALTER TABLE presences.alert_history DROP COLUMN exceed_date,
                                    DROP COLUMN modified,
                                    ADD COLUMN delete_at timestamp without time zone NOT NULL;