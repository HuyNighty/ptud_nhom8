package com.quanlynhakhoa.security;

@FunctionalInterface
public interface PasswordVerifier {
    boolean matches(String rawPassword, String storedHash);
}
