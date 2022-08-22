package fr.openent.presences.model;

import io.vertx.core.buffer.*;
import io.vertx.ext.unit.*;
import io.vertx.ext.unit.junit.*;
import org.junit.*;
import org.junit.runner.*;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class ExportFileTest {

    @Test
    public void testExportFileNotNull(TestContext ctx) {
        Buffer bufferMock = mock(Buffer.class);
        ExportFile exportFile = new ExportFile(bufferMock, "content-type", "filename");
        ctx.assertNotNull(exportFile);
    }

    @Test
    public void testExportFileHasContentWithObject(TestContext ctx) {
        Buffer bufferMock = mock(Buffer.class);
        ExportFile exportFile = new ExportFile(bufferMock, "content-type", "filename");
        boolean isNotEmpty = !exportFile.getBuffer().toString().isEmpty() &&
                !exportFile.getContentType().isEmpty() &&
                !exportFile.getFilename().isEmpty();
        ctx.assertTrue(isNotEmpty);
    }

}
