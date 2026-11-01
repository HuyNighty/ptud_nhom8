package com.quanlynhakhoa.nghiepvu.kho.hopcapcuu.view;

import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.nghiepvu.kho.hopcapcuu.service.EmergencyKitService;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public final class EmergencyKitPanel extends JPanel {
    final JTabbedPane tabs = new JTabbedPane();
    final JTextField checkId = new JTextField(), note = new JTextField();
    final JButton refreshCatalog = new JButton("Tải danh mục"), save = new JButton("Lưu lần kiểm tra"), refreshHistory = new JButton("Tải lịch sử");
    final DefaultTableModel catalogModel = new DefaultTableModel(new Object[]{"Mã mục", "Tên mục", "ĐVT", "Tối thiểu", "Đang có", "Hạn gần nhất", "SL thực tế", "Hạn thực tế yyyy-MM-dd", "Ghi chú danh mục"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return c == 6 || c == 7;
        }
    };
    final DefaultTableModel historyModel = new DefaultTableModel(new Object[]{"Mã kiểm tra", "Thời gian", "Nhân viên", "Kết quả", "Ghi chú", "Số mục"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    final JTable catalog = new JTable(catalogModel), history = new JTable(historyModel);

    public EmergencyKitPanel(DesktopSession session) {
        setLayout(new BorderLayout());
        JPanel page = new JPanel(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(0, 4, 7, 7));
        form.add(new JLabel("Mã lần kiểm tra (≤10)"));
        form.add(checkId);
        form.add(new JLabel("Ghi chú"));
        form.add(note);
        form.add(refreshCatalog);
        form.add(save);
        page.add(form, BorderLayout.NORTH);
        page.add(new JScrollPane(catalog), BorderLayout.CENTER);
        page.add(new JLabel("Nhập số lượng thực tế và hạn còn lại cho từng mục; kết quả DAT/CAN_BO_SUNG được tính tự động."), BorderLayout.SOUTH);

        JPanel historyPage = new JPanel(new BorderLayout());
        historyPage.add(refreshHistory, BorderLayout.NORTH);
        historyPage.add(new JScrollPane(history), BorderLayout.CENTER);

        tabs.addTab("Kiểm tra danh mục", page);
        tabs.addTab("Lịch sử kiểm tra", historyPage);
        add(tabs, BorderLayout.CENTER);
        new EmergencyKitController(this, new EmergencyKitService(session));
    }
}