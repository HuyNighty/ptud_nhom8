package com.quanlynhakhoa.view;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.navigation.Module;
import com.quanlynhakhoa.navigation.NavigationPolicy;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * R4-A3: synthetic session lifecycle and fail-closed regression checks.
 * Does not create JFrame, connect to SQL, or access any real account.
 */
public final class R4SessionLifecycleSelfTest {
    private static int checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError("R4-A3 check " + checks + " failed: " + label);
    }
    private static void denied(Runnable task, String label) {
        boolean rejected = false;
        try { task.run(); } catch (AccessDeniedException expected) { rejected = true; }
        check(rejected, label);
    }
    private static DesktopSession make(Role role, String id) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, id, role, false));
    }
    private static void rejectSession(AuthDecision decision, String label) {
        denied(() -> DesktopSession.fromSuccessfulLogin(decision), label);
    }

    public static void main(String[] args) throws Exception {
        final NavigationPolicy navigation = new NavigationPolicy();
        final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
        check(!navigation.canOpen(null, Module.HOME), "no principal cannot open HOME");
        check(!navigation.canOpen(null, Module.PATIENTS), "no principal cannot open PATIENTS");
        check(navigation.visibleModules(null).isEmpty(), "no principal has no visible modules");
        for (Permission permission : Permission.values()) {
            check(!authorization.can(null, permission), "null principal deny " + permission);
            denied(() -> authorization.require(null, permission), "null principal require " + permission);
        }
        rejectSession(null, "null auth result");
        rejectSession(AuthDecision.rejected(), "rejected auth");
        rejectSession(new AuthDecision(true, "SYNTH", Role.SYSTEM_ADMIN, true), "forced password reset");
        rejectSession(new AuthDecision(true, "SYNTH", null, false), "missing role");
        rejectSession(new AuthDecision(true, " ", Role.SYSTEM_ADMIN, false), "blank account id");

        for (Role role : Role.values()) {
            DesktopSession session = make(role, "SYNTH-" + role);
            MainNavigationModel model = new MainNavigationModel(session);
            Instant began = session.startedAt();
            check(session.isActive(), "session starts active " + role);
            check(model.isActive(), "model starts active " + role);
            check(session.accountId().equals("SYNTH-" + role), "id retained " + role);
            check(session.role() == role, "role retained " + role);
            check(began != null, "creation timestamp " + role);
            check(navigation.canOpen(session, Module.HOME), "home visible " + role);
            check(model.availableModules().contains(Module.HOME), "home in model " + role);
            List<Module> before = model.availableModules();
            check(before.equals(navigation.visibleModules(session)), "model matches policy " + role);
            for (Module module : Module.values()) {
                if (before.contains(module)) {
                    model.select(module);
                    check(model.selected() == module, "select permitted " + role + ":" + module);
                } else {
                    denied(() -> model.select(module), "forbidden pre-logout " + role + ":" + module);
                }
            }
            model.logout(); // Same operation used by the R4-A2 window.
            check(!session.isActive(), "session inactive after logout " + role);
            check(!model.isActive(), "model inactive after logout " + role);
            session.logout();
            model.logout();
            check(!session.isActive(), "logout idempotent " + role);
            check(session.startedAt().equals(began), "creation time stable " + role);
            check(model.availableModules().isEmpty(), "no navigation after logout " + role);
            check(navigation.visibleModules(session).isEmpty(), "policy hides all after logout " + role);
            for (Module module : Module.values()) {
                check(!navigation.canOpen(session, module), "cannot open after logout " + role + ":" + module);
                denied(() -> navigation.requireOpen(session, module), "navigation require denied after logout " + role + ":" + module);
                denied(() -> model.select(module), "model cannot select after logout " + role + ":" + module);
            }
            for (Permission permission : Permission.values()) {
                check(!authorization.can(session, permission), "cannot authorize after logout " + role + ":" + permission);
                denied(() -> authorization.require(session, permission), "auth require denied after logout " + role + ":" + permission);
            }
        }

        DesktopSession old = make(Role.SYSTEM_ADMIN, "SYNTH-RELOGIN");
        MainNavigationModel oldModel = new MainNavigationModel(old);
        old.logout();
        DesktopSession fresh = make(Role.SYSTEM_ADMIN, "SYNTH-RELOGIN");
        MainNavigationModel freshModel = new MainNavigationModel(fresh);
        check(old != fresh, "relogin makes distinct instance");
        check(!old.isActive(), "old remains inactive");
        check(fresh.isActive(), "fresh is active");
        denied(() -> oldModel.select(Module.HOME), "old model cannot be reused");
        freshModel.select(Module.HOME);
        check(freshModel.isActive(), "new model works");
        check(oldModel.availableModules().isEmpty(), "relogin does not revive old module visibility");
        fresh.logout();
        denied(() -> freshModel.select(Module.HOME), "new model also invalidates after logout");

        DesktopSession concurrent = make(Role.SYSTEM_ADMIN, "SYNTH-RACE");
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                futures.add(pool.submit(() -> {
                    try { start.await(); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new RuntimeException(ex); }
                    concurrent.logout();
                }));
            }
            start.countDown();
            for (Future<?> future : futures) future.get();
            check(!concurrent.isActive(), "concurrent logout finishes inactive");
            check(navigation.visibleModules(concurrent).isEmpty(), "concurrent logout denies navigation");
        } finally {
            pool.shutdownNow();
        }

        // Check MainFrame's PUBLIC constructor contract without creating a Swing window.
        boolean acceptsSession = false;
        boolean allowsNoArg = false;
        for (Constructor<?> c : MainFrame.class.getConstructors()) {
            Class<?>[] types = c.getParameterTypes();
            if (types.length == 0) allowsNoArg = true;
            if (types.length == 2 && types[0] == DesktopSession.class && types[1] == Runnable.class)
                acceptsSession = true;
        }
        check(acceptsSession, "main frame requires session and logout callback");
        check(!allowsNoArg, "main frame has no public unauthenticated constructor");
        System.out.println("PASS: R4-A3 LOCAL SESSION LIFECYCLE TESTS (" + checks + " checks)");
        System.out.println("NO SWING WINDOW, NO JDBC, NO SQL WRITES, NO REAL ACCOUNTS");
    }
}