/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mammotion.internal;

import static org.openhab.binding.mammotion.internal.MammotionBindingConstants.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.mammotion.internal.MammotionResponse.TokenResponse;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link BridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Matthew Skinner - Initial contribution
 */
@NonNullByDefault
public class BridgeHandler extends BaseBridgeHandler {
    private final HttpClient httpClient;
    private final Logger logger = LoggerFactory.getLogger(BridgeHandler.class);
    private final Gson gson = new Gson();
    private MammotionResponse mammotionResponse = new MammotionResponse();
    private String aesKey = "";
    private String ivString = "";

    private MammotionConfiguration config = new MammotionConfiguration();

    public BridgeHandler(Bridge bridge, HttpClient httpClient) {
        super(bridge);
        this.httpClient = httpClient;
    }

    public String sendGetRequest(String url) {
        Request request;
        String errorReason = "";
        request = httpClient.newRequest(MAMMOTION_CLOUD_URL + url);
        request.header("Authorization", "Bearer " + mammotionResponse.dataState.access_token);
        request.header("Accept-Encoding", "gzip, deflate");
        request.header("Accept", "*/*");
        request.header("Connection", "keep-alive");
        request.header("Request-Id", "173435294882546542637");
        request.header("Access-Control-Allow-Origin", "*");
        request.header("Access-Control-Allow-Headers", "*");
        request.header("Access-Control-Allow-Methods", "*");
        request.header("Access-Control-Max-Age", "3600");
        request.header("Access-Control-Allow-Credentials", "true");
        request.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        request.header("User-Agent", "okhttp/3.14.9");
        request.header("App-Version", "google Pixel 2 XL taimen-Android 11,1.11.332");
        request.header("Content-Length", "0");
        request.header("Content-Type", "application/json");
        request.timeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        request.method(HttpMethod.GET);

        logger.debug("Sending Mammotion cloud GET:{} token:{}", url, mammotionResponse.dataState.access_token);

        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                logger.trace("Mammotion cloud returned:{}", contentResponse.getContentAsString());
                return contentResponse.getContentAsString();
            } else {
                errorReason = String.format("Mammotion cloud request failed with %d: %s", contentResponse.getStatus(),
                        contentResponse.getContentAsString());
                logger.trace("Mammotion cloud returned:{}", errorReason);
            }
        } catch (TimeoutException e) {
            errorReason = "TimeoutException: Mammotion cloud was not reachable on your network";
        } catch (ExecutionException e) {
            errorReason = String.format("ExecutionException: %s", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorReason = String.format("InterruptedException: %s", e.getMessage());
        }
        return errorReason;
    }

    public String encryptKey() throws Exception {
        String combined = aesKey + "," + ivString;
        logger.info("Encrypt-Key input string: {}", combined);
        byte[] decoded = Base64.getDecoder().decode(PUBLIC_KEY);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(combined.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String buildJson() throws Exception {
        Map<String, String> params = Map.of("username", encryptAES(config.email), "password",
                encryptAES(config.password), "client_id", encryptAES(CLIENT_ID), "client_secret",
                encryptAES(CLIENT_SECRET), "grant_type", encryptAES("password"));
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(params);
    }

    private String encryptAES(String input) throws Exception {
        IvParameterSpec vector = new IvParameterSpec(ivString.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, vector);
        return Base64.getEncoder().encodeToString(cipher.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    }

    public String sendPostRequest(String url, String content, String type) {
        Request request;
        String errorReason = "";
        if (!url.equalsIgnoreCase("/oauth/token")) {
            request = httpClient.newRequest("https://domestic.mammotion.com" + url);
            request.header("Authorization", "Bearer " + mammotionResponse.dataState.access_token);
        } else {
            request = httpClient.newRequest(MAMMOTION_CLOUD_URL + url);
        }
        request.header("Content-Type", type);
        Date openHABTime = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-M-d'T'H:m:s'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        request.header("x-timestamp", dateFormat.format(openHABTime.getTime()));
        // request.header("x-nonce", "b03d2a66-01aa-4dc0-8b8d-9c92e0f2c839");
        // request.header("x-signature", "b03d2a66-01aa-4dc0-8b8d-9c92e0f2c839");
        // request.header("Authorization", "b03d2a66-01aa-4dc0-8b8d-9c92e0f2c839");
        request.header("Accept-Encoding", "gzip, deflate");
        request.header("Accept", "*/*");
        request.header("Connection", "keep-alive");
        request.header("Request-Id", "173435294882546542637");
        request.header("Access-Control-Allow-Origin", "*");
        request.header("Access-Control-Allow-Headers", "*");
        request.header("Access-Control-Allow-Methods", "*");
        request.header("Access-Control-Max-Age", "3600");
        request.header("Access-Control-Allow-Credentials", "true");
        request.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        request.header("User-Agent", "okhttp/3.14.9");
        request.header("App-Version", "google Pixel 2 XL taimen-Android 11,1.11.332");
        request.header("Decrypt-Type", "3");
        request.header("Ec-Version", "v1");
        request.header("Request-Id", "173435294882546542637");

        // Authorization: Bearer <access-token>
        /*
         * String RSAresult = "";
         * try {
         * RSAresult = encryptKey();
         * logger.info("Header Encrypt-Key:{}", RSAresult);
         * request.header("Encrypt-Key", RSAresult);
         * } catch (Exception e) {
         * logger.info("Error {}", e.getMessage());
         * }
         */

        request.timeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        request.method(HttpMethod.POST);
        try {
            request.content(new StringContentProvider(content), type);
        } catch (Exception e) {
            logger.info("gson content of POST:{}", e.getMessage());
        }

        logger.trace("Sending Mammotion cloud, POST:{} content:{} type:{}", url, content, type);

        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                logger.trace("Mammotion cloud returned:{}:{}", contentResponse.getContentAsString(),
                        contentResponse.getHeaders().toString());
                return contentResponse.getContentAsString();
            } else {
                errorReason = String.format("Mammotion cloud request failed with %d: %s", contentResponse.getStatus(),
                        contentResponse.getContentAsString());
                logger.trace("Mammotion cloud returned:{}", errorReason);
            }
        } catch (TimeoutException e) {
            errorReason = "TimeoutException: Mammotion cloud was not reachable on your network";
        } catch (ExecutionException e) {
            errorReason = String.format("ExecutionException: %s", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorReason = String.format("InterruptedException: %s", e.getMessage());
        }
        return errorReason;
    }

    private String requestToken() {
        String body = "client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8) + "&client_secret="
                + URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8) + "&username="
                + URLEncoder.encode(config.email, StandardCharsets.UTF_8) + "&password="
                + URLEncoder.encode(config.password, StandardCharsets.UTF_8) + "&grant_type="
                + URLEncoder.encode("password", StandardCharsets.UTF_8);
        return body;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    private void connect() {
        String response = sendPostRequest("/oauth/token", requestToken(), "application/x-www-form-urlencoded");
        mammotionResponse.tokenResponse = gson.fromJson(response, TokenResponse.class);
        mammotionResponse.unpackJsonObjects();

        if (mammotionResponse.tokenResponse.data == null || mammotionResponse.tokenResponse.code != 0) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    mammotionResponse.tokenResponse.msg);
        } else {
            updateStatus(ThingStatus.ONLINE);
            // sendPostRequest("/user-server/v1/user/oauth/check", "", "application/x-www-form-urlencoded");//works
            sendPostRequest("/device-server/v1/pool-robot/device/list", "", "application/x-www-form-urlencoded");
            sendPostRequest("/device-server/v1/pool-robot/list", "", "application/x-www-form-urlencoded");
            // sendPostRequest("/device-server/v1/device/nickname", "", "application/x-www-form-urlencoded");//works
            // needs body
            // sendPostRequest("/device-server/v1/pool-robot/version/check", "", "application/x-www-form-urlencoded");
            // sendPostRequest("/device-server/v1/pool-robot/appoint-version-upgrade", "",
            // "application/x-www-form-urlencoded");
            sendGetRequest("/device-server/v1/list");
            sendGetRequest("/device-server/v1/pool-robot/list");
            // sendGetRequest("/v1/device/logs");
            // sendGetRequest("/v1/device/config");
            // sendGetRequest("/user-server/v1/code/record/export-data");
        }

        // /device/mower/{mowerId}/status
        // /device/mower/{mowerId}/config
        // /device/mower/{mowerId}/command
        // /user/oauth/token //refresh the token or The same endpoint used with grant_type=refresh_token to obtain a new
    }

    @Override
    public void initialize() {
        config = getConfigAs(MammotionConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);
        // Generate random 16-byte AES key and IV with Base64 Encoding.
        aesKey = new Random().ints(16, 0, 62)
                .mapToObj(i -> "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

        ivString = new Random().ints(16, 0, 10).mapToObj(i -> "0123456789".charAt(i))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

        scheduler.schedule(this::connect, 0, TimeUnit.SECONDS);
    }
}
