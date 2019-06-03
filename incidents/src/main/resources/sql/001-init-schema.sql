CREATE SCHEMA incidents;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE incidents.scripts (
    filename character varying (255) NOT NULL,
    passed timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT scripts_pkey PRIMARY KEY(filename)
);

CREATE TABLE incidents.place (
    id bigserial,
    structure_id character varying (36) NOT NULL,
    label text NOT NULL,
    CONSTRAINT place_pkey PRIMARY KEY (id)
);

CREATE TABLE incidents.seriousness (
    id bigserial,
    structure_id character varying (36) NOT NULL,
    label text NOT NULL,
    level bigint,
    CONSTRAINT seriousness_pkey PRIMARY KEY (id)
);

CREATE TABLE incidents.partner (
    id bigserial,
    structure_id character varying (36) NOT NULL,
    label text NOT NULL,
    CONSTRAINT partner_pkey PRIMARY KEY (id)
);

CREATE TABLE incidents.incident_type (
    id bigserial,
    structure_id character varying (36) NOT NULL,
    label text NOT NULL,
    CONSTRAINT incident_type_pkey PRIMARY KEY (id)
);

CREATE TABLE incidents.incident (
    id bigserial,
    owner character varying (36) NOT NULL,
    structure_id character varying (36) NOT NULL,
    date timestamp without time zone,
    selected_hour boolean NOT NULL DEFAULT false,
    description text,
    created timestamp without time zone,
    processed boolean NOT NULL DEFAULT false,
    place_id bigint,
    partner_id bigint,
    type_id bigint,
    seriousness_id bigint,
    CONSTRAINT incident_pkey PRIMARY KEY (id),
    CONSTRAINT fk_place_id FOREIGN KEY (place_id) REFERENCES incidents.place(id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_partner_id FOREIGN KEY (partner_id) REFERENCES incidents.partner(id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_type_id FOREIGN KEY (type_id) REFERENCES incidents.incident_type(id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_seriousness_id FOREIGN KEY (seriousness_id) REFERENCES incidents.seriousness(id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE incidents.protagonist_type (
    id bigserial,
    structure_id character varying (36) NOT NULL,
    label text NOT NULL,
    CONSTRAINT protagonist_type_pkey PRIMARY KEY (id)
);

CREATE TABLE incidents.protagonist (
    user_id character varying (36) NOT NULL,
    incident_id bigint,
    type_id bigint,
    CONSTRAINT protagonist_pkey PRIMARY KEY (user_id, incident_id),
    CONSTRAINT fk_incident_id FOREIGN KEY(incident_id) REFERENCES incidents.incident (id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_type_id FOREIGN KEY (type_id) REFERENCES incidents.protagonist_type (id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE incidents.punishment_type (
    id bigserial,
    structure_id character varying (36) NOT NULL,
    label text NOT NULL,
    type text,
    periode_required boolean NOT NULL DEFAULT false,
    CONSTRAINT punishment_type_pkey PRIMARY KEY (id)
);

CREATE TABLE incidents.punishment (
    id bigserial,
    owner character varying (36) NOT NULL,
    structure_id character varying (36) NOT NULL,
    date timestamp without time zone,
    supervisor character varying (36) NOT NULL,
    selected_hour boolean NOT NULL DEFAULT false,
    student_id character varying (36) NOT NULL,
    description text,
    processed boolean NOT NULL DEFAULT false,
    created timestamp without time zone,
    incident_id bigint,
    type_id bigint,
    CONSTRAINT fk_type_id FOREIGN KEY (type_id) REFERENCES incidents.punishment_type (id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_incident_id FOREIGN KEY (incident_id) REFERENCES incidents.incident (id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX idx_date
ON incidents.incident
USING btree
(date DESC);

CREATE INDEX idx_structure_id
ON incidents.incident
USING btree
(structure_id);

CREATE INDEX idx_indicent_id
ON incidents.protagonist
USING btree
(incident_id);