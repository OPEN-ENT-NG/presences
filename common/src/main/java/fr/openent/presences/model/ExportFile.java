package fr.openent.presences.model;

import io.vertx.core.buffer.*;

public class ExportFile {

    private final Buffer buffer;
    private final String contentType;
    private final String filename;

    public ExportFile(Buffer buffer, String contentType, String filename) {
        this.buffer = buffer;
        this.contentType = contentType;
        this.filename = filename;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFilename() {
        return filename;
    }

}
