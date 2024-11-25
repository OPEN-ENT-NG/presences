CREATE TABLE presences.absence (
		id bigserial,
		start_date timestamp without time zone,
		end_date timestamp without time zone,
		student_id character varying (36) NOT NULL,
		reason_id bigint,
	    CONSTRAINT absence_pkey PRIMARY KEY (id),
        CONSTRAINT fk_reason_id FOREIGN KEY (reason_id) REFERENCES presences.reason (id)
);