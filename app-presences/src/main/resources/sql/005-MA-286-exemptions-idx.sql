CREATE INDEX idx_exemption_date
  ON presences.exemption
  USING btree
  (start_date ASC, end_date DESC);

CREATE INDEX idx_exemption_structure_id
  ON presences.exemption
  USING btree
  (structure_id);
