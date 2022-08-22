package fr.openent.presences.common.service.impl;

import fr.openent.presences.common.helper.*;
import fr.openent.presences.common.service.*;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.*;
import fr.wseduc.mongodb.*;
import io.vertx.core.*;
import io.vertx.core.eventbus.*;
import io.vertx.core.json.*;
import org.entcore.common.folders.*;
import org.entcore.common.share.impl.*;
import org.entcore.common.storage.*;
import org.entcore.common.user.*;
import java.util.*;

public class DefaultWorkspaceService implements WorkspaceService  {

    private final EventBus eb;
    private final FolderManager folderManager;

    public DefaultWorkspaceService (Vertx vertx, Storage storage, JsonObject config) {
        this.eb = vertx.eventBus();

        String node = (String) vertx.sharedData().getLocalMap("server").get("node");
        if (node == null) {
            node = "";
        }
        String imageResizerAddress = node + config.getString("image-resizer-address", "wse.image.resizer");
        final boolean useOldQueryChildren = config.getBoolean("old-query", false);
        GenericShareService shareService = new MongoDbShareService(vertx.eventBus(), MongoDb.getInstance(), "documents", null, new HashMap<>());

        folderManager = FolderManager.mongoManager("documents", storage, vertx, shareService, imageResizerAddress, useOldQueryChildren);
    }

    @Override
    public Future<JsonObject> addFolder(String name, String owner, String ownerName, String parentFolderId) {
        Promise<JsonObject> promise = Promise.promise();

        JsonObject action = new JsonObject()
                .put(Field.ACTION, EventBusActions.ADDFOLDER.action())
                .put(Field.NAME, name)
                .put(Field.OWNER, owner)
                .put(Field.OWNERNAME, ownerName)
                .put(Field.PARENTFOLDERID, parentFolderId);


        EventBusHelper.requestJsonObject(EventBusActions.EventBusAddresses.WORKSPACE_BUS_ADDRESS.address(), eb, action)
                .onFailure(fail -> {
                    String message = String.format("[Presences@%s::addFolder]Error while adding folder %s",
                            this.getClass().getSimpleName(), name);
                    promise.fail(message);
                })
                .onSuccess(promise::complete);

        return promise.future();
    }

    @Override
    public Future<JsonArray> listRootDocuments(UserInfos user) {
        Promise<JsonArray> promise = Promise.promise();

        if (user != null && user.getUserId() != null) {
            ElementQuery query = new ElementQuery(false);
            query.setHierarchical(false);
            query.setTrash(false);
            query.setNoParent(true);
            query.setHasBeenShared(false);
            query.setVisibilitiesNotIn(new HashSet<>());
            query.getVisibilitiesNotIn().add("protected");
            query.getVisibilitiesNotIn().add("public");
            query.setType(FolderManager.FILE_TYPE);
            query.setProjection(ElementQuery.defaultProjection());
            query.getProjection().add("comments");
            query.getProjection().add("application");
            query.getProjection().add("trasher");
            query.getProjection().add("protected");
            query.getProjection().add("ancestors");
            query.getProjection().add("externalId");
            query.getProjection().add("isShared");

            query.setType(null);

            folderManager.findByQuery(query, user, res -> {
                if (res.failed()) {
                    promise.fail(res.cause().getMessage());
                } else {
                    promise.complete(res.result());
                }
            });

        } else {
            String message = String.format("[Presences@%s::listRootDocuments] Error fetching user id", this.getClass().getName());
            promise.fail(message);
        }

        return promise.future();

    }

}
