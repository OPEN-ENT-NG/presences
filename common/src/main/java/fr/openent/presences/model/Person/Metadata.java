package fr.openent.presences.model.Person;

import fr.openent.presences.model.Model;
import io.vertx.core.json.JsonObject;

import java.util.Collections;

public class Metadata extends Model {

    private String name;
    private String contentType;
    private String contentTransferEncoding;
    private String filename;
    private Integer size;
    private String charset;

    public Metadata(String oMetadata) {
        fillables.put("name", Collections.emptyList());
        fillables.put("contentType", Collections.emptyList());
        fillables.put("contentTransferEncoding", Collections.emptyList());
        fillables.put("filename", Collections.emptyList());
        fillables.put("size", Collections.emptyList());
        fillables.put("charset", Collections.emptyList());

        if(oMetadata != null) {
            JsonObject metadata = new JsonObject(oMetadata);
            this.setFromJson(metadata);
            this.contentType = metadata.getString("content-type", null);
            this.contentTransferEncoding = metadata.getString("content-transfer-encoding", null);
        }
    }
}
