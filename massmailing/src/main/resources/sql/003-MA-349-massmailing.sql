ALTER TABLE massmailing.mailing
DROP COLUMN owner;

ALTER TABLE massmailing.mailing_event
ADD CONSTRAINT fk_mailing_id FOREIGN KEY (mailing_id) REFERENCES massmailing.mailing(id);

CREATE OR REPLACE FUNCTION massmailing.massmail_event() RETURNS TRIGGER AS
    $BODY$
    DECLARE

    BEGIN
        IF (NEW.event_type = 'ABSENCE' OR NEW.event_type = 'LATENESS') THEN
            UPDATE presences.event SET massmailed = true WHERE id = NEW.event_id;
        END IF;

        RETURN NEW;
    END
    $BODY$
LANGUAGE plpgsql;

CREATE TRIGGER auto_massmail_events AFTER INSERT ON massmailing.mailing_event
FOR EACH ROW EXECUTE PROCEDURE massmailing.massmail_event();