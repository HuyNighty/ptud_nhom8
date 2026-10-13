package vn.n08.nhakhoa.view.screen;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.dto.NhatKyView;
import vn.n08.nhakhoa.service.NhatKyService;
import vn.n08.nhakhoa.util.Fmt;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.view.component.Btn;
import vn.n08.nhakhoa.view.component.Card;
import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Dialogs;
import vn.n08.nhakhoa.view.component.Fields;
import vn.n08.nhakhoa.view.component.HintField;
import vn.n08.nhakhoa.view.component.Layouts;
import vn.n08.nhakhoa.view.component.Toolbar;

import javax.swing.JComboBox;
import java.time.LocalDate;
import java.util.List;

/** Nhật ký thao tác: tài khoản thực hiện (không mạo danh) và người chịu trách nhiệm chuyên môn khi khác. */
public class NhatKyPage extends Page {
    private final NhatKyService svc = new NhatKyService();
    private final HintField tu = Fields.date(Now.today().minusDays(7)), den = Fields.date(Now.today());
    private final JComboBox<String> cn = Fields.combo(List.of(), s -> s, "Tất cả chức năng");
    private final HintField q = Fields.text("Tài khoản, hành động, đối tượng, nội dung");
    private final DataTable<NhatKyView> table = new DataTable<NhatKyView>()
            .col("Thời gian", 120, NhatKyView::thoiGian).col("Tài khoản", 95, v -> v.tenDangNhap() == null ? "hệ thống" : v.tenDangNhap())
            .col("Chức năng", 110, NhatKyView::chucNang).col("Hành động", 230, NhatKyView::hanhDong).col("Đối tượng", 140, NhatKyView::doiTuong)
            .col("Nội dung", 300, NhatKyView::noiDung).col("Người chịu trách nhiệm", 150, NhatKyView::tenNguoiChiuTrachNhiem).emptyText("Không có nhật ký.");

    public NhatKyPage() {
        super("Nhật ký thao tác", "Chỉ đọc · tối đa 1.000 dòng mới nhất theo bộ lọc");
        q.addActionListener(e -> reload());
        body(Card.titled("Nhật ký", null, Layouts.stack(new Toolbar().label("Từ").with(tu).label("đến").with(den).with(cn, q, Btn.secondary("Lọc", e -> reload())), table)));
    }

    @Override public void reload() {
        if (cn.getItemCount() <= 1) Async.run(this, svc::chucNang, r -> Fields.setItems(cn, r, true));
        LocalDate a, b;
        try {
            a = Fmt.parseDate(tu.getText(), "tu", "Từ ngày");
            b = Fmt.parseDate(den.getText(), "den", "Đến ngày");
        } catch (RuntimeException e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        String c = Fields.selected(cn), s = q.getText();
        Async.load(table, () -> svc.tim(a, b, c, s));
    }
}
