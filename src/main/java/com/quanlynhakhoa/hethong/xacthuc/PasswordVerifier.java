package com.quanlynhakhoa.hethong.xacthuc;

@FunctionalInterface
public interface PasswordVerifier {
    boolean matches(String rawPassword, String storedHash);
}
