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
package org.openhab.binding.androidnotifications.internal.androidtvnotifications;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link AndroidTvNotificationsDisplayConfiguration} is responsible for the config settings.
 *
 * @author MatthewSkinner - Initial contribution
 */
@NonNullByDefault
public class AndroidTvNotificationsDisplayConfiguration {
    public String address = "";
    public int port = 0;
}
