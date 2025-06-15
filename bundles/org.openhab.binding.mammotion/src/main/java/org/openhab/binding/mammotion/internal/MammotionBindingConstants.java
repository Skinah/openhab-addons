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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link MammotionBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Matthew Skinner - Initial contribution
 */
@NonNullByDefault
public class MammotionBindingConstants {

    private static final String BINDING_ID = "mammotion";
    public static final String MAMMOTION_CLOUD_URL = "https://domestic.mammotion.com/user-server/v1";
    public static final int HTTP_TIMEOUT_SECONDS = 10;

    // List of all Thing Type UIDs

    public static final ThingTypeUID THING_TYPE_MOWER = new ThingTypeUID(BINDING_ID, "mower");
    public static final ThingTypeUID THING_TYPE_MAMMOTION = new ThingTypeUID(BINDING_ID, "mammotion");
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_MAMMOTION, THING_TYPE_MOWER);

    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";
}
