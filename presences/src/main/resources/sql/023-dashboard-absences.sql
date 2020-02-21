CREATE OR REPLACE FUNCTION presences.spread_absence_reason() RETURNS TRIGGER AS
$BODY$
BEGIN
    UPDATE presences.event
    SET reason_id = NEW.reason_id
    FROM presences.register
    WHERE register.structure_id = NEW.structure_id
      AND event.student_id = NEW.student_id
      AND event.start_date >= NEW.start_date
      AND event.end_date <= NEW.end_date;

    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION presences.spread_absence_regularisation() RETURNS TRIGGER AS
$BODY$
BEGIN
    UPDATE presences.event
    SET counsellor_regularisation = NEW.counsellor_regularisation
    FROM presences.register
    WHERE register.structure_id = NEW.structure_id
      AND event.student_id = NEW.student_id
      AND event.start_date >= NEW.start_date
      AND event.end_date <= NEW.end_date;

    RETURN NEW;
END
$BODY$
    LANGUAGE plpgsql;

CREATE TRIGGER spread_absence_reason
    AFTER UPDATE OF reason_id
    ON presences.absence
    FOR EACH ROW
EXECUTE PROCEDURE presences.spread_absence_reason();
CREATE TRIGGER spread_absence_regularisation
    AFTER UPDATE OF counsellor_regularisation
    ON presences.absence
    FOR EACH ROW
EXECUTE PROCEDURE presences.spread_absence_regularisation();