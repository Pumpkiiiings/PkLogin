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
    public void init() {
        try {
            java.io.File file = new java.io.File("config/pklogin/2fa/email.yml");
            if (!file.exists()) file = new java.io.File("plugins/PkLogin/2fa/email.yml");
            if (!file.exists()) return;

            // Simple yaml parsing
            boolean isEnabled = false;
            String host = "";
            int port = 587;
            String user = "";
            String pass = "";
            String authEmail = "";
            String encryption = "auto";
            
            for (String line : java.nio.file.Files.readAllLines(file.toPath())) {
                String t = line.trim();
                if (t.startsWith("enable:")) isEnabled = Boolean.parseBoolean(t.split(":", 2)[1].trim());
                else if (t.startsWith("host:")) host = t.split(":", 2)[1].trim().replace("\"", "").replace("'", "");
                else if (t.startsWith("port:")) port = Integer.parseInt(t.split(":", 2)[1].trim());
                else if (t.startsWith("user:")) user = t.split(":", 2)[1].trim().replace("\"", "").replace("'", "");
                else if (t.startsWith("password:")) pass = t.split(":", 2)[1].trim().replace("\"", "").replace("'", "");
                else if (t.startsWith("email:")) authEmail = t.split(":", 2)[1].trim().replace("\"", "").replace("'", "");
                else if (t.startsWith("encryption:")) encryption = t.split(":", 2)[1].trim().replace("\"", "").replace("'", "");
            }

            this.enabled = isEnabled;
            if (!this.enabled) return;

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
            message.setSubject("PkLogin - Código de Verificación");
            message.setText("Hola " + account.getRealName() + ",\n\nTu código de verificación de PkLogin es: " + code);

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
