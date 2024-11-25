CREATE INDEX idx_register_course_id
  ON presences.register
  USING btree
  (course_id);