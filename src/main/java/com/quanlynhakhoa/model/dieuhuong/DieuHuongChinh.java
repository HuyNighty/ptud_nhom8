package com.quanlynhakhoa.model.dieuhuong;

import com.quanlynhakhoa.enums.Module;
import com.quanlynhakhoa.security.NavigationPolicy;
import com.quanlynhakhoa.security.DesktopSession;
import java.util.List;
import java.util.Objects;

/** Pure-Java state model; can be tested without Swing, SQL, or credentials. */
public final class DieuHuongChinh {
    private final DesktopSession session;
    private final NavigationPolicy policy;
    private Module selected = Module.HOME;

    public DieuHuongChinh(DesktopSession session) {
        this(session, new NavigationPolicy());
    }

    public DieuHuongChinh(DesktopSession session, NavigationPolicy policy) {
        this.session = Objects.requireNonNull(session, "session");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.policy.requireOpen(session, Module.HOME);
    }

    public List<Module> availableModules() { return policy.visibleModules(session); }
    public Module selected() { return selected; }
    public boolean isActive() { return session.isActive(); }
    public void select(Module module) {
        policy.requireOpen(session, module);
        selected = module;
    }
    public void logout() { session.logout(); }
}
