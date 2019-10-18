package fr.openent.massmailing.service;

import fr.openent.massmailing.enums.MailingType;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface SettingsService {

    /**
     * Retrieve all templates for given type and given structure identifier.
     *
     * @param type      Mailing type
     * @param structure Structure identifier
     * @param handler   Function handler returning data
     */
    void getTemplates(MailingType type, String structure, Handler<Either<String, JsonArray>> handler);

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
}
