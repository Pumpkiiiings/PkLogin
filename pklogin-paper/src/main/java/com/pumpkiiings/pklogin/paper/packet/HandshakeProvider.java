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

package com.pumpkiiings.pklogin.paper.packet;

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import com.pumpkiiings.pklogin.paper.packet.nativehandshake.NativeLoginHook;

import java.security.KeyPair;

/**
 * Picks how a standalone server sends Mojang's encryption request.
 *
 * <p>Two settings, because nobody runs two packet libraries: {@code native} uses
 * the handshake built into PkLogin, {@code plugin} uses whichever of PacketEvents
 * or ProtocolLib is installed.</p>
 *
 * <p>Every failure resolves to something that works rather than to nothing. Both
 * settings fall back to the other when their own path is unavailable, and the
 * server is told which one it ended up on. The one case with no way out — no
 * library and no native hook — is reported the way a missing library always
 * was.</p>
 */
public final class HandshakeProvider {

    public enum Choice {
        NATIVE,
        PLUGIN
    }

    private HandshakeProvider() {}

    /** What the server ended up running, for the console line. */
    public enum Installed {
        NATIVE("native"),
        PACKETEVENTS("PacketEvents"),
        PROTOCOLLIB("ProtocolLib"),
        NONE("none");

        private final String label;

        Installed(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public static Choice parse(PkLoginPaper plugin, String configured) {
        if (configured == null) {
            return Choice.NATIVE;
        }

        String value = configured.trim().toLowerCase(java.util.Locale.ROOT);
        if (value.equals("native")) {
            return Choice.NATIVE;
        }
        if (value.equals("plugin")) {
            return Choice.PLUGIN;
        }

        plugin.sendMessage("§ehandshake-provider: '" + configured
                + "' is not a valid value. Using 'native'. Valid values are 'native' and 'plugin'.");
        return Choice.NATIVE;
    }

    public static Installed install(PkLoginPaper plugin, Choice choice) {
        if (choice == Choice.PLUGIN) {
            Installed viaPlugin = installLibrary(plugin);
            if (viaPlugin != Installed.NONE) {
                return viaPlugin;
            }

            plugin.sendMessage("§c=========================================================");
            plugin.sendMessage("§chandshake-provider is set to 'plugin', but no packet library");
            plugin.sendMessage("§cwas usable. Falling back to the native handshake — premium");
            plugin.sendMessage("§clogins still work.");
            plugin.sendMessage("§cInstall PacketEvents or ProtocolLib, or set");
            plugin.sendMessage("§chandshake-provider: native to silence this.");
            plugin.sendMessage("§c=========================================================");

            return installNative(plugin, true);
        }

        return installNative(plugin, false);
    }

    /**
     * @param alreadyWarned true when this is the fallback path, so a second
     *                      failure does not print two walls of red
     */
    private static Installed installNative(PkLoginPaper plugin, boolean alreadyWarned) {
        KeyPair keyPair = EncryptionUtil.generateKeyPair();
        NativeLoginHook hook = NativeLoginHook.install(plugin, keyPair);

        if (hook != null) {
            plugin.setNativeLoginHook(hook);
            return Installed.NATIVE;
        }

        // The native path could not attach. A library is the only thing left.
        Installed viaPlugin = installLibrary(plugin);
        if (viaPlugin != Installed.NONE) {
            if (!alreadyWarned) {
                plugin.sendMessage("§eThe native handshake could not attach to this server, so "
                        + viaPlugin.getLabel() + " is being used instead.");
            }
            return viaPlugin;
        }

        return Installed.NONE;
    }

    /** @return what was installed, or {@link Installed#NONE} if neither is usable */
    private static Installed installLibrary(PkLoginPaper plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("packetevents") != null) {
            try {
                com.pumpkiiings.pklogin.paper.packet.packetevents.PacketEventsHook.init(plugin);
                return Installed.PACKETEVENTS;
            } catch (Throwable ex) {
                plugin.sendMessage("§ePacketEvents is installed but could not be hooked: " + ex);
            }
        }

        if (plugin.getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            try {
                com.pumpkiiings.pklogin.paper.packet.protocollib.ProtocolLibHook.init(plugin);
                return Installed.PROTOCOLLIB;
            } catch (Throwable ex) {
                plugin.sendMessage("§eProtocolLib is installed but could not be hooked: " + ex);
            }
        }

        return Installed.NONE;
    }
}
