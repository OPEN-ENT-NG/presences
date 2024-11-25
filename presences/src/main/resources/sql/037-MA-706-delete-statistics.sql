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
        INSERT INTO presences_statistics.user(id, structure)
        VALUES (OLD.student_id, structureid)
        ON CONFLICT (id, structure) DO NOTHING;
    END IF;

    RETURN OLD;
END
$BODY$
    LANGUAGE plpgsql;


CREATE TRIGGER delete_statistics_on_event_handler
    AFTER DELETE
    ON presences.event
    FOR EACH ROW
EXECUTE PROCEDURE presences.delete_statistics_on_event_handler();