package fr.openent.presences.service.impl;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.InitTypeEnum;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({I18n.class, Renders.class}) //Prepare the static class you want to test
public class DefaultInitServiceTest {
    DefaultInitService defaultInitService;
    I18n i18n;

    @Before
    public void setUp() throws Exception {
        this.defaultInitService = new DefaultInitService();
        PowerMockito.spy(I18n.class);
        PowerMockito.spy(Renders.class);
        this.i18n = PowerMockito.spy(I18n.getInstance());
        PowerMockito.doAnswer(invocation -> invocation.getArgument(0)).when(i18n).translate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        PowerMockito.when(I18n.getInstance()).thenReturn(this.i18n);
        PowerMockito.doReturn(null).when(Renders.class, "getHost", Mockito.any());
        PowerMockito.doReturn(null).when(I18n.class, "acceptLanguage", Mockito.any());
    }

    @Test
    public void testGetReasonsStatement(TestContext ctx) {
        Async async = ctx.async();
        PowerMockito.doAnswer(invocation -> "true")
                .when(this.i18n).translate(Mockito.eq("incidents.init.2d.incident.seriousness.exclude.alert.2"), Mockito.anyString(), Mockito.anyString());
        JsonHttpServerRequest request = Mockito.mock(JsonHttpServerRequest.class);
        String structure = "structureId";
        InitTypeEnum initTypeEnum = InitTypeEnum.TWO_D;
        String statement = "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) VALUES " +
                "(nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id)" +
                " VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id)" +
                " VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id)" +
                " VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) " +
                "VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) " +
                "VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id) VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);";
        String values = "[\"structureId\",\"presences.reasons.init.2d.0\",false,true,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.1\",true,false,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.2\",true,false,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.3\",false,false,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.4\",false,false,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.5\",true,false,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.6\",false,false,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.7\",false,true,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.8\",false,true,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.9\",true,false,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.10\",true,true,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.11\",true,true,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.12\",false,true,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"structureId\",\"presences.reasons.init.2d.13\",false,true,1,\"structureId\"," +
                "\"presences.reasons.init.2d.14\",true,true,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.15\",true,true,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.16\",true,true,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.17\",false,true,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.18\",true,false,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.19\",true,false,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.20\",false,true,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.21\",false,true,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.22\",true,false,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.23\",true,false,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.24\",false,true,1,\"structureId\"," +
                "\"presences.reasons.init.2d.25\",true,false,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.26\",true,false,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.27\",true,false,1,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.28\",false,true,1,\"structureId\"," +
                "\"presences.reasons.init.2d.29\",true,false,2,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.30\",true,false,2,\"structureId\",\"structureId\",\"structureId\",\"structureId\"," +
                "\"presences.reasons.init.2d.31\",true,true,2]";
        Promise<JsonObject> promise = Promise.promise();
        promise.future().onSuccess(result -> {
            ctx.assertEquals(result.getString(Field.STATEMENT), statement);
            ctx.assertEquals(result.getJsonArray(Field.VALUES).toString(), values);
            ctx.assertEquals(result.getString(Field.ACTION), Field.PREPARED);
            async.complete();
        });
        this.defaultInitService.getReasonsStatement(request, structure, initTypeEnum, promise);

        async.awaitSuccess(10000);
    }
}
