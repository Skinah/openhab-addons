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
 * The {@link Overlay} class defines JSON structures that are
 * used across the whole binding.
 *
 * @author MatthewSkinner - Initial contribution
 */
@NonNullByDefault
public class Overlay {
    public @Nullable Integer clockOverlayVisibility; // optional | int (0~95) | how visible the clock is
    public @Nullable String hotCorner; // corner message displays in
    public @Nullable Integer overlayVisibility; // optional | int (0~95) | how visible the overlay background is
}
