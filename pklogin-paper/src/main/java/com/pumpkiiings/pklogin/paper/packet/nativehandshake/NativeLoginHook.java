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

import com.pumpkiiings.pklogin.paper.PkLoginPaper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Puts {@link LoginChannelHandler} in front of every incoming connection.
 *
 * <p>The only part that touches the server's internals is finding the bound
 * listening channels. Everything after that is Netty's own
 * {@code ServerBootstrapAcceptor}, whose {@code childHandler} field is what
 * decides how a newly accepted connection is set up. Wrapping a Netty field
 * rather than a Mojang one is what keeps this from breaking every release.</p>
 *
 * <p>Nothing is located by name. Paper remaps its internals, and names differ
 * between versions and between Spigot and Paper, so the search is by shape: the
 * one field holding a list of {@link ChannelFuture}. Names would have to be
 * revisited every version; shapes rarely change.</p>
 */
public final class NativeLoginHook {

    private static final String HANDLER_NAME = "pklogin_native_login";

    /** Where the handler goes, in order of preference. */
    private static final String[] ANCHOR_AFTER = {"splitter", "decompress", "timeout"};

    private static final int MAX_BIND_RETRIES = 40;
    private static final long BIND_RETRY_TICKS = 5L;

    private final PkLoginPaper plugin;
    private final KeyPair keyPair;
    private final SecureRandom random = new SecureRandom();

    /** Kept so the server can be handed back exactly what it had on disable. */
    private final List<Restore> restores = new ArrayList<>();

    private NativeLoginHook(PkLoginPaper plugin, KeyPair keyPair) {
        this.plugin = plugin;
        this.keyPair = keyPair;
    }

    private static final class Restore {
        private final Object acceptor;
        private final Field field;
        private final ChannelHandler original;

        Restore(Object acceptor, Field field, ChannelHandler original) {
            this.acceptor = acceptor;
            this.field = field;
            this.original = original;
        }

        void undo() {
            try {
                field.set(acceptor, original);
            } catch (ReflectiveOperationException ignored) {
                // Server is going down anyway.
            }
        }
    }

    /**
     * Installs the hook.
     *
     * @return the installed hook, or null if the server could not be reached, in
     *         which case the caller falls back to a packet library
     */
    public static NativeLoginHook install(PkLoginPaper plugin, KeyPair keyPair) {
        NativeLoginHook hook = new NativeLoginHook(plugin, keyPair);
        return hook.bind() ? hook : null;
    }

    private boolean bind() {
        List<Channel> channels;
        try {
            channels = findListeningChannels();
        } catch (Throwable ex) {
            plugin.getLogger().warning("Native handshake: could not reach the server's "
                    + "network listener (" + ex + ").");
            return false;
        }

        if (channels.isEmpty()) {
            // Plugins can enable before the port is bound. Nothing is wrong yet;
            // come back for it rather than declaring the hook unavailable.
            retryLater(1);
            return true;
        }

        return wrapAll(channels);
    }

    private void retryLater(int attempt) {
        if (attempt > MAX_BIND_RETRIES) {
            plugin.getLogger().warning("Native handshake: the server never bound a listening "
                    + "port, so premium auto-login could not be installed.");
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            List<Channel> channels;
            try {
                channels = findListeningChannels();
            } catch (Throwable ex) {
                plugin.getLogger().warning("Native handshake: could not reach the server's "
                        + "network listener (" + ex + ").");
                return;
            }

            if (channels.isEmpty()) {
                retryLater(attempt + 1);
                return;
            }
            wrapAll(channels);
        }, BIND_RETRY_TICKS);
    }

    private boolean wrapAll(List<Channel> channels) {
        int wrapped = 0;
        for (Channel channel : channels) {
            if (wrap(channel)) {
                wrapped++;
            }
        }

        if (wrapped == 0) {
            plugin.getLogger().warning("Native handshake: found " + channels.size()
                    + " listening channel(s) but could not wrap any of them.");
            return false;
        }
        return true;
    }

    /**
     * Replaces the acceptor's child handler with one that runs the original and
     * then adds ours.
     */
    private boolean wrap(Channel channel) {
        try {
            for (String name : channel.pipeline().names()) {
                ChannelHandler handler = channel.pipeline().get(name);
                if (handler == null || !isBootstrapAcceptor(handler)) {
                    continue;
                }

                Field field = handler.getClass().getDeclaredField("childHandler");
                field.setAccessible(true);

                ChannelHandler original = (ChannelHandler) field.get(handler);
                if (original instanceof WrappedInitializer) {
                    return true;
                }

                field.set(handler, new WrappedInitializer(original));
                restores.add(new Restore(handler, field, original));
                return true;
            }
            return false;
        } catch (Throwable ex) {
            plugin.getLogger().warning("Native handshake: could not wrap a listening channel ("
                    + ex + ").");
            return false;
        }
    }

    private static boolean isBootstrapAcceptor(ChannelHandler handler) {
        return handler.getClass().getName().endsWith("ServerBootstrapAcceptor");
    }

    /**
     * Sets up a connection the way the server intended, then adds the login
     * handler on top.
     *
     * <p>The original runs first so the vanilla handlers exist to anchor against.
     * A failure here is contained: the connection is left exactly as the server
     * built it, which means a password login rather than a broken one.</p>
     */
    private final class WrappedInitializer extends ChannelInitializer<Channel> {

        private final ChannelHandler original;

        WrappedInitializer(ChannelHandler original) {
            this.original = original;
        }

        @Override
        protected void initChannel(Channel channel) throws Exception {
            Method initChannel = findInitChannel(original.getClass());
            initChannel.setAccessible(true);
            initChannel.invoke(original, channel);

            try {
                insertHandler(channel.pipeline());
            } catch (Throwable ex) {
                plugin.getLogger().warning("Native handshake: could not attach to a connection ("
                        + ex + "). It will fall back to a password.");
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // The wrapped initializer may itself be a ChannelInitializer whose
            // removal races with the first read; forwarding keeps that intact.
            ctx.fireChannelRead(msg);
        }
    }

    private void insertHandler(ChannelPipeline pipeline) {
        LoginChannelHandler handler = new LoginChannelHandler(
                new NativeHandshakePolicy(plugin, keyPair), keyPair.getPublic(), random);

        for (String anchor : ANCHOR_AFTER) {
            if (pipeline.get(anchor) != null) {
                pipeline.addAfter(anchor, HANDLER_NAME, handler);
                return;
            }
        }

        // No known anchor: sitting before the decoder is the requirement, and
        // being first satisfies it even if the names have all changed.
        pipeline.addFirst(HANDLER_NAME, handler);
    }

    private static Method findInitChannel(Class<?> type) throws NoSuchMethodException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredMethod("initChannel", Channel.class);
            } catch (NoSuchMethodException keepLooking) {
                // ChannelInitializer is generic; the bridge method lives further up.
            }
        }
        throw new NoSuchMethodException("initChannel(Channel) on " + type.getName());
    }

    /**
     * Walks the server object for the field holding its bound channels.
     *
     * <p>Identified by shape rather than by name: the one list whose contents are
     * {@link ChannelFuture}. An empty list is still the right field, so the type
     * argument is what is checked, not what happens to be in it.</p>
     */
    private List<Channel> findListeningChannels() throws ReflectiveOperationException {
        Object craftServer = plugin.getServer();
        Method getServer = craftServer.getClass().getMethod("getServer");
        getServer.setAccessible(true);
        Object minecraftServer = getServer.invoke(craftServer);

        Object connection = findConnectionHolder(minecraftServer);
        if (connection == null) {
            throw new NoSuchFieldException("no field holding a List<ChannelFuture>");
        }

        List<Channel> channels = new ArrayList<>();
        for (Field field : allFields(connection.getClass())) {
            if (!holdsChannelFutures(field)) {
                continue;
            }

            field.setAccessible(true);
            Object value = field.get(connection);
            if (!(value instanceof List)) {
                continue;
            }

            synchronized (value) {
                for (Object element : (List<?>) value) {
                    if (element instanceof ChannelFuture) {
                        channels.add(((ChannelFuture) element).channel());
                    }
                }
            }
        }
        return channels;
    }

    private Object findConnectionHolder(Object server) throws ReflectiveOperationException {
        for (Field field : allFields(server.getClass())) {
            field.setAccessible(true);
            Object value = field.get(server);
            if (value == null) {
                continue;
            }

            for (Field candidate : allFields(value.getClass())) {
                if (holdsChannelFutures(candidate)) {
                    return value;
                }
            }
        }
        return null;
    }

    private static boolean holdsChannelFutures(Field field) {
        if (!List.class.isAssignableFrom(field.getType())) {
            return false;
        }
        if (!(field.getGenericType() instanceof java.lang.reflect.ParameterizedType)) {
            return false;
        }

        java.lang.reflect.Type[] args =
                ((java.lang.reflect.ParameterizedType) field.getGenericType())
                        .getActualTypeArguments();
        return args.length == 1 && args[0] == ChannelFuture.class;
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                fields.add(field);
            }
        }
        return fields;
    }

    /** Hands the server's own child handler back. */
    public void uninstall() {
        for (Restore restore : restores) {
            restore.undo();
        }
        restores.clear();
    }
}
