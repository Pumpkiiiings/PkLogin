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

package com.pumpkiiings.pklogin.paper.packet.nativehandshake;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pumpkiiings.pklogin.common.PluginConstants;
import com.pumpkiiings.pklogin.common.settings.Settings;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.packet.AutoLoginSession;
import com.pumpkiiings.pklogin.paper.packet.EncryptionUtil;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.UUID;

/**
 * Checks one encryption response against Mojang and, if it holds up, lets the
 * connection carry on.
 *
 * <p>Same steps as the ProtocolLib and PacketEvents versions, and the two parts
 * that matter — turning encryption on through the server's own network manager,
 * and the {@code hasJoined} call — never used either library to begin with.</p>
 *
 * <p>Runs off the Netty event loop because it makes a blocking HTTP request.
 * Handing the connection back is bounced onto the loop by the handler.</p>
 */
public final class NativeVerifyTask implements Runnable {

    private final PkLoginPaper plugin;
    private final HandshakeExchange exchange;
    private final KeyPair keyPair;
    private final LoginChannelHandler handler;

    public NativeVerifyTask(PkLoginPaper plugin, HandshakeExchange exchange, KeyPair keyPair,
                            LoginChannelHandler handler) {
        this.plugin = plugin;
        this.exchange = exchange;
        this.keyPair = keyPair;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            if (exchange.getEncryptedVerifyToken() == null
                    || !EncryptionUtil.verifyNonce(exchange.getVerifyToken(), keyPair.getPrivate(),
                    exchange.getEncryptedVerifyToken())) {
                reject("verify token did not match");
                return;
            }

            SecretKey loginKey = EncryptionUtil.decryptSharedKey(
                    keyPair.getPrivate(), exchange.getEncryptedSharedSecret());

            // The client switched to encrypted output the moment it sent the
            // response, so this has to happen before anything else is read.
            if (!enableEncryption(exchange.getContext().channel(), loginKey)) {
                reject("could not enable encryption on the connection");
                return;
            }

            String serverId = EncryptionUtil.getServerIdHashString("", loginKey, keyPair.getPublic());
            UUID premiumUUID = fetchMojangProfile(exchange.getUsername(), serverId);

            if (premiumUUID == null) {
                reject("Mojang rejected the session");
                return;
            }

            AutoLoginSession session =
                    new AutoLoginSession(exchange.getUsername(), exchange.getVerifyToken(), null);
            session.setPremiumUUID(premiumUUID);
            session.setVerified(true);
            plugin.storeVerifiedSession(exchange.getIp(), session);

            // Ownership of the captured packet passes back to the handler here.
            handler.resume(exchange.getContext(), exchange.getLoginStart());
        } catch (Exception ex) {
            plugin.getLogger().warning("Native premium handshake failed for "
                    + exchange.getUsername() + ": " + ex);
            reject("authentication error");
        }
    }

    /**
     * Switches the connection to AES through whichever method this server build
     * exposes.
     *
     * <p>Paper remaps NMS, so the network manager is reached by shape rather than
     * by name: newer builds take the secret key directly, older ones take a
     * decrypt and an encrypt cipher.</p>
     */
    private boolean enableEncryption(Channel channel, SecretKey loginKey) {
        try {
            Object networkManager = channel.pipeline().get("packet_handler");
            if (networkManager == null) {
                return false;
            }

            Class<?> type = networkManager.getClass();

            for (Method m : type.getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == SecretKey.class) {
                    m.setAccessible(true);
                    m.invoke(networkManager, loginKey);
                    return true;
                }
            }

            for (Method m : type.getMethods()) {
                if (m.getParameterCount() == 2
                        && m.getParameterTypes()[0] == Cipher.class
                        && m.getParameterTypes()[1] == Cipher.class) {
                    Cipher decrypt = Cipher.getInstance("AES/CFB8/NoPadding");
                    decrypt.init(Cipher.DECRYPT_MODE, loginKey,
                            new IvParameterSpec(loginKey.getEncoded()));

                    Cipher encrypt = Cipher.getInstance("AES/CFB8/NoPadding");
                    encrypt.init(Cipher.ENCRYPT_MODE, loginKey,
                            new IvParameterSpec(loginKey.getEncoded()));

                    m.setAccessible(true);
                    m.invoke(networkManager, decrypt, encrypt);
                    return true;
                }
            }

            return false;
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not enable encryption natively: " + ex);
            return false;
        }
    }

    /**
     * @return the account's real UUID, or null when Mojang does not vouch for
     *         this connection
     */
    private UUID fetchMojangProfile(String username, String serverId) {
        try {
            String query = PluginConstants.MOJANG_HAS_JOINED
                    + "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8.name())
                    + "&serverId=" + URLEncoder.encode(serverId, StandardCharsets.UTF_8.name());

            HttpURLConnection conn = (HttpURLConnection) new URL(query).openConnection();
            int timeout = Settings.AUTOLOGIN_PREMIUM_MOJANG_TIMEOUT.asInt();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            JsonObject json = new JsonParser()
                    .parse(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
                    .getAsJsonObject();

            return parseUndashedUuid(json.get("id").getAsString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static UUID parseUndashedUuid(String id) {
        return UUID.fromString(id.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5"));
    }

    /**
     * Drops the connection. Deliberately no fallback to a password prompt: the
     * account this name maps to has none, so offering one would only be a way in
     * for whoever failed the handshake.
     */
    private void reject(String reason) {
        ReferenceCountUtil.release(exchange.getLoginStart());
        plugin.getLogger().info("Native premium handshake rejected "
                + exchange.getUsername() + " (" + exchange.getIp() + "): " + reason);
        exchange.getContext().close();
    }
}
