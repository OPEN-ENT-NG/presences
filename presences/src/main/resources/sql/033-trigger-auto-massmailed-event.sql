CREATE OR REPLACE FUNCTION presences.auto_set_massmailed_event() RETURNS TRIGGER AS
    $BODY$
    DECLARE
        structure_identifier character varying;
        recovery_method character varying;
        end_of_half_day time without time zone;
        count_massmailed bigint;
        before_half_day boolean;
    BEGIN

    -- Retrieve structure_id from register where we create our event
    SELECT structure_id FROM presences.register WHERE id = NEW.register_id INTO structure_identifier;

    -- Retrieve recovery method based on structure_id fetched right abose us
    SELECT event_recovery_method FROM presences.settings WHERE structure_id = structure_identifier INTO recovery_method;

    -- Case for recovery method
    CASE recovery_method
        WHEN 'HALF_DAY' THEN
        -- Retrieve half day hour
        SELECT time_slots.end_of_half_day FROM viesco.time_slots WHERE id_structure = structure_identifier INTO end_of_half_day;

        -- Defining if it is before the half of day (before_half_day)
        SELECT NEW.start_date::time < end_of_half_day::time INTO before_half_day;

        if before_half_day THEN
            -- Morning case
            SELECT count(id) FROM presences.event WHERE massmailed = true
            AND student_id = NEW.student_id
            AND start_date::date = NEW.start_date::date AND start_date::time < end_of_half_day::time into count_massmailed;
        ELSE
            -- Afternoon case
            SELECT count(id) FROM presences.event WHERE massmailed = true
            AND student_id = NEW.student_id
            AND start_date::date = NEW.start_date::date AND start_date::time > end_of_half_day::time into count_massmailed;
        END IF;

            -- Setting our created new event massmailed TRUE if we have a matching
        IF count_massmailed > 0 THEN
          UPDATE presences.event SET massmailed = true WHERE id = NEW.id;
        END IF;

    WHEN 'DAY' THEN

        -- Check if we have at least one massmailed set TRUE at the same date
        SELECT count(id) massmailed FROM presences.event WHERE student_id = NEW.student_id AND massmailed = true
        AND start_date::date = NEW.start_date::date into count_massmailed;

        -- Setting our created new event massmailed TRUE if we have a matching
        IF count_massmailed > 0 THEN
            UPDATE presences.event SET massmailed = true WHERE id = NEW.id;
        END IF;
    ELSE
        RETURN NEW;
  END CASE;
       RETURN NEW;
    END
    $BODY$
LANGUAGE plpgsql;


CREATE TRIGGER auto_set_massmailed_event AFTER INSERT ON presences.event
FOR EACH ROW EXECUTE PROCEDURE presences.auto_set_massmailed_event();