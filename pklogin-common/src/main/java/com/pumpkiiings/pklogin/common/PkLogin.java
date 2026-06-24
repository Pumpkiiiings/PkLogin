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

import com.pumpkiiings.pklogin.api.PkLoginAPI;
import lombok.NonNull;

public class PkLogin {

    private static PkLoginAPI api;

    public static PkLoginAPI getApi() {
        if (api == null) {
            throw new IllegalStateException("The api instance has not yet been defined.");
        }
        return api;
    }

    public static void setApi(@NonNull PkLoginAPI api) {
        if (PkLogin.api != null) {
            throw new IllegalStateException("The api instance has already been defined.");
        }
        PkLogin.api = api;
    }

    private static com.pumpkiiings.pklogin.common.manager.AccountManagement accountManagement;

    public static com.pumpkiiings.pklogin.common.manager.AccountManagement getAccountManagement() {
        return accountManagement;
    }

    public static void setAccountManagement(com.pumpkiiings.pklogin.common.manager.AccountManagement accountManagement) {
        PkLogin.accountManagement = accountManagement;
    }
}

