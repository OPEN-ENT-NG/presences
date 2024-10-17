DROP TRIGGER IF EXISTS increment_notebook_alert ON presences.forgotten_notebook;
DROP TRIGGER IF EXISTS decrement_notebook_alert ON presences.forgotten_notebook;
DROP FUNCTION IF EXISTS presences.increment_notebook_alert();
DROP FUNCTION IF EXISTS presences.decrement_notebook_alert();

CREATE OR REPLACE FUNCTION presences.increment_notebook_alert() RETURNS TRIGGER AS
    $BODY$
    BEGIN
        -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
        EXECUTE presences.create_alert(NEW.student_id, NEW.structure_id, 'FORGOTTEN_NOTEBOOK');

        -- Increment student forgotten_notebook alert
        UPDATE presences.alerts SET count = (count + 1)
        WHERE student_id = NEW.student_id AND structure_id = NEW.structure_id AND type = 'FORGOTTEN_NOTEBOOK';

        RETURN NEW;
    END
    $BODY$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION presences.decrement_notebook_alert() RETURNS TRIGGER AS
    $BODY$
    DECLARE
        countAlert bigint;
    BEGIN
        -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
        EXECUTE presences.create_alert(OLD.student_id, OLD.structure_id, 'FORGOTTEN_NOTEBOOK');

        -- Get alert count. We need it because we need to check if it is > 0
        SELECT count FROM presences.alerts WHERE student_id = OLD.student_id AND structure_id = OLD.structure_id AND type = 'FORGOTTEN_NOTEBOOK' INTO countAlert ;

        -- If count > 0 then update student alert for current type
        IF countAlert > 0 THEN
            UPDATE presences.alerts SET count = (count - 1)
            WHERE student_id = OLD.student_id AND structure_id = OLD.structure_id AND type = 'FORGOTTEN_NOTEBOOK';
        END IF;
        RETURN OLD;
    END
    $BODY$
LANGUAGE plpgsql;

CREATE TRIGGER increment_notebook_alert AFTER INSERT ON presences.forgotten_notebook FOR EACH ROW EXECUTE PROCEDURE presences.increment_notebook_alert();
CREATE TRIGGER decrement_notebook_alert AFTER DELETE ON presences.forgotten_notebook FOR EACH ROW EXECUTE PROCEDURE presences.decrement_notebook_alert();