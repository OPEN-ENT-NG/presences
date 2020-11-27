package fr.openent.presences.model;

import fr.openent.presences.model.Person.Student;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class StudentModelTest {

    private final JsonObject studentJsonObject_1 = new JsonObject()
            .put("id", "aaa751b4-3c04-4d58-8e93-71bd20396484")
            .put("firstName", "Hugo")
            .put("lastName", "MILIEN")
            .put("displayName", "MILIEN Hugo")
            .put("classId", "62173c9d-3bb3-44ec-9240-f30ecfb1b4b3")
            .put("classeName", "3EME1");

    @Test
    public void testStudentNotNull(TestContext ctx) {
        JsonObject studentObjectMock = mock(JsonObject.class);
        Student student = new Student(studentObjectMock);
        ctx.assertNotNull(student);
    }

    @Test
    public void testStudentHasContentWithObject(TestContext ctx) {
        Student student = new Student(studentJsonObject_1);
        boolean isNotEmpty = !student.getId().isEmpty() &&
                !student.getFirstName().isEmpty() &&
                !student.getLastName().isEmpty() &&
                !student.getName().isEmpty() &&
                !student.getClassId().isEmpty() &&
                !student.getClassName().isEmpty();
        ctx.assertTrue(isNotEmpty);
    }

}
