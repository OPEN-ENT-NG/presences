package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DB;
import fr.openent.presences.db.DBService;
import fr.openent.presences.service.AbsenceService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class AbsenceServiceTest extends DBService {

    private static final String STRUCTURE_ID = "111";
    private static final List<String> STUDENT_IDS = Arrays.asList("333", "444");
    private static final List<Integer> REASON_IDS = Arrays.asList(12, 13);
    private static final String START_AT = "2022-04-01 08:00:00";
    private static final String END_AT = "2021-04-30 23:59:59";
    private static final Integer PAGE = 1;
    Sql sql = Mockito.mock(Sql.class);
    private AbsenceService absenceService;

    @Before
    public void setUp() {
        DB.getInstance().init(null, sql, null);
        this.absenceService = new DefaultAbsenceService(Vertx.vertx().eventBus());
    }

    @Test
    public void testGetAbsences(TestContext ctx) throws Exception {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(params, new JsonArray()
                    .add(Field.STRUCTURE_ID)
                    .add(Field.START_DATE)
                    .add(Field.END_DATE)
                    .add(Field.ID));

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        Whitebox.invokeMethod(absenceService, "get",
                Field.STRUCTURE_ID, Field.START_DATE, Field.END_DATE, Collections.singletonList(Field.ID));


    }

    @Test
    public void testRetrieveWithPaginate(TestContext ctx) throws Exception {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(query,
                    "SELECT id, start_date, end_date, student_id, reason_id, counsellor_regularisation, followed, " +
                            " structure_id, owner FROM " + Presences.dbSchema + ".absence WHERE structure_id = ?" +
                            " AND student_id IN (?,?) AND ((reason_id IN (?,?))) AND (counsellor_regularisation = false) " +
                            " AND end_date > ?  AND start_date < ?  ORDER BY start_date DESC LIMIT ? OFFSET ?");

            ctx.assertEquals(params, new JsonArray()
                    .add(STRUCTURE_ID)
                    .addAll(new JsonArray(STUDENT_IDS))
                    .addAll(new JsonArray(REASON_IDS))
                    .add(START_AT)
                    .add(END_AT)
                    .add(Presences.PAGE_SIZE)
                    .add(Presences.PAGE_SIZE * PAGE)
            );

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        Whitebox.invokeMethod(absenceService, "retrieve",
                STRUCTURE_ID, STUDENT_IDS, START_AT, END_AT, false, null, null, REASON_IDS, PAGE);
    }

    @Test
    public void testFilterReasonsWithNoReasonAndRegularized(TestContext ctx) throws Exception {
        JsonArray params = new JsonArray();

        String result = Whitebox.invokeMethod(absenceService, "filterReasons",
                ((List<Integer>) null), true, true, params);

        ctx.assertEquals(" AND (reason_id IS NULL OR (reason_id IS NOT NULL AND counsellor_regularisation = true))",
                result);

        ctx.assertEquals(new JsonArray(), params);
    }

    @Test
    public void testFilterReasonsWithReasonDefinedAndRegularized(TestContext ctx) throws Exception {
        JsonArray params = new JsonArray();

        String result = Whitebox.invokeMethod(absenceService, "filterReasons",
                ((List<Integer>) null), null, true, params);

        ctx.assertEquals(" AND (counsellor_regularisation = true) ", result);

        ctx.assertEquals(new JsonArray(), params);
    }

    @Test
    public void testFilterReasonsWithReasonDefinedAndNoRegularized(TestContext ctx) throws Exception {
        JsonArray params = new JsonArray();

        String result = Whitebox.invokeMethod(absenceService, "filterReasons",
                ((List<Integer>) null), null, false, params);

        ctx.assertEquals(" AND (counsellor_regularisation = false) ",
                result);

        ctx.assertEquals(new JsonArray(), params);
    }

    @Test
    public void testFilterReasonsWithNoReasonAndNoRegularizedAndReasonIdsEmpty(TestContext ctx) throws Exception {
        JsonArray params = new JsonArray();

        String result = Whitebox.invokeMethod(absenceService, "filterReasons",
                Collections.emptyList(), true, null, params);

        ctx.assertEquals(" AND (reason_id IS NULL ) ",
                result);

        ctx.assertEquals(new JsonArray(), params);
    }

    @Test
    public void testFilterReasonsWithNoReasonAndRegularizedAndReasonIds(TestContext ctx) throws Exception {
        JsonArray params = new JsonArray();

        String result = Whitebox.invokeMethod(absenceService, "filterReasons",
                REASON_IDS, true, true, params);

        ctx.assertEquals(" AND (reason_id IS NULL OR (reason_id IS NOT NULL AND counsellor_regularisation = true)" +
                        " AND reason_id IN (?,?))",
                result);

        ctx.assertEquals(new JsonArray(REASON_IDS), params);
    }

    @Test
    public void testFilterReasonsWithNoReasonAndNoRegularizedAndReasonIds(TestContext ctx) throws Exception {
        JsonArray params = new JsonArray();

        String result = Whitebox.invokeMethod(absenceService, "filterReasons",
                REASON_IDS, true, null, params);

        ctx.assertEquals(" AND ((reason_id IN (?,?)) OR reason_id IS NULL)", result);

        ctx.assertEquals(new JsonArray(REASON_IDS), params);
    }

    @Test
    public void testFilterReasonsWithReasonDefinedAndRegularizedAndReasonIds(TestContext ctx) throws Exception {
        JsonArray params = new JsonArray();

        String result = Whitebox.invokeMethod(absenceService, "filterReasons",
                REASON_IDS, null, true, params);

        ctx.assertEquals(" AND ((reason_id IN (?,?))) AND (counsellor_regularisation = true) ", result);

        ctx.assertEquals(new JsonArray(REASON_IDS), params);
    }

    @Test
    public void testCountAbsentStudents(TestContext ctx) {
        String structureId = "AAA";
        List<String> studentIds = Arrays.asList("BBB", "CCC");
        String preparedStudentIds = Sql.listPrepared(studentIds);
        String startAt = "2023-02-10 12:00:00.000000";
        String endAt = "2023-02-10 13:00:00.000000";

        String expected = String.format("SELECT COUNT(*)" +
                        " FROM (SELECT DISTINCT student_id " +
                        " FROM presences.absence a " +
                        " WHERE structure_id = ? AND a.start_date < ? AND a.end_date > ? AND student_id in %s " +
                        " UNION DISTINCT " +
                        " SELECT DISTINCT student_id " +
                        " FROM presences.event a " +
                        " INNER JOIN presences.register r on a.register_id = r.id " +
                        " WHERE structure_id = ? AND a.start_date < ? AND a.end_date > ? AND student_id in %s) " +
                        " as student_ids ", preparedStudentIds, preparedStudentIds
        );

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(expected.trim().replaceAll("\\s+"," "),
                    query.trim().replaceAll("\\s+"," "));
            ctx.assertEquals(new JsonArray().add(structureId).add(endAt).add(startAt).addAll(new JsonArray(studentIds))
                            .add(structureId).add(endAt).add(startAt).addAll(new JsonArray(studentIds)), params);

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        absenceService.countAbsentStudents(structureId, studentIds, startAt, endAt);
    }

    @Test
    public void testCountAbsentStudentsWithoutStudentIdsFiler(TestContext ctx) {
        String structureId = "AAA";
        String startAt = "2023-02-10 12:00:00.000000";
        String endAt = "2023-02-10 13:00:00.000000";

        String expected = "SELECT COUNT(*)" +
                        " FROM (SELECT DISTINCT student_id " +
                        " FROM presences.absence a " +
                        " WHERE structure_id = ? AND a.start_date < ? AND a.end_date > ? " +
                        " UNION DISTINCT " +
                        " SELECT DISTINCT student_id " +
                        " FROM presences.event a " +
                        " INNER JOIN presences.register r on a.register_id = r.id " +
                        " WHERE structure_id = ? AND a.start_date < ? AND a.end_date > ?) " +
                        " as student_ids ";

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String query = invocation.getArgument(0);
            JsonArray params = invocation.getArgument(1);

            ctx.assertEquals(expected.trim().replaceAll("\\s+"," "),
                    query.trim().replaceAll("\\s+"," "));
            ctx.assertEquals(new JsonArray().add(structureId).add(endAt).add(startAt)
                            .add(structureId).add(endAt).add(startAt), params);

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        absenceService.countAbsentStudents(structureId, null, startAt, endAt);
    }
}