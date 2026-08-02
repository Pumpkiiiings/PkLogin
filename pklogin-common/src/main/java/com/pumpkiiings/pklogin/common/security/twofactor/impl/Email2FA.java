package com.pumpkiiings.pklogin.common.security.twofactor.impl;

import com.pumpkiiings.pklogin.common.model.Account;
import com.pumpkiiings.pklogin.common.security.twofactor.TwoFactorProvider;

public class Email2FA implements TwoFactorProvider {

    private boolean enabled;
    private javax.mail.Session session;
    private String fromEmail;

    @Override
    public String getProviderId() {
        return "EMAIL";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void init(java.io.File dataFolder) {
        try {
            java.io.File file = new java.io.File(dataFolder, "2fa/email.yml");
            if (!file.exists()) return;

            // Read the file as YAML rather than scanning lines: "enable" appears
            // both at the root and under options.link-required, and a line-based
            // parser picks up whichever comes last, so the setting never applied.
            dev.dejvokep.boostedyaml.YamlDocument config = dev.dejvokep.boostedyaml.YamlDocument.create(file);

            this.enabled = config.getBoolean("enable", false);
            if (!this.enabled) return;

            String host = config.getString("authentication.host", "");
            int port = config.getInt("authentication.port", 587);
            String user = config.getString("authentication.user", "");
            String pass = config.getString("authentication.password", "");
            String authEmail = config.getString("authentication.email", "");
            String encryption = config.getString("authentication.encryption", "auto").toLowerCase();

            this.fromEmail = authEmail;

            java.util.Properties props = new java.util.Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));
            props.put("mail.smtp.auth", "true");

            if (encryption.equals("tls") || encryption.equals("auto")) {
                props.put("mail.smtp.starttls.enable", "true");
            } else if (encryption.equals("ssl")) {
                props.put("mail.smtp.ssl.enable", "true");
            }

            final String finalUser = user;
            final String finalPass = pass;
            this.session = javax.mail.Session.getInstance(props, new javax.mail.Authenticator() {
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(finalUser, finalPass);
                }
            });

            System.out.println("[PkLogin] Email 2FA initialized successfully!");
        } catch (Throwable t) {
            System.err.println("[PkLogin] Failed to initialize Email 2FA: " + t.getMessage());
            this.enabled = false;
        }
    }

    @Override
    public boolean isLinked(Account account) {
        return account.getEmailAddress() != null && !account.getEmailAddress().isEmpty();
    }

    @Override
    public boolean sendVerificationCode(Account account, String code) {
        if (!enabled || session == null || !isLinked(account)) return false;

        try {
            javax.mail.Message message = new javax.mail.internet.MimeMessage(session);
            message.setFrom(new javax.mail.internet.InternetAddress(fromEmail));
            message.setRecipients(
                    javax.mail.Message.RecipientType.TO,
                    javax.mail.internet.InternetAddress.parse(account.getEmailAddress())
            );
            message.setSubject(com.pumpkiiings.pklogin.common.settings.Messages.EMAIL_SUBJECT
                    .asString("PkLogin - Verification Code"));
            message.setText(com.pumpkiiings.pklogin.common.settings.Messages.EMAIL_BODY
                    .asString("Hello {0},\n\nYour PkLogin verification code is: {1}")
                    .replace("{0}", account.getRealName())
                    .replace("{1}", code));

            javax.mail.Transport.send(message);
            return true;
        } catch (javax.mail.MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void shutdown() {
        // JavaMail sessions don't necessarily require active shutdown unless pooling transport
    }
}
