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
package org.openhab.binding.androidnotifications.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link AndroidNotificationsBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author MatthewSkinner - Initial contribution
 */
@NonNullByDefault
public class AndroidNotificationsBindingConstants {
    public static final String BINDING_ID = "androidnotifications";
    public static final int HTTP_TIMEOUT_SECONDS = 5;

    // List of all Things and Thing Types
    public static final String NFATV_DISPLAY_THING = "nfatvdisplay";
    public static final ThingTypeUID NFATV_DISPLAY_THING_TYPE = new ThingTypeUID(BINDING_ID, NFATV_DISPLAY_THING);
    public static final String TV_OVERLAY_DISPLAY_THING = "tvoverlaydisplay";
    public static final ThingTypeUID TV_OVERLAY_DISPLAY_THING_TYPE = new ThingTypeUID(BINDING_ID,
            TV_OVERLAY_DISPLAY_THING);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = new HashSet<ThingTypeUID>(
            Arrays.asList(NFATV_DISPLAY_THING_TYPE, TV_OVERLAY_DISPLAY_THING_TYPE));

    // Configs
    public static final String CONFIG_ADDRESS = "address";

    // List of all Channel ids
    public static final String CHANNEL_DISPLAY_FIXED_NOTIFICATIONS = "displayFixedNotifications";
    public static final String CHANNEL_FIXED_NOTIFICATIONS_VISIBILITY = "fixedNotificationsVisibility";
    public static final String CHANNEL_OVERLAY_VISIBILITY = "overlayVisibility";
    public static final String CHANNEL_CLOCK_VISABILITY = "clockOverlayVisibility";
    public static final String CHANNEL_DISPLAY_CORNER = "hotCorner";// TvOverlay
    public static final String CHANNEL_DISPLAY_POSITION = "displayCorner";// ATVN
    public static final String CHANNEL_NOTIFICATION_DURATION = "notificationDuration";
    public static final String CHANNEL_DISPLAY_NOTIFICATIONS = "displayNotifications";
    public static final String CHANNEL_SEND_NOTIFICATION = "sendNotification";
    public static final String CHANNEL_SEND_TEST_NOTIFICATION = "sendTestNotification";
    public static final String CHANNEL_FONT_SIZE = "fontSize";
    public static final String CHANNEL_PIXEL_SHIFT = "pixelShift";
}
