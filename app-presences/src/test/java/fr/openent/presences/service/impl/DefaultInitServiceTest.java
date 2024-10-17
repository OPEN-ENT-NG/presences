package fr.openent.presences.service.impl;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.InitTypeEnum;
import fr.openent.presences.enums.ReasonType;
import fr.openent.presences.helper.init.IInitPresencesHelper;
import fr.openent.presences.model.Action;
import fr.openent.presences.model.Discipline;
import fr.openent.presences.model.ReasonModel;
import fr.openent.presences.model.Settings;
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

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({I18n.class, Renders.class, IInitPresencesHelper.class}) //Prepare the static class you want to test
public class DefaultInitServiceTest {
    IInitPresencesHelper iInitPresencesHelper = new IInitPresencesHelper() {
        @Override
        public List<ReasonModel> getReasonsInit() {
            List<ReasonModel> list = new ArrayList<>();
            list.add(new ReasonModel().setLabel("presences.reasons.init.1d.0").setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
            list.add(new ReasonModel().setLabel("presences.reasons.init.1d.1").setUnregularizedAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
            list.add(new ReasonModel().setLabel("presences.reasons.init.1d.2").setRegularizedAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
            list.add(new ReasonModel().setLabel("presences.reasons.init.1d.3").setUnregularizedAlertExclude(false).setRegularizedAlertExclude(false).setProving(false).setAbsenceCompliance(true).setReasonTypeId(ReasonType.ABSENCE));
            list.add(new ReasonModel().setLabel("presences.reasons.init.1d.4").setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.LATENESS));
            list.add(new ReasonModel().setLabel("presences.reasons.init.1d.5").setLatenessAlertExclude(false).setProving(true).setAbsenceCompliance(false).setReasonTypeId(ReasonType.LATENESS));
            return list;
        }

        @Override
        public List<Action> getActionsInit() {
            return null;
        }

        @Override
        public Settings getSettingsInit() {
            return null;
        }

        @Override
        public List<Discipline> getDisciplinesInit() {
            return null;
        }
    };

    DefaultInitService defaultInitService;
    I18n i18n;

    @Before
    public void setUp() throws Exception {
        this.defaultInitService = new DefaultInitService();
        PowerMockito.spy(I18n.class);
        PowerMockito.spy(Renders.class);
        PowerMockito.spy(IInitPresencesHelper.class);
        this.i18n = PowerMockito.spy(I18n.getInstance());
        PowerMockito.doAnswer(invocation -> invocation.getArgument(0)).when(i18n).translate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        PowerMockito.when(I18n.getInstance()).thenReturn(this.i18n);
        PowerMockito.doReturn(null).when(Renders.class, "getHost", Mockito.any());
        PowerMockito.doReturn(null).when(I18n.class, "acceptLanguage", Mockito.any());
        PowerMockito.doReturn(iInitPresencesHelper).when(IInitPresencesHelper.class, "getDefaultInstance", Mockito.any());
    }

    @Test
    public void testGetReasonsStatement(TestContext ctx) {
        Async async = ctx.async();
        JsonHttpServerRequest request = Mockito.mock(JsonHttpServerRequest.class);
        String structure = "structureId";
        InitTypeEnum initTypeEnum = InitTypeEnum.TWO_D;
        String statement = "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id)" +
                " VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);" +
                "INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id)" +
                " VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id)" +
                " VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);" +
                "INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id)" +
                " VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id)" +
                " VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);" +
                "INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id)" +
                " VALUES (?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id)" +
                " VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);" +
                "INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id)" +
                " VALUES (?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id)" +
                " VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);" +
                "INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id)" +
                " VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2),(?, currval('presences.reason_id_seq'), 3);" +
                "INSERT INTO null.reason(id, structure_id, label, proving, comment, absence_compliance, reason_type_id)" +
                " VALUES (nextval('presences.reason_id_seq'), ?,?,?,'',?,?);" +
                "INSERT INTO null.reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id)" +
                " VALUES (?, currval('presences.reason_id_seq'), 1),(?, currval('presences.reason_id_seq'), 2);";
        String values = "[\"structureId\",\"presences.reasons.init.1d.0\",false,true,1,\"structureId\",\"structureId\",\"structureId\"," +
                "\"structureId\",\"presences.reasons.init.1d.1\",false,true,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"presences.reasons.init.1d.2\",false,true,1,\"structureId\",\"structureId\"," +
                "\"structureId\",\"presences.reasons.init.1d.3\",false,true,1,\"structureId\"," +
                "\"structureId\",\"presences.reasons.init.1d.4\",true,false,2,\"structureId\",\"structureId\",\"structureId\"," +
                "\"structureId\",\"presences.reasons.init.1d.5\",true,false,2,\"structureId\",\"structureId\"]";
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
