package fr.openent.massmailing.service;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.model.Mailing.*;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface SettingsService {

    /**
     * Retrieve all templates for given type and given structure identifier.
     *
     * @param type      Mailing type
     * @param structure Structure identifier
     * @param listCategory list of category
     */
    Future<List<Template>> getTemplates(MailingType type, String structure, List<String> listCategory);

    /**
     * Create given template.  Must contains structure identifier, template name, template content et template type
     *
     * @param template Template
     * @param userId   User identifier
     * @param handler  Function handler returning data
     */
    void createTemplate(JsonObject template, String userId, Handler<Either<String, JsonObject>> handler);

    /**
     * Update given template
     *
     * @param id       Template identifier
     * @param template Template object. Must contains structure identifier, template name, template content et template type
     * @param handler  Function handler returning data
     */
    void updateTemplate(Integer id, JsonObject template, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete given template
     *
     * @param id      Template identifier
     * @param handler Function handler returning data
     */
    void deleteTemplate(Integer id, Handler<Either<String, JsonObject>> handler);

    /**
     * Retrieve template based on template identifier and structure identifier
     *
     * @param id        Template identifier
     * @param structure Structure identifier
     * @param handler   Function handler returning data
     */
    void get(MailingType type, Integer id, String structure, Handler<Either<String, JsonObject>> handler);
}
