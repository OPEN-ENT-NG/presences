CREATE INDEX idx_alert
    ON presences.alerts
        USING btree
        (structure_id ASC, student_id ASC, type ASC, date DESC);