import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

public class PasswordHasher {

    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";
    public static final int ITERATION_INDEX = 0;
    public static final int SALT_INDEX = 1;
    public static final int PBKDF2_INDEX = 2;
    public static final String PRNG_ALGORITHM = "SHA1PRNG";
    public static final int SALT_BYTES = 24;
    public static final int HASH_BYTES = 64;
    public static final int PBKDF2_ITERATIONS = 1000;

    public static OperationResult<String> createHash(String password) {
        OperationResult<String> funcResult = null;
        try {
            funcResult = OperationResult.getSuccess(createHash(password.toCharArray()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            funcResult = OperationResult.getExceptionResult(e);
        }
        return funcResult;
    }

    public static OperationResult<Boolean> isValidPassword(String password, String hash) {
        OperationResult<Boolean> funcResult = null;
        try {
            funcResult = OperationResult.getSuccess(isValidPassword(password.toCharArray(), hash));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            funcResult = OperationResult.getExceptionResult(e);
        }
        return funcResult;
    }

    private static String createHash(char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = getSalt();
        byte[] hash = pbkdf2(password, salt, PBKDF2_ITERATIONS, HASH_BYTES);

        return PBKDF2_ITERATIONS + ":" + toHex(salt) + ":" + toHex(hash);
    }

    private static byte[] getSalt() throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstance(PRNG_ALGORITHM);
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);

        return salt;
    }

    private static boolean isValidPassword(char[] password, String storedAuthentication) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String[] params = storedAuthentication.split(":");
        int iterations = Integer.parseInt(params[ITERATION_INDEX]);
        byte[] salt = fromHex(params[SALT_INDEX]);
        byte[] hash = fromHex(params[PBKDF2_INDEX]);
        byte[] testHash = pbkdf2(password, salt, iterations, hash.length);

        return slowEquals(hash, testHash);
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }

        return diff == 0;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);

        return skf.generateSecret(spec).getEncoded();
    }

    private static byte[] fromHex(String hex) {
        byte[] binary = new byte[hex.length() / 2];
        for (int i = 0; i < binary.length; i++) {
            binary[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }

        return binary;
    }

    private static String toHex(byte[] array) {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if (paddingLength > 0) {
            return String.format("%0" + paddingLength + "d", 0) + hex;
        } else {
            return hex;
        }
    }

}