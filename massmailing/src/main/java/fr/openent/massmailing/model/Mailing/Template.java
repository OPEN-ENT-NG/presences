package fr.openent.massmailing.model.Mailing;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.model.*;
import io.vertx.core.json.*;

import java.util.*;

public class Template implements IModel<Template> {
    private int id;
    private String structureId;
    private String name;
    private String content;
    private MailingType type;
    private String created;
    private String owner;
    private String category;

    public Template(JsonObject template) {
        this.id = template.getInteger(Field.ID, null);
        this.structureId = template.getString(Field.STRUCTURE_ID, null);
        this.name = template.getString(Field.NAME, null);
        this.content = template.getString(Field.CONTENT, null);
        this.type = MailingType.valueOf(template.getString(Field.TYPE, null));
        this.created = template.getString(Field.CREATED, null);
        this.owner = template.getString(Field.OWNER, null);
        this.category = template.getString(Field.CATEGORY, null);
    }

    public Template(int id, String structureId, String name, String content, MailingType type, String created, String owner,
                    String category) {
        this.id = id;
        this.structureId = structureId;
        this.name = name;
        this.content = content;
        this.type = type;
        this.created = created;
        this.owner = owner;
        this.category = category;
    }

    public Template(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Template setId(int id) {
        this.id = id;
        return this;
    }

    public String getStructureId() {
        return structureId;
    }

    public Template setStructureId(String structureId) {
        this.structureId = structureId;
        return this;
    }

    public String getName() {
        return name;
    }

    public Template setName(String name) {
        this.name = name;
        return this;
    }

    public String getContent() {
        return content;
    }

    public Template setContent(String content) {
        this.content = content;
        return this;
    }

    public MailingType getType() {
        return type;
    }

    public Template setType(MailingType type) {
        this.type = type;
        return this;
    }

    public String getCreated() {
        return created;
    }

    public Template setCreated(String created) {
        this.created = created;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public Template setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public String getCategory() {
        return (Objects.equals(category, Field.ABSENCE_UPPERCASE)) ? Field.ABSENCES_UPPERCASE : category;
    }

    public Template setCategory(String category) {
        this.category = category;
        return this;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put(Field.ID, this.getId())
                .put(Field.STRUCTURE_ID, this.getStructureId())
                .put(Field.NAME, this.getName())
                .put(Field.CONTENT, this.getContent())
                .put(Field.TYPE, this.getType().toString())
                .put(Field.CREATED, this.getCreated())
                .put(Field.OWNER, this.getOwner())
                .put(Field.CATEGORY, this.getCategory());
    }

    @Override
    public boolean validate() {
        return false;
    }
}
