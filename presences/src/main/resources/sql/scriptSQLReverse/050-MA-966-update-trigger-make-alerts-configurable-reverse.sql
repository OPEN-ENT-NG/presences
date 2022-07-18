DROP TRIGGER update_absence_alert ON presences.event;
DROP FUNCTION presences.update_absence_alert();

DROP TRIGGER update_lateness_alert ON presences.event;
DROP FUNCTION presences.update_lateness_alert();

DROP TRIGGER update_incident_alert ON incidents.incident;
DROP FUNCTION incidents.update_incident_alert();