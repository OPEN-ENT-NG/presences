-- Delete existing table. Because the latest version is not in production we can easily delete this table.
DROP TABLE presences.alerts;

-- Create the new alert table.
CREATE TABLE presences.alerts (
    id bigserial,
    student_id character varying (36),
    structure_id character varying (36),
    type character varying,
    count bigint NOT NULL DEFAULT 0,
    exceed_date timestamp without time zone,
    modified timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT alerts_pkey PRIMARY KEY (id),
    CONSTRAINT check_type CHECK (type IN ('ABSENCE', 'LATENESS', 'INCIDENT', 'FORGOTTEN_NOTEBOOK')),
    CONSTRAINT uniq_alert UNIQUE(structure_id, student_id, type)
);

-- Create the alert history table.
CREATE TABLE presences.alert_history (
    id bigserial,
    student_id character varying (36),
    structure_id character varying (36),
    type character varying,
    count bigint NOT NULL DEFAULT 0,
    modified timestamp without time zone,
    exceed_date timestamp without time zone,
    date timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT alerts_history_pkey PRIMARY KEY (id)
);

-- Drop existing function. We replace it with a new one matching the new model.
DROP FUNCTION IF EXISTS presences.create_alert(studentId varchar, structureId varchar);

-- Create a new function that initialize an alert for given student.
CREATE OR REPLACE FUNCTION presences.create_alert(studentId varchar, structureId varchar, alertType varchar) RETURNS void AS
    $BODY$
    DECLARE
        studentExist boolean;
    BEGIN
        -- Check if student alert exists
        SELECT exists(SELECT * FROM presences.alerts WHERE student_id = studentId AND structure_id = structureId AND type = alertType) INTO studentExist;

        -- IF student alert does not exists, creates one.
        IF studentExist IS FALSE THEN
            INSERT INTO presences.alerts (student_id, structure_id, type) VALUES (studentId, structureId, alertType);
        END IF;
        RETURN;
    END;
    $BODY$
LANGUAGE plpgsql;