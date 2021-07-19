package fr.openent.presences.common.service.impl;

import fr.openent.presences.common.helper.FileHelper;
import fr.openent.presences.common.service.ExportZIPService;
import fr.openent.presences.model.ZIPFile;
import io.vertx.core.*;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.Zip;

import java.util.List;
import java.util.zip.Deflater;

public class ExportZIPServiceImpl implements ExportZIPService {

    private static final Logger log = LoggerFactory.getLogger(ExportZIPServiceImpl.class);
    private final Vertx vertx;
    private final Storage storage;

    /**
     * Service for managing .zip files from a list of file ids
     */
    public ExportZIPServiceImpl(Vertx vertx, Storage storage) {
        super();
        this.vertx = vertx;
        this.storage = storage;
    }

    /**
     * Create a .zip file from a list of file identifiers.
     *
     * @param fileIds           list of file identifiers
     * @param fileNames         list of file names for each id
     *                          (ex: {'id1' : 'name1.pdf', 'id2': 'name2.pdf', 'id3': 'name3.jpg'})
     * @param zipFile           Object containing the desired .zip file name, path and subfolder path
     * @return {@link Future}   String containing ZIP path in storage
     */
    @Override
    public Future<String> createZIP(List<String> fileIds, JsonObject fileNames, ZIPFile zipFile) {
        Promise<String> promise = Promise.promise();
        generateTempFolder(fileIds, fileNames, zipFile)
                .compose(res -> createZIPFromTempFolder(zipFile))
                .onSuccess(promise::complete)
                .onFailure(promise::fail);

        return promise.future();
    }

    /**
     * Create temporary folder containing a copy of each file.
     * @param fileIds           list of file identifiers
     * @param fileNames         list of file names for each id
     * @param zipFile           Object containing the desired .zip file name, path and subfolder path
     * @return                  temporary folder path
     */
    private Future<JsonObject> generateTempFolder(List<String> fileIds, JsonObject fileNames, ZIPFile zipFile) {
        Promise<JsonObject> promise = Promise.promise();
        storage.writeToFileSystem(fileIds.toArray(new String[0]), zipFile.getDirPath(), fileNames, res -> {
            if ("ok".equals(res.getString("status"))) {
                promise.complete(res);
            } else {
                String message = "[PresenceCommon@ExportZipServiceImpl::generateTempFolder] Failed to generate temporary folder: " + res.getString("error", "");
                log.error(message);
                promise.fail(res.getString("error"));
            }
        });

        return promise.future();
    }

    /**
     * Compress the temp folder into a .zip file.
     * @param zipFile       Object containing the desired .zip file name, path and subfolder path
     * @return              Zip file location path
     */
    private Future<String> createZIPFromTempFolder(ZIPFile zipFile) {
        Promise<String> promise = Promise.promise();

        Zip.getInstance().zipFolder(zipFile.getDirPath(), zipFile.getZipPath(), true, Deflater.NO_COMPRESSION, res2 -> {
            if ("ok".equals(res2.body().getString("status"))) {
                promise.complete(res2.body().getString("destZip"));
            } else {
                String message = "[PresenceCommon@ExportZipServiceImpl::createZIPFromTempFolder] Failed to compress temporary folder to ZIP.";
                log.error(message);
                promise.fail(message);
            }
        });

        return promise.future();
    }

    /**
     * Remove a zip file.
     * @param zipFile       Object containing the .zip file name, path and subfolder path
     * @return {@link Void}
     */
    @Override
    public Future<Void> deleteZIP(ZIPFile zipFile) {
        Promise<Void> promise = Promise.promise();

        FileSystem fs = vertx.fileSystem();
        FileHelper.removeDirectory(fs, zipFile.getRootPath())
                .onSuccess(promise::complete)
                .onFailure(promise::fail);
        return promise.future();
    }
}
