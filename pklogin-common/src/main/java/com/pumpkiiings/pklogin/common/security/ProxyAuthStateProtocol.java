/*
 * The MIT License (MIT)
 *
 * Copyright © 2020 - 2026 - PkLogin Contributors
 */
package com.pumpkiiings.pklogin.common.security;

import com.pumpkiiings.pklogin.common.PluginConstants;

/** Wire values and signed field order for the proxy-authoritative auth decision. */
public final class ProxyAuthStateProtocol {

    public static final String AUTHENTICATED = "AUTHENTICATED";
    public static final String PASSWORD_REQUIRED = "PASSWORD_REQUIRED";

    private ProxyAuthStateProtocol() {}

    public static boolean isDecision(String value) {
        return AUTHENTICATED.equals(value) || PASSWORD_REQUIRED.equals(value);
    }

    public static String[] responseParts(String username, String uuid, String decision,
                                         String requestNonce, long timestamp, String responseNonce) {
        return new String[] {
                PluginConstants.SUBCHANNEL_AUTH_STATE_RESPONSE,
                username,
                uuid,
                decision,
                requestNonce,
                Long.toString(timestamp),
                responseNonce
        };
    }
}
