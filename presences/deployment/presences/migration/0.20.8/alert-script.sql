-- Update ABSENCE alert types
UPDATE presences.alerts as a
SET count = e.count -- assign the count result of the next FROM query
    FROM (
         -- We count absences by student_ids
         SELECT student_id, COUNT(*) as count
         FROM presences.event e
         WHERE type_id = 1 -- 1 == ABSENCE
           AND created >= '2021-09-02 00:00:00' -- we only get events from the school year begin
         GROUP BY student_id
     ) AS e
WHERE a.type = 'ABSENCE'
  AND a.student_id = e.student_id;

-- Update LATENESS alert types
UPDATE presences.alerts as a
SET count = e.count -- assign the count result of the next FROM query
    FROM (
         -- We count lateness by student_ids
         SELECT student_id, COUNT(*) as count
         FROM presences.event e
         WHERE type_id = 2 -- 2 == LATENESS
           AND created >= '2021-09-02 00:00:00' -- we only get events from the school year begin
         GROUP BY student_id
     ) AS e
WHERE a.type = 'LATENESS'
  AND a.student_id = e.student_id;

-- Update FORGOTTEN_NOTEBOOK alert types
UPDATE presences.alerts as a
SET count = fn.count -- assign the count result of the next FROM query
    FROM (
         -- We count forgotten notebooks by student_ids and structure_ids
         SELECT student_id, structure_id, COUNT(*) as count
         FROM presences.forgotten_notebook
         WHERE created >= '2021-09-02 00:00:00' -- we only get forgotten notebooks from the school year begin
         GROUP BY student_id, structure_id
     ) AS fn
WHERE a.type = 'FORGOTTEN_NOTEBOOK'
  AND a.student_id = fn.student_id
  AND a.structure_id = fn.structure_id;

-- Update INCIDENT alert types
UPDATE presences.alerts as a
SET count = i.count -- assign the count result of the next FROM query
    FROM (
         -- We count incidents by user_ids and structure_ids
         SELECT user_id, structure_id, COUNT(*) as count
         FROM incidents.incident
         INNER JOIN incidents.protagonist p on incident.id = p.incident_id
         WHERE created >= '2021-09-02 00:00:00' -- we only get forgotten notebooks from the school year begin
         GROUP BY user_id, structure_id
     ) AS i
WHERE a.type = 'INCIDENT'
  AND a.student_id = i.user_id
  AND a.structure_id = i.structure_id;