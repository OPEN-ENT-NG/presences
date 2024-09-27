package fr.openent.presences.model;

import fr.openent.presences.Presences;

import java.util.Arrays;
import java.util.Collections;

public class StatementAbsence extends Model {

    private String id;
    private String start_at;
    private String end_at;
    private String student_id;
    private String structure_id;
    private String description;
    private String treated_at;
    private String validator_id;
    private String parent_id;
    private String attachment_id;
    private String created_at;
    private String metadata;


    public StatementAbsence() {
        table = Presences.dbSchema + ".statement_absence";

        fillables.put("id", Collections.emptyList());
        fillables.put("start_at", Arrays.asList("CREATE", "mandatory"));
        fillables.put("end_at", Arrays.asList("CREATE", "mandatory"));
        fillables.put("student_id", Arrays.asList("CREATE", "mandatory"));
        fillables.put("structure_id", Arrays.asList("CREATE", "mandatory"));
        fillables.put("description", Collections.singletonList("CREATE"));
        fillables.put("treated_at", Collections.singletonList("UPDATE"));
        fillables.put("parent_id", Collections.singletonList("CREATE"));
        fillables.put("validator_id", Collections.singletonList("UPDATE"));
        fillables.put("attachment_id", Collections.singletonList("CREATE"));
        fillables.put("created_at", Collections.emptyList());
        fillables.put("metadata", Collections.singletonList("CREATE"));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStartAt() {
        return start_at;
    }

    public void setStartAt(String start_at) {
        this.start_at = start_at;
    }

    public String getEndAt() {
        return end_at;
    }

    public void setEndAt(String end_at) {
        this.end_at = end_at;
    }

    public String getStudentId() {
        return student_id;
    }

    public void setStudentId(String student_id) {
        this.student_id = student_id;
    }

    public String getStructureId() {
        return structure_id;
    }

    public void setStructureId(String structure_id) {
        this.structure_id = structure_id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTreatedAt() {
        return treated_at;
    }

    public void setTreatedAt(String treated_at) {
        this.treated_at = treated_at;
    }

    public String getValidatorId() {
        return validator_id;
    }

    public void setValidatorId(String validator_id) {
        this.validator_id = validator_id;
    }

    public String getParentId() {
        return parent_id;
    }

    public void setParentId(String parent_id) {
        this.parent_id = parent_id;
    }

    public String getAttachmentId() {
        return attachment_id;
    }

    public void setAttachmentId(String attachment_id) {
        this.attachment_id = attachment_id;
    }

    public String getCreatedAt() {
        return created_at;
    }

    public void setCreatedAt(String created_at) {
        this.created_at = created_at;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
