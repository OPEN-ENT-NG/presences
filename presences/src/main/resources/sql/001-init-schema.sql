CREATE SCHEMA presences;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE presences.scripts (
    filename character varying (255) NOT NULL,
    passed timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT scripts_pkey PRIMARY KEY(filename)
);

CREATE TABLE presences.etablissements_actifs (
    id_etablissement character varying (36),
    actif boolean NOT NULL,
    CONSTRAINT active_structure_pkey PRIMARY KEY (id_etablissement)
);

CREATE TABLE presences.register_state (
    id bigserial,
    structure_id character varying (36) NOT NULL,
    label text NOT NULL,
    CONSTRAINT register_state_pkey PRIMARY KEY(id)
);

CREATE TABLE presences.register_proof (
    id bigserial,
    structure_id character varying (36) NOT NULL,
    label text NOT NULL,
    CONSTRAINT register_proof_pkey PRIMARY KEY(id)
);

CREATE TABLE presences.register (
    id bigserial,
    personnel_id character varying (36) NOT NULL,
    course_id bigint NOT NULL,
    state_id bigint NOT NULL,
    proof_id bigint,
    counsellor_input boolean,
    owner character varying (36),
    created timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT register_pkey PRIMARY KEY (id),
    CONSTRAINT fk_state_id FOREIGN KEY (state_id) REFERENCES presences.register_state (id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_proof_id FOREIGN KEY (proof_id) REFERENCES presences.register_proof (id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE presences.reason_type (
    id bigserial,
    label text NOT NULL,
    structure_id character varying (36) NOT NULL,
    CONSTRAINT reason_type_pkey PRIMARY KEY (id)
);

CREATE TABLE presences.reason (
    id bigserial,
    structure_id character varying (36) NOT NULL,
    label text NOT NULL,
    proving boolean NOT NULL DEFAULT false,
    comment text,
    "default" boolean NOT NULL DEFAULT false,
    "group" boolean NOT NULL DEFAULT false,
    type_id bigint,
    CONSTRAINT reason_pkey PRIMARY KEY (id),
    CONSTRAINT fk_reason_type_id FOREIGN KEY (type_id) REFERENCES presences.reason_type (id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE presences.event_type (
    id bigserial,
    label text,
    CONSTRAINT event_type_pkey PRIMARY KEY (id)
);

CREATE TABLE presences.event (
    id bigserial,
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    comment text,
    counsellor_input boolean NOT NULL DEFAULT false,
    student_id character varying (36) NOT NULL,
    register_id bigint NOT NULL,
    type_id bigint NOT NULL,
    reason_id bigint,
    owner character varying (36) NOT NULL,
    created timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT event_pkey PRIMARY KEY (id),
    CONSTRAINT fk_register_id FOREIGN KEY (register_id) REFERENCES presences.register (id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_event_type_id FOREIGN KEY (type_id) REFERENCES presences.event_type (id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_reason_id FOREIGN KEY (reason_id) REFERENCES presences.reason (id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE presences.exemption (
    id bigserial,
    student_id character varying (36) NOT NULL,
    structure_id character varying (36) NOT NULL,
    subject_id character varying (36) NOT NULL,
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone NOT NULL,
    comment text,
    attendance boolean NOT NULL DEFAULT false,
    CONSTRAINT exemption_pkey PRIMARY KEY (id)
);

CREATE TABLE presences.testimony (
    id bigserial,
    label text,
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone NOT NULL,
    comment text,
    student_id character varying (36) NOT NULL,
    structure_id character varying (36) NOT NULL,
    processed boolean NOT NULL DEFAULT false,
    fullday boolean NOT NULL DEFAULT false,
    owner character varying  (36) NOT NULL,
    created timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT testimony_pkey PRIMARY KEY (id)
);

CREATE TABLE presences.testimony_attachment (
    id character varying(36),
    testimony_id bigint NOT NULL,
    CONSTRAINT testimony_attachement_pkey PRIMARY KEY (id),
    CONSTRAINT fk_testimony_id FOREIGN KEY (testimony_id) REFERENCES presences.testimony (id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);