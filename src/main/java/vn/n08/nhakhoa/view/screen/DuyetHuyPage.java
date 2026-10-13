package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.YeuCauHuyView;
import vn.n08.nhakhoa.model.TrangThaiDuyet;
import vn.n08.nhakhoa.service.HoaDonService;
import vn.n08.nhakhoa.service.NhatKyService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.DetailGrid;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Timeline;
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;

/** Duyệt / từ chối yêu cầu hủy hóa đơn (Figma QL-08, QL-09). Không tài khoản nào xử lý yêu cầu do chính mình tạo. */
public class DuyetHuyPage extends Page {
    private final HoaDonService svc = new HoaDonService();
    private final JComboBox<TrangThaiDuyet> tt = Fields.combo(List.of(TrangThaiDuyet.values()), TrangThaiDuyet::label, "Tất cả");
    private final DataTable<YeuCauHuyView> table = new DataTable<YeuCauHuyView>()
            .col("Mã YC", 80, v -> v.yc().maYeuCau()).col("Hóa đơn", 80, v -> v.yc().maHoaDon()).col("Bệnh nhân", 140, YeuCauHuyView::tenBenhNhan)
            .col("Tổng tiền", 100, YeuCauHuyView::tongTien).col("Người tạo", 140, v -> v.tenNguoiTao() + " (" + v.tkTao() + ")")
            .col("Thời gian", 110, v -> v.yc().thoiGianTao()).col("Trạng thái", 100, v -> v.yc().trangThai()).emptyText("Không có yêu cầu.");
    private final DetailGrid info = new DetailGrid(1);
    private final JPanel note = new JPanel(new java.awt.BorderLayout());
    private final Timeline tl = new Timeline();
    private final Btn duyet = Btn.success("Duyệt hủy", e -> xuLy(true));
    private final Btn tuChoi = Btn.danger("Từ chối", e -> xuLy(false));

    public DuyetHuyPage() {
        super("Duyệt hủy hóa đơn", "Figma QL-08/09 — hiển thị người tạo, người duyệt và lịch sử; chặn tự duyệt ở Service và CSDL");
        tt.setSelectedItem(TrangThaiDuyet.CHO_DUYET);
        tt.addActionListener(e -> reload());
        Card list = Card.titled("Yêu cầu hủy", null, Layouts.stack(new Toolbar().label("Trạng thái").with(tt), table));
        JPanel d = new JPanel();
        d.setOpaque(false);
        d.setLayout(new BoxLayout(d, BoxLayout.Y_AXIS));
        note.setOpaque(false);
        JPanel box = Layouts.stack(info, note);
        box.add(new Toolbar().with(duyet, tuChoi), java.awt.BorderLayout.SOUTH);
        d.add(Card.titled("Chi tiết yêu cầu", null, box));
        d.add(javax.swing.Box.createVerticalStrut(10));
        d.add(Card.titled("Lịch sử", null, tl));
        body(Layouts.masterDetail(list, d, 480));
        table.autoSelect();
        table.onSelect(this::show);
        show(null);
    }

    @Override public void reload() {
        TrangThaiDuyet t = Fields.selected(tt);
        Async.load(table, () -> svc.danhSachYeuCau(t), r -> show(table.selected()));
    }

    private void show(YeuCauHuyView v) {
        info.clear();
        note.removeAll();
        boolean cho = v != null && v.yc().trangThai() == TrangThaiDuyet.CHO_DUYET;
        boolean tuTao = v != null && v.yc().maTaiKhoanTao().equals(Session.user().maTaiKhoan());
        if (v == null) {
            info.row("Yêu cầu", "Chọn một yêu cầu.");
            tl.set(List.of());
        } else {
            info.row("Mã yêu cầu", v.yc().maYeuCau()).row("Hóa đơn", v.yc().maHoaDon() + " · " + v.tenBenhNhan()).row("Trạng thái hóa đơn", v.trangThaiHoaDon())
                    .row("Tổng tiền / đã thu", Fmt.money(v.tongTien()) + " / " + Fmt.money(v.daThanhToan()))
                    .row("Người tạo", v.tenNguoiTao() + " (" + v.tkTao() + ") · " + Fmt.dateTime(v.yc().thoiGianTao())).row("Lý do", v.yc().lyDo())
                    .row("Trạng thái", v.yc().trangThai());
            if (v.tkDuyet() != null) info.row("Người xử lý", v.tenNguoiDuyet() + " (" + v.tkDuyet() + ") · " + Fmt.dateTime(v.yc().thoiGianDuyet()))
                    .row("Ghi chú xử lý", v.yc().lyDoDuyet());
            JComponent n = null;
            if (cho && tuTao) n = Layouts.note("Yêu cầu do chính tài khoản <b>" + Session.user().tenDangNhap() + "</b> tạo nên tài khoản này <b>không được duyệt hay từ chối</b>. "
                    + "Cần một tài khoản khác có quyền duyệt.", true);
            else if (cho && v.daThanhToan().signum() > 0) n = Layouts.note("Hóa đơn còn " + Fmt.money(v.daThanhToan()) + " đã thu: cần hủy (hoàn) các lần thu trước khi duyệt hủy.", true);
            if (n != null) note.add(n);
            Async.run(this, () -> new NhatKyService().lichSu(v.yc().maYeuCau()), tl::set);
        }
        duyet.setEnabled(cho && !tuTao);
        tuChoi.setEnabled(cho && !tuTao);
        info.revalidate();
        note.revalidate();
    }

    private void xuLy(boolean dongY) {
        YeuCauHuyView v = table.selected();
        if (v == null) return;
        String ly = Dialogs.reason(this, (dongY ? "Duyệt hủy " : "Từ chối ") + v.yc().maHoaDon(), dongY ? "Hóa đơn sẽ chuyển Đã hủy. Nhập ghi chú duyệt." : "Nhập lý do từ chối.",
                dongY ? "Duyệt hủy" : "Từ chối", !dongY);
        if (ly != null) Async.run(this, () -> { svc.xuLyYeuCau(v.yc().maYeuCau(), dongY, ly); return true; }, r -> {
            Toast.show(this, dongY ? "Đã duyệt hủy hóa đơn." : "Đã từ chối yêu cầu.");
            reload();
            if (MainFrame.current() != null) MainFrame.current().refreshCounts();
        });
    }
}
