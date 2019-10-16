CREATE SCHEMA massmailing;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE massmailing.scripts (
    filename character varying(255) NOT NULL,
    passed timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT scripts_pkey PRIMARY KEY(filename)
);

CREATE TABLE massmailing.mailing (
    id bigserial,
    owner character varying(36) NOT NULL,
    student_id character varying(36) NOT NULL,
    structure_id character varying(36) NOT NULL,
    type character varying(50),
    content text NOT NULL,
    recipient_id character varying(36),
    recipient character varying(255),
    created timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT mailing_pkey PRIMARY KEY(id),
    CONSTRAINT type_check CHECK (type = 'MAIL')
);

CREATE TABLE massmailing.mailing_event (
    id bigserial,
    mailing_id bigint,
    event_id bigint,
    event_type character varying(50),
    CONSTRAINT mailing_event_pkey PRIMARY KEY(id),
    CONSTRAINT uniq_mailing_events UNIQUE (mailing_id, event_id, event_type)
);