package fr.openent.massmailing.mailing;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.ExportPDFService;
import fr.openent.presences.common.service.impl.ExportPDFServiceImpl;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Pdf extends MassMailingProcessor {
    private final ExportPDFService exportPDFService;
    private final HttpServerRequest request;
    private final Storage storage;
    private final EventBus eb;

    public Pdf(EventBus eb, Vertx vertx, Storage storage, JsonObject config, HttpServerRequest request, String structure, Template template,
               Boolean massmailed, List<MassmailingType> massmailingTypeList, List<Integer> reasons, List<Integer> punishmentsTypes,
               List<Integer> sanctionsTypes, String start, String end, Boolean noReason, boolean isMultiple, JsonObject students) {
        super(MailingType.PDF, structure, template, massmailed, massmailingTypeList, reasons, punishmentsTypes, sanctionsTypes,
                start, end, noReason,  isMultiple, students);
        this.exportPDFService = new ExportPDFServiceImpl(vertx, config);
        this.request = request;
        this.eb = eb;
        this.storage = storage;
    }

    @Override
    public void massmail(HttpServerRequest request, Handler<Either<String, Boolean>> handler) {
        super.process(event -> {
            if (event.isLeft()) {
                String message = "[Massmailing@Pdf::massmail] Failed to process pdf";
                LOGGER.error(String.format("%s %s", message, event.left().getValue()));
                handler.handle(new Either.Left<>(message));
                return;
            }

            List<JsonObject> pdfs = event.right().getValue();
            StringBuilder formattedPdfs = new StringBuilder();

            for (JsonObject pdf : pdfs) {
                formatPdf(formattedPdfs, pdf);
            }

            send(formattedPdfs.toString(), pdfs, sendAsync -> {
                if (sendAsync.failed()) {
                    String message = "[Massmailing@Pdf::massmail::futureJoin] Failed to send pdf";
                    LOGGER.error(String.format("%s %s", message, sendAsync.cause().getMessage()));
                    handler.handle(new Either.Left<>(message));
                } else {
                    handler.handle(new Either.Right<>(sendAsync.result()));
                }
            });
        });
    }

    private void formatPdf(StringBuilder formattedPdfs, JsonObject pdf) {
        String beginText = "<div style=\"page-break-after: always; margin:64px;\">";
        String message = pdf.getString("message");
        String end = "</div>";
        formattedPdfs.append(beginText.concat(message).concat(end));
    }

    private void send(String formattedPdfs, List<JsonObject> pdfs, Handler<AsyncResult<Boolean>> handler) {
        UserUtils.getUserInfos(eb, this.request, user ->
                this.exportPDFService.generatePDF(this.request, formattedPdfs, user, buffer -> {
                    String fileName = I18n.getInstance().translate("massmailing.title",
                            Renders.getHost(request), I18n.acceptLanguage(request)) + "_" +
                            DateHelper.getCurrentDate(DateHelper.DAY_MONTH_YEAR_DASH) + ".pdf";

                    this.request.response()
                            .putHeader("Content-type", "application/pdf; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=" + fileName)
                            .end(buffer);

                    savePdf(buffer, fileName, file -> {
                        if (file.failed()) {
                            String message = "[Massmailing@Pdf::send] Failed to save pdf mailing";
                            LOGGER.error(String.format("%s %s", message, file.cause().getMessage()));
                            handler.handle(Future.failedFuture(file.cause().getMessage()));
                            return;
                        }

                        saveMailings(file.result(), pdfs, handler);
                    });
                })
        );
    }

    private void savePdf(Buffer buffer, String fileName, Handler<AsyncResult<JsonObject>> handler) {
        final String id = UUID.randomUUID().toString();
        storage.writeBuffer(id, buffer, "application/pdf", fileName, file -> {
            if (!"ok".equals(file.getString("status"))) {
                String message = "[Massmailing@Pdf::send] Failed to store pdf";
                LOGGER.error(message);
                handler.handle(Future.failedFuture(message));
                return;
            }

            handler.handle(Future.succeededFuture(file));
        });
    }

    private void saveMailings(JsonObject file, List<JsonObject> pdfs, Handler<AsyncResult<Boolean>> handler) {
        String file_id = file.getString("_id");
        String metadata = file.getJsonObject("metadata").toString();

        FutureHelper.join(listPdfFuture(pdfs, file_id, metadata)).setHandler(event -> {
            if (event.failed()) {
                String message = "[Massmailing@Pdf::saveMailings] Failed to save pdf mailing";
                LOGGER.error(String.format("%s %s", message, event.cause().getMessage()));
                handler.handle(Future.failedFuture(message));
                return;
            }

            handler.handle(Future.succeededFuture(event.succeeded()));
        });
    }

    private List<Future<JsonObject>> listPdfFuture(List<JsonObject> pdfs, String file_id, String metadata) {
        List<Future<JsonObject>> futures = new ArrayList<>();
        for (JsonObject pdf : pdfs) {
            Future<JsonObject> future = Future.future();
            futures.add(future);
            pdf.put("file_id", file_id);
            pdf.put("metadata", metadata);
            saveMassmailing(pdf, FutureHelper.handlerJsonObject(future));
        }
        return futures;
    }
}
