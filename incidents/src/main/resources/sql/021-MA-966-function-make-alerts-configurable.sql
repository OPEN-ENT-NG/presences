-- Return true if incident is exclude to create alert
CREATE or replace FUNCTION incidents.incident_exclude_alert(incident incidents.incident) RETURNS BOOLEAN AS
$BODY$
DECLARE
    eventExclude boolean;
BEGIN
    SELECT exclude_alert_seriousness FROM incidents.seriousness as s WHERE s.id = incident.seriousness_id INTO eventExclude;
    RETURN eventExclude;
END
$BODY$
    LANGUAGE plpgsql;

-- Return true if incident of the protagonist is exclude to create alert
CREATE or replace FUNCTION incidents.incident_protagonist_exclude_alert(protagonist incidents.protagonist, structureId varchar) RETURNS BOOLEAN AS
$BODY$
DECLARE
    eventExclude boolean;
BEGIN
    SELECT exclude_alert_seriousness FROM incidents.seriousness as s INNER JOIN incidents.incident i on s.id = i.seriousness_id
    WHERE protagonist.incident_id = i.id AND s.structure_id = structureId LIMIT 1 INTO eventExclude;

    RETURN eventExclude;
END
$BODY$
    LANGUAGE plpgsql;