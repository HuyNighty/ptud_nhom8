package com.quanlynhakhoa.auth;

import org.mindrot.jbcrypt.BCrypt;

/** Verify existing BCrypt $2a hashes, never create or print new credentials here. */
public final class BCryptPasswordVerifier implements PasswordVerifier {
    @Override
    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || !storedHash.startsWith("$2a$")) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, storedHash);
        } catch (IllegalArgumentException malformedHash) {
            return false;
        }
    }
}
