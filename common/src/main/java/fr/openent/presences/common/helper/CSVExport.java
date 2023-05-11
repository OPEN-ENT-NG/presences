package fr.openent.presences.common.helper;

import fr.openent.presences.model.*;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.buffer.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

public abstract class CSVExport {
    public static final Logger LOGGER = LoggerFactory.getLogger(CSVExport.class);
    private I18n i18n;
    public StringBuilder value;
    public String SEPARATOR;
    public String EOL;
    public String SPACE;
    public String header;
    public String filename;

    public HttpServerRequest request;
    private static final String UTF8_BOM = "\uFEFF";

    public CSVExport() {
        this.i18n = I18n.getInstance();
        this.value = new StringBuilder(UTF8_BOM);
        this.SEPARATOR = ";";
        this.EOL = "\n";
        this.SPACE = " ";
        this.header = "";
        this.filename = "";
    }

    public void export() {
        if (this.request == null) {
            LOGGER.error("[Common@CSVExport] Failed to export CSV due to null request");
            return;
        }
        this.generate();
        String name = i18n.translate(this.filename, Renders.getHost(this.request), I18n.acceptLanguage(this.request));
        this.request.response()
                .putHeader("Content-Type", "text/csv; charset=utf-8")
                .putHeader("Content-Disposition", "attachment; filename=" + name)
                .end(this.value.toString());
    }

    public ExportFile getExportFile(String domain, String local) {
        this.generate();
        String name = i18n.translate(this.filename, domain, local);
        Buffer buffer = Buffer.buffer(this.value.toString());
        return new ExportFile(buffer, "text/csv; charset=utf-8", name);
    }

    public void export(HttpServerRequest request) {
        this.setRequest(request);
        this.export();
    }

    public void setRequest(HttpServerRequest request) {
        this.request = request;
    }

    public void setHeader(String header) {
        this.header = header + this.EOL;
        this.value.append(this.header);
    }

    public void setHeader(List<String> headers) {
        StringBuilder line = new StringBuilder();
        for (String head : headers) {
            if (this.request != null) {
                line.append(i18n.translate(head, Renders.getHost(this.request), I18n.acceptLanguage(this.request)))
                        .append(this.SEPARATOR);
            } else {
                line.append(head)
                        .append(this.SEPARATOR);
            }
        }

        this.setHeader(line.toString());
    }

    public void setHeader(List<String> headers, String domain, String locale) {
        StringBuilder line = new StringBuilder();
        for (String head : headers) {
                line.append(i18n.translate(head, domain, locale)).append(this.SEPARATOR);
        }

        this.setHeader(line.toString());
    }

    public String translate(String key) {
        return i18n.translate(key, Renders.getHost(this.request), I18n.acceptLanguage(this.request));
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public abstract void generate();
}
