package fr.openent.presences.common.service;

import fr.openent.presences.model.ZIPFile;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface ExportZIPService {

    /**
     * Create a .zip file from a list of file identifiers.
     *
     * @param fileIds           list of file identifiers
     * @param fileNames         list of file names for each id
     *                          (ex: {'id1' : 'name1.pdf', 'id2': 'name2.pdf', 'id3': 'name3.jpg'})
     * @param zipFile           Object containing the desired .zip file name, path and subfolder path
     * @return {@link Future}   String containing ZIP path in storage
     */
    Future<String> createZIP(List<String> fileIds, JsonObject fileNames, ZIPFile zipFile);


    /**
     * Remove a zip file.
     * @param zipFile       Object containing the .zip file name, path and subfolder path
     * @return {@link Void}
     */
    Future<Void> deleteZIP(ZIPFile zipFile);

}
