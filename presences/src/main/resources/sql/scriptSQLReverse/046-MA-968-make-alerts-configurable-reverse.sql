ALTER TABLE presences.settings DROP COLUMN exclude_alert_absence_no_reason,
                               DROP COLUMN exclude_alert_lateness_no_reason,
                               DROP COLUMN exclude_alert_forgotten_notebook;
ALTER TABLE incidents.seriousness DROP COLUMN exclude_alert_seriousness;

DROP TABLE presences.reason_alert;
DROP TABLE presences.reason_alert_exclude_rules_type;
