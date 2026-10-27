package com.quanlynhakhoa.auth;

@FunctionalInterface
public interface PasswordVerifier {
    boolean matches(String rawPassword, String storedHash);
}
