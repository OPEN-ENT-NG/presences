ALTER TABLE presences.register
    ADD COLUMN notified boolean NOT NULL DEFAULT false,
    ADD UNIQUE (course_id, start_date, end_date);