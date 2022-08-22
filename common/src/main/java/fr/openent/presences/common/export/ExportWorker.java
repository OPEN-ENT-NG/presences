package fr.openent.presences.common.export;

import fr.openent.presences.common.helper.*;
import fr.openent.presences.common.service.*;
import fr.openent.presences.common.service.impl.*;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.model.*;
import io.vertx.core.*;
import io.vertx.core.eventbus.*;
import io.vertx.core.json.*;
import io.vertx.core.logging.*;
import org.entcore.common.bus.*;
import org.entcore.common.notification.*;
import org.entcore.common.storage.*;
import org.entcore.common.user.*;

import java.util.*;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public abstract class ExportWorker extends AbstractVerticle {

    protected final Logger log = LoggerFactory.getLogger(ExportWorker.class);
    protected ExportLogs exportLogs;
    protected Context exportContext;
    protected String exportNotification;

    private WorkspaceHelper workspaceHelper;
    private TimelineHelper timelineHelper;
    private FileService fileService;
    private WorkspaceService workspaceService;


    @Override
    public void start() {
        exportContext = vertx.getOrCreateContext();
        exportLogs = new ExportLogs(this.getClass().getSimpleName() + ".log");
        String launchLog = String.format("[Common@%s::start] Launching worker %s, deploy verticle %s",
                this.getClass().getSimpleName(), this.getClass().getSimpleName(), exportContext.deploymentID());
        log.info(launchLog);
        exportLogs.addLog(launchLog);

        Storage storage = new StorageFactory(vertx, new JsonObject()).getStorage();
        workspaceService = new DefaultWorkspaceService(this.vertx, storage, config());
        fileService = new DefaultFileService(storage);
        workspaceHelper = new WorkspaceHelper(vertx.eventBus(), storage);
        timelineHelper = new TimelineHelper(this.vertx, this.vertx.eventBus(), config());

        vertx.eventBus().consumer(this.getClass().getName(), this::run);
    }

    protected void run(Message<JsonObject> event) {
        JsonObject params = event.body().getJsonObject(Field.PARAMS);
        String action = event.body().getString(Field.ACTION);
        String type = event.body().getString(Field.TYPE);
        UserInfos user = UserInfosHelper.getUserInfosFromJSON(params.getJsonObject(Field.USER));

        log.info(String.format("[Common@%s::run] Starting worker %s process, generating export file",
                this.getClass().getSimpleName(), this.getClass().getSimpleName()));

        init(config())
                .compose(v -> getData(action, type, params))
                .compose(file -> exportData(file, user))
                .compose(fileInfos -> sendNotification(user, fileInfos))
                .onFailure(fail -> {
                    this.exportLogs.addLog(fail.getMessage());
                    exportData(this.exportLogs.getLogFile(), user);
                });
    }

    protected abstract Future<Void> init(JsonObject config);

    protected abstract String getFolderName();

    protected abstract Future<ExportFile> getData(String action, String exportType, JsonObject params);

    protected Future<JsonObject> exportData(ExportFile exportFile, UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();

        addFolderIfExists(getFolderName(), user)
                .compose(folder -> fileService.add(exportFile.getBuffer(), exportFile.getContentType(), exportFile.getFilename())
                        .onSuccess(file -> this.workspaceHelper.addDocument(file, user, exportFile.getFilename(), "media-library",
                                false, new JsonArray(), handlerToAsyncHandler(message -> {
                                    if (Field.OK.equals(message.body().getString(Field.STATUS))) {
                                        String documentId = message.body().getString(Field._ID);
                                        workspaceHelper.moveDocument(documentId, folder.getString(Field._ID), user,
                                                res -> promise.complete(res.result().body()));
                                    }
                                }))))
                .onFailure(fail -> {
                    String message = String.format("[Common@%s::exportData] Error adding folder/file in workspace for export",
                            this.getClass().getSimpleName());
                    log.error(message, fail.getMessage());
                    exportLogs.addLog(message);
                    promise.fail(fail.getMessage());
                });

        return promise.future();
    }

    @SuppressWarnings("unchecked")
    private Future<JsonObject> addFolderIfExists(String folderName, UserInfos user) {
        Promise<JsonObject> promise = Promise.promise();
        workspaceService.listRootDocuments(user)
                .onFailure(fail -> {
                    exportLogs.addLog(fail.getMessage());
                    promise.fail(fail.getMessage());
                })
                .onSuccess(files -> {
                    boolean folderExists = ((List<JsonObject>) files.getList())
                            .stream().anyMatch(res -> Objects.equals(res.getString(Field.NAME), folderName));
                    if (folderExists) {
                        promise.complete(((List<JsonObject>) files.getList())
                                .stream()
                                .filter(res -> Objects.equals(res.getString(Field.NAME), folderName))
                                .findFirst()
                                .orElse(null));
                    } else {
                        workspaceService.addFolder(folderName, user.getUserId(), user.getUsername(), null)
                                .onFailure(fail -> promise.fail(fail.getMessage()))
                                .onSuccess(promise::complete);
                    }
                });

        return promise.future();
    }

    protected Future<Void> sendNotification(UserInfos user, JsonObject fileInfos) {
        Promise<Void> promise = Promise.promise();
        JsonObject params = new JsonObject()
                .put(Field.FILENAME, fileInfos.getString(Field.NAME))
                .put(Field.FOLDERURI, "/workspace/workspace#/folder/" + fileInfos.getString(Field.EPARENT))
                .put(Field.PUSHNOTIF, new JsonObject()
                        .put(Field.TITLE,"presences.push.export.finished")
                        .put(Field.BODY, ""));
        timelineHelper.notifyTimeline(null, this.exportNotification, user,
                Collections.singletonList(user.getUserId()), "", params);
        promise.complete();
        return promise.future();
    }

}
