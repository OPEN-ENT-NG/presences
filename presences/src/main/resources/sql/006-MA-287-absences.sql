CREATE INDEX idx_events_date
  ON presences.event
  USING btree
  (start_date ASC, end_date DESC);

CREATE INDEX idx_register_structure
  ON presences.register
  USING btree
  (structure_id);

ALTER TABLE presences.event
    ADD COLUMN counsellor_regularisation boolean NOT NULL DEFAULT false;
