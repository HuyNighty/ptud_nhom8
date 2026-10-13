package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.dto.BenhNhanChiTiet;
import vn.n08.nhakhoa.dto.HoaDonView;
import vn.n08.nhakhoa.dto.KeHoachView;
import vn.n08.nhakhoa.dto.LichHenView;
import vn.n08.nhakhoa.dto.PhieuKhamView;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.DetailGrid;
import vn.n08.nhakhoa.view.component.Theme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/** UC09 Xem hồ sơ bệnh nhân: hành chính + bệnh án, lượt khám, kế hoạch, lịch hẹn, hóa đơn. */
public class BenhNhanChiTietPanel extends JTabbedPane {
    public BenhNhanChiTietPanel(BenhNhanChiTiet d) {
        DetailGrid g = new DetailGrid(2);
        var bn = d.bn();
        g.row("Mã bệnh nhân", bn.maBenhNhan()).row("Mã hồ sơ", d.hoSo() == null ? null : d.hoSo().maHoSo())
                .row("Họ tên", bn.hoTen()).row("Ngày sinh", Fmt.date(bn.ngaySinh()) + " (" + bn.tuoi(Now.today()) + " tuổi)")
                .row("Giới tính", bn.gioiTinh().label()).row("Số điện thoại", bn.soDienThoai())
                .row("Địa chỉ", bn.diaChi()).row("Người liên hệ", bn.nguoiLienHe())
                .row("Tiền sử bệnh", d.hoSo() == null ? null : d.hoSo().tienSuBenh()).row("Dị ứng", d.hoSo() == null ? null : d.hoSo().diUng())
                .row("Ngày tạo hồ sơ", bn.ngayTao());
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.SURFACE);
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        p.add(g, BorderLayout.NORTH);
        addTab("Hành chính & bệnh án", p);
        DataTable<PhieuKhamView> pk = new DataTable<PhieuKhamView>().col("Mã phiếu", 90, v -> v.pk().maPhieuKham()).col("Tiếp nhận", 120, v -> v.pk().thoiGianTiepNhan())
                .col("Bác sĩ", 140, v -> v.tenBacSi()).col("Chẩn đoán", 260, v -> v.pk().chanDoan()).col("Trạng thái", 110, v -> v.pk().trangThai())
                .emptyText("Chưa có lượt khám.");
        pk.setRows(d.luotKham());
        addTab("Lượt khám (" + d.luotKham().size() + ")", pk);
        DataTable<KeHoachView> kh = new DataTable<KeHoachView>().col("Mã", 90, v -> v.kh().maKeHoach()).col("Ngày lập", 90, v -> v.kh().ngayLap())
                .col("Mục tiêu", 240, v -> v.kh().mucTieu()).col("Tiến độ", 80, v -> v.soDaThucHien() + "/" + v.soHangMuc())
                .col("Chi phí dự kiến", 110, v -> v.chiPhiDuKien()).col("Trạng thái", 110, v -> v.kh().trangThai()).emptyText("Chưa có kế hoạch điều trị.");
        kh.setRows(d.keHoach());
        addTab("Kế hoạch (" + d.keHoach().size() + ")", kh);
        DataTable<LichHenView> lh = new DataTable<LichHenView>().col("Mã", 90, v -> v.lh().maLichHen()).col("Thời gian", 120, v -> v.lh().thoiGianHen())
                .col("Bác sĩ", 140, v -> v.tenBacSi()).col("Lý do", 200, v -> v.lh().lyDoHen()).col("Trạng thái", 110, v -> v.lh().trangThai()).emptyText("Chưa có lịch hẹn.");
        lh.setRows(d.lichHen());
        addTab("Lịch hẹn (" + d.lichHen().size() + ")", lh);
        DataTable<HoaDonView> hd = new DataTable<HoaDonView>().col("Mã HĐ", 90, v -> v.hd().maHoaDon()).col("Ngày lập", 120, v -> v.hd().ngayLap())
                .col("Tổng tiền", 110, HoaDonView::tongTien).col("Đã thu", 110, HoaDonView::daThanhToan).col("Còn nợ", 110, HoaDonView::conNo)
                .col("Trạng thái", 130, v -> v.hd().trangThai()).emptyText("Chưa có hóa đơn.");
        hd.setRows(d.hoaDon());
        addTab("Hóa đơn (" + d.hoaDon().size() + ")", hd);
    }
}
