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
 * The {@link NotificationSettings} class defines JSON structures that are
 * used across the whole binding.
 *
 * @author MatthewSkinner - Initial contribution
 */
@NonNullByDefault
public class NotificationSettings {
    public @Nullable Boolean displayNotifications;
    public @Nullable Boolean displayFixedNotifications;
    public @Nullable String notificationLayoutName;
    public @Nullable Integer notificationDuration;
    public @Nullable Integer fixedNotificationsVisibility;
}
