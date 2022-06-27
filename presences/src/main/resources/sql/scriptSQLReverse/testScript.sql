do $$
    declare
        structureId varchar = '46094e4c-a86f-4b73-812e-890e791a6900';
        studentId1 varchar = 'a732e55d-744f-456a-a9be-271ef3e443b9';
        studentId2 varchar = 'e2d5f0f4-0085-45ce-8c42-537c881c6783';
        countAlert bigint = 0;
    begin
        -- Insert and define default value
        TRUNCATE TABLE presences.alerts;
        TRUNCATE TABLE presences.alert_history;
        DELETE FROM presences.forgotten_notebook WHERE student_id = studentId1 OR student_id = studentId2;
        UPDATE presences.settings SET alert_forgotten_notebook_threshold = 2 WHERE structure_id = structureId;
        UPDATE presences.settings SET alert_incident_threshold = 3 WHERE structure_id = structureId;
        UPDATE presences.settings SET alert_lateness_threshold = 4 WHERE structure_id = structureId;
        UPDATE presences.settings SET alert_absence_threshold = 5 WHERE structure_id = structureId;

        UPDATE presences.settings SET event_recovery_method = 'HOUR' WHERE structure_id = structureId;
        UPDATE presences.settings SET exclude_alert_absence_no_reason = false WHERE structure_id = structureId;
        UPDATE presences.settings SET exclude_alert_forgotten_notebook = false WHERE structure_id = structureId;
        UPDATE presences.settings SET exclude_alert_lateness_no_reason = false WHERE structure_id = structureId;

        INSERT INTO presences.reason(id, structure_id, label, reason_type_id) VALUES (9999, structureId, '', 1);
        INSERT INTO presences.reason(id, structure_id, label, reason_type_id) VALUES (9998, structureId, '', 1);
        INSERT INTO presences.reason(id, structure_id, label, reason_type_id) VALUES (9995, structureId, '', 1);
        INSERT INTO presences.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) VALUES (structureId, 9999, 2);
        INSERT INTO presences.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) VALUES (structureId, 9998, 2);
        INSERT INTO presences.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) VALUES (structureId, 9998, 1);
        INSERT INTO presences.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) VALUES (structureId, 9995, 1);
        INSERT INTO presences.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) VALUES (structureId, 9995, 2);

        INSERT INTO presences.reason(id, structure_id, label, reason_type_id) VALUES (9997, structureId, '', 2);
        INSERT INTO presences.reason(id, structure_id, label, reason_type_id) VALUES (9996, structureId, '', 2);
        INSERT INTO presences.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) VALUES (structureId, 9996, 3);

        INSERT INTO incidents.seriousness(id, structure_id, label, exclude_alert_seriousness) VALUES (9899, structureId, '', false);
        INSERT INTO incidents.seriousness(id, structure_id, label, exclude_alert_seriousness) VALUES (9898, structureId, '', true);
        INSERT INTO incidents.protagonist_type(id, structure_id, label) VALUES (9799, structureId, '');

        -- Test forgotten notebook
        INSERT INTO presences.forgotten_notebook(id, date, student_id, structure_id) VALUES (9699, now(), studentId1, structureId);
        INSERT INTO presences.forgotten_notebook(id, date, student_id, structure_id) VALUES (9698, now(), studentId2, structureId);
        INSERT INTO presences.forgotten_notebook(id, date, student_id, structure_id) VALUES (9697, now() - INTERVAL '1 day', studentId1, structureId);
        SELECT count(*) FROM presences.alerts WHERE student_id = studentId2 INTO countAlert;
        assert countAlert = 1, 'create alert for studentId2 assert 1 != ' || countAlert;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 3, 'create alert forgotten_notebook assert 3 != ' || countAlert;
        UPDATE presences.settings SET exclude_alert_forgotten_notebook = true WHERE structure_id = structureId;
        INSERT INTO presences.forgotten_notebook(id, date, student_id, structure_id) VALUES (9696, now() - INTERVAL '1 day', studentId2, structureId);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 3, 'do not create alert when exclude_alert_forgotten_notebook assert 3 != ' || countAlert;
        DELETE FROM presences.alerts WHERE 1 = 1;
        DELETE FROM presences.forgotten_notebook WHERE id IN (9699, 9698, 9697, 9696);
        UPDATE presences.settings SET exclude_alert_forgotten_notebook = false WHERE structure_id = structureId;

        -- Test incidents
        INSERT INTO incidents.incident(id, owner, structure_id, date, seriousness_id) VALUES (9599, '', structureId, now(), 9899);
        INSERT INTO incidents.incident(id, owner, structure_id, date, seriousness_id) VALUES (9598, '', structureId, now(), 9898);
        INSERT INTO incidents.protagonist(user_id, incident_id, type_id) VALUES (studentId1, 9599, 9799);
        INSERT INTO incidents.protagonist(user_id, incident_id, type_id) VALUES (studentId2, 9599, 9799);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'create alert for incident assert 2 != ' || countAlert;
        INSERT INTO incidents.protagonist(user_id, incident_id, type_id) VALUES (studentId1, 9598, 9799);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'do not create alert when seriousness is exclude assert 2 != ' || countAlert;
        UPDATE incidents.incident SET seriousness_id = 9899 WHERE seriousness_id = 9898;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 3, 'create alert when update to a non exclude seriousness assert 3 != ' || countAlert;
        UPDATE incidents.incident SET seriousness_id = 9898 WHERE seriousness_id = 9899;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 0, 'delete alert when update to a exclude seriousness assert 0 != ' || countAlert;
        UPDATE incidents.incident SET seriousness_id = 9899 WHERE seriousness_id = 9898;
        EXECUTE incidents.delete_incident(9599);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'delete alert if the incident is delete assert 1 != ' || countAlert;
        DELETE FROM incidents.protagonist WHERE user_id = studentId1;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 0, 'delete alert if the protagonist is delete assert 0 != ' || countAlert;
        DELETE FROM incidents.incident WHERE id IN (9599, 9598, 9597, 9596);
        DELETE FROM incidents.seriousness WHERE id IN (9899, 9898, 9897, 9896);
        DELETE FROM incidents.protagonist_type WHERE id IN (9799, 9798, 9797, 9796);
        TRUNCATE TABLE presences.alerts;

        --Test event
        INSERT INTO presences.register(id, personnel_id, course_id, state_id, owner, structure_id) VALUES (9399, '', '', 3, '', structureId);
        INSERT INTO presences.register(id, personnel_id, course_id, state_id, owner, structure_id) VALUES (9398, '', '', 3, '', structureId);

        -- Test lateness event
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9499, now()::date + '09:00:00'::time, now()::date + '09:55:00'::time, studentId1, 9398, 2, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'create alert for lateness without reason assert 1 != ' || countAlert;

        UPDATE presences.settings SET exclude_alert_lateness_no_reason = true WHERE structure_id = structureId;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9496, now()::date + '09:00:00'::time, now()::date + '09:55:00'::time, studentId1, 9398, 2, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'do not create alert when exclude_alert_lateness_no_reason assert 1 != ' || countAlert;

        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9498, now()::date + '09:00:00'::time, now()::date + '09:55:00'::time, studentId1, 9398, 2, 9997, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'create alert for lateness with reason not exclude assert 2 != ' || countAlert;

        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9497, now()::date + '09:00:00'::time, now()::date + '09:55:00'::time, studentId1, 9398, 2, 9996, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'do not create alert for lateness with reason exclude assert 2 != ' || countAlert;

        UPDATE presences.event SET reason_id = 9996 WHERE id = 9498;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'delete alert when update lateness with reason exclude assert 1 != ' || countAlert;
        DELETE FROM presences.event WHERE id IN (9499, 9498, 9497, 9496, 9495, 9494, 9493, 9492, 9491, 9490);
        TRUNCATE TABLE presences.alerts;

        -- Test absence event exclude_alert_absence_no_reason
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9499, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId2, 9399, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'create alert for absence without reason assert 1 != ' || countAlert;

        UPDATE presences.settings SET exclude_alert_absence_no_reason = true WHERE structure_id = structureId;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9497, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId2, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'do not create when exclude_alert_absence_no_reason assert 1 != ' || countAlert;

        -- Test absence event no_regularized
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9498, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId1, 9399, 1, 9999, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'do not create alert for exclude reason assert 1 != ' || countAlert;

        UPDATE presences.reason_alert SET deleted_at = now() WHERE reason_id = 9999 AND structure_id = structureId;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9495, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId1, 9399, 1, 9999, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'create alert for not exclude reason assert 12: 2 != ' || countAlert;

        -- Test absence event regularized
        UPDATE presences.event SET reason_id = 9998 WHERE id = 9495;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'delete alert when update to a exclude reason assert 13: 1 != ' || countAlert;

        UPDATE presences.event SET reason_id = 9999 WHERE id = 9495;
        UPDATE presences.event SET counsellor_regularisation = true WHERE id = 9495;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'create alert when regularised is not exclude assert 14: 2 != ' || countAlert;

        UPDATE presences.event SET reason_id = 9998 WHERE id = 9495;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'delete alert when regularised is exclude assert 15: 1 != ' || countAlert;

        DELETE FROM presences.event WHERE id IN (9499, 9498, 9497, 9496, 9495, 9494, 9493, 9492, 9491, 9490);
        TRUNCATE TABLE presences.alerts;

        -- Test absence event recoveryMethod HOUR
        UPDATE presences.settings SET exclude_alert_absence_no_reason = false WHERE structure_id = structureId;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9499, now()::date + '09:00:00'::time, now()::date + '09:55:00'::time, studentId2, 9399, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 16: 1 != ' || countAlert;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9498, now()::date + '09:00:00'::time, now()::date + '09:55:00'::time, studentId2, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'create alert when recoveryMethod HOUR and same time assert 2 != ' || countAlert;

        -- Test absence event recoveryMethod HALF_DAY
        UPDATE presences.settings SET event_recovery_method = 'HALF_DAY' WHERE structure_id = structureId;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9497, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId2, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'do not create alert when recoveryMethod HALF_DAY and same time assert 2 != ' || countAlert;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9496, now()::date + '16:00:00'::time, now()::date + '16:55:00'::time, studentId2, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 3, 'create alert when recoveryMethod HALF_DAY and not same time assert 3 != ' || countAlert;

        -- Test absence event recoveryMethod DAY
        UPDATE presences.settings SET event_recovery_method = 'DAY' WHERE structure_id = structureId;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9495, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId1, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 4, 'create alert when recoveryMethod DAY and not same day assert 4 != ' || countAlert;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9494, now()::date + '16:00:00'::time, now()::date + '16:55:00'::time, studentId1, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 4, 'do not create alert when recoveryMethod DAY and same day assert 4 != ' || countAlert;
        DELETE FROM presences.event WHERE id IN (9499, 9498, 9497, 9496, 9495, 9494, 9493, 9492, 9491, 9490);
        TRUNCATE TABLE presences.alerts;

        -- Test complex case Update alert when having siblings in same time
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9499, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId2, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 20: 1 != ' || countAlert;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9498, now()::date + '09:00:00'::time, now()::date + '09:55:00'::time, studentId2, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 21: 1 != ' || countAlert;
        SELECT count(*) FROM presences.alerts WHERE event_id = 9499 INTO countAlert;
        assert countAlert = 1, 'assert 22: 1 != ' || countAlert;

        UPDATE presences.event SET reason_id = 9998 WHERE id = 9499;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 23: 1 != ' || countAlert;
        SELECT count(*) FROM presences.alerts WHERE event_id = 9498 INTO countAlert;
        assert countAlert = 1, 'assert 24: 1 != ' || countAlert;

        UPDATE presences.event SET reason_id = 9995 WHERE id = 9499;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 24.1: 1 != ' || countAlert;
        SELECT count(*) FROM presences.alerts WHERE event_id = 9498 INTO countAlert;
        assert countAlert = 1, 'assert 24.2: 1 != ' || countAlert;

        UPDATE presences.event SET reason_id = 9999 WHERE id = 9499;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 25: 1 != ' || countAlert;
        SELECT count(*) FROM presences.alerts WHERE event_id = 9498 INTO countAlert;
        assert countAlert = 1, 'assert 26: 1 != ' || countAlert;

        DELETE FROM presences.event WHERE id = 9498;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 27: 1 != ' || countAlert;
        SELECT count(*) FROM presences.alerts WHERE event_id = 9499 INTO countAlert;
        assert countAlert = 1, 'assert 28: 1 != ' || countAlert;

        TRUNCATE TABLE presences.alerts;
        TRUNCATE TABLE presences.alert_history;
        UPDATE presences.settings SET alert_absence_threshold = 3 WHERE structure_id = structureId;

        --Test history
        INSERT INTO presences.alerts(id, student_id, structure_id, type, created, event_id) VALUES (1, studentId1, structureId, 'ABSENCE', now(), 1);
        DELETE FROM presences.alerts WHERE id = 1;
        SELECT count(*) FROM presences.alert_history INTO countAlert;
        assert countAlert = 0, 'assert 29: 0 != ' || countAlert;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 0, 'assert 29.1: 0 != ' || countAlert;

        INSERT INTO presences.alerts(id, student_id, structure_id, type, created, event_id) VALUES (1, studentId1, structureId, 'ABSENCE', now(), 1);
        INSERT INTO presences.alerts(id, student_id, structure_id, type, created, event_id) VALUES (2, studentId1, structureId, 'ABSENCE', now(), 2);
        INSERT INTO presences.alerts(id, student_id, structure_id, type, created, event_id) VALUES (3, studentId1, structureId, 'ABSENCE', now(), 3);
        INSERT INTO presences.alerts(id, student_id, structure_id, type, created, event_id) VALUES (4, studentId1, structureId, 'ABSENCE', now(), 4);
        DELETE FROM presences.alerts WHERE id = 1;
        SELECT count(*) FROM presences.alert_history INTO countAlert;
        assert countAlert = 0, 'do not create histrory when < to thresholder assert 0 != ' || countAlert;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 3, 'assert 30.1: 3 != ' || countAlert;

        INSERT INTO presences.alerts(id, student_id, structure_id, type, created, event_id) VALUES (1, studentId1, structureId, 'ABSENCE', now(), 1);
        DELETE FROM presences.alerts WHERE 1 = 1;
        SELECT count(*) FROM presences.alert_history INTO countAlert;
        assert countAlert = 4, 'create history when > to thresholder assert 4 != ' || countAlert;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 0, 'assert 31.1: 0 != ' || countAlert;

        --Test delete reason_alert
        DELETE FROM presences.event WHERE id IN (9499, 9498, 9497, 9496, 9495, 9494, 9493, 9492, 9491, 9490);
        DELETE FROM presences.register WHERE id IN (9399, 9398, 9397, 9396);
        DELETE FROM presences.reason WHERE id IN (9999, 9998, 9997, 9996, 9995);
        SELECT count(*) FROM presences.reason_alert WHERE reason_id IN (9999, 9998, 9997, 9996, 9995) INTO countAlert;
        assert countAlert = 0, 'clear associated reason_alert when deleting reason assert 0 != ' || countAlert;
    end$$;