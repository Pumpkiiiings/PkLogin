/*
 * The MIT License (MIT)
 *
 * Copyright © 2020 - 2026 - PkLogin Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.pumpkiiings.pklogin.common.security;

import com.pumpkiiings.pklogin.common.PluginConstants;

/**
 * The field layout of the proxy's connection check, shared by both ends.
 *
 * <p>A signature only means something if signer and verifier hash the same fields
 * in the same order, and the two sides build their payloads with entirely
 * different APIs — so the ordering lives here instead of being written twice.</p>
 *
 * <p>Challenge and answer: the proxy signs the name it knows a backend by, and
 * the backend signs that name back together with the challenge. A correct answer
 * proves the plugin is installed, resolved the same key, and is replying to this
 * request rather than an older one.</p>
 */
public final class ProxyVerifyProtocol {

    /**
     * Only has to cover a round trip on a loaded server. Going far above
     * {@link ProxyMessageSecurity#MAX_CLOCK_SKEW_MILLIS} buys nothing — a late
     * reply is rejected as stale anyway.
     */
    public static final long REPLY_TIMEOUT_MILLIS = 10_000L;

    private ProxyVerifyProtocol() {}

    /** @param nonce the challenge the backend has to echo back */
    public static String[] requestParts(String serverName, long timestamp, String nonce) {
        return new String[] {
                PluginConstants.SUBCHANNEL_VERIFY,
                serverName,
                Long.toString(timestamp),
                nonce
        };
    }

    /**
     * @param challenge the nonce of the check being answered
     * @param nonce     a fresh nonce for the answer itself
     */
    public static String[] replyParts(String serverName, String version, String challenge,
                                      long timestamp, String nonce) {
        return new String[] {
                PluginConstants.SUBCHANNEL_VERIFY_REPLY,
                serverName,
                version,
                challenge,
                Long.toString(timestamp),
                nonce
        };
    }
}
