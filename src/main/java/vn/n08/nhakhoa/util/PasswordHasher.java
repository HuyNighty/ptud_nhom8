package vn.n08.nhakhoa.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Băm mật khẩu PBKDF2-HMAC-SHA256 có salt. Định dạng lưu: pbkdf2$vòng_lặp$salt_base64$hash_base64. */
public final class PasswordHasher {
    private static final int ITERATIONS = 65536;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    public static String hash(String raw) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return format(ITERATIONS, salt, derive(raw, salt, ITERATIONS));
    }

    public static boolean verify(String raw, String stored) {
        if (raw == null || stored == null) return false;
        String[] p = stored.split("\\$");
        if (p.length != 4 || !"pbkdf2".equals(p[0])) return false;
        int it = Integer.parseInt(p[1]);
        byte[] salt = Base64.getDecoder().decode(p[2]);
        byte[] expected = Base64.getDecoder().decode(p[3]);
        return MessageDigest.isEqual(expected, derive(raw, salt, it));
    }

    private static byte[] derive(String raw, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Không băm được mật khẩu", e);
        }
    }

    private static String format(int it, byte[] salt, byte[] hash) {
        return "pbkdf2$" + it + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(hash);
    }

    /** Quy tắc mật khẩu mới: tối thiểu 8 ký tự, có cả chữ và số. */
    public static void checkStrength(String raw) {
        if (raw == null || raw.length() < 8) throw new BusinessException("matKhau", "Mật khẩu tối thiểu 8 ký tự.");
        if (!raw.matches(".*[A-Za-z].*") || !raw.matches(".*\\d.*")) {
            throw new BusinessException("matKhau", "Mật khẩu phải có cả chữ và số.");
        }
    }
}
