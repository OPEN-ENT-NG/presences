package fr.openent.presences.common.service.impl;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

@RunWith(VertxUnitRunner.class)
public class ExportPDFServiceImplTest {
    private Vertx vertx;

    @Before
    public void setUp() {
        this.vertx = Vertx.vertx();
    }

    @Test
    public void testExportPDFServiceImpl(TestContext ctx) {
        JsonObject config = new JsonObject();
        ExportPDFServiceImpl exportPDFService = new ExportPDFServiceImpl(vertx, config);
        ctx.assertNull(Whitebox.getInternalState(exportPDFService, "authHeader"));
        ctx.assertNull(Whitebox.getInternalState(exportPDFService, "pdfGeneratorURL"));

        config = new JsonObject().put("pdf-generator", new JsonObject().put("auth", "authString").put("url", "urlString"));
        exportPDFService = new ExportPDFServiceImpl(vertx, config);
        ctx.assertEquals(Whitebox.getInternalState(exportPDFService, "authHeader"), "Basic authString");
        ctx.assertEquals(Whitebox.getInternalState(exportPDFService, "pdfGeneratorURL"), "urlString");
    }

    @Test
    public void testWebServiceNodePdfGeneratorPostInvalidAuth(TestContext ctx) throws Exception {
        Async async = ctx.async();
        JsonObject config = new JsonObject();
        ExportPDFServiceImpl exportPDFService = new ExportPDFServiceImpl(vertx, config);

        Handler<Either<String, Buffer>> handler = event -> {
            ctx.assertTrue(event.isLeft());
            async.complete();
        };

        Whitebox.invokeMethod(exportPDFService, "webServiceNodePdfGeneratorPost", "file", "token", "nodePdfGeneratorUrl", handler);
        async.awaitSuccess(10000);
    }
}