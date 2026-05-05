package fr.doranco.nexilock.service;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du moteur cryptographique NexiLock.
 * Vérifie PBKDF2, Key Wrapping, chiffrement/déchiffrement et générateurs.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CryptoServiceTest {

    private static CryptoService crypto;
    private static String saltB64;
    private static byte[] wrappedMk;
    private static final char[] PASSWORD = "TestPass123!".toCharArray();

    @BeforeAll
    static void initVault() throws Exception {
        CryptoService.InitResult init = CryptoService.initNewVault(PASSWORD);
        crypto = init.crypto;
        saltB64 = init.saltB64;
        wrappedMk = init.wrappedMasterKey;
    }

    // ---- Dérivation et déverrouillage ----

    @Test
    @Order(1)
    void testUnlockWithCorrectPassword() throws Exception {
        CryptoService unlocked = CryptoService.unlock(PASSWORD, saltB64, wrappedMk);
        assertNotNull(unlocked, "Déverrouillage avec le bon mot de passe doit réussir.");
    }

    @Test
    @Order(2)
    void testUnlockWithWrongPasswordReturnsNull() throws Exception {
        CryptoService result = CryptoService.unlock("MauvaisMotDePasse!".toCharArray(), saltB64, wrappedMk);
        assertNull(result, "Déverrouillage avec mauvais mot de passe doit retourner null.");
    }

    // ---- Chiffrement / Déchiffrement ----

    @Test
    @Order(3)
    void testEncryptDecryptRoundTrip() throws Exception {
        String original = "monSuperMotDePasse@2026!";
        byte[] blob = crypto.encrypt(original);
        String decrypted = crypto.decrypt(blob);
        assertEquals(original, decrypted, "Le texte déchiffré doit être identique à l'original.");
    }

    @Test
    @Order(4)
    void testEncryptProducesDifferentBlobsForSameInput() throws Exception {
        String pwd = "identique";
        byte[] b1 = crypto.encrypt(pwd);
        byte[] b2 = crypto.encrypt(pwd);
        assertFalse(java.util.Arrays.equals(b1, b2),
            "Deux chiffrements du même texte doivent produire des BLOBs différents (IV aléatoire).");
    }

    @Test
    @Order(5)
    void testEncryptBlobStartsWithIv() throws Exception {
        byte[] blob = crypto.encrypt("test");
        assertTrue(blob.length > 12,
            "Le BLOB doit contenir au moins 12 octets (IV) plus le ciphertext.");
    }

    @Test
    @Order(6)
    void testDecryptWithWrongKeyFails() throws Exception {
        CryptoService otherCrypto = CryptoService.initNewVault("AutreMdp!99".toCharArray()).crypto;
        byte[] blob = crypto.encrypt("secret");
        assertThrows(Exception.class, () -> otherCrypto.decrypt(blob),
            "Déchiffrement avec une mauvaise clé doit lever une exception.");
    }

    // ---- Vérification du mot de passe ----

    @Test
    @Order(7)
    void testVerifyPasswordCorrect() throws Exception {
        CryptoService.InitResult init = CryptoService.initNewVault("Azerty123!".toCharArray());
        boolean ok = CryptoService.verifyPassword(
            "Azerty123!".toCharArray(), init.saltB64, init.hashB64
        );
        assertTrue(ok, "La vérification avec le bon mot de passe doit réussir.");
    }

    @Test
    @Order(8)
    void testVerifyPasswordIncorrect() throws Exception {
        CryptoService.InitResult init = CryptoService.initNewVault("Azerty123!".toCharArray());
        boolean ok = CryptoService.verifyPassword(
            "Mauvais!".toCharArray(), init.saltB64, init.hashB64
        );
        assertFalse(ok, "La vérification avec un mauvais mot de passe doit échouer.");
    }

    // ---- Générateur de mots de passe ----

    @Test
    @Order(9)
    void testGeneratePasswordLength() {
        for (int len : new int[]{8, 12, 16, 24, 32}) {
            String pwd = CryptoService.generatePassword(len);
            assertEquals(len, pwd.length(), "Le mot de passe généré doit avoir exactement " + len + " caractères.");
        }
    }

    @Test
    @Order(10)
    void testGeneratePasswordComplexity() {
        String pwd = CryptoService.generatePassword(16);
        assertTrue(pwd.chars().anyMatch(Character::isUpperCase), "Doit contenir une majuscule.");
        assertTrue(pwd.chars().anyMatch(Character::isLowerCase), "Doit contenir une minuscule.");
        assertTrue(pwd.chars().anyMatch(Character::isDigit),     "Doit contenir un chiffre.");
        assertTrue(pwd.chars().anyMatch(c -> "!@#$%^&*()-_=+[]{}|;:,.<>?".indexOf(c) >= 0),
            "Doit contenir un caractère spécial.");
    }

    @Test
    @Order(11)
    void testGeneratePasswordTooShortThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> CryptoService.generatePassword(7),
            "Longueur < 8 doit lever IllegalArgumentException.");
    }

    @Test
    @Order(12)
    void testGeneratePasswordTooLongThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> CryptoService.generatePassword(33),
            "Longueur > 32 doit lever IllegalArgumentException.");
    }

    @Test
    @Order(13)
    void testGeneratePasswordIsRandom() {
        String p1 = CryptoService.generatePassword(20);
        String p2 = CryptoService.generatePassword(20);
        assertNotEquals(p1, p2, "Deux mots de passe générés ne doivent pas être identiques.");
    }

    // ---- Recovery Key ----

    @Test
    @Order(14)
    void testGenerateRecoveryKeyFormat() {
        String rk = CryptoService.generateRecoveryKey();
        assertNotNull(rk);
        // Format : XXXXXX-XXXXXX-XXXXXX-XXXXXX (27 chars avec tirets)
        assertTrue(rk.matches("[A-Z0-9]{6}-[A-Z0-9]{6}-[A-Z0-9]{6}-[A-Z0-9]{6}"),
            "La Recovery Key doit respecter le format XXXXXX-XXXXXX-XXXXXX-XXXXXX.");
    }

    @Test
    @Order(15)
    void testGenerateRecoveryKeyIsUnique() {
        String rk1 = CryptoService.generateRecoveryKey();
        String rk2 = CryptoService.generateRecoveryKey();
        assertNotEquals(rk1, rk2, "Deux Recovery Keys ne doivent pas être identiques.");
    }
}
