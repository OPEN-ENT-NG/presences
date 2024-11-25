CREATE TABLE presences.statement_absence (
    id bigserial,
    start_at timestamp without time zone NOT NULL,
    end_at timestamp without time zone NOT NULL,
    student_id character varying (36) NOT NULL,
    structure_id character varying (36) NOT NULL,
    description text,
    treated_at timestamp without time zone,
    validator_id character varying (36),
    attachment_id character varying (36),
    created_at timestamp without time zone DEFAULT now()
);