package fr.openent.statistics_presences.helper;

import fr.openent.presences.model.SlotModel;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class TimeslotHelperTest {

    @Test
    public void getSlotModelsFromPeriodTest(TestContext ctx) {
        SlotModel slotModel1 = new SlotModel().setName("slot1").setStartHour("08:30").setEndHour("09:25");
        SlotModel slotModel2 = new SlotModel().setName("slot2").setStartHour("09:30").setEndHour("10:25");
        SlotModel slotModel3 = new SlotModel().setName("slot3").setStartHour("10:30").setEndHour("11:25");
        SlotModel slotModel4 = new SlotModel().setName("slot4").setStartHour("11:30").setEndHour("12:25");
        List<SlotModel> slotList = Arrays.asList(slotModel1, slotModel2, slotModel3, slotModel4);

        List<SlotModel> actualList = TimeslotHelper.getSlotModelsFromPeriod("2000-10-20T10:00:00",
                "2000-10-20T12:00:00", slotList);

        ctx.assertEquals(actualList.size(), 3);
        ctx.assertEquals(actualList.get(0).getName(), slotModel2.getName());
        ctx.assertEquals(actualList.get(1).getName(), slotModel3.getName());
        ctx.assertEquals(actualList.get(2).getName(), slotModel4.getName());

        actualList = TimeslotHelper.getSlotModelsFromPeriod("2000-10-20T09:30:00",
                "2000-10-20T10:25:00", slotList);

        ctx.assertEquals(actualList.size(), 1);
        ctx.assertEquals(actualList.get(0).getName(), slotModel2.getName());

        actualList = TimeslotHelper.getSlotModelsFromPeriod("2000-10-20T19:30:00",
                "2000-10-20T20:25:00", slotList);

        ctx.assertEquals(actualList.size(), 0);
    }
}
