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

import static org.openhab.binding.androidnotifications.internal.AndroidNotificationsBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.androidnotifications.internal.androidtvnotifications.AndroidTvNotificationsDisplayHandler;
import org.openhab.binding.androidnotifications.internal.tvoverlay.TvOverlayDisplayHandler;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link AndroidNotificationsHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author MatthewSkinner - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.androidnotifications", service = ThingHandlerFactory.class)
public class AndroidNotificationsHandlerFactory extends BaseThingHandlerFactory {
    private final HttpClient httpClient;

    @Activate
    public AndroidNotificationsHandlerFactory(final @Reference HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (NFATV_DISPLAY_THING_TYPE.equals(thingTypeUID)) {
            return new AndroidTvNotificationsDisplayHandler(thing, httpClient);
        } else if (TV_OVERLAY_DISPLAY_THING_TYPE.equals(thingTypeUID)) {
            return new TvOverlayDisplayHandler(thing, httpClient);
        }
        return null;
    }
}
