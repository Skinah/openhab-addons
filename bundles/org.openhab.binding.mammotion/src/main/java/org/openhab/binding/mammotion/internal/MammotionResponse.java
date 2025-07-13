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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;

/**
 * The {@link MammotionResponse} Is the DTO from JSON responses from the mammotion cloud
 *
 * @author Matthew Skinner - Initial contribution
 */
@NonNullByDefault
public class MammotionResponse {
    protected final Gson gson = new Gson();
    @Nullable
    public TokenResponse tokenResponse = new TokenResponse();
    public DataState dataState = new DataState();

    public void unpackJsonObjects() {
        TokenResponse localTokenResponse = tokenResponse;
        if (localTokenResponse == null || localTokenResponse.data == null) {
            return;
        }
        @Nullable
        DataState localDataState = gson.fromJson(localTokenResponse.data.toString(), DataState.class);
        if (localDataState != null) {
            dataState = localDataState;
        }
    }

    public class TokenResponse {
        @Nullable
        public Object data = "{}";
        public String msg = "";
        public int code = 0;
    }

    public class DataState {
        public String access_token = "";
        public String authorization_code = "";
        public String userId = "";
    }
}
