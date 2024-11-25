CREATE OR REPLACE FUNCTION presences.delete_statistics_on_event_handler() RETURNS TRIGGER AS
$BODY$
DECLARE
    stat_exists boolean;
    structureid character varying;
BEGIN
    SELECT EXISTS(SELECT
                  FROM information_schema.tables
                  WHERE table_schema = 'presences_statistics'
                    AND table_name = 'user')
    INTO stat_exists;
    IF stat_exists THEN
        SELECT structure_id FROM presences.register WHERE id = OLD.register_id INTO structureid;

        INSERT INTO presences_statistics."user"(id, structure, modified)
        VALUES (OLD.student_id, structureid, OLD.start_date)
        ON CONFLICT (id, structure)
            DO UPDATE SET modified = LEAST(OLD.start_date, presences_statistics."user".modified)
            WHERE presences_statistics."user".modified > OLD.start_date;
    END IF;

    RETURN OLD;
END
$BODY$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION presences.statistics_on_event_handler() RETURNS TRIGGER AS
$BODY$
DECLARE
    stat_exists boolean;
    structureid character varying;
BEGIN
    SELECT EXISTS(SELECT
                  FROM information_schema.tables
                  WHERE table_schema = 'presences_statistics'
                    AND table_name = 'user')
    INTO stat_exists;
    IF stat_exists THEN
        SELECT structure_id FROM presences.register WHERE id = NEW.register_id INTO structureid;

        INSERT INTO presences_statistics."user"(id, structure, modified)
        VALUES (NEW.student_id, structureid, NEW.start_date)
        ON CONFLICT (id, structure)
            DO UPDATE SET modified = LEAST(NEW.start_date, presences_statistics."user".modified)
            WHERE presences_statistics."user".modified > NEW.start_date;
    END IF;

    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;
