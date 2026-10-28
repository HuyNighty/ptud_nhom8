package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.response.StockHistoryResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockLotViewResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockMonitorRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockMovementResponse;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.StockMonitorService;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

/** Lịch sử tồn của một mặt hàng: các lô đang còn và mọi biến động đã ghi sổ (mới nhất trên cùng). */
final class StockHistoryDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] LOT_COLUMNS = {"#", "Số lô", "Hạn sử dụng", "Tồn", "Giá nhập", "Nhà cung cấp"};
    private static final String[] MOVE_COLUMNS = {"Thời điểm", "Chứng từ", "Loại", "Số lô", "Nhập", "Xuất", "Tồn sau",
            "Đối tác / lý do", "Người thực hiện"};

    private final JPanel pnlBody = new JPanel(new BorderLayout(0, 12));

    StockHistoryDialog(Window owner, StockMonitorService service, Supplier<InventoryActor> session,
                       StockMonitorRowResponse item) {
        super(owner, "Lịch sử tồn " + item.ma(), ModalityType.APPLICATION_MODAL);
        Objects.requireNonNull(service);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pnlBody.setBackground(SURFACE);
        pnlBody.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        pnlBody.add(new JLabel("Đang tải lịch sử…"), BorderLayout.CENTER);
        setContentPane(pnlBody);
        setSize(1260, 720);
        setLocationRelativeTo(owner);
        Set<Role> roles = session.get().roles();
        new SwingWorker<StockHistoryResponse, Void>() {
            @Override
            protected StockHistoryResponse doInBackground() {
                return service.history(roles, item.loai(), item.ma());
            }

            @Override
            protected void done() {
                try {
                    render(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    pnlBody.removeAll();
                    JLabel lblError = new JLabel(InventoryItemPanel.describe(e.getCause()));
                    lblError.setForeground(RED);
                    pnlBody.add(lblError, BorderLayout.CENTER);
                    pnlBody.revalidate();
                }
            }
        }.execute();
    }

    private void render(StockHistoryResponse h) {
        StockMonitorRowResponse r = h.matHang();
        pnlBody.removeAll();
        JLabel lblTitle = new JLabel(r.ma() + " — " + r.ten());
        lblTitle.setFont(font(Font.BOLD, 21));
        lblTitle.setForeground(TEXT);
        JLabel lblInfo = new JLabel(StockMonitorPanel.typeLabel(r.loai()) + " · Tồn thực tế " + r.tonThucTe() + " "
                + r.donViTinh() + " (còn hạn " + r.tonKhaDung() + ") · Định mức " + r.dinhMucToiThieu() + " · "
                + StockMonitorPanel.levelLabel(r.mucTon()) + " · Vị trí " + (r.viTriLuuTru() == null ? "chưa xếp"
                : r.viTriLuuTru()) + " · Giá trị " + InventoryFormat.money(r.giaTri()));
        lblInfo.setFont(font(Font.PLAIN, 14));
        lblInfo.setForeground(TEXT_MUTED);
        JPanel pnlTop = new JPanel(new BorderLayout(0, 6));
        pnlTop.setOpaque(false);
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        pnlTop.add(lblInfo, BorderLayout.CENTER);
        pnlBody.add(pnlTop, BorderLayout.NORTH);

        JTable tblLots = new JTable(new LotModel(h.lo()));
        tblLots.setFocusable(false);
        tblLots.setRowSelectionAllowed(false);
        styleTable(tblLots, 34, new int[] {40, 160, 130, 90, 130, 300}, Set.of(0, 3, 4));
        List<StockMovementResponse> newestFirst = new ArrayList<>(h.bienDong());
        java.util.Collections.reverse(newestFirst);
        JTable tblMoves = new JTable(new MoveModel(newestFirst));
        tblMoves.setFocusable(false);
        tblMoves.setRowSelectionAllowed(false);
        styleTable(tblMoves, 34, new int[] {150, 100, 95, 110, 65, 65, 85, 320, 170}, Set.of(4, 5, 6));
        JSplitPane splContent = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                section("CÁC LÔ CÒN TỒN (hết hạn sớm nhất trước) — " + h.lo().size() + " lô", tblLots),
                section("BIẾN ĐỘNG TỒN (mới nhất trước) — " + h.bienDong().size() + " dòng", tblMoves));
        splContent.setResizeWeight(0.35);
        splContent.setBorder(BorderFactory.createEmptyBorder());
        splContent.setOpaque(false);
        pnlBody.add(splContent, BorderLayout.CENTER);

        JButton btnClose = createButton("Đóng", ButtonKind.PRIMARY);
        btnClose.addActionListener(e -> dispose());
        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setOpaque(false);
        pnlFooter.add(btnClose, BorderLayout.EAST);
        pnlBody.add(pnlFooter, BorderLayout.SOUTH);
        pnlBody.revalidate();
        pnlBody.repaint();
    }

    private static JPanel section(String title, JTable table) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(font(Font.PLAIN, 13));
        lbl.setForeground(TEXT_MUTED);
        JPanel pnl = new JPanel(new BorderLayout(0, 6));
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        pnl.add(lbl, BorderLayout.NORTH);
        pnl.add(tableScroll(table), BorderLayout.CENTER);
        return pnl;
    }

    private static final class LotModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<StockLotViewResponse> rows;

        LotModel(List<StockLotViewResponse> rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return LOT_COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return LOT_COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            StockLotViewResponse l = rows.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> l.soLo();
                case 2 -> InventoryFormat.date(l.hanSuDung());
                case 3 -> l.soLuongTon();
                case 4 -> l.donGiaNhap() == null ? "—" : InventoryFormat.money(l.donGiaNhap());
                default -> l.nhaCungCap();
            };
        }
    }

    private static final class MoveModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<StockMovementResponse> rows;

        MoveModel(List<StockMovementResponse> rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return MOVE_COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return MOVE_COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            StockMovementResponse m = rows.get(row);
            return switch (column) {
                case 0 -> InventoryFormat.dateTime(m.thoiDiem());
                case 1 -> m.maChungTu();
                case 2 -> m.loai();
                case 3 -> m.soLo();
                case 4 -> m.nhap() == 0 ? "" : String.valueOf(m.nhap());
                case 5 -> m.xuat() == 0 ? "" : String.valueOf(m.xuat());
                case 6 -> m.tonSau();
                case 7 -> m.doiTac();
                default -> m.nguoiThucHien();
            };
        }
    }
}
