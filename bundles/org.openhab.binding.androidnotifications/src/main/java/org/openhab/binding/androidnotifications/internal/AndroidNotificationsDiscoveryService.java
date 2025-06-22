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

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AndroidNotificationsDiscoveryService} Discovers and adds any Android TV devices found that have the
 * correct port answering to indicate a supported app is running.
 *
 * @author Matthew Skinner - Initial contribution
 */
@NonNullByDefault
@Component(service = MDNSDiscoveryParticipant.class, immediate = true, configurationPid = "discovery.androidnotifications")
public class AndroidNotificationsDiscoveryService implements MDNSDiscoveryParticipant {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final HttpClient httpClient;

    @Activate
    public AndroidNotificationsDiscoveryService(@Reference HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    private boolean sendGetRequest(String fullURL) {
        try {
            Request request = httpClient.newRequest(fullURL);
            request.timeout(5, TimeUnit.SECONDS);
            request.method(HttpMethod.GET);
            request.header(HttpHeader.ACCEPT_ENCODING, "gzip");
            logger.trace("Sending possible TV GET:{}", fullURL);
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (TimeoutException | IllegalArgumentException | ExecutionException e) {
            logger.debug("Discovery hit an Exception which may have blocked a device from getting discovered:{}",
                    e.getMessage());
        }
        return false;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return SUPPORTED_THING_TYPES;
    }

    @Override
    public String getServiceType() {
        return "._androidtvremote2._tcp.local.";
    }

    @Override
    public @Nullable DiscoveryResult createResult(ServiceInfo service) {
        if (!service.hasData()) {
            return null;
        }
        String name = service.getName().replaceAll("\\s+", ""); // remove spaces as UID does not allow them
        String macAddress = service.getPropertyString("bt");
        String address;
        InetAddress[] ipAddresses = service.getInet4Addresses();
        if (ipAddresses.length > 0) {
            address = ipAddresses[0].getHostAddress();
        } else {
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("mDNS service nice name: {}", service.getNiceTextString());
            logger.debug("mDNS address: {}", address);
            logger.debug("mDNS macAddress: {}", macAddress);
            logger.debug("mDNS name `{}`", name);
        }
        if (sendGetRequest("http://" + address + ":5001/notifications")) {
            ThingUID thingUID = new ThingUID(TV_OVERLAY_DISPLAY_THING_TYPE, name);
            Map<String, Object> properties = Map.of(Thing.PROPERTY_MAC_ADDRESS, macAddress, CONFIG_ADDRESS, address);
            return DiscoveryResultBuilder.create(thingUID).withLabel(name + " running TvOverlay")
                    .withProperties(properties).withRepresentationProperty(Thing.PROPERTY_MAC_ADDRESS).build();
        }
        if (sendGetRequest("http://" + address + ":7676/show?title=openHAB&msg=Discovered&fontsize=0&position=0")) {
            ThingUID thingUID = new ThingUID(NFATV_DISPLAY_THING_TYPE, name);
            Map<String, Object> properties = Map.of(Thing.PROPERTY_MAC_ADDRESS, macAddress, CONFIG_ADDRESS, address);
            return DiscoveryResultBuilder.create(thingUID).withLabel(name + " running TvOverlay")
                    .withProperties(properties).withRepresentationProperty(Thing.PROPERTY_MAC_ADDRESS).build();
        }
        if (sendGetRequest("http://" + address + ":7979/")) {
            logger.info("Android may be running PiPup which could be supported, consider requesting this feature");
        }
        return null;
    }

    @Override
    public @Nullable ThingUID getThingUID(ServiceInfo service) {
        return null;
    }
}
