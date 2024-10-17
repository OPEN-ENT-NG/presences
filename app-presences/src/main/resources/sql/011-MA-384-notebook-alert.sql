CREATE TABLE presences.forgotten_notebook
(
    id  bigserial,
    date date NOT NULL,
    student_id character varying (36) NOT NULL,
    structure_id character varying (36) NOT NULL,
    CONSTRAINT forgotten_notebook_pkey PRIMARY KEY(id),
    CONSTRAINT uniq_forgotten_notebook UNIQUE (date, student_id, structure_id)
);

CREATE TABLE presences.alerts
(
    student_id character varying (36) NOT NULL,
    structure_id character varying (36) NOT NULL,
    absence bigint NOT NULL DEFAULT 0,
    lateness bigint NOT NULL DEFAULT 0,
    incident bigint NOT NULL DEFAULT 0,
    forgotten_notebook bigint NOT NULL DEFAULT 0,
    CONSTRAINT alerts_pkey PRIMARY KEY(student_id)
);

CREATE TABLE presences.alerts_settings
(
    structure_id character varying (36) NOT NULL,
    absence bigint NOT NULL DEFAULT 0,
    lateness bigint NOT NULL DEFAULT 0,
    incident bigint NOT NULL DEFAULT 0,
    forgotten_notebook bigint NOT NULL DEFAULT 0,
    CONSTRAINT alerts_settings_pkey PRIMARY KEY(structure_id)
);

-- Create alert if there is no alert with this student_id
CREATE OR REPLACE FUNCTION presences.create_alert(studentId varchar, structureId varchar) RETURNS void AS
	$BODY$
	DECLARE
        studentExist boolean;
    BEGIN
        SELECT exists(SELECT * FROM presences.alerts WHERE student_id = studentId) INTO studentExist;
		IF studentExist IS FALSE THEN
			INSERT INTO presences.alerts (student_id, structure_id) VALUES (studentId, structureId);
		END IF;
		RETURN;
	END;
	$BODY$
LANGUAGE plpgsql;

-- Incrementing notebook
CREATE OR REPLACE FUNCTION presences.increment_notebook_alert() RETURNS TRIGGER AS
    $BODY$
    BEGIN
		EXECUTE presences.create_alert(NEW.student_id, NEW.structure_id);
		UPDATE presences.alerts SET forgotten_notebook = (forgotten_notebook + 1)
		WHERE student_id = NEW.student_id AND structure_id = NEW.structure_id;
		RETURN NEW;
    END
    $BODY$
LANGUAGE plpgsql;

-- Decrementing notebook
CREATE OR REPLACE FUNCTION presences.decrement_notebook_alert() RETURNS TRIGGER AS
    $BODY$
    BEGIN
		EXECUTE presences.create_alert(OLD.student_id, OLD.structure_id);
		UPDATE presences.alerts SET forgotten_notebook = (forgotten_notebook - 1)
		WHERE student_id = OLD.student_id AND structure_id = OLD.structure_id;
		RETURN NEW;
    END
    $BODY$
LANGUAGE plpgsql;

CREATE TRIGGER increment_notebook_alert AFTER INSERT ON presences.forgotten_notebook
FOR EACH ROW EXECUTE PROCEDURE presences.increment_notebook_alert();
CREATE TRIGGER decrement_notebook_alert AFTER DELETE ON presences.forgotten_notebook
FOR EACH ROW EXECUTE PROCEDURE presences.decrement_notebook_alert();