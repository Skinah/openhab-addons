/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

    public static boolean sendImage(@Nullable ThingActions actions, String imageURL, @Nullable String title,
            @Nullable String message) {
        if (actions instanceof AndroidNotificationActions) {
            ((AndroidNotificationActions) actions).sendImage(imageURL, title, message);
            return true;
        } else {
            throw new IllegalArgumentException("Instance is not an AndroidNotificationActions class.");
        }
    }

    public static boolean sendText(@Nullable ThingActions actions, @Nullable String title, String message) {
        if (actions instanceof AndroidNotificationActions) {
            ((AndroidNotificationActions) actions).sendText(title, message);
            return true;
        } else {
            throw new IllegalArgumentException("Instance is not an AndroidNotificationActions class.");
        }
    }

    public static boolean sendVideo(@Nullable ThingActions actions, String videoURL, @Nullable String title,
            @Nullable String message) {
        if (actions instanceof AndroidNotificationActions) {
            ((AndroidNotificationActions) actions).sendVideo(videoURL, title, message);
            return true;
        } else {
            throw new IllegalArgumentException("Instance is not an AndroidNotificationActions class.");
        }
    }

    @RuleAction(label = "@text/actionSendImageLabel", description = "@text/actionSendImageDesc")
    public boolean sendImage(@ActionInput(name = "imageURL") String imageURL,
            @ActionInput(name = "title") @Nullable String title,
            @ActionInput(name = "message") @Nullable String message) {
        TvOverlayDisplayHandler localHandler = tvOverlayDisplayHandler;
        if (localHandler != null) {
            localHandler.sendImage(imageURL, title, message);
            return true;
        }
        return false;
    }

    @RuleAction(label = "@text/actionSendTextLabel", description = "@text/actionSendTextDesc")
    public boolean sendText(@ActionInput(name = "title") @Nullable String title,
            @ActionInput(name = "message") String message) {
        TvOverlayDisplayHandler localHandler = tvOverlayDisplayHandler;
        if (localHandler != null) {
            localHandler.sendText(title, message);
            return true;
        }
        return false;
    }

    @RuleAction(label = "@text/actionSendVideoLabel", description = "@text/actionSendVideoDesc")
    public boolean sendVideo(@ActionInput(name = "videoURL") String videoURL,
            @ActionInput(name = "title") @Nullable String title,
            @ActionInput(name = "message") @Nullable String message) {
        TvOverlayDisplayHandler localHandler = tvOverlayDisplayHandler;
        if (localHandler != null) {
            localHandler.sendVideo(videoURL, title, message);
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
