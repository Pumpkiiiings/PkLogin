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

package com.pumpkiiings.pklogin.paper.autologin;

import com.pumpkiiings.pklogin.common.manager.PremiumNameLookup;
import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.paper.PkLoginPaper;

import java.util.Optional;

/**
 * Decides whether a joining player should be asked to prove they own a premium
 * account. Shared by the ProtocolLib and PacketEvents paths, which answer the
 * same question through different libraries.
 *
 * <p>Saying yes starts Mojang's encryption handshake, and a client that cannot
 * complete it does not fall back to a password prompt — it cannot join at all.
 * Hence yes only on a real reason to believe the account is premium.</p>
 */
public final class PremiumDecision {

    private PremiumDecision() {}

    /** @return true to send the encryption request */
    public static boolean shouldRequestEncryption(PkLoginPaper plugin, String username) {
        Optional<Account> accountOpt = plugin.getAccountManagement().retrieveOrLoad(username);
        if (accountOpt.isPresent()) {
            // A known player keeps whatever mode their account already says. This
            // is what stops a server's existing offline players from being locked
            // out the day passwordless premium logins are switched on.
            String type = accountOpt.get().getUuidType();
            return "REAL".equals(type) || "PREMIUM".equals(type);
        }

        if (!PremiumNameLookup.isEnabled()) {
            return false;
        }

        // Only an outright yes. An unreachable Mojang means the player registers a
        // password today and can convert later with /premium; guessing yes would
        // leave them unable to connect.
        return PremiumNameLookup.lookup(username) == PremiumNameLookup.Answer.PREMIUM;
    }
}
