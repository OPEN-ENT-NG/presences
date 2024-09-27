CREATE OR REPLACE FUNCTION presences.get_thresholder_fieldname(type character varying) RETURNS character varying AS
    $$
    DECLARE
        fieldname character varying;
    BEGIN
        CASE type
            WHEN 'ABSENCE' THEN
                fieldname = 'alert_absence_threshold';
            WHEN 'LATENESS' THEN
                fieldname = 'alert_lateness_threshold';
            WHEN 'INCIDENT' THEN
                fieldname = 'alert_incident_threshold';
            WHEN 'FORGOTTEN_NOTEBOOK' THEN
                fieldname = 'alert_forgotten_notebook_threshold';
            END CASE;

            RETURN fieldname;
    END;
    $$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION presences.update_modified_and_compute_exceed_date() RETURNS TRIGGER AS
    $BODY$
    DECLARE
        thresholder_field_name character varying;
        thresholder bigint;
    BEGIN
        -- Update modified date
        UPDATE presences.alerts SET modified = now() WHERE id = NEW.id;

        SELECT presences.get_thresholder_fieldname(NEW.type) INTO thresholder_field_name;

        -- Retrieve thresholder
        EXECUTE 'SELECT ' || thresholder_field_name || ' FROM presences.settings WHERE structure_id = $1' INTO thresholder USING NEW.structure_id;

        -- If count < thresholder then remove exceed date
        IF NEW.count < thresholder THEN
           UPDATE presences.alerts SET exceed_date = NULL WHERE id = NEW.id;
        END IF;

        -- If count = thresholder then set exceed date
        IF NEW.count = thresholder THEN
           UPDATE presences.alerts SET exceed_date = now() WHERE id = NEW.id;
        END IF;

        -- If count > thresholder do nothing
        RETURN NEW;
    END
    $BODY$
LANGUAGE plpgsql;

CREATE TRIGGER update_modified_and_compute_exceed_date AFTER UPDATE OF count ON presences.alerts FOR EACH ROW EXECUTE PROCEDURE presences.update_modified_and_compute_exceed_date();

CREATE OR REPLACE FUNCTION presences.save_alert() RETURNS TRIGGER AS
    $BODY$
    BEGIN
        IF OLD.exceed_date IS NOT NULL THEN
            INSERT INTO presences.alert_history(student_id, structure_id, type, count, modified, exceed_date)
            VALUES (OLD.student_id, OLD.structure_id, OLD.type, OLD.count, OLD.modified, OLD.exceed_date);
        END IF;

        RETURN OLD;
    END
    $BODY$
LANGUAGE plpgsql;

CREATE TRIGGER save_alert_on_reset AFTER DELETE ON presences.alerts FOR EACH ROW EXECUTE PROCEDURE presences.save_alert();