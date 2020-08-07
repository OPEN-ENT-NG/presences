CREATE SCHEMA presences_statistics;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE presences_statistics.scripts
(
    filename character varying(255)      NOT NULL,
    passed   timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT scripts_pkey PRIMARY KEY (filename)
);

CREATE TABLE presences_statistics.user
(
    id        character varying,
    structure character varying,
    PRIMARY KEY (id, structure)
);