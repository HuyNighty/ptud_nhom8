package com.quanlynhakhoa.modules.identity.exception;

import com.quanlynhakhoa.modules.identity.enums.Permission;

/** Lỗi từ chối quyền; REST adapter sau này ánh xạ thành HTTP 403. */
public final class PermissionDeniedException extends RuntimeException {
    public PermissionDeniedException(Permission permission) {
        super("Permission denied: " + permission);
    }
}
