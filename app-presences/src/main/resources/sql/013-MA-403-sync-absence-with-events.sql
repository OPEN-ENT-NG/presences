ALTER TABLE presences.absence
    ADD COLUMN structure_id character varying (36),
    ADD COLUMN counsellor_regularisation boolean NOT NULL DEFAULT false;

CREATE OR REPLACE FUNCTION presences.regularize_absences() RETURNS TRIGGER AS
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

CREATE TRIGGER regularize_absences BEFORE INSERT OR UPDATE OF reason_id ON presences.absence
FOR EACH ROW EXECUTE PROCEDURE presences.regularize_absences();

UPDATE presences.absence
SET counsellor_regularisation = true
WHERE reason_id IN (
	SELECT id
	FROM presences.reason
	WHERE proving = true
);