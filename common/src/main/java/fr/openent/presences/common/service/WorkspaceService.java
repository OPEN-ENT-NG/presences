package fr.openent.presences.common.service;

import io.vertx.core.*;
import io.vertx.core.json.*;
import org.entcore.common.user.*;

public interface WorkspaceService {

    Future<JsonObject> addFolder(String name, String owner, String ownerName, String parentFolderId);

    Future<JsonArray> listRootDocuments(UserInfos user);
}
