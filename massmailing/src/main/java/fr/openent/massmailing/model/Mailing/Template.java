package fr.openent.massmailing.model.Mailing;

import fr.openent.massmailing.enums.MailingType;

public class Template {
    private int id;
    private String structureId;
    private String name;
    private String content;
    private MailingType type;
    private String created;
    private String owner;
    private String category;

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
        return category;
    }

    public Template setCategory(String category) {
        this.category = category;
        return this;
    }
}
