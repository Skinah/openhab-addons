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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ShieldTvDiscoveryService} Discovers and adds any Shield TV devices found that have the
 * correct port answering to indicate a supported app is running.
 *
 * @author Matthew Skinner - Initial contribution
 */
@NonNullByDefault
@Component(service = MDNSDiscoveryParticipant.class, immediate = true, configurationPid = "discovery.androidnotifications")
public class ShieldTvDiscoveryService extends AndroidNotificationsDiscoveryService {

    @Activate
    public ShieldTvDiscoveryService(@Reference HttpClientFactory httpClientFactory) {
        super(httpClientFactory);
    }

    @Override
    public String getServiceType() {
        return "_nv_shield_remote._tcp.local.";
    }
}
