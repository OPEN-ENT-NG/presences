package fr.openent.presences.common.service.impl;

import fr.openent.presences.common.service.*;
import fr.openent.presences.core.constants.*;
import io.vertx.core.*;
import io.vertx.core.buffer.*;
import io.vertx.core.buffer.impl.*;
import io.vertx.core.http.*;
import io.vertx.core.json.*;
import org.entcore.common.storage.*;

public class DefaultFileService implements FileService {

    private final Storage storage;

    public DefaultFileService(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void get(String fileId, Handler<Buffer> handler) {
        storage.readFile(fileId, handler);
    }

    @Override
    public Future<JsonObject> add(HttpServerRequest request, String contentType, String filename) {
        Promise<JsonObject> promise = Promise.promise();
        Buffer responseBuffer = new BufferImpl();
        request.handler(responseBuffer::appendBuffer);
        request.endHandler(aVoid -> storage.writeBuffer(responseBuffer, contentType, filename, entries -> {
            if (Field.OK.equals(entries.getString(Field.STATUS))) {
                promise.complete(entries);
            } else {
                promise.fail("[Common@DefaultFileService::add] An error occurred while writing file in the storage");
            }
        }));
        request.exceptionHandler(throwable -> promise.fail("[Common@DefaultFileService::add] An error occurred when uploading file"));
        return promise.future();
    }

    @Override
    public Future<JsonObject> add(HttpServerRequest request) {
        Promise<JsonObject> promise = Promise.promise();
        storage.writeUploadFile(request, message -> {
            if (!Field.OK.equals(message.getString(Field.STATUS))) {
                promise.fail("[Common@DefaultFileService::add] Failed to upload file from http request");
            } else {
                message.remove(Field.STATUS);
                promise.complete(message);
            }
        });
        return promise.future();
    }

    @Override
    public Future<JsonObject> add(Buffer file, String contentType, String filename) {
        Promise<JsonObject> promise = Promise.promise();
        storage.writeBuffer(file, contentType, filename, message -> {
            if (!Field.OK.equals(message.getString(Field.STATUS))) {
                promise.fail("[Common@DefaultFileService::add] Failed to upload file from buffer");
            } else {
                message.remove(Field.STATUS);
                promise.complete(message);
            }
        });
        return promise.future();
    }

}
