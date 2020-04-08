DROP TABLE IF EXISTS presences.exemption_recursive CASCADE;
ALTER TABLE presences.exemption DROP COLUMN IF EXISTS recursive_id;
DROP VIEW IF EXISTS presences.exemption_view;

CREATE TABLE presences.exemption_recursive (
    id bigserial,
    student_id character varying (36) NOT NULL,
    structure_id character varying (36) NOT NULL,
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone NOT NULL,
    day_of_week character varying (36) ARRAY[4] NOT NULL,
    is_every_two_weeks boolean NOT NULL DEFAULT false,
    comment text,
    attendance boolean NOT NULL DEFAULT false,
    CONSTRAINT exemption_recursive_id_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_exemption_recursive_date
  ON presences.exemption_recursive
  USING btree
  (start_date ASC, end_date DESC);

CREATE INDEX idx_exemption_recursive_structure_id
  ON presences.exemption_recursive
  USING btree
  (structure_id);

ALTER TABLE presences.exemption
ADD COLUMN recursive_id bigint REFERENCES presences.exemption_recursive(id) ON DELETE CASCADE;

CREATE VIEW presences.exemption_view
AS (
SELECT
    exemption.id as exemption_id,
	null as exemption_recursive_id,
	exemption.structure_id as structure_id,
	exemption.start_date as start_date,
	exemption.end_date as end_date,
    exemption.student_id as student_id,
	exemption.comment as comment,
	exemption.subject_id as subject_id,
	exemption.recursive_id as recursive_id,
	null as day_of_week,
    null as is_every_two_weeks,
	exemption.attendance as attendance,
	'PONCTUAL' as type
	FROM
	presences.exemption AS exemption
UNION
SELECT
	null as exemption_id,
	exemption_recursive.id as exemption_recursive_id,
	exemption_recursive.structure_id as structure_id,
 	exemption_recursive.start_date as recursive_start_date,
	exemption_recursive.end_date as recursive_end_date,
	exemption_recursive.student_id as student_id,
	exemption_recursive.comment as comment,
	null as subject_id,
	null as recursive_id,
	exemption_recursive.day_of_week as day_of_week,
	exemption_recursive.is_every_two_weeks as is_every_two_weeks,
	exemption_recursive.attendance as attendance,
	'RECURSIVE' as type
	FROM
	presences.exemption_recursive as exemption_recursive
)