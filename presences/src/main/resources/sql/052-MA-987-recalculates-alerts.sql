do $$
    declare
        structureId varchar;
        eventItems presences.event;
        forgottenNotebook presences.forgotten_notebook;
        incident incidents.incident;
        protagonist incidents.protagonist;
        existingAbsenceWithAlert bigint;
    begin
        TRUNCATE presences.alerts, presences.alert_history;
        FOR eventItems IN SELECT * FROM presences.event LEFT JOIN presences.alerts a on event.id = a.event_id WHERE a IS NULL LOOP
                SELECT structure_id FROM presences.register WHERE id = eventItems.register_id INTO structureId;
                if (eventItems.type_id = 1) THEN
                    existingAbsenceWithAlert = NULL;
                    SELECT * FROM presences.get_id_absence_event_siblings(eventItems, structureId, true) INTO existingAbsenceWithAlert;
                    IF existingAbsenceWithAlert IS NULL AND NOT presences.absence_exclude_alert(eventItems, structureId) THEN -- If we have no exclude condition and not siblings
                        INSERT INTO presences.alerts(student_id, structure_id, type, event_id, created) VALUES (eventItems.student_id , structureId, 'ABSENCE', eventItems.id, eventItems.created);
                    END IF;
                ELSE
                    IF NOT presences.lateness_exclude_alert(eventItems, structureId) THEN -- If we have no exclude condition
                    -- Create alert
                        INSERT INTO presences.alerts(student_id, structure_id, type, event_id, created) VALUES (eventItems.student_id , structureId, 'LATENESS', eventItems.id, eventItems.created);
                    END IF;
                end if;
            end loop;

        FOR forgottenNotebook IN SELECT * FROM presences.forgotten_notebook as f LEFT JOIN presences.alerts a on f.id = a.event_id WHERE a IS NULL LOOP
                IF presences.notebook_exclude_alert(structureId) IS FALSE THEN -- If we have no exclude condition
                -- Create alert
                    INSERT INTO presences.alerts(student_id, structure_id, type, event_id, created) VALUES (forgottenNotebook.student_id , forgottenNotebook.structure_id, 'FORGOTTEN_NOTEBOOK', forgottenNotebook.id, forgottenNotebook.created);
                END IF;
            end loop;

        FOR incident IN SELECT * FROM incidents.incident AS i WHERE incidents.incident_exclude_alert(i) IS FALSE LOOP
                FOR protagonist IN SELECT * FROM incidents.protagonist as p WHERE p.incident_id = incident.id AND NOT exists(SELECT * FROM presences.alerts as a WHERE a.type = 'INCIDENT' AND a.event_id = incident.id)
                    LOOP
                        INSERT INTO presences.alerts(student_id, structure_id, type, event_id, created) VALUES (protagonist.user_id, incident.structure_id, 'INCIDENT', incident.id, incident.created);
                    end loop;
            end loop;
    end$$;