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
package org.openhab.binding.androidnotifications.internal.tvoverlay;

import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link Notification} class defines JSON structures that are
 * used across the whole binding.
 *
 * @author MatthewSkinner - Initial contribution
 */
public class Notification {
    public int id = 0;
    public String title = null;
    public String message = null;
    public String source = null;
    public String image = null;
    public String video = null;
    public String largeIcon = null;
    public String smallIcon = null;
    public String smallIconColor = null;
    public String corner = null;
    public int duration = 10;

    public Notification(int id, @Nullable String title, @Nullable String message) {
        this.id = id;
        this.title = title;
        this.message = message;
    }

    public void setVideo(String url) {
        video = url;
    }

    public void setImage(String url) {
        image = url;
    }

    public void setDuration(int seconds) {
        duration = seconds;
    }
}
