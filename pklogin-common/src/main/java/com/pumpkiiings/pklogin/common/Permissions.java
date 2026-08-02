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

package com.pumpkiiings.pklogin.common;

/**
 * Every permission node PkLogin checks.
 *
 * <p>These stay in code rather than moving to config on purpose: server owners
 * grant them through their permission plugin, and a node that could be renamed
 * per-server would break every published setup guide and every group template.
 * They live here so the strings exist once instead of being retyped at each of
 * the two dozen check sites, where a typo silently grants or denies access.</p>
 */
public final class Permissions {

    private Permissions() {}

    /** Blanket admin node; still honoured for the console-side command paths. */
    public static final String ADMIN = "pklogin.admin";

    public static final String ADMIN_HELP = ADMIN + ".help";
    public static final String ADMIN_RELOAD = ADMIN + ".reload";
    public static final String ADMIN_FORCELOGIN = ADMIN + ".forcelogin";
    public static final String ADMIN_UNREGISTER = ADMIN + ".unregister";
    public static final String ADMIN_DELETE = ADMIN + ".delete";
    public static final String ADMIN_CHANGEPASS = ADMIN + ".changepass";
    public static final String ADMIN_VERIFY = ADMIN + ".verify";
    public static final String ADMIN_DUPEIP = ADMIN + ".dupeip";
    public static final String ADMIN_SETSPAWN = ADMIN + ".setspawn";
    public static final String ADMIN_UPDATE = ADMIN + ".update";
    public static final String ADMIN_AUTHME_IMPORT = ADMIN + ".authme-import";
}
