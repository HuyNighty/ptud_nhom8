package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.DieuChinhDong;
import vn.n08.nhakhoa.dto.DieuChinhView;
import vn.n08.nhakhoa.model.TrangThaiDuyet;
import vn.n08.nhakhoa.service.DieuChinhKhoService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.DetailGrid;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.DieuChinhDialog;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.util.List;

/** UC28 Kiểm kê kho, UC35 Hủy / hao hụt — phiếu điều chỉnh chờ tài khoản khác duyệt (Figma QL-10). */
public class KiemKePage extends Page {
    private final DieuChinhKhoService svc = new DieuChinhKhoService();
    private final JComboBox<TrangThaiDuyet> tt = Fields.combo(List.of(TrangThaiDuyet.values()), TrangThaiDuyet::label, "Tất cả");
    private final DataTable<DieuChinhView> table = new DataTable<DieuChinhView>()
            .col("Mã phiếu", 85, v -> v.p().maPhieu()).col("Loại", 90, v -> v.p().loai()).col("Lập bởi", 140, v -> v.tenNguoiTao() + " (" + v.tkTao() + ")")
            .col("Thời gian", 110, v -> v.p().thoiGianTao()).col("Số lô", 50, DieuChinhView::soDong).col("Chênh lệch", 75, DieuChinhView::tongChenhLech)
            .col("Trạng thái", 100, v -> v.p().trangThai()).col("Người duyệt", 100, DieuChinhView::tkDuyet).emptyText("Không có phiếu.");
    private final DetailGrid info = new DetailGrid(1);
    private final JPanel note = new JPanel(new java.awt.BorderLayout());
    private final DataTable<DieuChinhDong> lines = new DataTable<DieuChinhDong>().col("Mã lô", 75, d -> d.ct().maLo()).col("Mặt hàng", 170, DieuChinhDong::tenHang)
            .col("Số lô", 70, DieuChinhDong::soLo).col("HSD", 85, DieuChinhDong::hanSuDung).col("Hệ thống", 65, d -> d.ct().soLuongHeThong())
            .col("Thực tế", 60, d -> d.ct().soLuongThucTe()).col("Chênh", 55, d -> d.ct().chenhLech()).col("Tồn hiện tại", 75, DieuChinhDong::tonHienTai);
    private final Btn duyet = Btn.success("Duyệt (điều chỉnh tồn)", e -> xuLy(true));
    private final Btn tuChoi = Btn.danger("Từ chối", e -> xuLy(false));

    public KiemKePage() {
        super("Kiểm kê & hao hụt", "UC28 Kiểm kê kho · UC35 Quản lý hủy / hao hụt — tồn chỉ thay đổi khi được tài khoản khác duyệt");
        if (Session.can(Quyen.KIEM_KE)) header.action(Btn.primary("Lập phiếu", e -> { if (new DieuChinhDialog(this).open() != null) reload(); }));
        tt.setSelectedItem(TrangThaiDuyet.CHO_DUYET);
        tt.addActionListener(e -> reload());
        Card list = Card.titled("Phiếu điều chỉnh", null, Layouts.stack(new Toolbar().label("Trạng thái").with(tt), table));
        JPanel d = new JPanel();
        d.setOpaque(false);
        d.setLayout(new BoxLayout(d, BoxLayout.Y_AXIS));
        note.setOpaque(false);
        lines.setPreferredSize(new Dimension(10, 200));
        JPanel b = Layouts.stack(info, lines);
        JPanel south = new JPanel(new java.awt.BorderLayout(0, 6));
        south.setOpaque(false);
        south.add(note, java.awt.BorderLayout.NORTH);
        if (Session.can(Quyen.DUYET_KHO)) south.add(new Toolbar().with(duyet, tuChoi), java.awt.BorderLayout.SOUTH);
        b.add(south, java.awt.BorderLayout.SOUTH);
        d.add(Card.titled("Chi tiết phiếu", null, b));
        body(Layouts.masterDetail(list, d, 600));
        table.autoSelect();
        table.onSelect(this::show);
        show(null);
    }

    @Override public void reload() {
        TrangThaiDuyet t = Fields.selected(tt);
        Async.load(table, () -> svc.danhSach(t), r -> show(table.selected()));
    }

    private void show(DieuChinhView v) {
        info.clear();
        note.removeAll();
        boolean cho = v != null && v.p().trangThai() == TrangThaiDuyet.CHO_DUYET;
        boolean tuLap = v != null && v.p().maTaiKhoanTao().equals(Session.user().maTaiKhoan());
        if (v == null) {
            info.row("Phiếu", "Chọn một phiếu.");
            lines.setRows(List.of());
        } else {
            info.row("Mã / loại", v.p().maPhieu() + " · " + v.p().loai().label()).row("Lập bởi", v.tenNguoiTao() + " (" + v.tkTao() + ") · " + Fmt.dateTime(v.p().thoiGianTao()))
                    .row("Lý do", v.p().lyDo()).row("Trạng thái", v.p().trangThai());
            if (v.tkDuyet() != null) info.row("Xử lý bởi", v.tenNguoiDuyet() + " (" + v.tkDuyet() + ") · " + Fmt.dateTime(v.p().thoiGianDuyet())).row("Ghi chú duyệt", v.p().ghiChuDuyet());
            if (cho && tuLap) note.add(Layouts.note("Phiếu do chính tài khoản <b>" + Session.user().tenDangNhap() + "</b> lập nên không được duyệt hay từ chối.", true));
            Async.load(lines, () -> svc.chiTiet(v.p().maPhieu()));
        }
        duyet.setEnabled(cho && !tuLap);
        tuChoi.setEnabled(cho && !tuLap);
        info.revalidate();
        note.revalidate();
    }

    private void xuLy(boolean ok) {
        DieuChinhView v = table.selected();
        String gc;
        if (ok) gc = Dialogs.confirm(this, "Duyệt " + v.p().maPhieu(), "Tồn các lô sẽ đặt bằng số thực tế. Nếu tồn đã đổi kể từ khi lập phiếu, hệ thống sẽ chặn.", "Duyệt", false) ? "" : null;
        else gc = Dialogs.reason(this, "Từ chối " + v.p().maPhieu(), "Nhập lý do từ chối.", "Từ chối", true);
        if (gc == null) return;
        Async.run(this, () -> { svc.xuLy(v.p().maPhieu(), ok, gc); return true; }, r -> {
            Toast.show(this, ok ? "Đã duyệt, tồn kho đã điều chỉnh." : "Đã từ chối phiếu.");
            reload();
            if (MainFrame.current() != null) MainFrame.current().refreshCounts();
        });
    }
}
