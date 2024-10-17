CREATE INDEX idx_reason_id
  ON presences.event
  USING btree
  (reason_id);

CREATE INDEX idx_type_id
  ON presences.event
  USING btree
  (type_id);

CREATE INDEX idx_student_id
  ON presences.absence
  USING btree
  (student_id);