ALTER TABLE presences.register
    ALTER COLUMN course_id TYPE character varying(255),
    ALTER COLUMN subject_id TYPE character varying(255);

ALTER TABLE presences.exemption
    ALTER COLUMN subject_id TYPE character varying (255);