package fr.openent.massmailing.service.impl;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.service.SettingsService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class DefaultSettingsServiceTest {

    private final Sql sql = mock(Sql.class);
    private Vertx vertx;
    private SettingsService settingsService;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.massmailing");
        this.settingsService = new DefaultSettingsService();
    }

    @Test
    public void testGetTemplatesAll(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT id, name, content, type, structure_id, category FROM massmailing.template WHERE structure_id = ? AND type = ?;";
        JsonArray expectedParams = new JsonArray().add("STRUCTURE_ID").add(MailingType.MAIL.toString());

        vertx.eventBus().consumer("fr.openent.massmailing", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());

            async.complete();
        });

        settingsService.getTemplates(MailingType.MAIL, "STRUCTURE_ID", Collections.singletonList("ALL"));
    }

    @Test
    public void testGetTemplatesFilteredByCategory(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT id, name, content, type, structure_id, category FROM massmailing.template" +
                " WHERE structure_id = ? AND type = ?AND ( 0 = 1 OR category = ? OR category = ? OR category = ?);";
        JsonArray expectedParams = new JsonArray().add("STRUCTURE_ID")
                .add(MailingType.MAIL.toString())
                .add("LATENESS")
                .add("PUNISHMENT_SANCTION")
                .add("ALL");

        vertx.eventBus().consumer("fr.openent.massmailing", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());

            async.complete();
        });

        settingsService.getTemplates(MailingType.MAIL, "STRUCTURE_ID", Arrays.asList("LATENESS", "PUNISHMENT_SANCTION"));
    }

    @Test
    public void testCreateTemplate(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "INSERT INTO massmailing.template (structure_id, name, content, type, owner, category)" +
                " VALUES (?, ?, ?, ?, ?, ?) RETURNING id, name, content, type, structure_id, category";
        JsonArray expectedParams = new JsonArray().add("structure_id")
                .add("name")
                .add("content")
                .add("MAIL")
                .add("USER_ID")
                .add("ALL");

        vertx.eventBus().consumer("fr.openent.massmailing", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());

            async.complete();
        });

        JsonObject template = new JsonObject()
                .put("type", "MAIL")
                .put("content", "content")
                .put("category", "ALL")
                .put("structure_id", "structure_id")
                .put("name", "name");
        settingsService.createTemplate(template, "USER_ID", (Handler) e -> {
        });
    }

    @Test
    public void updateTemplate(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "UPDATE massmailing.template SET structure_id = ?, name = ?, content = ?, type = ?," +
                " category = ? WHERE id = ? RETURNING id, name, content, type, structure_id, category";
        JsonArray expectedParams = new JsonArray().add("structure_id")
                .add("name")
                .add("content")
                .add("MAIL")
                .add("ALL")
                .add(3);

        vertx.eventBus().consumer("fr.openent.massmailing", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());

            async.complete();
        });

        JsonObject template = new JsonObject()
                .put("type", "MAIL")
                .put("content", "content")
                .put("category", "ALL")
                .put("structure_id", "structure_id")
                .put("name", "name");

        settingsService.updateTemplate(3, template, (Handler) e -> {
        });
    }

    @Test
    public void testDeleteTemplate(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "DELETE FROM massmailing.template WHERE id = ?;";
        JsonArray expectedParams = new JsonArray()
                .add(3);

        vertx.eventBus().consumer("fr.openent.massmailing", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());

            async.complete();
        });

        settingsService.deleteTemplate(3, (Handler) e -> {
        });
    }

    @Test
    public void testGetTemplate(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT name, content FROM massmailing.template WHERE id = ? AND structure_id = ? AND type = ?;";
        JsonArray expectedParams = new JsonArray()
                .add(3)
                .add("STRUCTURE_ID")
                .add(MailingType.MAIL.toString());

        vertx.eventBus().consumer("fr.openent.massmailing", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());

            async.complete();
        });

        settingsService.get(MailingType.MAIL, 3, "STRUCTURE_ID", (Handler) e -> {
        });
    }

    @Test
    public void testIsRespectedSmsLengthContent(TestContext ctx) throws Exception {
        Async async = ctx.async();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append("20 caractaires .....");
        }

        boolean res = Whitebox.invokeMethod(this.settingsService, "isRespectedSmsLengthContent",
                MailingType.MAIL.toString(), sb.toString(), (Handler) e -> {
                });
        ctx.assertTrue(res);

        res = Whitebox.invokeMethod(this.settingsService, "isRespectedSmsLengthContent",
                MailingType.PDF.toString(), sb.toString(), (Handler) e -> {
                });
        ctx.assertTrue(res);

        sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append("20 caractaires .....");
        }

        res = Whitebox.invokeMethod(this.settingsService, "isRespectedSmsLengthContent",
                MailingType.SMS.toString(), sb.toString(), (Handler) e -> {
                });
        ctx.assertTrue(res);
        sb.append(".");
        res = Whitebox.invokeMethod(this.settingsService, "isRespectedSmsLengthContent",
                MailingType.SMS.toString(), sb.toString(), (Handler) e -> {
                });
        ctx.assertFalse(res);

        async.complete();
    }
}
