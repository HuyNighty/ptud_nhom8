package com.quanlynhakhoa.hethong.dieuhuong;

import com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Navigation projection. Access checks in business services must be independent. */
public final class NavigationPolicy {
    private final AuthorizationService authorization;

    public NavigationPolicy(AuthorizationService authorization) {
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public NavigationPolicy() {
        this(new AuthorizationService(new RolePermissionPolicy()));
    }

    public boolean canOpen(DesktopSession session, Module module) {
        if (session == null || !session.isActive() || module == null) return false;
        return module.openPermission() == null
                || authorization.can(session, module.openPermission());
    }

    public void requireOpen(DesktopSession session, Module module) {
        if (!canOpen(session, module)) throw new AccessDeniedException();
    }

    public List<Module> visibleModules(DesktopSession session) {
        List<Module> visible = new ArrayList<>();
        for (Module module : Module.values()) {
            if (canOpen(session, module)) visible.add(module);
        }
        return List.copyOf(visible);
    }
}
