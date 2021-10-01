CREATE TABLE presences.rel_teacher_register
(
    register_id bigint,
    teacher_id    character varying(36),
    CONSTRAINT rel_teacher_register_pkey PRIMARY KEY (register_id, teacher_id),
    CONSTRAINT fk_register_id FOREIGN KEY (register_id) REFERENCES presences.register (id)
        MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);