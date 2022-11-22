package fr.openent.incidents.helper;

import fr.openent.incidents.model.Punishment;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.core.constants.Field;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;

@RunWith(PowerMockRunner.class) //Using the PowerMock runner
@PowerMockRunnerDelegate(VertxUnitRunner.class) //And the Vertx runner
@PrepareForTest({DateHelper.class}) //Prepare the static class you want to test
public class PunishmentHelperTest {
    private PunishmentHelper punishmentHelper;

    @Before
    public void setUp() {
        punishmentHelper = new PunishmentHelper(Vertx.vertx().eventBus());
    }

    @Test
    public void testSortPunishmentByField(TestContext ctx) throws Exception {
        JsonObject student1 = new JsonObject().put(Field.NAME, "name1").put(Field.CLASSNAME, "class2");
        JsonObject student2 = new JsonObject().put(Field.NAME, "name1").put(Field.CLASSNAME, "class1");
        JsonObject owner1 = new JsonObject().put(Field.DISPLAYNAME, "displayName2");
        JsonObject owner2 = new JsonObject().put(Field.DISPLAYNAME, "displayName1");
        JsonObject type1 = new JsonObject().put(Field.LABEL, "label1");
        JsonObject type2 = new JsonObject().put(Field.LABEL, "label2");
        JsonObject punishment1 = new JsonObject().put(Field.STUDENT, student1).put(Field.OWNER, owner1).put(Field.TYPE, type1);
        JsonObject punishment2 = new JsonObject().put(Field.STUDENT, student2).put(Field.OWNER, owner2).put(Field.TYPE, type2);
        JsonArray punishments = new JsonArray(Arrays.asList(punishment1, punishment2));
        String order = Field.TYPE;
        JsonArray jsonArray = Whitebox.invokeMethod(this.punishmentHelper, "sortPunishmentByField", punishments, order, false);
        ctx.assertEquals("[{\"student\":{\"name\":\"name1\",\"className\":\"class1\"},\"owner\":{\"displayName\":\"displayName1\"}," +
                "\"type\":{\"label\":\"label2\"}},{\"student\":{\"name\":\"name1\",\"className\":\"class2\"},\"owner\":{\"displayName\":\"displayName2\"}," +
                "\"type\":{\"label\":\"label1\"}}]", jsonArray.toString());

        order = Field.DISPLAYNAME;
        jsonArray = Whitebox.invokeMethod(this.punishmentHelper, "sortPunishmentByField", punishments, order, false);
        ctx.assertEquals("[{\"student\":{\"name\":\"name1\",\"className\":\"class1\"},\"owner\":{\"displayName\":\"displayName1\"}," +
                "\"type\":{\"label\":\"label2\"}},{\"student\":{\"name\":\"name1\",\"className\":\"class2\"},\"owner\":{\"displayName\":\"displayName2\"}," +
                "\"type\":{\"label\":\"label1\"}}]", jsonArray.toString());

        order = Field.CLASSNAME;
        jsonArray = Whitebox.invokeMethod(this.punishmentHelper, "sortPunishmentByField", punishments, order, false);
        ctx.assertEquals("[{\"student\":{\"name\":\"name1\",\"className\":\"class2\"},\"owner\":{\"displayName\":\"displayName2\"}," +
                "\"type\":{\"label\":\"label1\"}},{\"student\":{\"name\":\"name1\",\"className\":\"class1\"},\"owner\":{\"displayName\":\"displayName1\"}," +
                "\"type\":{\"label\":\"label2\"}}]", jsonArray.toString());

        order = Field.OWNER;
        jsonArray = Whitebox.invokeMethod(this.punishmentHelper, "sortPunishmentByField", punishments, order, false);
        ctx.assertEquals("[{\"student\":{\"name\":\"name1\",\"className\":\"class2\"},\"owner\":{\"displayName\":\"displayName2\"}," +
                "\"type\":{\"label\":\"label1\"}},{\"student\":{\"name\":\"name1\",\"className\":\"class1\"},\"owner\":{\"displayName\":\"displayName1\"}," +
                "\"type\":{\"label\":\"label2\"}}]", jsonArray.toString());

        jsonArray = Whitebox.invokeMethod(this.punishmentHelper, "sortPunishmentByField", punishments, order, true);
        ctx.assertEquals("[{\"student\":{\"name\":\"name1\",\"className\":\"class1\"},\"owner\":{\"displayName\":\"displayName1\"}," +
                "\"type\":{\"label\":\"label2\"}},{\"student\":{\"name\":\"name1\",\"className\":\"class2\"},\"owner\":{\"displayName\":\"displayName2\"}," +
                "\"type\":{\"label\":\"label1\"}}]", jsonArray.toString());

        order = null;
        jsonArray = Whitebox.invokeMethod(this.punishmentHelper, "sortPunishmentByField", punishments, order, false);
        ctx.assertEquals("[{\"student\":{\"name\":\"name1\",\"className\":\"class1\"},\"owner\":{\"displayName\":\"displayName1\"}," +
                "\"type\":{\"label\":\"label2\"}},{\"student\":{\"name\":\"name1\",\"className\":\"class2\"},\"owner\":{\"displayName\":\"displayName2\"}," +
                "\"type\":{\"label\":\"label1\"}}]", jsonArray.toString());
    }

    @Test
    public void testGetStartDateFromPunishment(TestContext ctx) throws Exception {
        PowerMockito.spy(DateHelper.class);
        PowerMockito.doReturn("getDateStringMock").when(DateHelper.class, "getDateString", Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        PowerMockito.doReturn("").when(DateHelper.class, "getCurrentDate", Mockito.any(), Mockito.any());
        Punishment punishment = new Punishment();
        JsonObject fields = new JsonObject().put(Field.START_AT, "startAt").put(Field.END_AT, "endAt");
        punishment.setFields(fields);
        ctx.assertEquals(PunishmentHelper.getStartDateFromPunishment(punishment), "startAt");

        fields = new JsonObject().put(Field.DELAY_AT, "delayAt");
        punishment.setFields(fields);
        ctx.assertEquals(PunishmentHelper.getStartDateFromPunishment(punishment), "delayAt");

        punishment.setFields(new JsonObject());
        ctx.assertEquals(PunishmentHelper.getStartDateFromPunishment(punishment), "getDateStringMock");

        punishment.setCreated_at("createAt");
        ctx.assertEquals(PunishmentHelper.getStartDateFromPunishment(punishment), "createAt");
    }
}
