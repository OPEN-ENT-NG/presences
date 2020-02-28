package fr.openent.massmailing.service.impl;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.service.InitService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.request.JsonHttpServerRequest;

public class DefaultInitService implements InitService {
    @Override
    public void getTemplateStatement(JsonHttpServerRequest request, String structure, String owner, Handler<Either<String, JsonObject>> handler) {
        Integer occurrences = 2;
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Massmailing.dbSchema + ".template(structure_id, name, content, type, owner) VALUES ";

        for (int i = 0; i < occurrences; i++) {
            String mailName = I18n.getInstance().translate("massmailing.init.template.mail." + i + ".name", Renders.getHost(request), I18n.acceptLanguage(request));
            String mailContent = I18n.getInstance().translate("massmailing.init.template.mail." + i + ".content", Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?, ?, ?, ?),";
            params.add(structure)
                    .add(mailName)
                    .add(mailContent)
                    .add("MAIL")
                    .add(owner);
        }

        for (int i = 0; i < occurrences; i++) {
            String smsName = I18n.getInstance().translate("massmailing.init.template.sms." + i + ".name", Renders.getHost(request), I18n.acceptLanguage(request));
            String smsContent = I18n.getInstance().translate("massmailing.init.template.sms." + i + ".content", Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?, ?, ?, ?),";
            params.add(structure)
                    .add(smsName)
                    .add(smsContent)
                    .add("SMS")
                    .add(owner);
        }

        query = query.substring(0, query.length() - 1) + ";";
        handler.handle(new Either.Right<>(new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared")));
    }
}
