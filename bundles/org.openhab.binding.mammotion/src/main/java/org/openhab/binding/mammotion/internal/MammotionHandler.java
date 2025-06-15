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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

/**
 * The {@link MammotionHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Matthew Skinner - Initial contribution
 */
@NonNullByDefault
public class MammotionHandler extends BaseBridgeHandler {
    private final HttpClient httpClient;
    private final Logger logger = LoggerFactory.getLogger(MammotionHandler.class);

    private MammotionConfiguration config = new MammotionConfiguration();

    public MammotionHandler(Bridge bridge, HttpClient httpClient) {
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

    public String sendPostRequest(String url, String content) {
        Request request;
        String errorReason = "";
        request = httpClient.newRequest(MAMMOTION_CLOUD_URL + url);
        request.header("User-Agent", "okhttp/3.14.9");
        request.header("App-Version", "google Pixel 2 XL taimen-Android 11,1.11.332");
        // request.header("Encrypt-Key", "google Pixel 2 XL taimen-Android 11,1.11.332");
        // //self.encryption_utils.encrypt_by_public_key(),
        // Authorization: Bearer <access-token>
        request.header("Decrypt-Type", "3");
        request.header("Ec-Version", "v1");
        request.timeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        request.method(HttpMethod.POST);
        request.content(new StringContentProvider(content), "application/json");
        logger.debug("Sending Mammotion cloud, POST:{} JSON:{}", url, content);

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

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        config = getConfigAs(MammotionConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);
        // Following requests work
        // sendGetRequest("/area");
        // sendPostRequest("/user-server/v1/code/record/export-data", ""); //needs auth working first

        // untested POST
        // /user-server/v1/code/record/export-data
        // /user-server/v1/user/oauth/check
        // /device-server/v1/iot/device/pairing json={"mowerName": mower_name, "rtkName": rtk_name}
        // /device-server/v1/iot/device/unpairing json={"mowerName": mower_name, "rtkName": rtk_name}

        // sendPostRequest("/oauth/token?username=" + config.email + "&password=" + config.password
        // + "&client_id=MADKALUBAS&client_secret=GshzGRZJjuMUgd2sYHM7&grant_type=password", "");

        if (!config.email.isEmpty() && config.verificationCode.isEmpty()) {

        }
    }
}
