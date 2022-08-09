-- Manages whether to create or delete an alert following the modification of an incident
CREATE FUNCTION incidents.update_incident_alert() RETURNS TRIGGER AS
$BODY$
DECLARE
    newEventExclude boolean;
    oldAlertId bigint = NULL;
    protagonist incidents.protagonist;
BEGIN
    -- Check if the new event must be exclude
    SELECT incidents.incident_exclude_alert(NEW) INTO newEventExclude;

    IF NEW.date != OLD.date THEN
        UPDATE presences.alerts SET date = NEW.date WHERE event_id = NEW.id AND type = 'INCIDENT';
    end if;

    -- For each protagonist
    FOR protagonist IN SELECT * FROM incidents.protagonist WHERE incident_id = OLD.id
        LOOP
            -- Check if the old protagonist have alert
            SELECT event_id FROM presences.alerts WHERE event_id = OLD.id AND type = 'INCIDENT' AND student_id = protagonist.user_id AND structure_id = OLD.structure_id INTO oldAlertId;
            IF oldAlertId IS NOT NULL AND newEventExclude THEN -- If we have previously a alert and now the seriousness is exclude we must delete alert
                EXECUTE presences.delete_alert(OLD.id, 'INCIDENT', protagonist.user_id, NEW.structure_id);
            ELSIF oldAlertId IS NULL AND NOT newEventExclude THEN -- If we have not previously a alert and now the seriousness is not exclude we must create alert
                EXECUTE presences.create_alert(NEW.id, 'INCIDENT', protagonist.user_id, NEW.structure_id, NEW.date);
            END IF;
    END LOOP;
    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER update_incident_alert BEFORE UPDATE OF seriousness_id, date ON incidents.incident
    FOR EACH ROW EXECUTE PROCEDURE incidents.update_incident_alert();
