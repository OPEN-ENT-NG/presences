CREATE TABLE presences.discipline (
    id bigserial,
    structure_id character varying (36) NOT NULL,
    label text NOT NULL,
    hidden boolean NOT NULL DEFAULT false,
    CONSTRAINT discipline_pkey PRIMARY KEY (id)
);

CREATE TABLE presences.presence (
    id bigserial,
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    discipline_id bigint NOT NULL,
    owner character varying (36) NOT NULL,
    structure_id character varying (36) NOT NULL,
    CONSTRAINT presence_pkey PRIMARY KEY (id)
);

CREATE TABLE presences.presence_student (
    student_id character varying (36) NOT NULL,
    comment text,
    presence_id bigint NOT NULL REFERENCES presences.presence(id) ON DELETE CASCADE
);