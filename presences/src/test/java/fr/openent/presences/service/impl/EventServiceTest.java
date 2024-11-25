package fr.openent.presences.service.impl;

import fr.openent.presences.core.constants.*;
import fr.openent.presences.db.*;
import fr.openent.presences.db.DB;
import fr.openent.presences.enums.EventRecoveryMethodEnum;
import fr.openent.presences.service.*;
import io.vertx.core.*;
import io.vertx.core.json.*;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.junit.*;
import org.entcore.common.sql.*;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.stubbing.*;
import org.powermock.reflect.Whitebox;

import java.util.*;

@RunWith(VertxUnitRunner.class)
public class EventServiceTest extends DBService {

    Sql sql = Mockito.mock(Sql.class);

    private EventService eventService;

    @Before
    public void setUp() {
        DB.getInstance().init(null, sql, null);
        this.eventService = new DefaultEventService(Vertx.vertx().eventBus());
    }

    @Test
    public void testGetEvents(TestContext ctx) {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            JsonArray params = invocation.getArgument(1);



            ctx.assertEquals(params, new JsonArray()
                    .add(Field.STRUCTURE_ID)
                    .add(Field.EVENT_TYPE)
                    .add(Field.START_DATE + " 00:00:00")
                    .add(Field.END_DATE + " 23:59:59")
                    .add(Field.END_TIME)
                    .add(Field.START_TIME)
                    .add(true)
                    .add(Field.EVENT_TYPE)
                    .add(0)
                    .add(20));

            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        eventService.get(Field.STRUCTURE_ID, Field.START_DATE, Field.END_DATE,
                Field.START_TIME, Field.END_TIME,  Collections.singletonList(Field.EVENT_TYPE), Collections.singletonList(Field.REASON_ID), false, false, new ArrayList<>(),
                new ArrayList<>(), true, true, 0, handler -> {});
    }

    @Test
    public void testGetEventQuery(TestContext ctx) throws Exception {
        List<String> students = new ArrayList<>();
        List<Integer> reasonsId = new ArrayList<>();
        JsonObject res = Whitebox.invokeMethod(eventService, "getEventQuery",0, students, "structure",
                reasonsId, true, true, "startDate", "endDate", false, "InvalidRecoveryMethod", "startTime", "endTime",
                "limit", "offset", true, true);

        ctx.assertEquals(res.toString(), "{\"query\":\"SELECT event.student_id, event.start_date, event.end_date, event.type_id," +
                " 'HOUR' as recovery_method, json_agg(json_build_object('id', event.id, 'start_date', event.start_date, 'end_date', event.end_date," +
                " 'comment', event.comment, 'counsellor_input', event.counsellor_input, 'student_id', event.student_id, 'register_id'," +
                " event.register_id, 'type_id', event.type_id, 'reason_id', event.reason_id, 'owner', event.owner, 'created', event.created," +
                " 'counsellor_regularisation', event.counsellor_regularisation, 'followed', event.followed, 'massmailed', event.massmailed," +
                " 'reason', json_build_object('id', reason.id, 'absence_compliance', reason.absence_compliance))) as events FROM" +
                " null.event LEFT JOIN presences.reason ON (reason.id = event.reason_id) INNER JOIN presences.register ON (register.id" +
                " = event.register_id) WHERE event.start_date >= ? AND event.end_date<= ? AND register.structure_id = ? AND type_id =" +
                " ? AND (reason.absence_compliance IS true OR reason.absence_compliance IS NULL)  AND ((type_id IN (?) AND type_id NOT" +
                " IN (1,2))) AND massmailed = ?  GROUP BY event.start_date, event.student_id, event.end_date, event.type_id  LIMIT ?" +
                "  OFFSET ? \",\"params\":[\"startDate 00:00:00\",\"endDate 23:59:59\",\"structure\",0,\"0\",true,\"limit\",\"offset\"]}");

        res = Whitebox.invokeMethod(eventService, "getEventQuery",0, students, "structure",
                reasonsId, true, true, "startDate", "endDate", false, EventRecoveryMethodEnum.HOUR.getValue(), "startTime", "endTime",
                "limit", "offset", true, true);

        ctx.assertEquals(res.toString(), "{\"query\":\"SELECT event.student_id, event.start_date, event.end_date, event.type_id," +
                " 'HOUR' as recovery_method, json_agg(json_build_object('id', event.id, 'start_date', event.start_date, 'end_date'," +
                " event.end_date, 'comment', event.comment, 'counsellor_input', event.counsellor_input, 'student_id', event.student_id," +
                " 'register_id', event.register_id, 'type_id', event.type_id, 'reason_id', event.reason_id, 'owner', event.owner," +
                " 'created', event.created, 'counsellor_regularisation', event.counsellor_regularisation, 'followed', event.followed," +
                " 'massmailed', event.massmailed, 'reason', json_build_object('id', reason.id, 'absence_compliance', reason.absence_compliance)))" +
                " as events FROM null.event LEFT JOIN presences.reason ON (reason.id = event.reason_id) INNER JOIN presences.register" +
                " ON (register.id = event.register_id) WHERE event.start_date >= ? AND event.end_date<= ? AND register.structure_id =" +
                " ? AND type_id = ? AND (reason.absence_compliance IS true OR reason.absence_compliance IS NULL)  AND ((type_id IN (?)" +
                " AND type_id NOT IN (1,2))) AND massmailed = ?  GROUP BY event.start_date, event.student_id, event.end_date," +
                " event.type_id  LIMIT ?  OFFSET ? \",\"params\":[\"startDate 00:00:00\",\"endDate 23:59:59\",\"structure\",0,\"0\"," +
                "true,\"limit\",\"offset\"]}");

        res = Whitebox.invokeMethod(eventService, "getEventQuery",0, students, "structure",
                reasonsId, true, true, "startDate", "endDate", false, EventRecoveryMethodEnum.HALF_DAY.getValue(), "startTime", "endTime",
                "limit", "offset", true, true);

        ctx.assertEquals(res.toString(), "{\"query\":\"SELECT event.student_id, event.start_date::date, event.end_date::date," +
                " event.type_id, 'HALF_DAY' as recovery_method, json_agg(json_build_object('id', event.id, 'start_date', event.start_date," +
                " 'end_date', event.end_date, 'comment', event.comment, 'counsellor_input', event.counsellor_input, 'student_id'," +
                " event.student_id, 'register_id', event.register_id, 'type_id', event.type_id, 'reason_id', event.reason_id, 'owner'," +
                " event.owner, 'created', event.created, 'counsellor_regularisation', event.counsellor_regularisation, 'followed'," +
                " event.followed, 'massmailed', event.massmailed, 'reason', json_build_object('id', reason.id, 'absence_compliance'," +
                " reason.absence_compliance))) as events FROM null.event LEFT JOIN presences.reason ON (reason.id = event.reason_id)" +
                " INNER JOIN presences.register ON (register.id = event.register_id) WHERE event.start_date::date >= ? AND event.end_date::date<=" +
                " ? AND register.structure_id = ? AND type_id = ? AND (reason.absence_compliance IS true OR reason.absence_compliance" +
                " IS NULL)  AND event.start_date::time > ? AND event.start_date::time < ? AND ((type_id IN (?) AND type_id NOT IN" +
                " (1,2))) AND massmailed = ?  GROUP BY event.start_date::date, event.student_id, event.end_date::date, event.type_id" +
                "  LIMIT ?  OFFSET ? \",\"params\":[\"startDate 00:00:00\",\"endDate 23:59:59\",\"structure\",0,\"startTime\",\"endTime\"" +
                ",\"0\",true,\"limit\",\"offset\"]}");

        res = Whitebox.invokeMethod(eventService, "getEventQuery",0, students, "structure",
                reasonsId, true, true, "startDate", "endDate", false, EventRecoveryMethodEnum.DAY.getValue(), "startTime", "endTime",
                "limit", "offset", true, true);

        ctx.assertEquals(res.toString(), "{\"query\":\"SELECT event.student_id, event.start_date::date, event.end_date::date," +
                " event.type_id, 'DAY' as recovery_method, json_agg(json_build_object('id', event.id, 'start_date', event.start_date," +
                " 'end_date', event.end_date, 'comment', event.comment, 'counsellor_input', event.counsellor_input, 'student_id'," +
                " event.student_id, 'register_id', event.register_id, 'type_id', event.type_id, 'reason_id', event.reason_id," +
                " 'owner', event.owner, 'created', event.created, 'counsellor_regularisation', event.counsellor_regularisation," +
                " 'followed', event.followed, 'massmailed', event.massmailed, 'reason', json_build_object('id', reason.id," +
                " 'absence_compliance', reason.absence_compliance))) as events FROM null.event LEFT JOIN presences.reason ON (reason.id" +
                " = event.reason_id) INNER JOIN presences.register ON (register.id = event.register_id) WHERE event.start_date::date" +
                " >= ? AND event.end_date::date<= ? AND register.structure_id = ? AND type_id = ? AND (reason.absence_compliance IS" +
                " true OR reason.absence_compliance IS NULL)  AND ((type_id IN (?) AND type_id NOT IN (1,2))) AND massmailed = ?" +
                "  GROUP BY event.start_date::date, event.student_id, event.end_date::date, event.type_id  LIMIT ?  OFFSET ? \"," +
                "\"params\":[\"startDate 00:00:00\",\"endDate 23:59:59\",\"structure\",0,\"0\",true,\"limit\",\"offset\"]}");
    }
}