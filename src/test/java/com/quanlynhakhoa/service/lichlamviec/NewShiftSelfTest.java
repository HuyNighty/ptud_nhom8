package com.quanlynhakhoa.service.lichlamviec;

import com.quanlynhakhoa.service.lichlamviec.PhanCongService;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.time.*;
import static com.quanlynhakhoa.service.lichlamviec.PhanCongSelfTest.*;

public final class NewShiftSelfTest {
    public static void main(String[] args) throws Exception {
        var db = new ShiftJdbcDouble();
        var manager = session(VaiTroHeThong.CLINIC_MANAGER);
        var service = new PhanCongService(manager, db::open);
        LocalDate day = LocalDate.now();
        LocalTime start = LocalTime.of(8, 0), end = LocalTime.of(12, 0);
        rejects(IllegalArgumentException.class, () -> service.createWithNewShift("NV1", " ", start, end, day, "Khám"));
        rejects(IllegalArgumentException.class, () -> service.createWithNewShift("NV1", "Ca mới", end, start, day, "Khám"));
        rejects(IllegalArgumentException.class, () -> service.createWithNewShift("NV1", "Ca mới", start, start, day, "Khám"));
        rejects(IllegalArgumentException.class, () -> service.createWithNewShift("NV1", "Ca mới", start.plusSeconds(1), end, day, "Khám"));
        check(db.opens == 0, "invalid shift rejected before JDBC");
        String id = service.createWithNewShift("NV1", "Ca mới", start, end, day, "Khám");
        check(db.commits == 1 && db.assignments.size() == 1, "shift and assignment committed");
        check(db.events.stream().anyMatch(e -> e.startsWith("INSERT INTO dbo.CaLamViec")), "new shift inserted");
        check(db.assignments.getFirst().get("maPhanCong").equals(id), "saved assignment id");
        int inserts = (int) db.events.stream().filter(e -> e.startsWith("INSERT INTO dbo.CaLamViec")).count();
        rejects(IllegalArgumentException.class, () -> service.createWithNewShift("NV1", "Trùng giờ", start, end, day, "Khám"));
        check(db.events.stream().filter(e -> e.startsWith("INSERT INTO dbo.CaLamViec")).count() == inserts, "overlap does not create orphan shift");
        var failing = new ShiftJdbcDouble(); failing.failAudit = true;
        var failService = new PhanCongService(manager, failing::open);
        rejects(java.sql.SQLException.class, () -> failService.createWithNewShift("NV1", "Lỗi nhật ký", start, end, day, "Khám"));
        check(failing.commits == 0 && failing.rollbacks == 1 && failing.assignments.isEmpty(), "audit failure rolls back transaction");
        var staff = new PhanCongService(session(VaiTroHeThong.DOCTOR), db::open);
        rejects(com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException.class,
                () -> staff.createWithNewShift("NV1", "Không có quyền", start, end, day, "Khám"));
        System.out.println("PASS: new shift validation, permissions, atomic assignment, overlap and audit rollback.");
    }
}
