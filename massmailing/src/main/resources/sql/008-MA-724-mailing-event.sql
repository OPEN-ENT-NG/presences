ALTER TABLE massmailing.mailing_event
ALTER
COLUMN event_id
type character varying (36);

DROP TRIGGER IF EXISTS auto_massmail_events ON massmailing.mailing_event;

CREATE OR REPLACE FUNCTION massmailing.massmail_event() RETURNS TRIGGER AS
    $BODY$
    DECLARE

    BEGIN
        IF (NEW.event_type = 'ABSENCE' OR NEW.event_type = 'LATENESS') THEN
            UPDATE presences.event SET massmailed = true WHERE id = CAST(NEW.event_id AS int);
        END IF;

        RETURN NEW;
    END
    $BODY$
LANGUAGE plpgsql;

CREATE TRIGGER auto_massmail_events AFTER INSERT ON massmailing.mailing_event
FOR EACH ROW EXECUTE PROCEDURE massmailing.massmail_event();