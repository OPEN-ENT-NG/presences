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
        INSERT INTO presences_statistics.user(id, structure)
        VALUES (NEW.student_id, structureid)
        ON CONFLICT (id, structure) DO NOTHING;
    END IF;

    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;


CREATE TRIGGER statistics_on_event_handler
    AFTER INSERT OR UPDATE
    ON presences.event
    FOR EACH ROW
EXECUTE PROCEDURE presences.statistics_on_event_handler();