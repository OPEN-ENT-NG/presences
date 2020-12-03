package fr.openent.massmailing.mailing;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.service.ExportPDFService;
import fr.openent.presences.common.service.impl.ExportPDFServiceImpl;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserUtils;

import java.util.List;

public class Pdf extends MassMailingProcessor {
    private final ExportPDFService exportPDFService;
    private final HttpServerRequest request;
    private final EventBus eb;

    public Pdf(EventBus eb, Vertx vertx, JsonObject config, HttpServerRequest request, String structure, Template template,
               Boolean massmailed, List<MassmailingType> massmailingTypeList, List<Integer> reasons, List<Integer> punishmentsTypes,
               List<Integer> sanctionsTypes, String start, String end, Boolean noReason, JsonObject students) {
        super(MailingType.PDF, structure, template, massmailed, massmailingTypeList, reasons, punishmentsTypes, sanctionsTypes,
                start, end, noReason, students);
        this.exportPDFService = new ExportPDFServiceImpl(vertx, config);
        this.request = request;
        this.eb = eb;
    }

    @Override
    public void massmail(Handler<Either<String, Boolean>> handler) {
        super.process(event -> {
            if (event.isLeft()) {
                LOGGER.error("[Massmailing@Pdf::massmail] Failed to process pdf", event.left().getValue());
                handler.handle(new Either.Left<>(event.left().getValue()));
                return;
            }

            List<JsonObject> pdfs = event.right().getValue();
            StringBuilder formattedPdfs = new StringBuilder();

            for (JsonObject pdf : pdfs) {
                formatPdf(formattedPdfs, pdf);
            }

            send(formattedPdfs.toString(), sendAsync -> {
                if (sendAsync.failed()) {
                    LOGGER.error("[Massmailing@Pdf::massmail::futureJoin] Failed to send pdf ", sendAsync.cause().getMessage());
                    handler.handle(new Either.Left<>(sendAsync.cause().getMessage()));
                } else {
                    handler.handle(new Either.Right<>(sendAsync.result()));
                }
            });
        });
    }

    private void formatPdf(StringBuilder formattedPdfs, JsonObject mail) {
        String beginText = "<div style=\"page-break-after: always; margin:64px;\">";
        String message = mail.getString("message");
        String end = "</div>";
        formattedPdfs.append(beginText.concat(message).concat(end));
    }
    
    private void send(String formattedPdfs, Handler<AsyncResult<Boolean>> handler) {
        UserUtils.getUserInfos(eb, this.request, user ->
                this.exportPDFService.generatePDF(this.request, formattedPdfs, user, buffer -> {
                    String fileName = I18n.getInstance().translate("massmailing.title",
                            Renders.getHost(request), I18n.acceptLanguage(request)) + "_" +
                            DateHelper.getCurrentDate(DateHelper.DAY_MONTH_YEAR_DASH);

                    this.request.response()
                            .putHeader("Content-type", "application/pdf; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=" + fileName + ".pdf")
                            .end(buffer);
                    handler.handle(Future.succeededFuture(true));
                }));
    }
}
