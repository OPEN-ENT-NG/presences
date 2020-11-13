package fr.openent.presences.model;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class SubjectModelTest {

    JsonObject subjectJsonObject_1 = new JsonObject()
            .put("id", "826343-15929165224768")
            .put("externalId", "3285$EPS")
            .put("name", "EPS")
            .put("code", "EPS")
            .put("rank", 4);

    @Test
    public void testSubjectNotNull(TestContext ctx) {
        JsonObject subjectObjectMock = mock(JsonObject.class);
        Subject subject = new Subject(subjectObjectMock);
        ctx.assertNotNull(subject);
    }

    @Test
    public void testSubjectHasContentWithObject(TestContext ctx) {
        Subject subject = new Subject(subjectJsonObject_1);
        boolean isNotEmpty = !subject.getId().isEmpty() &&
                !subject.getExternalId().isEmpty() &&
                !subject.getName().isEmpty() &&
                !subject.getRank().toString().isEmpty() &&
                !subject.getCode().isEmpty();
        ctx.assertTrue(isNotEmpty);
    }

    @Test
    public void testSubjectHasBeenInstantiated(TestContext ctx) {
        Subject subject = new Subject(subjectJsonObject_1);
        ctx.assertEquals(subject.toJSON(), subjectJsonObject_1);
    }

}
