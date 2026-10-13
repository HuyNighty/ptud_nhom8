package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.PhanCongView;
import vn.n08.nhakhoa.model.CaLamViec;
import vn.n08.nhakhoa.model.TrangThaiPhanCong;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.NhatKyService;
import vn.n08.nhakhoa.service.PhanCongService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.NhanVienCombo;
import vn.n08.nhakhoa.view.component.Theme;
import vn.n08.nhakhoa.view.component.Timeline;
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.ChonNhanVienDialog;
import vn.n08.nhakhoa.view.dialog.PhanCongDialog;
import vn.n08.nhakhoa.view.dialog.XemDialog;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * UC24 Quản lý lịch làm việc (lịch tuần suy ra từ phân công, thêm/bỏ người, sao chép tuần trước),
 * UC18 Nhiệm vụ được phân công, UC17/UC20 Xem lịch làm việc theo nhân viên.
 */
public class PhanCongPage extends Page {
    private final PhanCongService svc = new PhanCongService();
    private final boolean ql = Session.can(Quyen.PHAN_CONG);
    private LocalDate thuHai = Now.today().with(DayOfWeek.MONDAY);
    private List<CaLamViec> ca = List.of();
    private List<PhanCongView> tuan = List.of();
    private final JLabel tuanLbl = new JLabel();
    private final GridModel grid = new GridModel();
    private final JTable gridTable = new JTable(grid);
    // nhiệm vụ theo ngày
    private final HintField ngay = Fields.date(Now.today());
    private final DataTable<PhanCongView> ds = new DataTable<PhanCongView>()
            .col("Mã", 80, v -> v.pc().maPhanCong()).col("Ca", 120, v -> v.tenCa() + " " + Fmt.time(v.gioBatDau()) + "–" + Fmt.time(v.gioKetThuc()))
            .col("Nhân viên", 160, PhanCongView::hoTen).col("Chức danh", 100, PhanCongView::chucDanh).col("Nhiệm vụ", 200, v -> v.pc().nhiemVu())
            .col("Trạng thái", 110, v -> v.pc().trangThai()).col("Ghi chú", 200, v -> v.pc().lyDoHuy() != null ? v.pc().lyDoHuy()
                    : v.pc().maPhanCongGoc() != null ? "Thay cho " + v.pc().maPhanCongGoc() : null).emptyText("Không có phân công trong ngày.");
    private final Btn batDau = Btn.primary("Bắt đầu", e -> act(true));
    private final Btn hoanTat = Btn.success("Hoàn tất", e -> act(false));
    private final Btn suaNv = Btn.secondary("Sửa nhiệm vụ", e -> suaNhiemVu());
    private final Btn thay = Btn.secondary("Thay người", e -> thayNguoi());
    private final Btn huy = Btn.danger("Hủy (bỏ khỏi ca)", e -> huy());
    private final Btn lichSu = Btn.ghost("Lịch sử", e -> lichSu());
    // lịch theo nhân viên
    private final NhanVienCombo nv = new NhanVienCombo(null, null, false);
    private final HintField tu = Fields.date(Now.today()), den = Fields.date(Now.today().plusDays(13));
    private final DataTable<PhanCongView> cuaNv = new DataTable<PhanCongView>()
            .col("Ngày", 90, v -> v.pc().ngayLam()).col("Thứ", 60, v -> Fmt.weekdayShort(v.pc().ngayLam()))
            .col("Ca", 140, v -> v.tenCa() + " " + Fmt.time(v.gioBatDau()) + "–" + Fmt.time(v.gioKetThuc()))
            .col("Nhiệm vụ", 220, v -> v.pc().nhiemVu()).col("Trạng thái", 110, v -> v.pc().trangThai()).emptyText("Không có lịch trong khoảng này.");

    public PhanCongPage() {
        super("Phân công & lịch làm việc", "UC24 Quản lý lịch làm việc · UC18 Nhiệm vụ được phân công · UC17/UC20 Xem lịch làm việc");
        if (ql) {
            header.action(Btn.secondary("Cập nhật ca", e -> capNhatCa()));
            header.action(Btn.secondary("Sao chép tuần trước", e -> saoChep()));
            header.action(Btn.primary("Thêm phân công", e -> them()));
        }
        JTabbedPane tabs = new JTabbedPane();
        // tab lịch tuần
        gridTable.setRowHeight(118);
        gridTable.setCellSelectionEnabled(true);
        gridTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gridTable.setDefaultRenderer(Object.class, new GridRenderer());
        gridTable.getTableHeader().setFont(Theme.SMALL_B);
        gridTable.getTableHeader().setReorderingAllowed(false);
        gridTable.setGridColor(Theme.LINE);
        gridTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int c = gridTable.getSelectedColumn();
                if (c >= 1) { ngay.setText(Fmt.date(thuHai.plusDays(c - 1))); if (e.getClickCount() == 2) { tabs.setSelectedIndex(1); loadNgay(); } }
            }
        });
        javax.swing.JScrollPane gsp = new javax.swing.JScrollPane(gridTable);
        gsp.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        gsp.getViewport().setBackground(Theme.SURFACE);
        tuanLbl.setFont(Theme.H2);
        Toolbar nav = new Toolbar().with(Btn.secondary("‹ Tuần trước", e -> { thuHai = thuHai.minusWeeks(1); loadTuan(); }), tuanLbl,
                Btn.secondary("Tuần sau ›", e -> { thuHai = thuHai.plusWeeks(1); loadTuan(); }), Btn.ghost("Tuần này", e -> { thuHai = Now.today().with(DayOfWeek.MONDAY); loadTuan(); }));
        tabs.addTab("Lịch tuần", Layouts.pad(Card.titled("Lịch tuần (suy ra từ phân công còn hiệu lực)", "Ô đỏ: thiếu người so với định mức ca. Nhấp đúp một ngày để xem nhiệm vụ.",
                Layouts.stack(nav, gsp)), 10, 0, 0, 0));
        // tab nhiệm vụ theo ngày
        ngay.addActionListener(e -> loadNgay());
        Toolbar t2 = new Toolbar().label("Ngày").with(ngay, Btn.secondary("Xem", e -> loadNgay()), batDau, hoanTat);
        if (ql) t2.with(suaNv, thay, huy);
        t2.add(lichSu);
        tabs.addTab("Nhiệm vụ theo ngày", Layouts.pad(Card.titled("Nhiệm vụ", "Nhân viên tự bắt đầu / hoàn tất nhiệm vụ của mình trong ngày làm; bỏ người = hủy có lý do",
                Layouts.stack(t2, ds)), 10, 0, 0, 0));
        ds.onSelect(v -> buttons());
        // tab theo nhân viên
        nv.selectMa(Session.user().maNhanVien());
        if (!ql) nv.setEnabled(false);
        Toolbar t3 = new Toolbar().label("Nhân viên").with(nv).label("Từ").with(tu).label("đến").with(den).with(Btn.secondary("Xem", e -> loadNv()));
        tabs.addTab("Lịch theo nhân viên", Layouts.pad(Card.titled("Lịch làm việc cá nhân", null, Layouts.stack(t3, cuaNv)), 10, 0, 0, 0));
        body(tabs);
        buttons();
    }

    @Override public void reload() {
        Async.run(this, svc::danhSachCa, r -> { ca = r; loadTuan(); });
        loadNgay();
        loadNv();
    }

    private void loadTuan() {
        tuanLbl.setText(" " + Fmt.date(thuHai) + " – " + Fmt.date(thuHai.plusDays(6)) + " ");
        Async.run(this, () -> svc.lich(thuHai, thuHai.plusDays(6), null), r -> {
            tuan = r;
            grid.fireTableStructureChanged();
            gridTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        });
    }

    private void loadNgay() {
        LocalDate d;
        try {
            d = Fmt.parseDate(ngay.getText(), "ngay", "Ngày");
        } catch (RuntimeException e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        Async.load(ds, () -> svc.lich(d, d, null), r -> buttons());
    }

    private void loadNv() {
        String m = nv.ma() == null ? Session.user().maNhanVien() : nv.ma();
        LocalDate a, b;
        try {
            a = Fmt.parseDate(tu.getText(), "tu", "Từ ngày");
            b = Fmt.parseDate(den.getText(), "den", "Đến ngày");
        } catch (RuntimeException e) {
            return;
        }
        Async.load(cuaNv, () -> svc.lich(a, b, m));
    }

    private void buttons() {
        PhanCongView v = ds.selected();
        boolean mine = v != null && (ql || v.pc().maNhanVien().equals(Session.user().maNhanVien()));
        batDau.setEnabled(mine && v.pc().trangThai() == TrangThaiPhanCong.CHO_HIEU_LUC && v.pc().ngayLam().equals(Now.today()));
        hoanTat.setEnabled(mine && v.pc().trangThai() == TrangThaiPhanCong.DANG_THUC_HIEN);
        suaNv.setEnabled(v != null && v.pc().conHieuLuc());
        thay.setEnabled(v != null && v.pc().trangThai() == TrangThaiPhanCong.CHO_HIEU_LUC && !v.pc().ngayLam().isBefore(Now.today()));
        huy.setEnabled(v != null && v.pc().conHieuLuc());
        lichSu.setEnabled(v != null);
    }

    private void after(String m) {
        Toast.show(this, m);
        reload();
    }

    private void them() {
        int c = gridTable.getSelectedColumn(), r = gridTable.getSelectedRow();
        LocalDate d = c >= 1 ? thuHai.plusDays(c - 1) : Now.today();
        String maCa = r >= 0 && r < ca.size() ? ca.get(r).maCa() : null;
        if (new PhanCongDialog(this, d.isBefore(Now.today()) ? Now.today() : d, maCa).open() != null) reload();
    }

    private void capNhatCa() {
        int r = gridTable.getSelectedRow();
        if (r < 0 || r >= ca.size()) {
            Dialogs.info(this, "Chọn một ô thuộc hàng ca cần cập nhật trên lịch tuần.");
            return;
        }
        if (new vn.n08.nhakhoa.view.dialog.CaLamViecDialog(this, ca.get(r)).open() != null) reload();
    }

    private void saoChep() {
        if (!Dialogs.confirm(this, "Sao chép tuần trước", "Sao chép các phân công còn hiệu lực của tuần " + Fmt.date(thuHai.minusWeeks(1))
                + " sang tuần " + Fmt.date(thuHai) + ". Ngày đã qua và chỗ trùng sẽ bỏ qua.", "Sao chép", false)) return;
        Async.run(this, () -> svc.saoChepTuanTruoc(thuHai), r -> after("Đã tạo " + r[0] + " phân công, bỏ qua " + r[1] + "."));
    }

    private void act(boolean start) {
        PhanCongView v = ds.selected();
        Async.run(this, () -> { if (start) svc.batDau(v.pc().maPhanCong()); else svc.hoanTat(v.pc().maPhanCong()); return true; },
                r -> after(start ? "Đã bắt đầu nhiệm vụ." : "Đã hoàn tất nhiệm vụ."));
    }

    private void suaNhiemVu() {
        PhanCongView v = ds.selected();
        String s = Dialogs.reason(this, "Sửa nhiệm vụ " + v.pc().maPhanCong(), "Hiện tại: " + v.pc().nhiemVu() + "\nNhập nhiệm vụ mới.", "Lưu", false);
        if (s != null) Async.run(this, () -> { svc.suaNhiemVu(v.pc().maPhanCong(), s); return true; }, r -> after("Đã sửa nhiệm vụ."));
    }

    private void thayNguoi() {
        PhanCongView v = ds.selected();
        String ly = Dialogs.reason(this, "Thay người — " + v.hoTen(), "Lý do thay người (bản cũ được hủy với lý do này, bản mới trỏ về bản cũ).", "Tiếp tục", false);
        if (ly == null) return;
        String r = new ChonNhanVienDialog(this, "Chọn người thay " + v.hoTen(), v.tenCa() + " · " + Fmt.date(v.pc().ngayLam()) + " · cùng chức danh " + v.chucDanh().label(),
                v.chucDanh(), false, "Thay người", m -> svc.thayNguoi(v.pc().maPhanCong(), m, ly), "Đã thay người.").open();
        if (r != null) reload();
    }

    private void huy() {
        PhanCongView v = ds.selected();
        String ly = Dialogs.reason(this, "Bỏ " + v.hoTen() + " khỏi ca", "Phân công được hủy (giữ lịch sử). Nhập lý do.", "Hủy phân công", true);
        if (ly != null) Async.run(this, () -> { svc.huy(v.pc().maPhanCong(), ly); return true; }, r -> after("Đã hủy phân công."));
    }

    private void lichSu() {
        PhanCongView v = ds.selected();
        Async.run(this, () -> new NhatKyService().lichSu(v.pc().maPhanCong()), log -> {
            Timeline tl = new Timeline();
            tl.set(log);
            XemDialog.show(this, "Lịch sử phân công " + v.pc().maPhanCong(), new javax.swing.JScrollPane(tl), 560, 420);
        });
    }

    // ---------- lưới tuần ----------
    private class GridModel extends AbstractTableModel {
        @Override public int getRowCount() { return ca.size(); }
        @Override public int getColumnCount() { return 8; }
        @Override public String getColumnName(int c) { return c == 0 ? "Ca" : Fmt.weekdayShort(thuHai.plusDays(c - 1)) + " " + thuHai.plusDays(c - 1).getDayOfMonth() + "/" + thuHai.plusDays(c - 1).getMonthValue(); }
        @Override public Object getValueAt(int r, int c) {
            CaLamViec k = ca.get(r);
            if (c == 0) return "<html><b>" + k.tenCa() + "</b><br><span style='color:#64748B'>" + Fmt.time(k.gioBatDau()) + "–" + Fmt.time(k.gioKetThuc())
                    + "<br>BS " + k.soBacSi() + " · YT " + k.soYTa() + " · LT " + k.soLeTan() + "</span></html>";
            LocalDate d = thuHai.plusDays(c - 1);
            List<PhanCongView> cell = new ArrayList<>();
            for (PhanCongView p : tuan) if (p.pc().ngayLam().equals(d) && p.pc().maCa().equals(k.maCa()) && p.pc().trangThai() != TrangThaiPhanCong.DA_HUY) cell.add(p);
            StringBuilder sb = new StringBuilder("<html>");
            List<String> thieu = new ArrayList<>();
            for (VaiTro v : List.of(VaiTro.BS, VaiTro.YT, VaiTro.LT, VaiTro.QL, VaiTro.KHO)) {
                List<PhanCongView> x = cell.stream().filter(p -> p.chucDanh() == v).toList();
                if (!x.isEmpty()) {
                    sb.append("<span style='color:#64748B'>").append(v.name()).append(":</span> ");
                    sb.append(String.join(", ", x.stream().map(p -> shortName(p.hoTen())).toList())).append("<br>");
                }
                if (x.size() < k.dinhMuc(v)) thieu.add(v.name() + "−" + (k.dinhMuc(v) - x.size()));
            }
            if (!thieu.isEmpty()) sb.append("<span style='color:#DC2626'>Thiếu: ").append(String.join(", ", thieu)).append("</span>");
            return new Cell(sb.append("</html>").toString(), !thieu.isEmpty(), d.equals(Now.today()));
        }
    }

    private record Cell(String html, boolean thieu, boolean homNay) {}

    private static String shortName(String s) {
        String[] p = s.replace("BS. ", "").split(" ");
        return (s.startsWith("BS. ") ? "BS " : "") + p[p.length - 1];
    }

    private static class GridRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
            Cell cell = v instanceof Cell x ? x : null;
            super.getTableCellRendererComponent(t, cell == null ? v : cell.html(), sel, false, r, c);
            setVerticalAlignment(SwingConstants.TOP);
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 6));
            setFont(Theme.SMALL);
            setBackground(sel ? Theme.PRI_SOFT : cell != null && cell.thieu() ? Theme.RED_S : cell != null && cell.homNay() ? Theme.GREEN_S : c == 0 ? Theme.HEAD : Theme.SURFACE);
            setForeground(Theme.INK);
            return this;
        }
    }

}
