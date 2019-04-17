ALTER TABLE presences.register
    ADD COLUMN subject_id character varying(36),
    ADD COLUMN start_date timestamp without time zone,
    ADD COLUMN end_date timestamp without time zone;

ALTER TABLE presences.register
    ALTER COLUMN course_id TYPE character varying(36),
    ALTER COLUMN counsellor_input SET DEFAULT false;;

CREATE TABLE presences.group
(
    id   character varying(36),
    type text NOT NULL,
    CONSTRAINT group_pkey PRIMARY KEY (id),
    CONSTRAINT check_types CHECK (type IN ('CLASS', 'GROUP'))
);

CREATE TABLE presences.rel_group_register
(
    register_id bigint,
    group_id    character varying(36),
    CONSTRAINT rel_group_register_pkey PRIMARY KEY (register_id, group_id),
    CONSTRAINT fk_register_id FOREIGN KEY (register_id) REFERENCES presences.register (id)
        MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_group_id FOREIGN KEY (group_id) REFERENCES presences.group (id)
        MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);

ALTER TABLE presences.register_state
    DROP COLUMN structure_id;

INSERT INTO presences.register_state (label)
VALUES ('presences.register.status.todo'),
       ('presences.register.status.inprogress'),
       ('presences.register.status.done');

ALTER TABLE presences.register
    ADD COLUMN structure_id character varying(36) NOT NULL;

INSERT INTO presences.event_type (label)
VALUES ('presences.register.event_type.absences'),
       ('presences.register.event_type.lateness'),
       ('presences.register.event_type.departure'),
       ('presences.register.event_type.remark');

CREATE INDEX idx_date
    ON presences.register
        USING btree
        (start_date, end_date DESC);

CREATE INDEX idx_student
    ON presences.event
        USING btree
        (student_id COLLATE pg_catalog."default");

CREATE INDEX idx_register_id
    ON presences.event
        USING btree
        (register_id);