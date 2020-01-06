CREATE OR REPLACE FUNCTION presences.exists_absence_in_day(date date, student_id character varying, structure_id character varying) RETURNS boolean AS
    $BODY$
    DECLARE
        count_event bigint;
    BEGIN
        SELECT count(event.id) as count
        FROM presences.event
        INNER JOIN presences.register ON (register.id = event.register_id)
        WHERE event.start_date::date = date
        AND register.structure_id = structure_id
        AND type_id = 1
        INTO count_event;

        RETURN count_event > 0;
    END
    $BODY$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION presences.exists_absence_in_half_day(date date, start_time time, end_time time, student_id character varying, structure_id character varying) RETURNS boolean AS
    $BODY$
    DECLARE
        count_event bigint;
    BEGIN
        SELECT count(event.id) as count
        FROM presences.event
        INNER JOIN presences.register ON (register.id = event.register_id)
        WHERE event.start_date::date = date
        AND event.start_date::time >= start_time
        AND event.start_date::time <= end_date
        AND type_id = 1
        INTO count_event;

        RETURN count_event > 0;
    END
    $BODY$
LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION presences.increment_event_alert() RETURNS TRIGGER AS
    $BODY$
    DECLARE
        structure_identifier character varying;
        recovery_method character varying;
        should_increment boolean;
        end_of_half_day time without time zone;
    BEGIN
        -- Retrieve event structure identifier based on new event register identifier
        SELECT structure_id FROM presences.register WHERE id = NEW.register_id INTO structure_identifier;

        CASE NEW.type_id
        WHEN 1 THEN -- Absence creation
            -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
            EXECUTE presences.create_alert(NEW.student_id, structure_identifier, 'ABSENCE');

            SELECT event_recovery_method FROM presences.settings WHERE structure_id = structure_identifier INTO recovery_method;

            CASE recovery_method
            WHEN 'HALF_DAY' THEN
                -- First retrieve half day hour
                SELECT end_of_half_day FROM viesco.time_slots WHERE id_structure = structure_identifier INTO end_of_half_day;
                if NEW.start_date::time < end_of_half_day THEN
                    SELECT !presences.exists_absence_in_half_day(NEW.start_date::date, '00:00:00'::time, end_of_half_day, NEW.student_id, structure_identifier) INTO should_increment;
                ELSE
                    SELECT !presences.exists_absence_in_half_day(NEW.start_date::date, end_of_half_day,'23:59:59'::time, NEW.student_id, structure_identifier) INTO should_increment;
                END IF;
            WHEN 'DAY' THEN
                -- Check if student already have absence for the day based on provided structure identifier
                SELECT !presences.exists_absence_in_day(NEW.start_date::date, NEW.student_id, structure_identifier) INTO should_increment;
            ELSE
                -- 'HOUR' is a classic case. Just increment count.
                should_increment = true;
            END CASE;

            IF should_increment THEN
                UPDATE presences.alerts SET count = (count + 1)
                WHERE student_id = NEW.student_id AND structure_id = structure_identifier AND type = 'ABSENCE';
            END IF;
        WHEN 2 THEN -- Lateness creation
            -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
            EXECUTE presences.create_alert(NEW.student_id, structure_identifier, 'LATENESS');

            -- Increment event alert number
            UPDATE presences.alerts SET count = (count + 1)
            WHERE student_id = NEW.student_id AND structure_id = structure_identifier AND type = 'LATENESS';
        END CASE;
        RETURN NEW;
    END
    $BODY$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION presences.decrement_event_alert() RETURNS TRIGGER AS
    $BODY$
    DECLARE
        structure_identifier character varying;
        recovery_method character varying;
        should_decrement boolean;
        end_of_half_day time without time zone;
        count_alert bigint;
    BEGIN
        -- Retrieve event structure identifier based on new event register identifier
        SELECT structure_id FROM presences.register WHERE id = OLD.register_id INTO structure_identifier;

        CASE OLD.type_id
        WHEN 1 THEN -- Absence creation
            -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
            EXECUTE presences.create_alert(OLD.student_id, structure_identifier, 'ABSENCE');

            SELECT event_recovery_method FROM presences.settings WHERE structure_id = structure_identifier INTO recovery_method;

            CASE recovery_method
            WHEN 'HALF_DAY' THEN
                -- First retrieve half day hour
                SELECT end_of_half_day FROM viesco.time_slots WHERE id_structure = structure_identifier INTO end_of_half_day;
                if OLD.start_date::time < end_of_half_day THEN
                    SELECT !presences.exists_absence_in_half_day(OLD.start_date::date, '00:00:00'::time, end_of_half_day, OLD.student_id, structure_identifier) INTO should_decrement;
                ELSE
                    SELECT !presences.exists_absence_in_half_day(OLD.start_date::date, end_of_half_day,'23:59:59'::time, OLD.student_id, structure_identifier) INTO should_decrement;
                END IF;
            WHEN 'DAY' THEN
                -- Check if student already have absence for the day based on provided structure identifier
                SELECT !presences.exists_absence_in_day(OLD.start_date::date, OLD.student_id, structure_identifier) INTO should_decrement;
            ELSE
                -- 'HOUR' is a classic case. Just increment count.
                should_decrement = true;
            END CASE;

            IF should_decrement THEN
                SELECT count FROM presences.alerts WHERE student_id = OLD.student_id AND structure_id = structure_identifier AND type = 'ABSENCE' INTO count_alert;

                IF count_alert > 0 THEN
                    UPDATE presences.alerts SET count = (count - 1)
                    WHERE student_id = OLD.student_id AND structure_id = structure_identifier AND type = 'ABSENCE';
                END IF;
            END IF;
        WHEN 2 THEN -- Lateness creation
            -- Check if alert exists. If it does not exists, it creates the alert based on provided student, structure and type information
            EXECUTE presences.create_alert(OLD.student_id, structure_identifier, 'LATENESS');

            SELECT count FROM presences.alerts WHERE student_id = OLD.student_id AND structure_id = structure_identifier AND type = 'LATENESS' INTO count_alert;
            IF count_alert > 0 THEN
                UPDATE presences.alerts SET count = (count - 1)
                WHERE student_id = OLD.student_id AND structure_id = structure_identifier AND type = 'LATENESS';
            END IF;
        END CASE;
        RETURN OLD;
    END
    $BODY$
LANGUAGE plpgsql;


CREATE TRIGGER increment_event_alert AFTER INSERT ON presences.event
FOR EACH ROW EXECUTE PROCEDURE presences.increment_event_alert();
CREATE TRIGGER decrement_event_alert AFTER DELETE ON presences.event
FOR EACH ROW EXECUTE PROCEDURE presences.decrement_event_alert();