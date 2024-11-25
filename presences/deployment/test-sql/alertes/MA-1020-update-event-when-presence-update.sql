    do $$
    declare
        structureId varchar = '46094e4c-a86f-4b73-812e-890e791a6900';
        studentId1 varchar = 'ae0c59ca-10e2-433f-8bd5-e33860b17901';
        studentId2 varchar = '07644b76-a0e6-4cbe-88ea-1f56e5973166';
        studentId3 varchar = 'a0a336b4-7bca-47f4-8e48-eba60682cf31';
        reasonId bigint;
    begin
        -- Insert and define default value
        INSERT INTO presences.register(id, personnel_id, course_id, state_id, owner, structure_id, start_date)
        VALUES (9100, '', '', 3, '', structureId, '2022-10-17 08:30:00.00000');

        INSERT INTO presences.register(id, personnel_id, course_id, state_id, owner, structure_id, start_date)
        VALUES (9101, '', '', 3, '', structureId, '2022-10-18 08:30:00.00000');

        --Init event
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9000, '2022-10-17 08:30:00.00000', '2022-10-17 09:25:00.000000', studentId1, 9100, 2, NULL, '', false);

        --Unregularized absence
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9001, '2022-10-17 08:30:00.00000', '2022-10-17 09:25:00.000000', studentId2, 9100, 2, 1, '', false);

        --Regularized absence
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9002, '2022-10-17 08:30:00.00000', '2022-10-17 09:25:00.000000', studentId3, 9100, 2, 12, '', false);

        --Different period
        INSERT INTO presences.event(id, start_date, end_date, student_id, register_id, type_id, reason_id, owner, counsellor_regularisation)
        VALUES (9003, '2022-10-18 08:30:00.00000', '2022-10-18 09:25:00.000000', studentId1, 9100, 2, NULL, '', false);

        -- Init absences
        INSERT INTO presences.absence(id, start_date, end_date, student_id, reason_id, structure_id, counsellor_regularisation)
        VALUES (9500, '2022-10-17 08:30:00.00000', '2022-10-17 09:25:00.000000', studentId1, NULL, structureId, false);

        INSERT INTO presences.absence(id, start_date, end_date, student_id, reason_id, owner, counsellor_regularisation)
        VALUES (9501, '2022-10-17 08:30:00.00000', '2022-10-17 09:25:00.000000', studentId1, 1, '', false);

        INSERT INTO presences.absence(id, start_date, end_date, student_id, reason_id, owner, counsellor_regularisation)
        VALUES (9502, '2022-10-17 08:30:00.00000', '2022-10-17 09:25:00.000000', studentId1, 12, '', false);

        INSERT INTO presences.absence(id, start_date, end_date, student_id, reason_id, structure_id, counsellor_regularisation)
        VALUES (9503, '2022-10-18 08:30:00.00000', '2022-10-18 09:25:00.000000', studentId1, NULL, structureId, false);

        --Test add presence
        INSERT INTO presences.presence(id, start_date, end_date, discipline_id, structure_id, owner)
        VALUES (9200, '2022-10-17 08:30:00.00000', '2022-10-17 09:25:00.000000', 1, structureId, '');

        INSERT INTO presences.presence(id, start_date, end_date, discipline_id, structure_id, owner)
        VALUES (9201, '2022-10-17 08:30:00.00000', '2022-10-17 19:25:00.000000', 1, structureId, '');

        INSERT INTO presences.presence_student(student_id, presence_id) VALUES (studentId1, 9200), (studentId2, 9200), (studentId3, 9200);
        SELECT reason_id FROM presences.event WHERE id = 9000 INTO reasonId;
        assert reasonId = -2, 'update event on presence -2 != ' || reasonId;

        SELECT reason_id FROM presences.event WHERE id = 9001 INTO reasonId;
        assert reasonId = 1, 'dont update event with reason 1 != ' || reasonId;

        SELECT reason_id FROM presences.event WHERE id = 9002 INTO reasonId;
        assert reasonId = 12, 'dont update event with reason 12 != ' || reasonId;

        SELECT reason_id FROM presences.event WHERE id = 9003 INTO reasonId;
        assert reasonId IS NULL, 'dont update event on other period NULL != ' || reasonId;

        SELECT reason_id FROM presences.absence WHERE id = 9500 INTO reasonId;
        assert reasonId = -2, 'update absence on presence -2 != ' || reasonId;

        SELECT reason_id FROM presences.absence WHERE id = 9501 INTO reasonId;
        assert reasonId = 1, 'dont update absence with reason 1 != ' || reasonId;

        SELECT reason_id FROM presences.absence WHERE id = 9502 INTO reasonId;
        assert reasonId = 12, 'dont update absence with reason 12 != ' || reasonId;

        SELECT reason_id FROM presences.absence WHERE id = 9503 INTO reasonId;
        assert reasonId IS NULL, 'dont update absence on other period NULL != ' || reasonId;

        --Test delete presences
        DELETE FROM presences.presence_student WHERE student_id = studentId1 OR student_id = studentId2 OR student_id = studentId3;

        SELECT reason_id FROM presences.event WHERE id = 9000 INTO reasonId;
        assert reasonId IS NULL , 'update event on presence delete NULL != ' || reasonId;

        SELECT reason_id FROM presences.event WHERE id = 9001 INTO reasonId;
        assert reasonId = 1, 'dont update event with reason 1 != ' || reasonId;

        SELECT reason_id FROM presences.event WHERE id = 9002 INTO reasonId;
        assert reasonId = 12, 'dont update event with reason 12 != ' || reasonId;

        SELECT reason_id FROM presences.absence WHERE id = 9500 INTO reasonId;
        assert reasonId IS NULL , 'update absence on presence delete NULL != ' || reasonId;

        SELECT reason_id FROM presences.absence WHERE id = 9501 INTO reasonId;
        assert reasonId = 1, 'dont update absence with reason 1 != ' || reasonId;

        SELECT reason_id FROM presences.absence WHERE id = 9502 INTO reasonId;
        assert reasonId = 12, 'dont update absence with reason 12 != ' || reasonId;

        INSERT INTO presences.presence_student(student_id, presence_id) VALUES (studentId1, 9200), (studentId1, 9201);
        SELECT reason_id FROM presences.event WHERE id = 9000 INTO reasonId;
        assert reasonId = -2 , 'update event on presence NULL != ' || reasonId;

        SELECT reason_id FROM presences.absence WHERE id = 9500 INTO reasonId;
        assert reasonId = -2 , 'update absence on presence NULL != ' || reasonId;

        DELETE FROM presences.presence_student WHERE student_id = studentId1 AND presence_id = 9200;
        SELECT reason_id FROM presences.event WHERE id = 9000 INTO reasonId;
        assert reasonId = -2 , 'do not update event on presence delete when having other presence NULL != ' || reasonId;

        SELECT reason_id FROM presences.absence WHERE id = 9500 INTO reasonId;
        assert reasonId = -2 , 'do not update absence on presence delete when having other presence NULL != ' || reasonId;

        DELETE FROM presences.presence WHERE id = 9201;

        SELECT reason_id FROM presences.event WHERE id = 9000 INTO reasonId;
        assert reasonId IS NULL , 'update event on presence delete NULL != ' || reasonId;

        SELECT reason_id FROM presences.absence WHERE id = 9500 INTO reasonId;
        assert reasonId IS NULL , 'update absence on presence delete NULL != ' || reasonId;

        DELETE FROM presences.absence WHERE id IN (9500, 9501, 9502, 9503);
        DELETE FROM presences.event WHERE id IN (9000, 9001, 9002, 9003);
        DELETE FROM presences.presence_student WHERE student_id IN (studentId1, studentId2, studentId3);
        DELETE FROM presences.presence WHERE id IN (9200, 9201);
        DELETE FROM presences.register WHERE id = 9100 OR id = 9101;
    end$$;