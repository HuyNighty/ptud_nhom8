package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.ChiDinhView;
import vn.n08.nhakhoa.dto.DonThuocView;
import vn.n08.nhakhoa.dto.PhieuKhamChiTiet;
import vn.n08.nhakhoa.dto.PhieuKhamView;
import vn.n08.nhakhoa.model.TrangThaiCLS;
import vn.n08.nhakhoa.model.TrangThaiPhieuKham;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.service.KhamService;
import vn.n08.nhakhoa.service.TiepNhanService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.DetailGrid;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.NhanVienCombo;
import vn.n08.nhakhoa.view.component.Theme;
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.ChiDinhDialog;
import vn.n08.nhakhoa.view.dialog.ChonNhanVienDialog;
import vn.n08.nhakhoa.view.dialog.DonThuocDialog;
import vn.n08.nhakhoa.view.dialog.KeHoachDialog;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.List;

/**
 * UC10 Ghi nhận kết quả khám, UC11 Chỉ định CLS, UC16 Kê đơn.
 * Admin bắt đầu khám phải chọn bác sĩ chịu trách nhiệm; tài khoản bác sĩ chỉ thao tác phiếu của mình.
 */
public class PhieuKhamPage extends Page {
    private final KhamService svc = new KhamService();
    private final HintField tu = Fields.date(Now.today()), den = Fields.date(Now.today());
    private final NhanVienCombo bs = new NhanVienCombo(VaiTro.BS, "Tất cả bác sĩ", false);
    private final JComboBox<TrangThaiPhieuKham> tt = Fields.combo(List.of(TrangThaiPhieuKham.values()), TrangThaiPhieuKham::label, "Tất cả trạng thái");
    private final DataTable<PhieuKhamView> table = new DataTable<PhieuKhamView>()
            .col("Phiếu", 80, v -> v.pk().maPhieuKham()).col("Tiếp nhận", 110, v -> v.pk().thoiGianTiepNhan()).col("Bệnh nhân", 150, PhieuKhamView::tenBenhNhan)
            .col("Bác sĩ", 130, PhieuKhamView::tenBacSi).col("CLS chờ", 60, v -> v.soClsCho() == 0 ? "" : String.valueOf(v.soClsCho()))
            .col("Trạng thái", 110, v -> v.pk().trangThai()).emptyText("Không có phiếu khám trong khoảng đã chọn.");
    private final DetailGrid info = new DetailGrid(1);
    private final JTextArea trieuChung = Fields.area(2), chanDoan = Fields.area(3), ketLuan = Fields.area(3);
    private final DataTable<ChiDinhView> cls = new DataTable<ChiDinhView>().col("Mã", 80, v -> v.cd().maChiDinh()).col("Dịch vụ", 150, ChiDinhView::tenDichVu)
            .col("Yêu cầu", 130, v -> v.cd().yeuCau()).col("Kết quả", 150, v -> v.cd().ketQua()).col("TT", 100, v -> v.cd().trangThai()).emptyText("Chưa có chỉ định.");
    private final DataTable<DonThuocView.Dong> don = new DataTable<DonThuocView.Dong>().col("Thuốc", 170, DonThuocView.Dong::tenThuoc)
            .col("SL", 40, d -> d.ct().soLuong()).col("Liều", 90, d -> d.ct().lieuDung()).col("Cách dùng", 150, d -> d.ct().cachDung())
            .col("KSĐB", 45, d -> d.kiemSoatDacBiet() ? "Có" : "").emptyText("Chưa kê đơn.");
    private final JLabel loiDan = new JLabel(" ");
    private final Btn batDau = Btn.primary("Bắt đầu khám", e -> batDau());
    private final Btn luu = Btn.secondary("Lưu kết quả", e -> luu());
    private final Btn ketThuc = Btn.success("Kết luận", e -> ketLuan());
    private final Btn huy = Btn.danger("Hủy phiếu", e -> huy());
    private final Btn chiDinh = Btn.secondary("Chỉ định", e -> chiDinh());
    private final Btn nhapKq = Btn.secondary("Nhập kết quả", e -> nhapKq());
    private final Btn keDon = Btn.secondary("Kê đơn / sửa đơn", e -> keDon());
    private final Btn keHoach = Btn.ghost("Lập kế hoạch điều trị", e -> keHoach());
    private PhieuKhamChiTiet cur;

    public PhieuKhamPage() {
        super("Phiếu khám", "UC10 Ghi nhận kết quả khám · UC11 Chỉ định cận lâm sàng · UC16 Kê đơn");
        if (Session.user().vaiTro() == VaiTro.BS) bs.selectMa(Session.user().maNhanVien());
        Toolbar f = new Toolbar().label("Từ").with(tu).label("đến").with(den).with(bs, tt, Btn.secondary("Lọc", e -> reload()));
        Card list = Card.titled("Danh sách phiếu khám", null, Layouts.stack(f, table));
        JPanel d = new JPanel();
        d.setOpaque(false);
        d.setLayout(new BoxLayout(d, BoxLayout.Y_AXIS));
        JPanel kq = new JPanel();
        kq.setOpaque(false);
        kq.setLayout(new BoxLayout(kq, BoxLayout.Y_AXIS));
        kq.add(info);
        kq.add(lbl("Triệu chứng"));
        kq.add(Fields.scroll(trieuChung));
        kq.add(lbl("Chẩn đoán *"));
        kq.add(Fields.scroll(chanDoan));
        kq.add(lbl("Kết luận / hướng điều trị *"));
        kq.add(Fields.scroll(ketLuan));
        kq.add(new Toolbar().with(batDau, luu, ketThuc, huy));
        for (java.awt.Component x : kq.getComponents()) ((javax.swing.JComponent) x).setAlignmentX(LEFT_ALIGNMENT);
        d.add(Card.titled("Kết quả khám", null, kq));
        d.add(javax.swing.Box.createVerticalStrut(12));
        cls.setPreferredSize(new Dimension(10, 130));
        d.add(Card.titled("Cận lâm sàng", null, cls, chiDinh, nhapKq));
        d.add(javax.swing.Box.createVerticalStrut(12));
        don.setPreferredSize(new Dimension(10, 130));
        JPanel donBox = Layouts.stack(null, don);
        loiDan.setFont(Theme.SMALL);
        loiDan.setForeground(Theme.MUTED);
        donBox.add(loiDan, java.awt.BorderLayout.SOUTH);
        d.add(Card.titled("Đơn thuốc", null, donBox, keDon));
        d.add(javax.swing.Box.createVerticalStrut(8));
        d.add(new Toolbar().with(keHoach));
        body(Layouts.masterDetail(list, d, 560));
        table.autoSelect();
        table.onSelect(v -> load());
        cls.onSelect(v -> buttons());
        show(null);
    }

    private static JLabel lbl(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.SMALL_B);
        l.setForeground(Theme.INK2);
        l.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 3, 0));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    @Override public void reload() {
        LocalDate a, b;
        try {
            a = Fmt.parseDate(tu.getText(), "tu", "Từ ngày");
            b = Fmt.parseDate(den.getText(), "den", "Đến ngày");
        } catch (RuntimeException e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        String m = bs.ma();
        TrangThaiPhieuKham t = Fields.selected(tt);
        Async.load(table, () -> svc.danhSach(a, b, m, t, null), r -> load());
    }

    private void load() {
        PhieuKhamView v = table.selected();
        if (v == null) { show(null); return; }
        Async.run(this, () -> svc.chiTiet(v.pk().maPhieuKham()), this::show);
    }

    private void show(PhieuKhamChiTiet c) {
        cur = c;
        info.clear();
        if (c == null) {
            info.row("Phiếu", "Chọn một phiếu khám ở danh sách bên trái.");
            trieuChung.setText(""); chanDoan.setText(""); ketLuan.setText("");
            cls.setRows(List.of());
            don.setRows(List.of());
            loiDan.setText(" ");
        } else {
            var p = c.phieu();
            info.row("Phiếu", p.pk().maPhieuKham() + (p.pk().maLichHen() == null ? " · tiếp nhận trực tiếp" : " · từ lịch hẹn " + p.pk().maLichHen()))
                    .row("Bệnh nhân", p.tenBenhNhan() + " · " + Fmt.date(p.ngaySinh()) + " · " + p.soDienThoai())
                    .row("Bác sĩ chịu trách nhiệm", p.tenBacSi()).row("Trạng thái", p.pk().trangThai())
                    .row("Tiền sử", p.tienSuBenh()).row("Dị ứng", p.diUng() == null ? "Không ghi nhận" : "⚠ " + p.diUng());
            if (p.pk().lyDoHuy() != null) info.row("Lý do hủy", p.pk().lyDoHuy());
            trieuChung.setText(nz(p.pk().trieuChung()));
            chanDoan.setText(nz(p.pk().chanDoan()));
            ketLuan.setText(nz(p.pk().ketLuan()));
            cls.setRows(c.chiDinh());
            don.setRows(c.donThuoc() == null ? List.of() : c.donThuoc().dong());
            loiDan.setText(c.donThuoc() == null ? " " : "Đơn " + c.donThuoc().don().maDonThuoc() + " · lời dặn: " + Fmt.nz(c.donThuoc().don().loiDan()));
        }
        info.revalidate();
        buttons();
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private void buttons() {
        TrangThaiPhieuKham t = cur == null ? null : cur.phieu().pk().trangThai();
        boolean dang = t == TrangThaiPhieuKham.DANG_KHAM;
        batDau.setEnabled(t == TrangThaiPhieuKham.CHO_KHAM);
        huy.setEnabled(t == TrangThaiPhieuKham.CHO_KHAM);
        luu.setEnabled(dang);
        ketThuc.setEnabled(dang);
        chiDinh.setEnabled(dang);
        keDon.setEnabled(dang);
        ChiDinhView c = cls.selected();
        nhapKq.setEnabled(dang && c != null && c.cd().trangThai() == TrangThaiCLS.CHO_THUC_HIEN);
        keHoach.setEnabled(Session.can(Quyen.KE_HOACH) && (dang || t == TrangThaiPhieuKham.DA_KET_LUAN));
        for (JTextArea a : new JTextArea[]{trieuChung, chanDoan, ketLuan}) a.setEditable(dang);
    }

    private String ma() { return cur.phieu().pk().maPhieuKham(); }

    private void done(String msg) {
        Toast.show(this, msg);
        reload();
    }

    private void batDau() {
        String m = ma();
        if (Session.user().vaiTro() == VaiTro.BS) {
            Async.run(this, () -> { svc.batDauKham(m, null); return true; }, r -> done("Đã bắt đầu khám " + m + "."));
            return;
        }
        String r = new ChonNhanVienDialog(this, "Bắt đầu khám " + m, "Chọn bác sĩ chịu trách nhiệm chuyên môn. Nhật ký vẫn ghi tài khoản "
                + Session.user().tenDangNhap() + " là người thao tác.", VaiTro.BS, true, "Bắt đầu khám",
                b -> { svc.batDauKham(m, b); return b; }, "Đã bắt đầu khám " + m + ".").preset(cur.phieu().pk().maNhanVien()).open();
        if (r != null) reload();
    }

    private void luu() {
        String m = ma(), a = trieuChung.getText(), b = chanDoan.getText(), c = ketLuan.getText();
        Async.run(this, () -> { svc.luuKetQua(m, a, b, c); return true; }, r -> { Toast.show(this, "Đã lưu kết quả khám."); load(); });
    }

    private void ketLuan() {
        String m = ma();
        if (!chanDoan.getText().equals(nz(cur.phieu().pk().chanDoan())) || !ketLuan.getText().equals(nz(cur.phieu().pk().ketLuan()))) {
            Dialogs.error(this, "Có thay đổi chưa lưu ở chẩn đoán / kết luận. Bấm \"Lưu kết quả\" trước.");
            return;
        }
        if (!Dialogs.confirm(this, "Kết luận lượt khám " + m, "Sau khi kết luận, phiếu không sửa được nữa và lượt khám chuyển sang thu ngân lập hóa đơn.", "Kết luận", false)) return;
        Async.run(this, () -> { svc.ketLuan(m); return true; }, r -> done("Đã kết luận " + m + "."));
    }

    private void huy() {
        String m = ma();
        String ly = Dialogs.reason(this, "Hủy phiếu " + m, "Lý do hủy (bắt buộc).", "Hủy phiếu", true);
        if (ly != null) Async.run(this, () -> { new TiepNhanService().huyPhieu(m, ly); return true; }, r -> done("Đã hủy phiếu."));
    }

    private void chiDinh() {
        if (new ChiDinhDialog(this, ma(), cur.phieu().tenBacSi()).open() != null) load();
    }

    private void nhapKq() {
        ChiDinhView c = cls.selected();
        if (c == null) return;
        String kq = Dialogs.reason(this, "Kết quả " + c.tenDichVu(), "Yêu cầu: " + c.cd().yeuCau() + "\nNhập kết quả cận lâm sàng.", "Lưu kết quả", false);
        if (kq != null) Async.run(this, () -> { svc.nhapKetQuaCls(c.cd().maChiDinh(), kq); return true; }, r -> { Toast.show(this, "Đã nhập kết quả."); load(); });
    }

    private void keDon() {
        var p = cur.phieu();
        if (new DonThuocDialog(this, ma(), p.tenBenhNhan(), p.diUng(), cur.donThuoc()).open() != null) load();
    }

    private void keHoach() {
        var p = cur.phieu();
        String ma = new KeHoachDialog(this, p.pk().maBenhNhan(), p.tenBenhNhan(), p.pk().maPhieuKham()).open();
        if (ma != null && MainFrame.current() != null && MainFrame.current().has("ke-hoach")) MainFrame.current().go("ke-hoach");
    }
}
