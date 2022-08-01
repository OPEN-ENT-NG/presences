INSERT INTO presences_statistics.user (id, structure)
SELECT DISTINCT student_id, structure_id
FROM presences.event
         INNER JOIN presences.register ON (event.register_id = register.id)

