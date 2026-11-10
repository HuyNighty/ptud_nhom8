package com.quanlynhakhoa.nghiepvu.dieutri;





import com.quanlynhakhoa.hethong.phanquyen.*;
import com.quanlynhakhoa.hethong.xacthuc.AuthDecision;
import com.quanlynhakhoa.quantri.vaitro.entity.Role;
import com.quanlynhakhoa.nghiepvu.dieutri.service.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

/** Every public service operation is checked without database or clinical writes. */
public final class TreatmentPlanFailClosedSelfTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    @FunctionalInterface private interface Call { void run() throws Exception; }
    private static void rejects(Class<? extends Throwable> type, Call call) {
        try { call.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "Unexpected failure: " + failure.getClass().getSimpleName());
            return;
        }
        throw new AssertionError("False success");
    }
    private static DesktopSession session(Role role) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTHETIC", role, false));
    }
    private static List<Call> mutations(TreatmentPlanService service) {
        return List.of(
            () -> service.taoKeHoach("KH01", "HS01", "PK01", LocalDate.of(2026,11,8), "Thử nghiệm"),
            () -> service.themBuoc("KH01", "DV01", "16", LocalDate.of(2026,11,8), ""),
            () -> service.hoanThanhBuoc(1, LocalDate.of(2026,11,8), "Thử nghiệm"),
            () -> service.huyBuoc(1, "Lý do"),
            () -> service.hoanThanhKeHoach("KH01"),
            () -> service.huyKeHoach("KH01", "Lý do"));
    }
    private static final class NoDatabaseDriver implements Driver {
        int attempts;
        @Override public Connection connect(String url, Properties properties) {
            attempts++;
            throw new AssertionError("H2 must not open JDBC");
        }
        @Override public boolean acceptsURL(String url) { return true; }
        @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
        @Override public int getMajorVersion() { return 1; }
        @Override public int getMinorVersion() { return 0; }
        @Override public boolean jdbcCompliant() { return false; }
        @Override public Logger getParentLogger() { return Logger.getLogger("synthetic.no-database"); }
    }
    public static void main(String[] args) throws Exception {
        NoDatabaseDriver driver = new NoDatabaseDriver();
        DriverManager.registerDriver(driver);
        try {
            for (Driver installed : Collections.list(DriverManager.getDrivers()))
                check(installed == driver, "Refusing real SQL drivers");
            rejects(IllegalArgumentException.class, () -> new TreatmentPlanService(null));
            for (Role role : Role.values()) {
                DesktopSession actor = session(role);
                TreatmentPlanService service = new TreatmentPlanService(actor);
                for (Call mutation : mutations(service))
                    rejects(role == Role.DOCTOR ? TreatmentPlanUnavailableException.class : AccessDeniedException.class, mutation);
                Class<? extends Throwable> readFailure = role == Role.DOCTOR || role == Role.SYSTEM_ADMIN
                        ? TreatmentPlanUnavailableException.class : AccessDeniedException.class;
                rejects(readFailure, service::danhSach);
                rejects(readFailure, () -> service.chiTiet("KH01"));

                actor.logout();
                for (Call mutation : mutations(service)) rejects(AccessDeniedException.class, mutation);
                rejects(AccessDeniedException.class, service::danhSach);
                rejects(AccessDeniedException.class, () -> service.chiTiet("KH01"));
            }
            // PR118 sessions carry a verifier that may open JDBC. Disabled treatment
            // operations must terminate locally even when that verifier is bound.
            for (Role role : Role.values()) {
                DesktopSession actor = session(role);
                actor.bindCurrentAccountCheck(ignored -> {
                    throw new AssertionError("Disabled operation invoked JDBC-backed verifier");
                });
                TreatmentPlanService service = new TreatmentPlanService(actor);
                for (Call mutation : mutations(service))
                    rejects(role == Role.DOCTOR ? TreatmentPlanUnavailableException.class : AccessDeniedException.class, mutation);
                Class<? extends Throwable> readFailure = role == Role.DOCTOR || role == Role.SYSTEM_ADMIN
                        ? TreatmentPlanUnavailableException.class : AccessDeniedException.class;
                rejects(readFailure, service::danhSach);
                rejects(readFailure, () -> service.chiTiet("KH01"));
            }
            // No argument validation can turn an unauthorized request into apparent success.
            TreatmentPlanService admin = new TreatmentPlanService(session(Role.SYSTEM_ADMIN));
            rejects(AccessDeniedException.class, () -> admin.taoKeHoach(null,null,null,null,null));
            rejects(AccessDeniedException.class, () -> admin.themBuoc(null,null,null,null,null));
            rejects(AccessDeniedException.class, () -> admin.hoanThanhBuoc(0,null,null));
            rejects(AccessDeniedException.class, () -> admin.huyBuoc(0,null));
            rejects(AccessDeniedException.class, () -> admin.hoanThanhKeHoach(null));
            rejects(AccessDeniedException.class, () -> admin.huyKeHoach(null,null));
            check(driver.attempts == 0, "Zero JDBC attempts across denials and unavailable operations");
            check(new TreatmentPlanUnavailableException().getCause() == null, "No SQL cause in unavailable error");
            System.out.println("PASS: H2 fail-closed boundary (" + checks + " checks; no JDBC/database).");
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }
}
