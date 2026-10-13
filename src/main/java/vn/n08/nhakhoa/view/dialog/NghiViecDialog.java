package vn.n08.nhakhoa.view.dialog;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.BanGiaoView;
import vn.n08.nhakhoa.dto.LichHenView;
import vn.n08.nhakhoa.dto.NghiViecCheck;
import vn.n08.nhakhoa.dto.PhanCongView;
import vn.n08.nhakhoa.dto.PhieuKhamView;
import vn.n08.nhakhoa.service.BanGiaoService;
import vn.n08.nhakhoa.service.LichHenService;
import vn.n08.nhakhoa.service.NhanVienService;
import vn.n08.nhakhoa.service.PhanCongService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Theme;
import vn.n08.nhakhoa.view.component.Toast;
import vn.n08.nhakhoa.view.component.Toolbar;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Thủ tục nghỉ việc có điều kiện: liệt kê phân công, lịch hẹn, phiếu khám, bàn giao còn mở; người dùng xử lý từng việc
 * (thay người / chuyển bác sĩ / hủy có lý do / xác nhận). Chỉ khi hết việc mở mới hoàn tất → nhân viên NGHI_VIEC, tài khoản VO_HIEU.
 */
public class NghiViecDialog extends JDialog {
    private final String maNv;
    private final NhanVienService svc = new NhanVienService();
    private final JLabel tomTat = new JLabel(" ");
    private final DataTable<PhanCongView> pc = new DataTable<PhanCongView>().col("Mã", 80, v -> v.pc().maPhanCong()).col("Ngày", 90, v -> v.pc().ngayLam())
            .col("Ca", 90, PhanCongView::tenCa).col("Nhiệm vụ", 180, v -> v.pc().nhiemVu()).col("TT", 110, v -> v.pc().trangThai()).emptyText("Không còn phân công mở.");
    private final DataTable<LichHenView> lh = new DataTable<LichHenView>().col("Mã", 80, v -> v.lh().maLichHen()).col("Thời gian", 115, v -> v.lh().thoiGianHen())
            .col("Bệnh nhân", 150, LichHenView::tenBenhNhan).col("TT", 110, v -> v.lh().trangThai()).emptyText("Không còn lịch hẹn mở.");
    private final DataTable<PhieuKhamView> pk = new DataTable<PhieuKhamView>().col("Phiếu", 80, v -> v.pk().maPhieuKham()).col("Tiếp nhận", 115, v -> v.pk().thoiGianTiepNhan())
            .col("Bệnh nhân", 150, PhieuKhamView::tenBenhNhan).col("TT", 110, v -> v.pk().trangThai()).emptyText("Không còn phiếu khám mở.");
    private final DataTable<BanGiaoView> bg = new DataTable<BanGiaoView>().col("Mã", 80, v -> v.bg().maBanGiao()).col("Ngày", 90, v -> v.bg().ngay())
            .col("Giao → nhận", 220, v -> v.tenNguoiGiao() + " → " + v.tenNguoiNhan()).col("Người lập", 90, BanGiaoView::tkTao).emptyText("Không còn biên bản chờ.");
    private final HintField ngay = Fields.date(Now.today());
    private final HintField lyDo = Fields.text("Lý do nghỉ việc");
    private final Btn hoanTat = Btn.danger("Hoàn tất nghỉ việc", e -> hoanTat());
    private boolean done;

    public NghiViecDialog(Component p, String maNv, String hoTen) {
        super(SwingUtilities.getWindowAncestor(p), "Thủ tục nghỉ việc — " + hoTen + " (" + maNv + ")", ModalityType.APPLICATION_MODAL);
        this.maNv = maNv;
        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBackground(Theme.BG);
        c.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        c.add(Layouts.note("Hệ thống <b>không tự hủy hay xóa</b> lịch sử. Xử lý từng việc còn mở dưới đây; khi không còn việc mở mới hoàn tất được. "
                + "Hoàn tất sẽ chuyển nhân viên sang <b>Đã nghỉ việc</b> và tài khoản sang <b>Vô hiệu</b> (không đăng nhập được); dữ liệu cũ giữ nguyên.", false));
        c.add(javax.swing.Box.createVerticalStrut(8));
        tomTat.setFont(Theme.H2);
        c.add(tomTat);
        for (DataTable<?> t : new DataTable<?>[]{pc, lh, pk, bg}) t.setPreferredSize(new Dimension(10, 110));
        PhanCongService pcs = new PhanCongService();
        c.add(javax.swing.Box.createVerticalStrut(10));
        c.add(section("Phân công còn hiệu lực", "Thay người (cùng chức danh, ca chưa diễn ra) hoặc hủy có lý do", pc,
                Btn.secondary("Thay người", e -> {
                    PhanCongView v = pc.selected();
                    if (v == null) return;
                    String r = new ChonNhanVienDialog(this, "Thay người — " + v.pc().maPhanCong(), v.tenCa() + " · " + Fmt.date(v.pc().ngayLam()),
                            v.chucDanh(), false, "Thay người", m -> pcs.thayNguoi(v.pc().maPhanCong(), m, "Nhân viên " + maNv + " nghỉ việc"),
                            "Đã thay người.").open();
                    if (r != null) refresh();
                }),
                Btn.danger("Hủy phân công", e -> {
                    PhanCongView v = pc.selected();
                    if (v == null) return;
                    String ly = Dialogs.reason(this, "Hủy " + v.pc().maPhanCong(), "Lý do hủy.", "Hủy", true);
                    if (ly != null) Async.run(this, () -> { pcs.huy(v.pc().maPhanCong(), ly); return true; }, r -> refresh());
                })));
        LichHenService lhs = new LichHenService();
        c.add(javax.swing.Box.createVerticalStrut(10));
        c.add(section("Lịch hẹn chưa diễn ra (bác sĩ)", "Đổi lịch sang bác sĩ khác hoặc hủy (đã báo bệnh nhân)", lh,
                Btn.secondary("Đổi lịch / chuyển bác sĩ", e -> {
                    LichHenView v = lh.selected();
                    if (v != null && new DoiLichDialog(this, v).open() != null) refresh();
                }),
                Btn.danger("Hủy lịch", e -> {
                    LichHenView v = lh.selected();
                    if (v == null) return;
                    String ly = Dialogs.reason(this, "Hủy " + v.lh().maLichHen(), "Lý do hủy (đã báo bệnh nhân?).", "Hủy lịch", true);
                    if (ly != null) Async.run(this, () -> { lhs.huy(v.lh().maLichHen(), ly); return true; }, r -> refresh());
                })));
        c.add(javax.swing.Box.createVerticalStrut(10));
        c.add(section("Phiếu khám chưa kết thúc", "Xử lý tại màn hình Phiếu khám (kết luận hoặc hủy phiếu chờ khám)", pk));
        BanGiaoService bgs = new BanGiaoService();
        c.add(javax.swing.Box.createVerticalStrut(10));
        c.add(section("Biên bản bàn giao chờ xác nhận", "Xác nhận (người khác người lập) hoặc hủy có lý do", bg,
                Btn.secondary("Xác nhận", e -> {
                    BanGiaoView v = bg.selected();
                    if (v == null) return;
                    String gc = Dialogs.reason(this, "Xác nhận " + v.bg().maBanGiao(), "Ghi chú xác nhận.", "Xác nhận", false);
                    if (gc != null) Async.run(this, () -> { bgs.xacNhan(v.bg().maBanGiao(), gc); return true; }, r -> refresh());
                }),
                Btn.danger("Hủy biên bản", e -> {
                    BanGiaoView v = bg.selected();
                    if (v == null) return;
                    String ly = Dialogs.reason(this, "Hủy " + v.bg().maBanGiao(), "Lý do hủy.", "Hủy", true);
                    if (ly != null) Async.run(this, () -> { bgs.huy(v.bg().maBanGiao(), ly); return true; }, r -> refresh());
                })));
        JPanel fin = new JPanel(new BorderLayout(8, 0));
        fin.setOpaque(false);
        fin.add(new Toolbar().label("Ngày nghỉ").with(ngay).label("Lý do").with(lyDo), BorderLayout.CENTER);
        lyDo.setColumns(26);
        c.add(Card.titled("Hoàn tất", null, fin, hoanTat));
        for (java.awt.Component x : c.getComponents()) ((javax.swing.JComponent) x).setAlignmentX(LEFT_ALIGNMENT);
        Layouts.TrackWidth track = new Layouts.TrackWidth();
        track.setOpaque(true);
        track.setBackground(Theme.BG);
        track.add(c);
        JScrollPane sp = new JScrollPane(track);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        setLayout(new BorderLayout());
        add(sp, BorderLayout.CENTER);
        JPanel f = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        f.setBackground(Theme.HEAD);
        f.add(Btn.primary("Đóng", e -> dispose()));
        add(f, BorderLayout.SOUTH);
        setSize(820, 760);
        setLocationRelativeTo(getOwner());
        refresh();
    }

    private Card section(String t, String sub, DataTable<?> table, Btn... acts) {
        Card c = Card.titled(t, sub, table, acts);
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    private void refresh() {
        Async.run(this, () -> svc.kiemTraNghiViec(maNv), (NghiViecCheck ck) -> {
            pc.setRows(ck.phanCong());
            lh.setRows(ck.lichHen());
            pk.setRows(ck.phieuKham());
            bg.setRows(ck.banGiao());
            hoanTat.setEnabled(ck.duDieuKien() && ck.nv().dangLamViec());
            tomTat.setText(!ck.nv().dangLamViec() ? "Nhân viên đã nghỉ việc." : ck.duDieuKien() ? "Đủ điều kiện hoàn tất."
                    : "Còn " + ck.soViecCon() + " việc cần xử lý trước khi hoàn tất.");
            tomTat.setForeground(ck.duDieuKien() ? Theme.GREEN : Theme.AMBER);
        });
    }

    private void hoanTat() {
        try {
            var d = Fmt.parseDate(ngay.getText(), "ngay", "Ngày nghỉ");
            String ly = lyDo.getText();
            if (!Dialogs.confirm(this, "Hoàn tất nghỉ việc", "Nhân viên chuyển Đã nghỉ việc từ " + Fmt.date(d) + "; tài khoản bị vô hiệu vĩnh viễn. Tiếp tục?", "Hoàn tất", true)) return;
            Async.run(this, () -> { svc.hoanTatNghiViec(maNv, d, ly); return true; }, r -> {
                done = true;
                Toast.show(this, "Đã hoàn tất thủ tục nghỉ việc.");
                dispose();
            });
        } catch (RuntimeException e) {
            Dialogs.error(this, e.getMessage());
        }
    }

    public boolean open() {
        setVisible(true);
        return done;
    }

}
