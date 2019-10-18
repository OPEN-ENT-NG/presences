CREATE TABLE massmailing.template (
    id bigserial,
    structure_id character varying(36),
    name character varying(250),
    content text,
    type character varying(50),
    created timestamp without time zone NOT NULL DEFAULT now(),
    owner character varying(36) NOT NULL,
    CONSTRAINT template_pkey PRIMARY KEY (id),
    CONSTRAINT type_check CHECK (type = 'MAIL')
)