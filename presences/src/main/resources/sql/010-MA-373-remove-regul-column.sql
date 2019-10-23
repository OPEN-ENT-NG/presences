ALTER TABLE presences.reason
DROP COLUMN regularisable;

CREATE OR REPLACE FUNCTION regularize_event() RETURNS TRIGGER AS
    $BODY$
    DECLARE
        provingReason boolean;
    BEGIN
        IF NEW.reason_id IS NOT NULL THEN
            SELECT reason.proving FROM presences.reason WHERE id = NEW.reason_id INTO provingReason;
            NEW.counsellor_regularisation = provingReason;
        ELSE
            NEW.counsellor_regularisation = false;
        END IF;

        RETURN NEW;
    END
    $BODY$
LANGUAGE plpgsql;

CREATE TRIGGER regularize_event BEFORE INSERT OR UPDATE OF reason_id ON presences.event
FOR EACH ROW EXECUTE PROCEDURE regularize_event();

UPDATE presences.event
SET counsellor_regularisation = true
WHERE reason_id IN (
	SELECT id
	FROM presences.reason
	WHERE proving = true
);