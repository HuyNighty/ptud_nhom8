package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.ChiTietHoaDonView;
import vn.n08.nhakhoa.dto.CongNoView;
import vn.n08.nhakhoa.dto.HoaDonChiTiet;
import vn.n08.nhakhoa.dto.HoaDonView;
import vn.n08.nhakhoa.dto.PhieuKhamView;
import vn.n08.nhakhoa.dto.ThanhToanView;
import vn.n08.nhakhoa.dto.YeuCauHuyView;
import vn.n08.nhakhoa.model.TrangThaiHoaDon;
import vn.n08.nhakhoa.model.TrangThaiThanhToan;
import vn.n08.nhakhoa.service.HoaDonService;
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
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.HoaDonDialog;
import vn.n08.nhakhoa.view.dialog.ThanhToanDialog;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** UC04 Quản lý thanh toán (thu nhiều lần, mỗi lần một phương thức), UC05 Theo dõi công nợ, yêu cầu hủy hóa đơn. */
public class HoaDonPage extends Page {
    private final HoaDonService svc = new HoaDonService();
    private final HintField tu = Fields.date(Now.today().minusDays(30)), den = Fields.date(Now.today());
    private final JComboBox<TrangThaiHoaDon> tt = Fields.combo(List.of(TrangThaiHoaDon.values()), TrangThaiHoaDon::label, "Tất cả trạng thái");
    private final HintField q = Fields.text("Tên, SĐT, mã BN / hóa đơn");
    private final DataTable<HoaDonView> table = new DataTable<HoaDonView>()
            .col("Mã HĐ", 80, v -> v.hd().maHoaDon()).col("Ngày lập", 110, v -> v.hd().ngayLap()).col("Bệnh nhân", 150, HoaDonView::tenBenhNhan)
            .col("Tổng tiền", 100, HoaDonView::tongTien).col("Đã thu", 100, HoaDonView::daThanhToan).col("Còn nợ", 100, v -> v.hd().trangThai() == TrangThaiHoaDon.DA_HUY ? null : v.conNo())
            .col("Trạng thái", 165, v -> v.hd().trangThai()).col("Chờ hủy", 60, v -> v.coYeuCauHuyCho() ? "Có" : "").emptyText("Không có hóa đơn.");
    private final DetailGrid info = new DetailGrid(2);
    private final DataTable<ChiTietHoaDonView> lines = new DataTable<ChiTietHoaDonView>().col("Loại", 65, ChiTietHoaDonView::loaiMuc)
            .col("Mục", 180, ChiTietHoaDonView::tenMuc).col("SL", 35, v -> v.ct().soLuong()).col("Đơn giá", 95, v -> v.ct().donGiaApDung())
            .col("Thành tiền", 100, v -> v.ct().thanhTien());
    private final DataTable<ThanhToanView> pays = new DataTable<ThanhToanView>().col("Mã", 75, v -> v.tt().maThanhToan()).col("Thời gian", 105, v -> v.tt().thoiGian())
            .col("Số tiền", 95, v -> v.tt().soTien()).col("Phương thức", 120, v -> v.tt().phuongThuc()).col("Mã GD / thối", 110,
                    v -> v.tt().maGiaoDich() != null ? v.tt().maGiaoDich() : "thối " + Fmt.money(v.tt().tienThoi()))
            .col("Thu ngân", 110, ThanhToanView::tenThuNgan).col("TT", 90, v -> v.tt().trangThai())
            .col("Hủy bởi / lý do", 150, v -> v.tkHuy() == null ? null : v.tkHuy() + ": " + v.tt().lyDoHuy()).emptyText("Chưa thu lần nào.");
    private final DataTable<YeuCauHuyView> ycs = new DataTable<YeuCauHuyView>().col("Mã", 75, v -> v.yc().maYeuCau()).col("Người tạo", 90, YeuCauHuyView::tkTao)
            .col("Lý do", 170, v -> v.yc().lyDo()).col("TT", 90, v -> v.yc().trangThai()).col("Người duyệt / ghi chú", 170,
                    v -> v.tkDuyet() == null ? null : v.tkDuyet() + ": " + v.yc().lyDoDuyet()).emptyText("Không có yêu cầu hủy.");
    private final Btn thu = Btn.primary("Thu tiền", e -> thu());
    private final Btn huyGd = Btn.danger("Hủy lần thu", e -> huyGd());
    private final Btn yeuCau = Btn.secondary("Gửi yêu cầu hủy HĐ", e -> yeuCau());
    private final DataTable<PhieuKhamView> cho = new DataTable<PhieuKhamView>().col("Phiếu", 80, v -> v.pk().maPhieuKham())
            .col("Tiếp nhận", 120, v -> v.pk().thoiGianTiepNhan()).col("Bệnh nhân", 160, PhieuKhamView::tenBenhNhan).col("Bác sĩ", 140, PhieuKhamView::tenBacSi)
            .col("Chẩn đoán", 260, v -> v.pk().chanDoan()).emptyText("Không có lượt khám chờ lập hóa đơn.");
    private final DataTable<CongNoView> no = new DataTable<CongNoView>().col("Mã BN", 80, CongNoView::maBenhNhan).col("Bệnh nhân", 170, CongNoView::tenBenhNhan)
            .col("SĐT", 100, CongNoView::soDienThoai).col("Số HĐ còn nợ", 90, CongNoView::soHoaDon).col("Tổng còn nợ", 120, CongNoView::tongNo)
            .col("HĐ cũ nhất", 120, CongNoView::hoaDonCuNhat).emptyText("Không có công nợ.");
    private final JLabel tongNo = new JLabel(" ");
    private HoaDonChiTiet cur;

    public HoaDonPage() {
        super("Hóa đơn & thanh toán", "UC04 Quản lý thanh toán · UC05 Theo dõi công nợ — mỗi lần thu một phương thức, có lịch sử");
        boolean thuNgan = Session.can(Quyen.THU_NGAN);
        if (thuNgan) header.action(Btn.primary("Lập hóa đơn", e -> { if (new HoaDonDialog(this, null, null, null, null).open() != null) reload(); }));
        q.addActionListener(e -> reload());
        Card list = Card.titled("Hóa đơn", null, Layouts.stack(new Toolbar().label("Từ").with(tu).label("đến").with(den).with(tt, q, Btn.secondary("Lọc", e -> reload())), table));
        JPanel d = new JPanel();
        d.setOpaque(false);
        d.setLayout(new BoxLayout(d, BoxLayout.Y_AXIS));
        lines.setPreferredSize(new Dimension(10, 140));
        pays.setPreferredSize(new Dimension(10, 150));
        ycs.setPreferredSize(new Dimension(10, 100));
        d.add(Card.titled("Chi tiết hóa đơn", null, Layouts.stack(info, lines)));
        d.add(javax.swing.Box.createVerticalStrut(10));
        Toolbar act = new Toolbar();
        if (thuNgan) act.add(thu);
        if (Session.can(Quyen.HUY_GIAO_DICH)) act.add(huyGd);
        if (Session.can(Quyen.YEU_CAU_HUY)) act.add(yeuCau);
        d.add(Card.titled("Các lần thu", "Lần thu đã hủy vẫn giữ trong lịch sử", Layouts.stack(act, pays)));
        d.add(javax.swing.Box.createVerticalStrut(10));
        d.add(Card.titled("Yêu cầu hủy hóa đơn", "Người duyệt phải khác người tạo yêu cầu", ycs));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Hóa đơn", Layouts.pad(Layouts.masterDetail(list, d, 640), 10, 0, 0, 0));
        if (thuNgan) {
            Btn lap = Btn.primary("Lập hóa đơn cho lượt khám", e -> lapTuPhieu());
            tabs.addTab("Chờ lập hóa đơn", Layouts.pad(Card.titled("Lượt khám đã kết luận chưa có hóa đơn", null, Layouts.stack(new Toolbar().with(lap), cho)), 10, 0, 0, 0));
            cho.onOpen(v -> lapTuPhieu());
        }
        tongNo.setFont(vn.n08.nhakhoa.view.component.Theme.H2);
        tabs.addTab("Công nợ", Layouts.pad(Card.titled("Công nợ theo bệnh nhân (UC05)", "Tính từ hóa đơn chưa hủy: tổng chi tiết − tổng lần thu thành công",
                Layouts.stack(tongNo, no)), 10, 0, 0, 0));
        no.onOpen(v -> { q.setText(v.maBenhNhan()); tabs.setSelectedIndex(0); tu.setText(Fmt.date(Now.today().minusYears(3))); reload(); });
        body(tabs);
        table.autoSelect();
        table.onSelect(v -> load());
        pays.onSelect(v -> buttons());
        show(null);
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
        TrangThaiHoaDon t = Fields.selected(tt);
        String s = q.getText();
        Async.load(table, () -> svc.danhSach(a, b, t, s), r -> load());
        if (Session.can(Quyen.THU_NGAN)) Async.load(cho, svc::phieuKhamChoLapHoaDon);
        Async.load(no, svc::congNo, r -> tongNo.setText("Tổng công nợ: " + Fmt.money(r.stream().map(CongNoView::tongNo).reduce(BigDecimal.ZERO, BigDecimal::add))
                + " · " + r.size() + " bệnh nhân"));
    }

    private void load() {
        HoaDonView v = table.selected();
        if (v == null) { show(null); return; }
        Async.run(this, () -> svc.chiTiet(v.hd().maHoaDon()), this::show);
    }

    private void show(HoaDonChiTiet c) {
        cur = c;
        info.clear();
        if (c == null) {
            info.row("Hóa đơn", "Chọn một hóa đơn.");
            lines.setRows(List.of());
            pays.setRows(List.of());
            ycs.setRows(List.of());
        } else {
            HoaDonView h = c.hoaDon();
            info.row("Mã", h.hd().maHoaDon()).row("Trạng thái", h.hd().trangThai()).row("Bệnh nhân", h.tenBenhNhan()).row("Lượt khám", h.hd().maPhieuKham())
                    .row("Người lập", h.tenNguoiLap()).row("Ngày lập", h.hd().ngayLap()).row("Tổng tiền", h.tongTien()).row("Đã thu", h.daThanhToan())
                    .row("Còn nợ", h.hd().trangThai() == TrangThaiHoaDon.DA_HUY ? "—" : Fmt.money(h.conNo()));
            lines.setRows(c.dong());
            pays.setRows(c.thanhToan());
            ycs.setRows(c.yeuCauHuy());
        }
        info.revalidate();
        buttons();
    }

    private void buttons() {
        boolean mo = cur != null && cur.hoaDon().hd().trangThai() != TrangThaiHoaDon.DA_HUY;
        thu.setEnabled(mo && cur.hoaDon().conNo().signum() > 0 && !cur.hoaDon().coYeuCauHuyCho());
        yeuCau.setEnabled(mo && !cur.hoaDon().coYeuCauHuyCho());
        ThanhToanView p = pays.selected();
        huyGd.setEnabled(mo && p != null && p.tt().trangThai() == TrangThaiThanhToan.THANH_CONG);
    }

    private void thu() {
        if (new ThanhToanDialog(this, cur.hoaDon()).open() != null) reload();
    }

    private void huyGd() {
        ThanhToanView p = pays.selected();
        String ly = Dialogs.reason(this, "Hủy lần thu " + p.tt().maThanhToan(), Fmt.money(p.tt().soTien()) + " · " + p.tt().phuongThuc().label()
                + "\nDùng khi nhập sai; lần thu vẫn giữ trong lịch sử. Nhập lý do.", "Hủy lần thu", true);
        if (ly != null) Async.run(this, () -> { svc.huyGiaoDich(p.tt().maThanhToan(), ly); return true; }, r -> { Toast.show(this, "Đã hủy lần thu."); reload(); });
    }

    private void yeuCau() {
        String m = cur.hoaDon().hd().maHoaDon();
        String ly = Dialogs.reason(this, "Yêu cầu hủy hóa đơn " + m, "Yêu cầu sẽ chờ một tài khoản KHÁC có quyền duyệt. Nhập lý do.", "Gửi yêu cầu", true);
        if (ly != null) Async.run(this, () -> svc.guiYeuCauHuy(m, ly), ma -> { Toast.show(this, "Đã gửi yêu cầu " + ma + "."); reload(); });
    }

    private void lapTuPhieu() {
        PhieuKhamView v = cho.selected();
        if (v == null) return;
        Async.run(this, () -> svc.goiYDong(v.pk().maPhieuKham()), g -> {
            if (new HoaDonDialog(this, v.pk().maBenhNhan(), v.tenBenhNhan(), v.pk().maPhieuKham(), g).open() != null) reload();
        });
    }

}
