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
 * The {@link FixedNotification} class defines fixed_notification.JSON structure that is used for sending fixed type
 * notifications.
 *
 * @author MatthewSkinner - Initial contribution
 */
@NonNullByDefault
public class FixedNotification {
    public String id = "0";
    public @Nullable Boolean visible;
    public @Nullable String message;
    public @Nullable String messageColor;
    public @Nullable String icon;
    public @Nullable String iconColor;
    public @Nullable String borderColor;
    public @Nullable String backgroundColor;
    public @Nullable String shape;
    public @Nullable Integer expiration;
}
