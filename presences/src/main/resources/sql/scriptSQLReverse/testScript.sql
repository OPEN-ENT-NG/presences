do $$
    declare
        structureId varchar = '46094e4c-a86f-4b73-812e-890e791a6900';
        studentId1 varchar = 'a732e55d-744f-456a-a9be-271ef3e443b9';
        studentId2 varchar = 'e2d5f0f4-0085-45ce-8c42-537c881c6783';
        countAlert bigint = 0;
    begin
        DELETE FROM presences.event WHERE id IN (9499, 9498, 9497, 9496, 9495, 9494, 9493, 9492, 9491, 9490);
        DELETE FROM presences.register WHERE id IN (9399, 9398, 9397, 9396);
        DELETE FROM presences.reason WHERE id IN (9999, 9998, 9997, 9996, 9995);
        DELETE FROM presences.alerts WHERE 1 = 1;
        DELETE FROM presences.alert_history WHERE 1 = 1;
        DELETE FROM presences.forgotten_notebook WHERE student_id = studentId1 OR student_id = studentId2;
        UPDATE presences.settings SET alert_forgotten_notebook_threshold = 2 WHERE structure_id = structureId;
        UPDATE presences.settings SET alert_incident_threshold = 3 WHERE structure_id = structureId;
        UPDATE presences.settings SET alert_lateness_threshold = 4 WHERE structure_id = structureId;
        UPDATE presences.settings SET alert_absence_threshold = 5 WHERE structure_id = structureId;

        UPDATE presences.settings SET event_recovery_method = 'HOUR' WHERE structure_id = structureId;
        UPDATE presences.settings SET exclude_absence_no_reason = false WHERE structure_id = structureId;
        UPDATE presences.settings SET exclude_absence_no_regularized = true WHERE structure_id = structureId;
        UPDATE presences.settings SET exclude_forgotten_notebook = false WHERE structure_id = structureId;
        UPDATE presences.settings SET exclude_absence_regularized = true WHERE structure_id = structureId;
        UPDATE presences.settings SET exclude_lateness_no_reason = false WHERE structure_id = structureId;

        INSERT INTO presences.reason(id, structure_id, label, reason_type_id, exclude_reason) VALUES (9999, structureId, '', 1, false);
        INSERT INTO presences.reason(id, structure_id, label, reason_type_id, exclude_reason) VALUES (9998, structureId, '', 1, true);
        INSERT INTO presences.reason(id, structure_id, label, reason_type_id, exclude_reason) VALUES (9995, structureId, '', 1, true);

        INSERT INTO presences.reason(id, structure_id, label, reason_type_id, exclude_reason) VALUES (9997, structureId, '', 2, false);
        INSERT INTO presences.reason(id, structure_id, label, reason_type_id, exclude_reason) VALUES (9996, structureId, '', 2, true);

        INSERT INTO incidents.seriousness(id, structure_id, label, exclude_seriousness) VALUES (9899, structureId, '', false);
        INSERT INTO incidents.seriousness(id, structure_id, label, exclude_seriousness) VALUES (9898, structureId, '', true);
        INSERT INTO incidents.protagonist_type(id, structure_id, label) VALUES (9799, structureId, '');

        -- Test forgotten notebook
        INSERT INTO presences.forgotten_notebook(id, date, student_id, structure_id) VALUES (9699, now(), studentId1, structureId);
        INSERT INTO presences.forgotten_notebook(id, date, student_id, structure_id) VALUES (9698, now(), studentId2, structureId);
        INSERT INTO presences.forgotten_notebook(id, date, student_id, structure_id) VALUES (9697, now() - INTERVAL '1 day', studentId1, structureId);
        SELECT count(*) FROM presences.alerts WHERE student_id = studentId2 INTO countAlert;
        assert countAlert = 1, 'assert 1: 1 != ' || countAlert;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 3, 'assert 2: 3 != ' || countAlert;
        UPDATE presences.settings SET exclude_forgotten_notebook = true WHERE structure_id = structureId;
        INSERT INTO presences.forgotten_notebook(id, date, student_id, structure_id) VALUES (9696, now() - INTERVAL '1 day', studentId2, structureId);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 3, 'assert 3: 3 != ' || countAlert;
        DELETE FROM presences.alerts WHERE 1 = 1;
        DELETE FROM presences.forgotten_notebook WHERE id IN (9699, 9698, 9697, 9696);
        UPDATE presences.settings SET exclude_forgotten_notebook = false WHERE structure_id = structureId;

        -- Test incidents
        INSERT INTO incidents.incident(id, owner, structure_id, date, seriousness_id) VALUES (9599, '', structureId, now(), 9899);
        INSERT INTO incidents.incident(id, owner, structure_id, date, seriousness_id) VALUES (9598, '', structureId, now(), 9898);
        INSERT INTO incidents.protagonist(user_id, incident_id, type_id) VALUES (studentId1, 9599, 9799);
        INSERT INTO incidents.protagonist(user_id, incident_id, type_id) VALUES (studentId2, 9599, 9799);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'assert 4: 2 != ' || countAlert;
        INSERT INTO incidents.protagonist(user_id, incident_id, type_id) VALUES (studentId1, 9598, 9799);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'assert 5: 2 != ' || countAlert;
        UPDATE incidents.incident SET seriousness_id = 9899 WHERE seriousness_id = 9898;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 3, 'assert 6: 3 != ' || countAlert;
        UPDATE incidents.incident SET seriousness_id = 9898 WHERE seriousness_id = 9899;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 0, 'assert 6.1: 0 != ' || countAlert;
        UPDATE incidents.incident SET seriousness_id = 9899 WHERE seriousness_id = 9898;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 3, 'assert 6.2: 3 != ' || countAlert;
        EXECUTE incidents.delete_incident(9599);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 6.1: 1 != ' || countAlert;
        DELETE FROM incidents.incident WHERE id IN (9599, 9598, 9597, 9596);
        DELETE FROM incidents.seriousness WHERE id IN (9899, 9898, 9897, 9896);
        DELETE FROM incidents.protagonist_type WHERE id IN (9799, 9798, 9797, 9796);
        DELETE FROM presences.alerts WHERE 1 = 1;

        --Test event
        INSERT INTO presences.register(id, personnel_id, course_id, state_id, owner, structure_id) VALUES (9399, '', '', 3, '', structureId);
        INSERT INTO presences.register(id, personnel_id, course_id, state_id, owner, structure_id) VALUES (9398, '', '', 3, '', structureId);

        -- Test lateness event
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9499, now()::date + '09:00:00'::time, now()::date + '09:55:00'::time, studentId1, 9398, 2, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 7.1: 1 != ' || countAlert;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9498, now()::date + '09:00:00'::time, now()::date + '09:55:00'::time, studentId1, 9398, 2, 9997, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'assert 7.2: 2 != ' || countAlert;
        UPDATE presences.event SET reason_id = 9996 WHERE id = 9498;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 7.3: 1 != ' || countAlert;
        DELETE FROM presences.event WHERE id IN (9499, 9498, 9497, 9496, 9495, 9494, 9493, 9492, 9491, 9490);
        DELETE FROM presences.alerts WHERE 1 = 1;

        -- Test absence event exclude_absence_no_reason
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9499, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId2, 9399, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 8: 1 != ' || countAlert;
        UPDATE presences.settings SET exclude_absence_no_reason = true WHERE structure_id = structureId;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9497, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId2, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 9: 1 != ' || countAlert;

        -- Test absence event exclude_absence_no_regularized and exclude_reason
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9498, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId1, 9399, 1, 9999, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 10: 1 != ' || countAlert;
        UPDATE presences.reason SET exclude_reason = false WHERE id = 9999;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9496, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId1, 9398, 1, 9999, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 11: 1 != ' || countAlert;
        UPDATE presences.settings SET exclude_absence_no_regularized = false WHERE structure_id = structureId;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9495, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId1, 9399, 1, 9999, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'assert 12: 2 != ' || countAlert;

        -- Test absence event counsellor_regularisation and exclude_absence_regularized
        UPDATE presences.event SET reason_id = 9998 WHERE id = 9495;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 13: 1 != ' || countAlert;
        UPDATE presences.event SET reason_id = 9999 WHERE id = 9495;
        UPDATE presences.event SET counsellor_regularisation = true WHERE id = 9495;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 14: 1 != ' || countAlert;
        UPDATE presences.settings SET exclude_absence_regularized = false WHERE structure_id = structureId;
        UPDATE presences.event SET counsellor_regularisation = false WHERE id = 9495;
        UPDATE presences.event SET counsellor_regularisation = true WHERE id = 9495;
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'assert 15: 2 != ' || countAlert;
        DELETE FROM presences.event WHERE id IN (9499, 9498, 9497, 9496, 9495, 9494, 9493, 9492, 9491, 9490);
        DELETE FROM presences.alerts WHERE 1 = 1;

        -- Test absence event recoveryMethod HOUR
        UPDATE presences.settings SET exclude_absence_no_reason = false WHERE structure_id = structureId;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9499, now()::date + '09:00:00'::time, now()::date + '09:55:00'::time, studentId2, 9399, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 1, 'assert 16: 1 != ' || countAlert;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9498, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId2, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'assert 17: 2 != ' || countAlert;

        -- Test absence event recoveryMethod HALF_DAY
        UPDATE presences.settings SET event_recovery_method = 'HALF_DAY' WHERE structure_id = structureId;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9497, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId2, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 2, 'assert 17.1: 2 != ' || countAlert;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9496, now()::date + '16:00:00'::time, now()::date + '16:55:00'::time, studentId2, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 3, 'assert 17.2: 3 != ' || countAlert;

        -- Test absence event recoveryMethod DAY
        UPDATE presences.settings SET event_recovery_method = 'DAY' WHERE structure_id = structureId;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9495, now()::date + '10:00:00'::time, now()::date + '10:55:00'::time, studentId2, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 3, 'assert 18: 3 != ' || countAlert;
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9494, now()::date + '16:00:00'::time, now()::date + '16:55:00'::time, studentId2, 9398, 1, NULL, '', false);
        SELECT count(*) FROM presences.alerts INTO countAlert;
        assert countAlert = 3, 'assert 19: 3 != ' || countAlert;
        DELETE FROM presences.event WHERE id IN (9499, 9498, 9497, 9496, 9495, 9494, 9493, 9492, 9491, 9490);
        DELETE FROM presences.alerts WHERE 1 = 1;

        -- Test complex case
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
    end$$;