/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.androidnotifications.internal.action;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.androidnotifications.internal.androidtvnotifications.AndroidTvNotificationsDisplayHandler;
import org.openhab.binding.androidnotifications.internal.tvoverlay.TvOverlayDisplayHandler;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AndroidNotificationActions} Provides the actions for the TvOverlay and Adroid TV Notifications API's.
 *
 * @author MatthewSkinner - Initial contribution
 */
@Component(scope = ServiceScope.PROTOTYPE, service = AndroidNotificationActions.class)
@ThingActionsScope(name = "androidnotifications")
@NonNullByDefault
public class AndroidNotificationActions implements ThingActions {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private @Nullable TvOverlayDisplayHandler tvOverlayDisplayHandler = null;
    private @Nullable AndroidTvNotificationsDisplayHandler androidTvNotificationsDisplayHandler = null;

    public static boolean sendImage(@Nullable ThingActions actions, int messageID, @Nullable String title,
            @Nullable String message, String imageURL, @Nullable String largeIcon, @Nullable String smallIcon,
            @Nullable String smallIconColor, @Nullable String corner, @Nullable Integer duration) {
        if (actions instanceof AndroidNotificationActions) {
            ((AndroidNotificationActions) actions).sendImage(messageID, title, message, imageURL, largeIcon, smallIcon,
                    smallIconColor, corner, duration);
            return true;
        } else {
            throw new IllegalArgumentException("Instance is not an AndroidNotificationActions class.");
        }
    }

    public static boolean sendImage(@Nullable ThingActions actions, int messageID, @Nullable String title,
            @Nullable String message, String imageURL) {
        if (actions instanceof AndroidNotificationActions) {
            ((AndroidNotificationActions) actions).sendImage(messageID, title, message, imageURL, null, null, null,
                    null, null);
            return true;
        } else {
            throw new IllegalArgumentException("Instance is not an AndroidNotificationActions class.");
        }
    }

    public static boolean sendText(@Nullable ThingActions actions, int messageID, @Nullable String title,
            String message, @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor,
            @Nullable String corner, @Nullable Integer duration) {
        if (actions instanceof AndroidNotificationActions) {
            ((AndroidNotificationActions) actions).sendText(messageID, title, message, largeIcon, smallIcon,
                    smallIconColor, corner, duration);
            return true;
        } else {
            throw new IllegalArgumentException("Instance is not an AndroidNotificationActions class.");
        }
    }

    public static boolean sendText(@Nullable ThingActions actions, int messageID, @Nullable String title,
            String message) {
        if (actions instanceof AndroidNotificationActions) {
            ((AndroidNotificationActions) actions).sendText(messageID, title, message, null, null, null, null, null);
            return true;
        } else {
            throw new IllegalArgumentException("Instance is not an AndroidNotificationActions class.");
        }
    }

    public static boolean sendVideo(@Nullable ThingActions actions, int messageID, @Nullable String title,
            @Nullable String message, String videoURL, @Nullable String largeIcon, @Nullable String smallIcon,
            @Nullable String smallIconColor, @Nullable String corner, @Nullable Integer duration) {
        if (actions instanceof AndroidNotificationActions) {
            ((AndroidNotificationActions) actions).sendVideo(messageID, title, message, videoURL, largeIcon, smallIcon,
                    smallIconColor, corner, duration);
            return true;
        } else {
            throw new IllegalArgumentException("Instance is not an AndroidNotificationActions class.");
        }
    }

    public static boolean sendVideo(@Nullable ThingActions actions, int messageID, @Nullable String title,
            @Nullable String message, String videoURL) {
        if (actions instanceof AndroidNotificationActions) {
            ((AndroidNotificationActions) actions).sendVideo(messageID, title, message, videoURL, null, null, null,
                    null, null);
            return true;
        } else {
            throw new IllegalArgumentException("Instance is not an AndroidNotificationActions class.");
        }
    }

    @RuleAction(label = "@text/actionSendImageLabel", description = "@text/actionSendImageDesc")
    public boolean sendImage(@ActionInput(name = "messageID") int messageID,
            @ActionInput(name = "title") @Nullable String title,
            @ActionInput(name = "message") @Nullable String message, @ActionInput(name = "imageURL") String imageURL,
            @ActionInput(name = "largeIcon") @Nullable String largeIcon,
            @ActionInput(name = "smallIcon") @Nullable String smallIcon,
            @ActionInput(name = "smallIconColor") @Nullable String smallIconColor,
            @ActionInput(name = "corner") @Nullable String corner,
            @ActionInput(name = "duration") @Nullable Integer duration) {
        TvOverlayDisplayHandler localHandler = tvOverlayDisplayHandler;
        if (localHandler != null) {
            localHandler.sendImage(messageID, title, message, imageURL, largeIcon, smallIcon, smallIconColor, corner,
                    duration);
            return true;
        }
        return false;
    }

    @RuleAction(label = "@text/actionSendTextLabel", description = "@text/actionSendTextDesc")
    public boolean sendText(@ActionInput(name = "messageID") int messageID,
            @ActionInput(name = "title") @Nullable String title, @ActionInput(name = "message") String message,
            @ActionInput(name = "largeIcon") @Nullable String largeIcon,
            @ActionInput(name = "smallIcon") @Nullable String smallIcon,
            @ActionInput(name = "smallIconColor") @Nullable String smallIconColor,
            @ActionInput(name = "corner") @Nullable String corner,
            @ActionInput(name = "duration") @Nullable Integer duration) {
        TvOverlayDisplayHandler localHandler = tvOverlayDisplayHandler;
        if (localHandler != null) {
            localHandler.sendText(messageID, title, message, largeIcon, smallIcon, smallIconColor, corner, duration);
            return true;
        }
        return false;
    }

    @RuleAction(label = "@text/actionSendVideoLabel", description = "@text/actionSendVideoDesc")
    public boolean sendVideo(@ActionInput(name = "messageID") int messageID,
            @ActionInput(name = "title") @Nullable String title,
            @ActionInput(name = "message") @Nullable String message, @ActionInput(name = "videoURL") String videoURL,
            @ActionInput(name = "largeIcon") @Nullable String largeIcon,
            @ActionInput(name = "smallIcon") @Nullable String smallIcon,
            @ActionInput(name = "smallIconColor") @Nullable String smallIconColor,
            @ActionInput(name = "corner") @Nullable String corner,
            @ActionInput(name = "duration") @Nullable Integer duration) {
        TvOverlayDisplayHandler localHandler = tvOverlayDisplayHandler;
        if (localHandler != null) {
            localHandler.sendVideo(messageID, title, message, videoURL, largeIcon, smallIcon, smallIconColor, corner,
                    duration);
            return true;
        }
        return false;
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof TvOverlayDisplayHandler) {
            tvOverlayDisplayHandler = (TvOverlayDisplayHandler) handler;
        } else if (handler instanceof AndroidTvNotificationsDisplayHandler) {
            androidTvNotificationsDisplayHandler = (AndroidTvNotificationsDisplayHandler) handler;
        } else {
            logger.warn("Handler is not a known type");
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return (tvOverlayDisplayHandler == null) ? androidTvNotificationsDisplayHandler : tvOverlayDisplayHandler;
    }
}
