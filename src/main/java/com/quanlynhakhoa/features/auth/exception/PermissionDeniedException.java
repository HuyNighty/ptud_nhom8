package com.quanlynhakhoa.features.auth.exception;

import com.quanlynhakhoa.features.auth.enums.Permission;

/** Lỗi từ chối quyền; REST adapter sau này ánh xạ thành HTTP 403. */
public final class PermissionDeniedException extends RuntimeException {
    public PermissionDeniedException(Permission permission) {
        super("Permission denied: " + permission);
    }
}
