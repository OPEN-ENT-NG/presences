package fr.openent.presences.common.service.impl;

import fr.openent.presences.common.service.ExportPDFService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.template.TemplateProcessor;
import fr.wseduc.webutils.template.lambdas.I18nLambda;
import fr.wseduc.webutils.template.lambdas.LocaleDateLambda;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.ProxyOptions;
import org.entcore.common.pdf.Pdf;
import org.entcore.common.pdf.PdfException;
import org.entcore.common.pdf.PdfFactory;
import org.entcore.common.pdf.PdfGenerator;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static fr.wseduc.webutils.Utils.isEmpty;
import static fr.wseduc.webutils.Utils.isNotEmpty;
import static fr.wseduc.webutils.http.Renders.badRequest;


public class ExportPDFServiceImpl implements ExportPDFService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportPDFServiceImpl.class);
    private String pdfGeneratorURL;
    private final Vertx vertx;
    private String authHeader;
    private final PdfFactory pdfFactory;


    public ExportPDFServiceImpl(Vertx vertx, JsonObject config) {
        super();
        this.vertx = vertx;
        pdfFactory = new PdfFactory(vertx, new JsonObject().put("node-pdf-generator",
                config.getJsonObject("node-pdf-generator", new JsonObject())));
        try {
            this.authHeader = config.getJsonObject("pdf-generator", new JsonObject()).getString("auth", null);
            this.authHeader = this.authHeader == null ? null : "Basic " + this.authHeader;
            this.pdfGeneratorURL = config.getJsonObject("pdf-generator", new JsonObject()).getString("url", null);
        } catch (Exception e) {
            LOGGER.info("[Common@ExportPDFServiceImpl::constructor] Failed to get pdf generator credentials");
        }
    }

    @Override
    public void generatePDF(HttpServerRequest request, String html, UserInfos user, Handler<Buffer> handler) {
        byte[] bytes;
        try {
            bytes = html.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("[Common@ExportPDFServiceImpl::generatePDF] Failed to get Bytes from template string",
                    e.getCause().getMessage());
            badRequest(request, e.getCause().getMessage());
            return;
        }
        callNodePdfGenerator(bytes, user, bufferEither -> {
            if (bufferEither.isLeft()) {
                badRequest(request, bufferEither.left().getValue());
                return;
            }
            Buffer either = bufferEither.right().getValue();
            handler.handle(either);
        });
    }

    private static HttpClient createHttpClient(Vertx vertx) {
        final HttpClientOptions options = new HttpClientOptions();
        options.setSsl(true);
        options.setTrustAll(true);
        options.setVerifyHost(false);
        if (System.getProperty("httpclient.proxyHost") != null) {
            ProxyOptions proxyOptions = new ProxyOptions()
                    .setHost(System.getProperty("httpclient.proxyHost"))
                    .setPort(Integer.parseInt(System.getProperty("httpclient.proxyPort")));
            options.setProxyOptions(proxyOptions);
        }
        return vertx.createHttpClient(options);
    }

    private static Buffer multipartBody(String content, String token, String boundary) {
        Buffer buffer = Buffer.buffer();
        // Add name
        buffer.appendString("--" + boundary + "\r\n")
                .appendString("Content-Disposition: form-data; name=\"name\"\r\n")
                .appendString("\r\n")
                .appendString("exportFile" + "\r\n");
        if (isNotEmpty(token)) {
            buffer.appendString("--" + boundary + "\r\n");
            buffer.appendString("Content-Disposition: form-data; name=\"token\"\r\n");
            buffer.appendString("\r\n");
            buffer.appendString(token + "\r\n");
        }
        // Add file
        buffer.appendString("--" + boundary + "\r\n")
                .appendString("Content-Disposition: form-data; name=\"template\"; filename=\"file\"\r\n")
                .appendString("Content-Type: application/xml\r\n")
                .appendString("\r\n")
                .appendString(content)
                .appendString("\r\n")
                .appendString("--" + boundary + "--\r\n");

        return buffer;
    }

    public String createToken(UserInfos user) throws Exception {
        String token = UserUtils.createJWTToken(vertx, user, null, null);
        if (isEmpty(token)) {
            throw new PdfException("[Common@ExportPDFServiceImpl::createToken] invalid.token");
        }
        return token;
    }

    private void webServiceNodePdfGeneratorPost(String file, String token, String nodePdfGeneratorUrl, Handler<Either<String, Buffer>> handler) {
        if (this.authHeader == null) {
            handler.handle(new Either.Left<>("[Common@ExportPDFServiceImpl::webServiceNodePdfGeneratorPost] Bad authHeader"));
            return;
        }
        AtomicBoolean responseIsSent = new AtomicBoolean(false);
        URI url;
        try {
            url = new URI(nodePdfGeneratorUrl);
        } catch (URISyntaxException e) {
            handler.handle(new Either.Left<>("[Common@ExportPDFServiceImpl::webServiceNodePdfGeneratorPost] Bad request"));
            return;
        }
        HttpClient httpClient = createHttpClient(this.vertx);
        HttpClientRequest httpClientRequest =
                httpClient.postAbs(url.toString(), response -> {
                    if (response.statusCode() == 200) {
                        final Buffer buff = Buffer.buffer();
                        response.handler(buff::appendBuffer);
                        response.endHandler(end -> {
                            handler.handle(new Either.Right<>(buff));
                            if (!responseIsSent.getAndSet(true)) {
                                httpClient.close();
                            }
                        });
                    } else {
                        LOGGER.error("[Common@ExportPDFServiceImpl::webServiceNodePdfGeneratorPost::postAbs] " +
                                "fail to post node-pdf-generator" + response.statusMessage());
                        response.bodyHandler(event -> {
                            LOGGER.error("[Common@ExportPDFServiceImpl::webServiceNodePdfGeneratorPost::postAbsResponseBodyHandler] " +
                                    "Returning body after POST CALL : " + nodePdfGeneratorUrl + ", Returning body : " + event.toString("UTF-8"));
                            if (!responseIsSent.getAndSet(true)) {
                                httpClient.close();
                            }
                        });
                    }
                });

        httpClientRequest.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                LOGGER.error(event.getMessage(), event);
                if (!responseIsSent.getAndSet(true)) {
                    handle(event);
                    httpClient.close();
                }
            }
        }).setFollowRedirects(true);
        final String boundary = UUID.randomUUID().toString();
        httpClientRequest.setChunked(true)
                .putHeader(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary)
                .putHeader("Authorization", authHeader)
                .putHeader(HttpHeaders.ACCEPT, "*/*")
                .end(multipartBody(file, token, boundary));

    }

    private void callNodePdfGenerator(byte[] bytes, UserInfos user, Handler<Either<String, Buffer>> asyncResultHandler) {
        String token = null;
        try {
            token = createToken(user);
        } catch (Exception e) {
            LOGGER.error("[Common@ExportPDFServiceImpl::callNodePdfGenerator] Failed to generate token " +
                    e.getCause().getMessage() + " processing pdf without token");
        }
        String nodePdfGeneratorUrl = pdfGeneratorURL;
        webServiceNodePdfGeneratorPost(Buffer.buffer(bytes).toString(), token, nodePdfGeneratorUrl, asyncResultHandler);
    }

    @Override
    public Future<Pdf> generatePDF(String filename, String resourceName, JsonObject resources) {
        Promise<Pdf> promise = Promise.promise();
        initTemplateProcessor().processTemplate(resourceName, resources, writer -> {
            if (writer == null) {
                String message = String.format("[Presences@%s::generatePDF] process template has no buffer result",
                        this.getClass().getSimpleName());
                promise.fail(message);
            } else {
                generatePDF(filename, writer)
                        .onSuccess(promise::complete)
                        .onFailure(promise::fail);
            }
        });
        return promise.future();
    }


    private TemplateProcessor initTemplateProcessor() {
        TemplateProcessor templateProcessor = new TemplateProcessor(vertx, "template").escapeHTML(false);
        templateProcessor.setLambda("i18n", new I18nLambda("fr"));
        templateProcessor.setLambda("datetime", new LocaleDateLambda("fr"));
        return templateProcessor;
    }

    @Override
    public Future<Pdf> generatePDF(String filename, String buffer) {
        Promise<Pdf> promise = Promise.promise();
        try {
            PdfGenerator pdfGenerator = pdfFactory.getPdfGenerator();
            pdfGenerator.generatePdfFromTemplate(filename, buffer, ar -> {
                if (ar.failed()) {
                    String message = String.format("[PresencesCommon@%s::generatePDF] Failed to generatePdfFromTemplate: " +
                            "%s", this.getClass().getSimpleName(), ar.cause().getMessage());
                    LOGGER.error(message, ar.cause());
                    promise.fail(ar.cause().getMessage());
                } else {
                    promise.complete(ar.result());
                }
            });
        } catch (Exception e) {
            String message = String.format("[PresencesCommon@%s::generatePDF] Failed to generatePDF: " +
                    "%s", this.getClass().getSimpleName(), e.getMessage());
            LOGGER.error(message);
            promise.fail(e.getMessage());
        }
        return promise.future();
    }
}