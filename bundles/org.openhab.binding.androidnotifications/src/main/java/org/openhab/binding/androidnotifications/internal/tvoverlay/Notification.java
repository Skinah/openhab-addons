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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link Notification} class defines Notification.JSON structure that is used for sending normal type
 * notifications.
 *
 * @author MatthewSkinner - Initial contribution
 */
@NonNullByDefault
public class Notification {
    public String id = "0";
    public @Nullable String title;
    public @Nullable String message;
    public @Nullable String source = "openHAB";
    public @Nullable String image;
    public @Nullable String video;
    public @Nullable String largeIcon;
    public @Nullable String smallIcon;
    public @Nullable String smallIconColor;
    public @Nullable String corner;
    public @Nullable Integer duration;

    public Notification(String id, @Nullable String title, @Nullable String message) {
        this.id = id;
        if (title == null) {
            this.title = ""; // workaround otherwise the API copies the message to the title
        } else {
            this.title = title;
        }
        this.message = message;
    }

    public void setVideo(String url) {
        video = url;
    }

    public void setImage(String url) {
        image = url;
    }
}
