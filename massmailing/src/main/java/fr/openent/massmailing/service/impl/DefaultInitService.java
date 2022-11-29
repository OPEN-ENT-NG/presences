package fr.openent.massmailing.service.impl;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.helper.init.IInitMassmailingHelper;
import fr.openent.massmailing.model.Mailing.Mailing;
import fr.openent.massmailing.model.Mailing.Template;
import fr.openent.massmailing.service.InitService;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.InitTypeEnum;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.request.JsonHttpServerRequest;

import java.util.List;

public class DefaultInitService implements InitService {
    @Override
    public void getTemplateStatement(JsonHttpServerRequest request, String structure, String owner, InitTypeEnum initTypeEnum, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Massmailing.dbSchema + ".template(structure_id, name, content, type, owner, category) VALUES ";

        List<Template> templateList = IInitMassmailingHelper.getDefaultInstance(initTypeEnum).getTemplates();
        for (Template template: templateList) {
            String mailName = I18n.getInstance().translate(template.getName(), Renders.getHost(request), I18n.acceptLanguage(request));
            String mailContent = I18n.getInstance().translate(template.getContent(), Renders.getHost(request), I18n.acceptLanguage(request));
            String mailCategory = I18n.getInstance().translate(template.getCategory(), Renders.getHost(request), I18n.acceptLanguage(request));
            query += "(?, ?, ?, ?, ?, ?),";
            params.add(structure)
                    .add(mailName)
                    .add(mailContent)
                    .add(template.getType().name())
                    .add(owner)
                    .add(mailCategory);
        }

        query = query.substring(0, query.length() - 1) + ";";
        handler.handle(new Either.Right<>(new JsonObject()
                .put(Field.STATEMENT, query)
                .put(Field.VALUES, params)
                .put(Field.ACTION, Field.PREPARED)));
    }
}
