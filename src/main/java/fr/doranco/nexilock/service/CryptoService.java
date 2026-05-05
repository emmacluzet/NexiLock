package fr.doranco.nexilock.service;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Moteur cryptographique de NexiLock.
 *
 * Architecture à deux couches :
 *   1. KEK (Key Encryption Key) : dérivée du mot de passe via PBKDF2-HmacSHA256
 *      avec 100 000 itérations et un salt aléatoire.
 *   2. Master Key : clé AES-256 aléatoire, wrappée (chiffrée) par la KEK via
 *      AES-256-GCM et stockée en base. Elle ne circule jamais en clair.
 *
 * Les mots de passe des comptes sont chiffrés avec la Master Key (AES-256-GCM).
 * Chaque chiffrement génère un IV aléatoire de 12 octets, préfixé au ciphertext.
 */
public class CryptoService {

    private static final String PBKDF2_ALGO = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERS = 100_000;
    private static final int KEY_BITS = 256;
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LEN = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int SALT_LEN = 16;

    private final SecretKey masterKey;

    private CryptoService(SecretKey masterKey) {
        this.masterKey = masterKey;
    }

    // --------------------------------------------
    // Initialisation du coffre (premier lancement)
    // --------------------------------------------

    /**
     * Crée un nouveau CryptoService pour un coffre vierge.
     * Génère un salt et une Master Key aléatoires, retourne les éléments à persister.
     *
     * @param password mot de passe maître de l'utilisateur
     * @return InitResult contenant salt, wrappedMasterKey et hash de vérification
     */
    public static InitResult initNewVault(char[] password) throws Exception {
        byte[] salt = generateRandom(SALT_LEN);
        SecretKey kek = deriveKek(password, salt);

        // Master Key aléatoire
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(KEY_BITS, new SecureRandom());
        SecretKey mk = kg.generateKey();

        byte[] wrappedMk = wrapKey(mk, kek);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = computeVerificationHash(password, salt);

        return new InitResult(saltB64, hashB64, wrappedMk, new CryptoService(mk));
    }

    /**
     * Ouvre un coffre existant avec le mot de passe Windows.
     *
     * @param password mot de passe maître
     * @param saltB64 salt stocké en base (Base64)
     * @param wrappedMk Master Key wrappée stockée en base
     * @return CryptoService prêt à l'emploi, ou null si le mot de passe est incorrect
     */
    public static CryptoService unlock(char[] password, String saltB64, byte[] wrappedMk)
            throws Exception {
        byte[] salt = Base64.getDecoder().decode(saltB64);
        SecretKey kek = deriveKek(password, salt);
        try {
            SecretKey mk = unwrapKey(wrappedMk, kek);
            return new CryptoService(mk);
        } catch (AEADBadTagException | InvalidKeyException e) {
            return null; // mot de passe incorrect
        }
    }

    /**
     * Ouvre un coffre existant avec la Recovery Key.
     * La Recovery Key est stockée comme un second wrapping de la Master Key.
     *
     * @param recoveryKey  clé de secours (24 caractères)
     * @param saltB64      salt stocké en base
     * @param wrappedMkRK  Master Key wrappée avec la Recovery Key
     */
    public static CryptoService unlockWithRecoveryKey(
            String recoveryKey, String saltB64, byte[] wrappedMkRK) throws Exception {
        byte[] salt = Base64.getDecoder().decode(saltB64);
        SecretKey kek = deriveKek(recoveryKey.toCharArray(), salt);
        try {
            SecretKey mk = unwrapKey(wrappedMkRK, kek);
            return new CryptoService(mk);
        } catch (AEADBadTagException | InvalidKeyException e) {
            return null;
        }
    }

    // ----------------------------------------------------
    // Chiffrement / Déchiffrement des mots de passe (CRUD)
    // ----------------------------------------------------

    /**
     * Chiffre un mot de passe clair avec la Master Key.
     * Le résultat est IV (12 octets) || ciphertext stocké en BLOB.
     */
    public byte[] encrypt(String plaintext) throws Exception {
        byte[] iv  = generateRandom(GCM_IV_LEN);
        Cipher c   = Cipher.getInstance(AES_GCM);
        c.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ct  = c.doFinal(plaintext.getBytes("UTF-8"));

        byte[] result = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ct, 0, result, iv.length, ct.length);
        return result;
    }

    /**
     * Déchiffre un BLOB (IV || ciphertext) avec la Master Key.
     */
    public String decrypt(byte[] blob) throws Exception {
        byte[] iv = Arrays.copyOfRange(blob, 0, GCM_IV_LEN);
        byte[] ct = Arrays.copyOfRange(blob, GCM_IV_LEN, blob.length);

        Cipher c = Cipher.getInstance(AES_GCM);
        c.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(c.doFinal(ct), "UTF-8");
    }

    // ------------------------------------
    // Vérification du mot de passe (login)
    // ------------------------------------

    public static boolean verifyPassword(char[] password, String saltB64, String storedHashB64)
            throws Exception {
        byte[] salt = Base64.getDecoder().decode(saltB64);
        String computed = computeVerificationHash(password, salt);
        return MessageDigest.isEqual(
            Base64.getDecoder().decode(computed),
            Base64.getDecoder().decode(storedHashB64)
        );
    }

    // ---------------------------
    // Générateur de mots de passe
    // ---------------------------

    private static final String UPPER   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER   = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS  = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    /**
     * Génère un mot de passe aléatoire robuste.
     *
     * @param length longueur souhaitée (8 à 32)
     * @return mot de passe contenant au moins 1 maj, 1 min, 1 chiffre, 1 spécial
     */
    public static String generatePassword(int length) {
        if (length < 8 || length > 32)
            throw new IllegalArgumentException("Longueur doit être entre 8 et 32.");

        String all = UPPER + LOWER + DIGITS + SPECIAL;
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        // Garantit au moins un caractère de chaque classe
        sb.append(UPPER.charAt(rng.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(rng.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(rng.nextInt(DIGITS.length())));
        sb.append(SPECIAL.charAt(rng.nextInt(SPECIAL.length())));

        for (int i = 4; i < length; i++)
            sb.append(all.charAt(rng.nextInt(all.length())));

        // Mélange les caractères pour éviter le pattern prévisible
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp;
        }
        return new String(chars);
    }

    /**
     * Génère une Recovery Key de 24 caractères alphanumériques
     * formatée en 4 groupes de 6 (ex: AB1C2D-EF3G4H-...).
     */
    public static String generateRecoveryKey() {
        String chars = UPPER + DIGITS;
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 24; i++) {
            if (i > 0 && i % 6 == 0) sb.append('-');
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // -------------------
    // Primitives internes
    // -------------------

    private static SecretKey deriveKek(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERS, KEY_BITS);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGO);
            byte[] keyBytes = skf.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] wrapKey(SecretKey toWrap, SecretKey kek) throws Exception {
        byte[] iv = generateRandom(GCM_IV_LEN);
        Cipher c = Cipher.getInstance(AES_GCM);
        c.init(Cipher.WRAP_MODE, kek, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] wrapped = c.wrap(toWrap);

        byte[] result = new byte[iv.length + wrapped.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(wrapped, 0, result, iv.length, wrapped.length);
        return result;
    }

    private static SecretKey unwrapKey(byte[] blob, SecretKey kek) throws Exception {
        byte[] iv = Arrays.copyOfRange(blob, 0, GCM_IV_LEN);
        byte[] wrapped = Arrays.copyOfRange(blob, GCM_IV_LEN, blob.length);

        Cipher c = Cipher.getInstance(AES_GCM);
        c.init(Cipher.UNWRAP_MODE, kek, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return (SecretKey) c.unwrap(wrapped, "AES", Cipher.SECRET_KEY);
    }

    private static String computeVerificationHash(char[] password, byte[] salt) throws Exception {
        SecretKey derived = deriveKek(password, salt);
        return Base64.getEncoder().encodeToString(derived.getEncoded());
    }

    private static byte[] generateRandom(int length) {
        byte[] b = new byte[length];
        new SecureRandom().nextBytes(b);
        return b;
    }

    // -----------------------------
    // DTO résultat d'initialisation
    // -----------------------------

    public static class InitResult {
        public final String saltB64;
        public final String hashB64;
        public final byte[] wrappedMasterKey;
        public final CryptoService crypto;

        InitResult(String saltB64, String hashB64, byte[] wrappedMasterKey, CryptoService crypto) {
            this.saltB64         = saltB64;
            this.hashB64         = hashB64;
            this.wrappedMasterKey = wrappedMasterKey;
            this.crypto           = crypto;
        }
    }
}
