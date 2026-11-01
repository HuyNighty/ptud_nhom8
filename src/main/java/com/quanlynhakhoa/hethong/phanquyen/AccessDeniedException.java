package com.quanlynhakhoa.hethong.phanquyen;

/** Domain error, not an HTTP or Spring exception. */
public final class AccessDeniedException extends RuntimeException {
    public AccessDeniedException() {
        super("Access denied");
    }
}
