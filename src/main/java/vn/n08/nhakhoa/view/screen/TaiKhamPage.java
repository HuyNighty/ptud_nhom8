package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.TaiKhamItem;
import vn.n08.nhakhoa.model.Tone;
import vn.n08.nhakhoa.service.KeHoachService;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Tag;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Theme;
import vn.n08.nhakhoa.view.component.Toolbar;
import vn.n08.nhakhoa.view.dialog.LichHenDialog;

import java.time.temporal.ChronoUnit;

/** UC14 Quản lý tái khám: hạng mục đến hạn / quá hạn của kế hoạch đang thực hiện, tạo lịch tái khám gắn kế hoạch. */
public class TaiKhamPage extends Page {
    private final KeHoachService svc = new KeHoachService();
    private final DataTable<TaiKhamItem> table = new DataTable<TaiKhamItem>()
            .col("Hạn", 90, v -> v.hangMuc().ct().ngayDuKien())
            .col("Tình trạng", 110, v -> {
                long d = ChronoUnit.DAYS.between(Now.today(), v.hangMuc().ct().ngayDuKien());
                return d < 0 ? new Tag("Quá hạn " + (-d) + " ngày", Tone.RED) : d == 0 ? new Tag("Hôm nay", Tone.AMBER) : new Tag("Còn " + d + " ngày", Tone.BLUE);
            })
            .col("Bệnh nhân", 150, TaiKhamItem::tenBenhNhan).col("SĐT", 100, TaiKhamItem::soDienThoai)
            .col("Hạng mục", 160, v -> v.hangMuc().tenDichVu() + " · răng " + v.hangMuc().ct().viTriRang())
            .col("Kế hoạch", 90, TaiKhamItem::maKeHoach).col("Bác sĩ", 130, TaiKhamItem::tenBacSi)
            .col("Lịch đã hẹn", 150, v -> v.maLichHenMo() == null ? "Chưa hẹn" : v.maLichHenMo() + " · " + vn.n08.nhakhoa.util.Fmt.shortDt(v.thoiGianHen()))
            .emptyText("Không có hạng mục đến hạn tái khám.")
            .rowTint(v -> v.hangMuc().ct().ngayDuKien().isBefore(Now.today()) ? Theme.RED_S : null);
    private final Btn hen = Btn.primary("Tạo lịch tái khám", e -> hen());
    private final Btn mo = Btn.secondary("Mở kế hoạch", e -> MainFrame.current().go("ke-hoach"));


    public TaiKhamPage() {
        super("Tái khám", "UC14 Quản lý tái khám — hạng mục chưa thực hiện đến hạn trong ngưỡng cấu hình hoặc đã quá hạn");
        Toolbar t = new Toolbar().with(hen);
        if (Session.can(Quyen.KE_HOACH)) t.add(mo);
        t.with(Btn.secondary("Tải lại", e -> reload()));
        body(Card.titled("Danh sách đến hạn", "Dòng nền đỏ: đã quá ngày dự kiến", Layouts.stack(t, table)));
        table.onSelect(v -> hen.setEnabled(v != null && v.maLichHenMo() == null));
        hen.setEnabled(false);
        table.onOpen(v -> hen());
    }

    @Override public void reload() {
        Async.load(table, svc::danhSachTaiKham);
    }

    private void hen() {
        TaiKhamItem v = table.selected();
        if (v == null || !hen.isEnabled()) return;
        if (new LichHenDialog(this, v.maBenhNhan(), v.maBacSi(), v.maKeHoach(), "Tái khám: " + v.hangMuc().tenDichVu() + " răng " + v.hangMuc().ct().viTriRang()).open() != null) reload();
    }

}
