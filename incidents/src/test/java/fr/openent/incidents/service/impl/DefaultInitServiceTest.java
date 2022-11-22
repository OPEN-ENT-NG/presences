package fr.openent.incidents.service.impl;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.InitTypeEnum;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
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
        PowerMockito.doAnswer(invocation -> invocation.getArgument(0))
                .when(i18n).translate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        PowerMockito.when(I18n.getInstance()).thenReturn(this.i18n);
        PowerMockito.doReturn("").when(Renders.class, "getHost", Mockito.any());
        PowerMockito.doReturn("").when(I18n.class, "acceptLanguage", Mockito.any());
    }

    @Test
    public void testGetInitIncidentSeriousnessStatement(TestContext ctx) {
        Async async = ctx.async();
        PowerMockito.doAnswer(invocation -> "true")
                .when(i18n).translate(Mockito.eq("incidents.init.2d.incident.seriousness.exclude.alert.2"), Mockito.anyString(), Mockito.anyString());
        JsonHttpServerRequest request = Mockito.mock(JsonHttpServerRequest.class);
        String structure = "structureId";
        InitTypeEnum initTypeEnum = InitTypeEnum.TWO_D;
        String statement = "INSERT INTO incidents.seriousness(structure_id, label, level, exclude_alert_seriousness) VALUES (?, ?, ?, ?),(?, ?, ?, ?),(?, ?, ?, ?),(?, ?, ?, ?),(?, ?, ?, ?);";
        String values = "[\"structureId\",\"incidents.init.2d.incident.seriousness.0\",0,false,\"structureId\"," +
                "\"incidents.init.2d.incident.seriousness.1\",2,false,\"structureId\",\"incidents.init.2d.incident.seriousness.2\"," +
                "4,true,\"structureId\",\"incidents.init.2d.incident.seriousness.3\",5,false,\"structureId\"," +
                "\"incidents.init.2d.incident.seriousness.4\",7,false]";

        this.defaultInitService.getInitIncidentSeriousnessStatement(request, structure, initTypeEnum, event -> {
            JsonObject result = event.right().getValue();
            ctx.assertEquals(result.getString(Field.STATEMENT), statement);
            ctx.assertEquals(result.getJsonArray(Field.VALUES).toString(), values);
            ctx.assertEquals(result.getString(Field.ACTION), Field.PREPARED);
            async.complete();
        });
        async.awaitSuccess(10000);
    }
}
