CREATE TABLE presences.collective_absence (
    id bigserial,
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone NOT NULL,
    counsellor_regularisation boolean NOT NULL DEFAULT false,
    comment text,
    reason_id bigint,
    owner_id character varying(36),
    structure_id character varying (36) NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    CONSTRAINT collective_absence_pkey PRIMARY KEY (id),
    CONSTRAINT fk_reason_id FOREIGN KEY (reason_id) REFERENCES presences.reason (id)
);

CREATE TABLE presences.rel_audience_collective
(
    collective_id bigint,
    audience_id   character varying(36),
    CONSTRAINT rel_group_collective_pkey PRIMARY KEY (collective_id, audience_id),
    CONSTRAINT fk_collective_id FOREIGN KEY (collective_id) REFERENCES presences.collective_absence (id)
        MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

ALTER TABLE presences.absence
    ADD COLUMN collective_id bigint REFERENCES presences.collective_absence(id);
