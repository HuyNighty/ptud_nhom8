package com.quanlynhakhoa.quantri.lichlamviec;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.quantri.lichlamviec.dao.PhanCongDao;
import com.quanlynhakhoa.quantri.lichlamviec.service.PhanCongService;
import com.quanlynhakhoa.quantri.lichlamviec.view.LichLamViecPanel;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDate;
import javax.swing.*;
import static com.quanlynhakhoa.quantri.lichlamviec.PhanCongSelfTest.*;

/** Opt-in local SQL acceptance; optional write test is always rolled back. */
public final class ShiftAssignmentSqlIT {
    private static <T> T field(Object owner, String name, Class<T> type) throws Exception {
        Field f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return type.cast(f.get(owner));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("--read-only") || args[0].equals("--rollback-check"))
                || !TeamDevConnection.hasLocalConfig()) throw new IllegalArgumentException("Local SQL test mode required");
        var session = session(VaiTroHeThong.CLINIC_MANAGER);
        var service = new PhanCongService(session, TeamDevConnection::open);
        LocalDate day = LocalDate.now();
        var data = service.management(day.minusDays(7), day.plusDays(7), "", null);
        try (Connection c = TeamDevConnection.open(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM dbo.NhanVien WHERE trangThai='DANG_LAM_VIEC'")) {
            r.next(); check(data.employees().size() == r.getInt(1), "empty search includes all active employees");
        }
        check(!data.employees().isEmpty() && !data.shifts().isEmpty(), "test requires employees and shifts");
        var employee = data.employees().getFirst();
        var filtered = service.management(day.minusDays(7), day.plusDays(7), employee.id(), employee.id());
        check(filtered.employees().stream().anyMatch(e -> e.id().equals(employee.id())), "employee search matches id");
        check(filtered.rows().stream().allMatch(p -> p.nhanVienId().equals(employee.id())), "employee schedule filter");
        LichLamViecPanel[] panel = new LichLamViecPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new LichLamViecPanel(session, true, service, false);
            panel[0].refreshData();
        });
        boolean[] ready = {false};
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        while (!ready[0] && System.nanoTime() < deadline) {
            SwingUtilities.invokeAndWait(() -> {
                try { ready[0] = field(panel[0], "create", JButton.class).isEnabled(); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            if (!ready[0]) Thread.sleep(30);
        }
        check(ready[0], "Add assignment available on initial load without employee search");
        if (args[0].equals("--rollback-check")) rollbackCheck();
        session.logout();
        System.out.println("PASS: SQL active employees, shift choices, schedule filtering and initial Add assignment button.");
        System.out.println("Available: " + data.employees().size() + " employees, " + data.shifts().size() + " shifts.");
    }

    private static void rollbackCheck() throws Exception {
        PhanCongDao dao = new PhanCongDao();
        String id = "TSTPCTRY";
        try (Connection c = TeamDevConnection.open()) {
            dao.requireDevDatabase(c);
            c.setAutoCommit(false);
            try {
                try (PreparedStatement s = c.prepareStatement("SELECT COUNT(*) FROM dbo.PhanCong WHERE maPhanCong=?")) {
                    s.setString(1, id);
                    try (ResultSet r = s.executeQuery()) { r.next(); check(r.getInt(1) == 0, "rollback-test id must be unused"); }
                }
                LocalDate day = LocalDate.of(2026, 11, 20);
                dao.lockEmployee(c, "TSTNV01", day, true);
                var shift = new com.quanlynhakhoa.entity.CaLamViec("TSTCATRY", "[KIỂM THỬ] Ca mới hoàn tác", java.time.LocalTime.of(8,0), java.time.LocalTime.of(12,0));
                PhanCongService.checkAssignment(shift, dao.schedule(c, day, day, "TSTNV01"), dao.maximumPerDay(c));
                dao.insertShift(c, shift);
                dao.insert(c, id, "TSTNV01", shift.id(), day, "[KIỂM THỬ] Phân công sẽ hoàn tác");
                dao.audit(c, "TSTTK04", id, "Kiểm thử phân công - hoàn tác");
                var assigned = dao.schedule(c, day, day, "TSTNV01");
                check(assigned.stream().anyMatch(p -> p.id().equals(id)), "insert read back in transaction");
                rejects(IllegalArgumentException.class, () -> PhanCongService.checkAssignment(shift, assigned, dao.maximumPerDay(c)));
                dao.cancel(c, id, "[KIỂM THỬ] Hủy trong transaction hoàn tác");
                check(dao.schedule(c, day, day, "TSTNV01").stream().anyMatch(p -> p.id().equals(id) && p.trangThai().equals("DA_HUY")), "cancel preserves history");
            } finally { c.rollback(); }
        }
        try (Connection c = TeamDevConnection.open(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM dbo.PhanCong WHERE maPhanCong='TSTPCTRY'")) {
            r.next(); check(r.getInt(1) == 0, "test assignment fully rolled back");
        }
        try (Connection c = TeamDevConnection.open(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM dbo.CaLamViec WHERE maCa='TSTCATRY'")) {
            r.next(); check(r.getInt(1) == 0, "new shift fully rolled back");
        }
        System.out.println("PASS: SQL new shift and assignment create/read, overlap rejection, audit and cancel; all writes rolled back.");
    }
}
