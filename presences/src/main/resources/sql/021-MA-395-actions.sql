CREATE TABLE presences.actions (
    id bigserial,
    structure_id character varying (36) NOT NULL,
    label text NOT NULL,
    abbreviation text NOT NULL,
    CONSTRAINT actions_pkey PRIMARY KEY (id)
);

CREATE TABLE presences.event_actions (
    id bigserial,
    event_id bigserial,
    action_id bigserial,
    owner character varying(36) NOT NULL,
    created_date timestamp without time zone NOT NULL DEFAULT now(),
    comment text,
    CONSTRAINT event_action_pkey PRIMARY KEY (id),
    CONSTRAINT fk_action_id FOREIGN KEY (action_id) REFERENCES presences.actions (id)
    MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);