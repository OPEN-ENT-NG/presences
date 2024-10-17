-- (ticket: MA-707, presence version: 0.12.7)
-- This script is use to regularize previous events that correspond to a regularized absence.
-- Due to previous problems of synchronisation between events and absences, we need to run it
-- to keep the logic up
UPDATE presences.event
SET counsellor_regularisation = true
WHERE presences.event.id in (
    SELECT e.id
    FROM presences.event e
             INNER JOIN presences.absence a
                        ON e.type_id = 1
                            AND (a.student_id = e.student_id)
                            AND ((a.start_date < e.end_date
                                AND e.start_date < a.end_date)
                                OR (e.start_date < a.end_date
                                    AND a.start_date < e.end_date))
    -- The date correspond to the beginning the prod delivery to the concerned synchronization fixture delivery (MA-707)
    WHERE a.end_date < 'Thu Nov 19 23:59:00 UTC 2020'
      AND a.counsellor_regularisation = true
      AND e.counsellor_regularisation = false
);