package fr.openent.presences.common.service;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.pdf.Pdf;
import org.entcore.common.user.UserInfos;

public interface ExportPDFService {
    /**
     * Generation of a PDF from an XHTML template
     *
     * @param request Http request
     * @param html    html text string wished to be converted into Pdf
     * @param user    User Infos
     * @param handler Handler returning data buffer type
     */
    void generatePDF(HttpServerRequest request, String html, UserInfos user, Handler<Buffer> handler);

    /**
     * Generation of a PDF
     *
     * @param filename  filename for pdf name
     * @param buffer    Buffer wanted to generate PDF (usually done after a template rendered) or HTML content
     *
     * @return {@link Pdf} result
     */
    Future<Pdf> generatePDF(String filename, String buffer);
}