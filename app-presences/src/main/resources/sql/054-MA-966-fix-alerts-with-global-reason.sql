CREATE OR REPLACE FUNCTION presences.absence_exclude_alert(event presences.event, structureId varchar) RETURNS BOOLEAN AS
$BODY$
DECLARE
    alertRulesTypeId bigint;
    noReasonAbsenceExclude boolean;
BEGIN
    SELECT reason_alert_exclude_rules_type_id FROM presences.reason_alert WHERE structure_id = structureId
                                                                            AND reason_id = event.reason_id
                                                                            AND deleted_at IS NULL
                                                                            AND ((event.counsellor_regularisation AND reason_alert_exclude_rules_type_id = 1)
            OR (NOT event.counsellor_regularisation AND reason_alert_exclude_rules_type_id = 2))
    LIMIT 1 INTO alertRulesTypeId;
    SELECT exclude_alert_absence_no_reason FROM presences.settings WHERE structure_id = structureId INTO noReasonAbsenceExclude;
    RETURN ((event.reason_id IS NULL AND noReasonAbsenceExclude) OR
            (event.reason_id IS NOT NULL AND (alertRulesTypeId IS NOT NULL OR event.reason_id < 1)));
END
$BODY$
    LANGUAGE plpgsql;

do $$
    declare
        eventRegisterItem record;
        registerItem presences.register;
        eventItem presences.event;
        forgottenNotebook presences.forgotten_notebook;
        incident incidents.incident;
        protagonist incidents.protagonist;
        existingAbsenceWithAlert bigint;
    begin
        TRUNCATE presences.alerts, presences.alert_history;
        FOR eventRegisterItem IN SELECT e as event, r as register
                                 FROM presences.event e
                                          INNER JOIN presences.register r on e.register_id = r.id
                                          LEFT JOIN viesco.setting_period s on r.structure_id = s.id_structure
                                          LEFT JOIN presences.alerts a on e.id = a.event_id
                                 WHERE a IS NULL AND s.code = 'YEAR' AND r.start_date >= s.start_date LOOP
                registerItem = (eventRegisterItem.register);
                eventItem = (eventRegisterItem.event);
                IF (eventItem.type_id = 1) THEN
                    existingAbsenceWithAlert = NULL;
                    SELECT * FROM presences.get_id_absence_event_siblings(eventItem, registerItem.structure_id, true) INTO existingAbsenceWithAlert;
                    IF existingAbsenceWithAlert IS NULL AND NOT presences.absence_exclude_alert(eventItem, registerItem.structure_id) THEN -- If we have no exclude condition and not siblings
                        INSERT INTO presences.alerts(student_id, structure_id, type, event_id, date) VALUES (eventItem.student_id , registerItem.structure_id, 'ABSENCE', eventItem.id, registerItem.start_date);
                    END IF;
                ELSE
                    IF NOT presences.lateness_exclude_alert(eventItem, registerItem.structure_id) THEN -- If we have no exclude condition
                    -- Create alert
                        INSERT INTO presences.alerts(student_id, structure_id, type, event_id, date) VALUES (eventItem.student_id , registerItem.structure_id, 'LATENESS', eventItem.id, registerItem.start_date);
                    END IF;
                end if;
            end loop;

        FOR forgottenNotebook IN SELECT * FROM presences.forgotten_notebook as f
                                                   LEFT JOIN viesco.setting_period s on f.structure_id = s.id_structure
                                                   LEFT JOIN presences.alerts a on f.id = a.event_id
                                 WHERE a IS NULL AND s.code = 'YEAR' AND f.date >= s.start_date LOOP
                IF presences.notebook_exclude_alert(forgottenNotebook.structure_id) IS FALSE THEN -- If we have no exclude condition
                -- Create alert
                    INSERT INTO presences.alerts(student_id, structure_id, type, event_id, date) VALUES (forgottenNotebook.student_id , forgottenNotebook.structure_id, 'FORGOTTEN_NOTEBOOK', forgottenNotebook.id, forgottenNotebook.date);
                END IF;
            end loop;

        FOR incident IN SELECT * FROM incidents.incident AS i
                                          LEFT JOIN viesco.setting_period s on i.structure_id = s.id_structure
                        WHERE incidents.incident_exclude_alert(i) IS FALSE AND s.code = 'YEAR' AND i.date >= s.start_date LOOP
                FOR protagonist IN SELECT * FROM incidents.protagonist as p WHERE p.incident_id = incident.id AND NOT exists(SELECT * FROM presences.alerts as a WHERE a.type = 'INCIDENT' AND a.event_id = incident.id)
                    LOOP
                        INSERT INTO presences.alerts(student_id, structure_id, type, event_id, date) VALUES (protagonist.user_id, incident.structure_id, 'INCIDENT', incident.id, incident.date);
                    end loop;
            end loop;
    end$$;