package com.pumpkiiings.pklogin.common.security.twofactor;

import com.pumpkiiings.pklogin.common.model.Account;

public interface TwoFactorProvider {
    
    /**
     * Identificador único del proveedor (ej. "DISCORD", "EMAIL", "TOTP")
     */
    String getProviderId();

    /**
     * @return true si este proveedor está habilitado en su respectiva configuración
     */
    boolean isEnabled();

    /**
     * Inicializa el proveedor (ej. conecta el bot, prepara sesión SMTP)
     */
    void init();

    /**
     * @param account Cuenta a verificar
     * @return true si la cuenta tiene vinculado este método de 2FA
     */
    boolean isLinked(Account account);

    /**
     * Envía un código de verificación al jugador.
     * @param account Cuenta a la que se le envía
     * @param code El código generado a enviar
     * @return true si se envió correctamente
     */
    boolean sendVerificationCode(Account account, String code);

    /**
     * Cierra cualquier conexión o recurso del proveedor
     */
    void shutdown();
}
