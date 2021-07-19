package fr.openent.presences.common.helper;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;

import java.util.List;


public class FileHelper {
    private static final Logger log = LoggerFactory.getLogger(FileHelper.class);

    private FileHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * remove files by ids
     * @param storage           Storage instance
     * @param fileIds           list of file identifiers to send
     * @return                  {@link Future<JsonObject>}
     */
    public static Future<JsonObject> removeFiles(Storage storage, List<String> fileIds) {
        Promise<JsonObject> promise = Promise.promise();
        if (fileIds.isEmpty()) {
            promise.complete(new JsonObject().put("remove file status", "ok"));
        } else {
            storage.removeFiles(new JsonArray(fileIds), result -> {
                if (!"ok".equals(result.getString("status"))) {
                    String message = "[PresenceCommon@FileHelper::removeFiles] Failed to remove files.";
                    log.error(message, result.getString("message"));
                    promise.fail(message);
                    return;
                }
               promise.complete(result);
            });
        }
        return promise.future();
    }

    /**
     * remove file
     * @param storage           Storage instance
     * @param fileId            file identifier to send
     * @return                  {@link Future<JsonObject>}
     */
    public static Future<JsonObject> removeFile(Storage storage, String fileId) {
        Promise<JsonObject> promise = Promise.promise();
        storage.removeFile(fileId, result -> {
            if (!"ok".equals(result.getString("status"))) {
                String message = "[PresenceCommon@FileHelper::removeFile] Failed to remove file.";
                log.error(message, result.getString("message"));
                promise.fail(message);
            } else {
                promise.complete(result);
            }
        });
        return promise.future();
    }

    /**
     * Create temporary folder containing a copy of each file.
     * @param fileSystem            file system
     * @param directoryPath         directory path
     * @return                      {@link Future<Void>}
     */
    public static Future<Void> removeDirectory(FileSystem fileSystem, String directoryPath) {
        Promise<Void> promise = Promise.promise();
        fileSystem.deleteRecursive(directoryPath, true, res -> {
            if (res.failed()) {
                String message = "[PresenceCommon@FileHelper::removeDirectory] Failed to remove directory.";
                promise.fail(message);
            } else {
                promise.complete(res.result());
            }
        });
        return promise.future();
    }

    /**
     * Create temporary folder containing a copy of each file.
     * @param storage           list of file identifiers
     * @param fileId            list of file names for each id
     * @return                  {@link Future<Boolean>} true if exist, false if none
     */
    public static Future<Boolean> exist(Storage storage, String fileId) {
        Promise<Boolean> promise = Promise.promise();
        storage.readFile(fileId, result -> {
            if (result == null) {
                promise.complete(false);
                return;
            }
            promise.complete(true);
        });
        return promise.future();
    }
}
