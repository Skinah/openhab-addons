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
package org.openhab.binding.androidnotifications.internal.tvoverlay;

import static org.openhab.binding.androidnotifications.internal.AndroidNotificationsBindingConstants.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.androidnotifications.internal.action.AndroidNotificationActions;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link TvOverlayDisplayHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author MatthewSkinner - Initial contribution
 */
@NonNullByDefault
public class TvOverlayDisplayHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final HttpClient httpClient;
    private @Nullable ScheduledFuture<?> pollingFuture = null;
    private TvOverlayDisplayConfiguration config = new TvOverlayDisplayConfiguration();
    public String baseUrlAndPort = "";
    // private Map<String, Notification> waitingNotifications = new HashMap<>();
    private Map<String, FixedNotification> waitingFixedNotifications = new HashMap<>();

    public TvOverlayDisplayHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    @Override
    public void initialize() {
        config = getConfigAs(TvOverlayDisplayConfiguration.class);
        if (config.address.contains("://")) {
            logger.warn("Address should be a pure IP or hostname that does not include http/s");
            baseUrlAndPort = config.address + ":" + config.port;
        } else {
            baseUrlAndPort = "http://" + config.address + ":" + config.port;
        }
        pollingFuture = scheduler.scheduleWithFixedDelay(this::pollState, 0, config.pollTime, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        Future<?> future = pollingFuture;
        if (future != null) {
            future.cancel(true);
            pollingFuture = null;
        }
        waitingFixedNotifications.clear();
    }

    private void pollState() {
        String result = sendGetRequest("/get");
        if (result.startsWith("{\"success\":true")) {
            updateStatus(ThingStatus.ONLINE);
            if (!waitingFixedNotifications.isEmpty()) {
                for (FixedNotification fixedNotification : waitingFixedNotifications.values()) {
                    sendPostRequest("/notify_fixed", toJson(fixedNotification));
                }
                waitingFixedNotifications.clear();
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, result);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!(command instanceof RefreshType)) {
            switch (channelUID.getId()) {
                case CHANNEL_DISPLAY_CORNER:
                    if (command instanceof StringType) {
                        Overlay overlay = new Overlay();
                        overlay.hotCorner = command.toString();
                        sendPostRequest("/set/overlay", toJson(overlay));
                    }
                    break;
                case CHANNEL_DISPLAY_FIXED_NOTIFICATIONS:
                    if (command instanceof OnOffType) {
                        NotificationSettings notificationSettings = new NotificationSettings();
                        notificationSettings.displayFixedNotifications = command == OnOffType.ON;
                        sendPostRequest("/set/notifications", toJson(notificationSettings));
                    }
                    break;
                case CHANNEL_OVERLAY_VISIBILITY:
                    if (command instanceof PercentType percentCommand) {
                        Overlay overlay = new Overlay();
                        overlay.overlayVisibility = percentCommand.intValue();
                        if (overlay.overlayVisibility > 95) {
                            overlay.overlayVisibility = 95;
                        }
                        sendPostRequest("/set/overlay", toJson(overlay));
                    }
                    break;
                case CHANNEL_CLOCK_VISABILITY:
                    if (command instanceof PercentType percentCommand) {
                        Overlay overlay = new Overlay();
                        overlay.clockOverlayVisibility = percentCommand.intValue();
                        if (overlay.clockOverlayVisibility > 95) {
                            overlay.clockOverlayVisibility = 95;
                        }
                        sendPostRequest("/set/overlay", toJson(overlay));
                    }
                    break;
                case CHANNEL_FIXED_NOTIFICATIONS_VISIBILITY:
                    if (command instanceof PercentType percentCommand) {
                        NotificationSettings notificationSettings = new NotificationSettings();
                        notificationSettings.fixedNotificationsVisibility = percentCommand.intValue();
                        if (notificationSettings.fixedNotificationsVisibility > 95) {
                            notificationSettings.fixedNotificationsVisibility = 95;
                        }
                        sendPostRequest("/set/notifications", toJson(notificationSettings));
                    }
                    break;
                case CHANNEL_NOTIFICATION_DURATION:
                    if (!(command instanceof QuantityType<?>)) {
                        logger.warn("Ignoring non-QuantityType command for duration.");
                        return;
                    }
                    QuantityType<?> quantity = (QuantityType<?>) command;
                    quantity = quantity.toUnit(Units.SECOND);
                    if (quantity != null) {
                        NotificationSettings notificationSettings = new NotificationSettings();
                        notificationSettings.notificationDuration = quantity.intValue();
                        sendPostRequest("/set/notifications", toJson(notificationSettings));
                    }
                    break;
                case CHANNEL_DISPLAY_NOTIFICATIONS:
                    if (command instanceof OnOffType) {
                        NotificationSettings notificationSettings = new NotificationSettings();
                        notificationSettings.displayNotifications = command == OnOffType.ON;
                        sendPostRequest("/set/notifications", toJson(notificationSettings));
                    }
                    break;
                case CHANNEL_SEND_TEST_NOTIFICATION:
                    if (command instanceof OnOffType) {
                        sendText("9999", "Empowering the smart home", "Congratulations you have a working test message",
                                "mdi:home-alert-outline", "mdi:chevron-up-circle-outline", "#f47d2e", null, null);
                    }
                    break;
                case CHANNEL_SEND_NOTIFICATION:
                    if (command instanceof StringType) {
                        sendText("9999", command.toString(), null, "mdi:home-alert-outline",
                                "mdi:chevron-up-circle-outline", "#f47d2e", null, null);
                    }
                    break;
                case CHANNEL_PIXEL_SHIFT:
                    if (command instanceof OnOffType) {
                        sendPostRequest("/set/settings", "{\"pixelShift\": " + OnOffType.ON.equals(command) + "}");
                    }
                    break;
            }
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(AndroidNotificationActions.class);
    }

    public String sendGetRequest(String url) {
        Request request;
        String errorReason = "";
        request = httpClient.newRequest(baseUrlAndPort + url);
        request.header("Host", config.address + ":" + config.port);
        request.header("Connection", "Keep-Alive");
        request.header("content-length", "0");
        request.timeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        request.method(HttpMethod.GET);

        logger.debug("Sending TV GET:{}", url);

        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                return contentResponse.getContentAsString();
            } else {
                errorReason = String.format("TV request failed with %d: %s", contentResponse.getStatus(),
                        contentResponse.getContentAsString());
            }
        } catch (TimeoutException e) {
            errorReason = "TimeoutException: TV was not reachable on your network";
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
        request = httpClient.newRequest(baseUrlAndPort + url);
        request.header("Host", config.address + ":" + config.port);
        request.header("Connection", "Keep-Alive");
        request.timeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        request.method(HttpMethod.POST);
        request.content(new StringContentProvider(content), "application/json");
        logger.trace("Sending TV POST:{} JSON:{}", url, content);

        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                logger.trace("TV returned:{}", contentResponse.getContentAsString());
                return contentResponse.getContentAsString();
            } else {
                errorReason = String.format("TV request failed with %d: %s", contentResponse.getStatus(),
                        contentResponse.getContentAsString());
            }
        } catch (TimeoutException e) {
            errorReason = "TimeoutException: TV was not reachable on your network";
        } catch (ExecutionException e) {
            errorReason = String.format("ExecutionException: %s", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorReason = String.format("InterruptedException: %s", e.getMessage());
        }
        return errorReason;
    }

    private String toJson(Object object) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        return gson.toJson(object);
    }

    public boolean sendText(String messageID, @Nullable String title, @Nullable String message,
            @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor,
            @Nullable String corner, @Nullable Integer duration) {
        Notification notification = new Notification(messageID, title, message);
        notification.corner = corner;
        notification.duration = duration;
        if (largeIcon != null) {
            notification.largeIcon = handleImage(largeIcon);
        }
        if (smallIcon != null) {
            notification.smallIcon = handleImage(smallIcon);
        }
        notification.smallIconColor = smallIconColor;
        return "{\"success\":true,\"message\":\"Notification received\"}"
                .equals(sendPostRequest("/notify", toJson(notification)));
    }

    public boolean sendVideo(String messageID, @Nullable String title, @Nullable String message, String videoURL,
            @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor,
            @Nullable String corner, @Nullable Integer duration) {
        Notification notification = new Notification(messageID, title, message);
        notification.setVideo(videoURL);
        notification.corner = corner;
        notification.duration = duration;
        if (largeIcon == null) {
            notification.largeIcon = "mdi:motion-sensor";
        } else {
            notification.largeIcon = handleImage(largeIcon);
        }
        if (smallIcon == null) {
            notification.smallIcon = "mdi:cctv";
        } else {
            notification.smallIcon = handleImage(smallIcon);
        }
        notification.smallIconColor = smallIconColor;
        return "{\"success\":true,\"message\":\"Notification received\"}"
                .equals(sendPostRequest("/notify", toJson(notification)));
    }

    public boolean sendImage(String messageID, @Nullable String title, @Nullable String message, String imageURL,
            @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor,
            @Nullable String corner, @Nullable Integer duration) {
        Notification notification = new Notification(messageID, title, message);
        notification.setImage(handleImage(imageURL));
        notification.corner = corner;
        notification.duration = duration;
        if (largeIcon == null) {
            notification.largeIcon = "mdi:image-album";
        } else {
            notification.largeIcon = handleImage(largeIcon);
        }
        if (smallIcon == null) {
            notification.smallIcon = "mdi:camera";
        } else {
            notification.smallIcon = handleImage(smallIcon);
        }
        notification.smallIconColor = smallIconColor;
        return "{\"success\":true,\"message\":\"Notification received\"}"
                .equals(sendPostRequest("/notify", toJson(notification)));
    }

    private String fileToBase64(String filename) {
        logger.debug("Reading file from local file system: {}", filename);
        File file;
        try {
            file = Path.of(new URL(filename).getPath()).toFile();
            FileInputStream fileInputStreamReader;
            fileInputStreamReader = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fileInputStreamReader.read(bytes);
            fileInputStreamReader.close();
            return new String(Base64.getEncoder().encode(bytes));
        } catch (IOException e) {
            logger.warn("Reading file failed: {}", e.getMessage());
        }
        return "";
    }

    private String downloadToBase64(String filename) {
        logger.debug("Downloading file from: {}", filename);
        Request request = httpClient.newRequest(filename).method(HttpMethod.GET).timeout(HTTP_TIMEOUT_SECONDS,
                TimeUnit.SECONDS);
        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                byte[] fileContent = contentResponse.getContent();
                return new String(Base64.getEncoder().encode(fileContent));
            } else {
                logger.warn("Downloading file failed with {}:{}", contentResponse.getStatus(),
                        contentResponse.getContentAsString());
            }
        } catch (TimeoutException | ExecutionException e) {
            logger.warn("Downloading file failed: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Downloading file failed: {}", e.getMessage());
        }
        return "";
    }

    /**
     * The {@link handleImage} Will check if it is a local file or the user wants to download a source, then turn it
     * into a base64 String the API can understand. It also can use a trick to ensure a http/s source always get
     * downloaded and not fetched from cache.
     *
     */
    private String handleImage(String image) {
        String lowercase = image.toLowerCase();
        if (lowercase.startsWith("file:")) {
            return fileToBase64(image);
        } else if (lowercase.startsWith("download:")) {
            return downloadToBase64(image.substring(9));// remove the download:
        } else if (lowercase.startsWith("http")) {
            // prevent cache from using an old image instead up updating every time.
            if (image.contains("?")) {
                return image + "&time=" + new Date().getTime();
            } else {
                return image + "?time=" + new Date().getTime();
            }
        }
        return image;
    }

    public boolean sendFixedNotification(String messageID, @Nullable String messageColor, @Nullable String message,
            @Nullable String icon, @Nullable String iconColor, @Nullable String borderColor,
            @Nullable String backgroundColor, @Nullable Integer expiration, @Nullable String shape,
            @Nullable Boolean visible) {
        FixedNotification fixedNotification = new FixedNotification();
        fixedNotification.id = messageID;
        fixedNotification.messageColor = messageColor;
        fixedNotification.message = message;
        fixedNotification.iconColor = iconColor;
        fixedNotification.borderColor = borderColor;
        fixedNotification.backgroundColor = backgroundColor;
        fixedNotification.expiration = expiration;
        fixedNotification.shape = shape;
        fixedNotification.visible = visible;
        if (icon != null) {
            fixedNotification.icon = handleImage(icon);
        }
        boolean sent = "{\"success\":true,\"message\":\"Notification received\"}"
                .equals(sendPostRequest("/notify_fixed", toJson(fixedNotification)));
        if (!sent && visible != null && visible == false) {
            waitingFixedNotifications.remove(messageID);
        } else if (!sent && config.resendFixed) {
            waitingFixedNotifications.put(messageID, fixedNotification);
        }
        return sent;
    }
}
