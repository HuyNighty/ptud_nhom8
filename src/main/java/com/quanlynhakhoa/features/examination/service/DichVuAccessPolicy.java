package com.quanlynhakhoa.features.examination.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public final class DichVuAccessPolicy {

    public static final String SERVICE_READ = "SERVICE_READ";
    public static final String SERVICE_MANAGE = "SERVICE_MANAGE";

    public void requireRead() {
        requireAuthority(SERVICE_READ, "Bạn không có quyền xem danh mục dịch vụ.");
    }

    public void requireManage() {
        requireAuthority(SERVICE_MANAGE, "Bạn không có quyền quản lý danh mục dịch vụ.");
    }

    private void requireAuthority(String authority, String message) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Bạn cần đăng nhập để thực hiện thao tác này.");
        }

        boolean granted = authentication.getAuthorities().stream()
                .anyMatch(item -> authority.equals(item.getAuthority()));

        if (!granted) {
            throw new AccessDeniedException(message);
        }
    }
}
