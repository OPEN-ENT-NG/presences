package fr.openent.presences.common.service;

import io.vertx.core.*;
import io.vertx.core.json.*;
import org.entcore.common.user.*;

public interface WorkspaceService {

    /**
     * Add a new folder to the user workspace
     * @param name              folder name
     * @param owner             user identifier
     * @param ownerName         user name
     * @param parentFolderId    parent folder identifier
     * @return                  created folder data
     */
    Future<JsonObject> addFolder(String name, String owner, String ownerName, String parentFolderId);

    /**
     * Get the user workspace root files/folders
     * @param user              user infos
     * @return                  list of files/folders
     */
    Future<JsonArray> listRootDocuments(UserInfos user);
}
