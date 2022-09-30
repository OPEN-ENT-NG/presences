package fr.openent.presences.common.helper;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.EventModel;
import fr.openent.presences.model.Person.User;
import fr.openent.presences.model.SlotModel;
import fr.openent.presences.model.TimeslotModel;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(VertxUnitRunner.class)
public class EventsHelperTest {

    @Test
    public void testGroupBySuccessiveEvent(TestContext ctx) throws Exception {
        List<SlotModel> slotModelList = Arrays.asList(new SlotModel().setId("slotId1").setStartHour("08:00"),
                new SlotModel().setId("slotId2").setStartHour("09:00"), new SlotModel().setId("slotId3").setStartHour("10:00"),
                new SlotModel().setId("slotId4").setStartHour("11:00"));
        TimeslotModel timeslotModel = new TimeslotModel().setSlots(slotModelList);
        EventModel event1 = new EventModel().setId(1).setStartDate("2021-12-13T08:30:00").setOwner(new User(""));
        EventModel event2 = new EventModel().setId(2).setStartDate("2021-12-13T09:30:00").setOwner(new User(""));
        EventModel event3 = new EventModel().setId(3).setStartDate("2021-12-13T11:30:00").setOwner(new User(""));
        List<EventModel > eventList = Arrays.asList(event1, event2, event3);
        List<JsonObject> newEventList = Whitebox.invokeMethod(EventsHelper.class, "groupBySuccessiveEvent", timeslotModel, eventList);

        final List<List<EventModel>> list = newEventList.stream()
                .map(events -> events.getJsonArray(Field.EVENTS))
                .map(eventJsonArray -> IModelHelper.toList(eventJsonArray, EventModel.class))
                .collect(Collectors.toList());

        ctx.assertEquals(list.size(), 2);
        ctx.assertEquals(list.get(0).size(), 2);
        ctx.assertEquals(list.get(1).size(), 1);
        ctx.assertEquals(list.get(0).get(0).getId(), event1.getId());
        ctx.assertEquals(list.get(0).get(1).getId(), event2.getId());
        ctx.assertEquals(list.get(1).get(0).getId(), event3.getId());
    }
}
