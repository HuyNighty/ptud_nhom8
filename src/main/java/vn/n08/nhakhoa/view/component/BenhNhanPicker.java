package vn.n08.nhakhoa.view.component;

import vn.n08.nhakhoa.controller.Async;
import vn.n08.nhakhoa.controller.ErrorHandler;
import vn.n08.nhakhoa.dto.BenhNhanView;
import vn.n08.nhakhoa.service.BenhNhanService;
import vn.n08.nhakhoa.util.Fmt;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;

/** Chọn bệnh nhân: nhập SĐT / mã / tên rồi Tìm (UC01), chọn trong danh sách kết quả. */
public class BenhNhanPicker extends JPanel {
    private final HintField q = Fields.text("SĐT, mã hoặc họ tên bệnh nhân");
    private final JComboBox<BenhNhanView> combo = Fields.combo(List.of(), v -> v.bn().maBenhNhan() + " · " + v.bn().hoTen() + " · "
            + Fmt.date(v.bn().ngaySinh()) + " · " + v.bn().soDienThoai(), "— chưa chọn —");
    private Consumer<BenhNhanView> onChange = v -> { };

    public BenhNhanPicker() {
        super(new BorderLayout(6, 6));
        setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setOpaque(false);
        top.add(q, BorderLayout.CENTER);
        top.add(Btn.secondary("Tìm", e -> search()), BorderLayout.EAST);
        q.addActionListener(e -> search());
        add(top, BorderLayout.NORTH);
        add(combo, BorderLayout.CENTER);
        combo.addActionListener(e -> onChange.accept(selected()));
    }

    public BenhNhanPicker onChange(Consumer<BenhNhanView> c) { onChange = c; return this; }

    private void search() {
        String s = q.getText();
        Async.run(this, () -> new BenhNhanService().tim(s), r -> {
            Fields.setItems(combo, r, true);
            if (r.size() == 1) combo.setSelectedIndex(1);
            else if (r.isEmpty()) Toast.show(this, "Không tìm thấy bệnh nhân phù hợp.");
        }, e -> Dialogs.error(this, ErrorHandler.message(e)));
    }

    /** Đặt sẵn một bệnh nhân (VD: mở từ danh sách tái khám). */
    public void preset(String q0) {
        q.setText(q0);
        search();
    }

    public BenhNhanView selected() { return Fields.selected(combo); }

    public String maBenhNhan() {
        BenhNhanView v = selected();
        return v == null ? null : v.bn().maBenhNhan();
    }
}
