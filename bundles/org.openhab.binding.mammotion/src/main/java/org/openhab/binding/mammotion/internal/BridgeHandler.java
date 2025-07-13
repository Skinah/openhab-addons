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

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
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
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

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

    // private byte[] aesKey = new byte[12];
    // private byte[] ivBytes = new byte[12];
    String aesKey = "";
    String ivBytes = "";

    private MammotionConfiguration config = new MammotionConfiguration();

    public BridgeHandler(Bridge bridge, HttpClient httpClient) {
        super(bridge);
        this.httpClient = httpClient;
    }

    public String sendGetRequest(String url) {
        Request request;
        String errorReason = "";
        request = httpClient.newRequest(MAMMOTION_CLOUD_URL + url);
        // request.header("Host", config.address + ":" + config.port);
        // request.header("Connection", "Keep-Alive");
        request.header("content-length", "0");
        request.timeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        request.method(HttpMethod.GET);

        logger.debug("Sending Mammotion cloud GET:{}", url);

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

    private String encryptKey() throws Exception {
        String combined = aesKey + "," + ivBytes;
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
        return gson.toJson(params);
    }

    private String encryptAES(String input) throws Exception {
        IvParameterSpec vector = new IvParameterSpec(ivBytes.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, vector);
        return Base64.getEncoder().encodeToString(cipher.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    }

    public String sendPostRequest(String url, String content) {
        Request request;
        String errorReason = "";
        request = httpClient.newRequest(MAMMOTION_CLOUD_URL + url);
        // request.header("Content-Type", "application/json");

        // Date openHABTime = new Date();
        // SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-M-d'T'H:m:s'Z'");
        // dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        // request.header("x-timestamp", dateFormat.format(openHABTime.getTime()));
        // request.header("x-nonce", "b03d2a66-01aa-4dc0-8b8d-9c92e0f2c839");
        // request.header("x-signature", "b03d2a66-01aa-4dc0-8b8d-9c92e0f2c839");
        // request.header("Authorization", "b03d2a66-01aa-4dc0-8b8d-9c92e0f2c839");

        request.header("User-Agent", "okhttp/3.14.9");
        request.header("App-Version", "google Pixel 2 XL taimen-Android 11,1.11.332");
        String RSAresult = "";
        try {
            RSAresult = encryptKey();
            logger.info("Header Encrypt-Key:{}", RSAresult);
            request.header("Encrypt-Key", RSAresult);
        } catch (Exception e) {
            logger.info("Error {}", e.getMessage());
        }
        // //self.encryption_utils.encrypt_by_public_key(),
        // Authorization: Bearer <access-token>
        request.header("Decrypt-Type", "3");
        request.header("Ec-Version", "v1");
        request.timeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        request.method(HttpMethod.POST);
        try {
            request.content(new StringContentProvider(buildJson()), "application/json");
        } catch (Exception e) {
            logger.info("gson content of POST:{}", e.getMessage());
        }

        // logger.info("Sending Mammotion cloud, POST:{} JSON:{} Encrypt-Key:{}", url, buildJson(), RSAresult);

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

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        config = getConfigAs(MammotionConfiguration.class);
        updateStatus(ThingStatus.ONLINE);
        // Generate random 16-byte AES key and IV with Base64 Encoding.
        aesKey = new Random().ints(16, 0, 62)
                .mapToObj(i -> "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

        ivBytes = new Random().ints(16, 0, 10).mapToObj(i -> "0123456789".charAt(i))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();

        // aesKey = "Tn3J82nEmoDk9sD1".getBytes(StandardCharsets.UTF_8);
        // ivBytes = "S6x2pMiKeYDp5qL3".getBytes(StandardCharsets.UTF_8);

        // sendPostRequest("/user-server/v1/user/oauth/token", "");
        // sendPostRequest("/user/oauth/token", "");
        logger.info("aesKey is {} ivBytes is {}", aesKey, ivBytes);
        sendPostRequest("/oauth/token", "");

        if (!config.email.isEmpty() && config.verificationCode.isEmpty()) {

        }
    }
}
