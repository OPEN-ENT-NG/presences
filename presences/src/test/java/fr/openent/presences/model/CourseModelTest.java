package fr.openent.presences.model;

import fr.openent.presences.core.constants.*;
import io.vertx.core.json.*;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.junit.*;
import org.junit.*;
import org.junit.runner.*;

import java.util.*;

@RunWith(VertxUnitRunner.class)
public class CourseModelTest {


    JsonObject courseJsonObject_1 = new JsonObject()
            .put(Field._ID, "826343-15929165224768")
            .put(Field.STRUCTUREID, "structureId")
            .put(Field.SUBJECTID, "9291652247")
            .put(Field.CLASSES, new JsonArray().add(new JsonObject().put("id", "id")))
            .put(Field.EXCEPTIONNAL, "exceptionnal")
            .put(Field.GROUPS, new JsonArray().add(new JsonObject().put("id", "id")))
            .put(Field.ROOMLABELS, new JsonArray().add(new JsonObject().put("id", "id")))
            .put(Field.EVENTS, new JsonArray().add(new JsonObject().put("id", "id")))
            .put(Field.EXEMPTED, false)
            .put(Field.EXEMPTION, new JsonObject().put("id", "id"))
            .put(Field.INCIDENT, new JsonObject().put("id", "id"))
            .put(Field.DAYOFWEEK, 6)
            .put(Field.MANUAL, false)
            .put(Field.LOCKED, false)
            .put(Field.UPDATED, "2021-07-06 00:00:00")
            .put(Field.LASTUSER, "lastUser")
            .put(Field.STARTDATE, "2021-07-06 00:00:00")
            .put(Field.ENDDATE, "2021-11-23 00:00:00")
            .put(Field.STARTCOURSE, "startCourse")
            .put(Field.ENDCOURSE, "endCourse")
            .put(Field.STARTMOMENTDATE, "2021-07-06")
            .put(Field.STARTMOMENTTIME, "00:00:00")
            .put(Field.ENDMOMENTDATE, "2021-11-23")
            .put(Field.ENDMOMENTTIME, "00:00:00")
            .put(Field.IS_RECURRENT, false)
            .put(Field.COLOR, "red")
            .put(Field.IS_PERIODIC, true)
            .put(Field.SUBJECTNAME, "Histoire")
            .put(Field.TEACHERS, new JsonArray().add(new JsonObject().put("id", "id")))
            .put(Field.SPLIT_SLOT, true)
            .put(Field.SUBJECT, new JsonObject()
                    .put(Field.ID, "id")
                    .put("externalId", "externalId")
                    .put(Field.NAME, "name")
                    .put("rank", 1)
                    .put("code", "code"))
            .put(Field.ISOPENEDBYPERSONNEL, true)
            .put(Field.ALLOWREGISTER, true);


    @Test
    public void testCourseHasContentWithObject(TestContext ctx) {
        Course course = new Course(courseJsonObject_1, new ArrayList<>(Collections.singletonList("_id")));
        boolean isNotEmpty = !course.getId().isEmpty() &&
                !course.getStructureId().isEmpty() &&
                !course.getSubjectId().isEmpty() &&
                !course.getClasses().toString().isEmpty() &&
                !course.getExceptionnal().isEmpty() &&
                !course.getGroups().toString().isEmpty() &&
                !course.getRoomLabels().toString().isEmpty() &&
                !course.getEvents().toString().isEmpty() &&
                !course.isExempted().toString().isEmpty() &&
                !course.getExemption().isEmpty() &&
                !course.getIncident().isEmpty() &&
                !course.getDayOfWeek().toString().isEmpty() &&
                !course.isManual().toString().isEmpty() &&
                !course.isLocked().toString().isEmpty() &&
                !course.getUpdated().isEmpty() &&
                !course.getLastUser().isEmpty() &&
                !course.getStartDate().isEmpty() &&
                !course.getEndDate().isEmpty() &&
                !course.getStartCourse().isEmpty() &&
                !course.getEndCourse().isEmpty() &&
                !course.getStartMomentDate().isEmpty() &&
                !course.getEndMomentDate().isEmpty() &&
                !course.getStartMomentTime().isEmpty() &&
                !course.getEndMomentTime().isEmpty() &&
                !course.isRecurrent().toString().isEmpty() &&
                !course.getColor().isEmpty() &&
                !course.isPeriodic().toString().isEmpty() &&
                !course.getSubjectName().isEmpty() &&
                !course.getTeachers().isEmpty() &&
                !course.isSplitSlot().toString().isEmpty() &&
                !course.getSubject().toString().isEmpty() &&
                !course.getIsOpenedByPersonnel().toString().isEmpty();

        ctx.assertTrue(isNotEmpty);
    }

    @Test
    public void testCourseHasBeenInstantiated(TestContext ctx) {
        Course course = new Course(courseJsonObject_1, new ArrayList<>());

        ctx.assertEquals(course.toJSON(), courseJsonObject_1);
    }
}
