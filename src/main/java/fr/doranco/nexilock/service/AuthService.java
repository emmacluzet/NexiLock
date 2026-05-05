package fr.doranco.nexilock.service;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;

import java.util.logging.Logger;

/**
 * Service d'authentification de NexiLock.
 *
 * Tente d'abord de valider le mot de passe via l'API Windows native LogonUser (JNA).
 * Si l'authentification native échoue (ex : mot de passe Windows changé),
 * l'appelant doit basculer vers la Recovery Key (voir AuthResult).
 */
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());

    /** Résultat d'une tentative d'authentification. */
    public enum AuthResult { 
        SUCCESS,          // Authentification Windows réussie
        WRONG_PASSWORD,   // Mot de passe Windows incorrect
        NEED_RECOVERY_KEY // API native indisponible ou erreur système → utiliser Recovery Key
    }

    /**
     * Tente d'authentifier l'utilisateur via LogonUser (Win32 JNA).
     *
     * @param username nom d'utilisateur Windows (pré-rempli depuis System.getProperty)
     * @param password mot de passe saisi
     * @return AuthResult
     */
    public static AuthResult authenticate(String username, char[] password) {
        try {
            WinNT.HANDLEByReference tokenRef = new WinNT.HANDLEByReference();
            boolean ok = Advapi32.INSTANCE.LogonUser(
                username,
                null, // domain null = machine locale
                new String(password),
                WinBase.LOGON32_LOGON_INTERACTIVE,
                WinBase.LOGON32_PROVIDER_DEFAULT,
                tokenRef
            );

            if (ok) {
                // Fermer le handle immédiatement, on n'en a pas besoin
                com.sun.jna.platform.win32.Kernel32.INSTANCE.CloseHandle(tokenRef.getValue());
                return AuthResult.SUCCESS;
            }

            int error = com.sun.jna.platform.win32.Kernel32.INSTANCE.GetLastError();
            // 1326 = ERROR_LOGON_FAILURE (mauvais mot de passe)
            if (error == 1326) return AuthResult.WRONG_PASSWORD;

            LOG.warning("LogonUser erreur système : " + error);
            return AuthResult.NEED_RECOVERY_KEY;

        } catch (UnsatisfiedLinkError | Exception e) {
            // JNA indisponible (non-Windows, tests unitaires…)
            LOG.warning("AuthService JNA indisponible : " + e.getMessage());
            return AuthResult.NEED_RECOVERY_KEY;
        }
    }

    /** Retourne le nom d'utilisateur Windows courant pour pré-remplir le champ. */
    public static String getCurrentWindowsUsername() {
        return System.getProperty("user.name", "utilisateur");
    }
}
