package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.request.StocktakeCountRequest;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeLineResponse;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.StocktakeService;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 * "Cập nhật" đợt kiểm kê: nhập số thực tế đếm được cho từng lô (để trống = chưa đếm) và lý do khi lệch.
 * "Lưu số đếm" lưu tạm (được lưu nhiều lần); "Hoàn tất" lưu rồi duyệt — chỉ người khác người lập mới hoàn tất được.
 */
final class StocktakeCountDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS =
            {"#", "Mặt hàng", "Số lô", "Hạn sử dụng", "Sổ sách", "Thực tế", "Chênh lệch", "Lý do chênh lệch"};
    private static final int COL_ACTUAL = 5;
    private static final int COL_DIFF = 6;
    private static final int COL_REASON = 7;

    private final transient StocktakeService service;
    private final transient Supplier<InventoryActor> session;
    private transient StocktakeDetailResponse detail;
    private boolean changed;

    private final CountModel model = new CountModel();
    private final JTable tblCounts = new JTable(model);
    private final HintTextField txtFilter = new HintTextField("Lọc theo mã, tên mặt hàng hoặc số lô...", 24);
    private final JLabel lblSummary = new JLabel(" ");
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnClose = createButton("Đóng", ButtonKind.OUTLINE);
    private final JButton btnSave = createButton("Lưu số đếm", ButtonKind.OUTLINE);
    private final JButton btnFinish = createButton("Hoàn tất & điều chỉnh tồn", ButtonKind.PRIMARY);

    StocktakeCountDialog(Window owner, StocktakeService service, Supplier<InventoryActor> session,
                         StocktakeDetailResponse detail) {
        super(owner, "Cập nhật kiểm kê " + detail.maKiemKe(), ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        this.detail = detail;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        model.load(detail.dong());
        refresh();
        wireEvents();
        setSize(1120, 720);
        setLocationRelativeTo(owner);
    }

    /** Có lưu/duyệt → danh sách cần tải lại. */
    boolean changed() {
        return changed;
    }

    private JComponent buildContent() {
        JPanel pnlRoot = new JPanel(new BorderLayout(0, 12));
        pnlRoot.setBackground(SURFACE);
        pnlRoot.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JLabel lblTitle = new JLabel(getTitle());
        lblTitle.setFont(font(Font.BOLD, 20));
        lblTitle.setForeground(TEXT);
        JLabel lblInfo = new JLabel("Ngày " + InventoryFormat.date(detail.ngayKiemKe()) + " · " + detail.phamVi().label()
                + " · Người lập: " + detail.nguoiLap() + " · Sổ sách chụp lúc "
                + InventoryFormat.dateTime(detail.thoiDiemLap()));
        lblInfo.setFont(font(Font.PLAIN, 14));
        lblInfo.setForeground(TEXT_MUTED);
        JPanel pnlSearch = new JPanel(new BorderLayout(8, 0));
        pnlSearch.setBackground(SURFACE);
        pnlSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), BorderFactory.createEmptyBorder(0, 10, 0, 8)));
        pnlSearch.setPreferredSize(new Dimension(380, 38));
        txtFilter.setBorder(BorderFactory.createEmptyBorder());
        txtFilter.setFont(font(Font.PLAIN, 14));
        pnlSearch.add(new JLabel(new GlyphIcon(MenuGlyph.SEARCH, 16, TEXT_MUTED)), BorderLayout.WEST);
        pnlSearch.add(txtFilter, BorderLayout.CENTER);
        JPanel pnlSearchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlSearchRow.setOpaque(false);
        pnlSearchRow.add(pnlSearch);
        JPanel pnlTop = new JPanel(new BorderLayout(0, 8));
        pnlTop.setOpaque(false);
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        pnlTop.add(lblInfo, BorderLayout.CENTER);
        pnlTop.add(pnlSearchRow, BorderLayout.SOUTH);
        pnlRoot.add(pnlTop, BorderLayout.NORTH);

        styleTable(tblCounts, 40, new int[] {44, 270, 90, 115, 95, 85, 115, 260}, Set.of(0, 4, 5, 6));
        tblCounts.setSurrendersFocusOnKeystroke(true);
        tblCounts.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tblCounts.getColumnModel().getColumn(COL_DIFF).setCellRenderer(new DiffRenderer());
        TableRowSorter<CountModel> sorter = new TableRowSorter<>(model);
        for (int i = 0; i < COLUMNS.length; i++) {
            sorter.setSortable(i, false);
        }
        tblCounts.setRowSorter(sorter);
        JScrollPane scrCounts = tableScroll(tblCounts);
        pnlRoot.add(scrCounts, BorderLayout.CENTER);

        lblSummary.setFont(font(Font.BOLD, 15));
        lblSummary.setForeground(TEXT);
        lblError.setFont(font(Font.PLAIN, 13));
        lblError.setForeground(RED);
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlButtons.setOpaque(false);
        pnlButtons.add(btnClose);
        pnlButtons.add(btnSave);
        boolean sameAsCreator = Objects.equals(session.get().maNhanVien(), detail.maNhanVienLap());
        if (!sameAsCreator) {
            // UC28: người lập không được tự hoàn tất — nút chỉ hiện với quản lý kho khác.
            pnlButtons.add(btnFinish);
        }
        JLabel lblRule = new JLabel(sameAsCreator
                ? "Người lập đợt chỉ nhập/lưu số đếm — một quản lý kho khác sẽ bấm \"Hoàn tất\" (UC28)."
                : "Hoàn tất: tồn mỗi lô được cộng/trừ đúng phần chênh lệch; lô lệch phải có lý do.");
        lblRule.setFont(font(Font.PLAIN, 13));
        lblRule.setForeground(TEXT_MUTED);
        JPanel pnlLeft = new JPanel(new BorderLayout(0, 4));
        pnlLeft.setOpaque(false);
        pnlLeft.add(lblSummary, BorderLayout.NORTH);
        pnlLeft.add(lblRule, BorderLayout.CENTER);
        pnlLeft.add(lblError, BorderLayout.SOUTH);
        JPanel pnlFooter = new JPanel(new BorderLayout(12, 0));
        pnlFooter.setOpaque(false);
        pnlFooter.add(pnlLeft, BorderLayout.CENTER);
        pnlFooter.add(pnlButtons, BorderLayout.EAST);
        pnlRoot.add(pnlFooter, BorderLayout.SOUTH);
        return pnlRoot;
    }

    private void wireEvents() {
        btnClose.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> save(false));
        btnFinish.addActionListener(e -> save(true));
        model.addTableModelListener(e -> refresh());
        txtFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void applyFilter() {
        String keyword = txtFilter.getText().trim().toLowerCase(Locale.ROOT);
        TableRowSorter<CountModel> sorter = (TableRowSorter<CountModel>) tblCounts.getRowSorter();
        sorter.setRowFilter(keyword.isEmpty() ? null : new RowFilter<CountModel, Integer>() {
            @Override
            public boolean include(Entry<? extends CountModel, ? extends Integer> entry) {
                StocktakeLineResponse l = model.lines.get(entry.getIdentifier());
                return (l.maMatHang() + " " + l.tenMatHang() + " " + l.soLo()).toLowerCase(Locale.ROOT).contains(keyword);
            }
        });
    }

    private void refresh() {
        int counted = 0;
        int shortage = 0;
        int surplus = 0;
        for (int i = 0; i < model.lines.size(); i++) {
            Integer actual = model.actual.get(i);
            if (actual != null) {
                counted++;
                int diff = actual - model.lines.get(i).soSach();
                if (diff < 0) {
                    shortage -= diff;
                } else {
                    surplus += diff;
                }
            }
        }
        lblSummary.setText("Đã đếm " + counted + "/" + model.lines.size() + " lô · "
                + StocktakePanel.differenceText(shortage, surplus));
    }

    private void save(boolean finish) {
        if (tblCounts.isEditing() && !tblCounts.getCellEditor().stopCellEditing()) {
            return;
        }
        List<StocktakeCountRequest> counts = new ArrayList<>();
        for (int i = 0; i < model.lines.size(); i++) {
            counts.add(new StocktakeCountRequest(model.lines.get(i).maLo(), model.actual.get(i), model.reasons.get(i)));
        }
        if (finish) {
            int answer = JOptionPane.showConfirmDialog(this, "Hoàn tất đợt " + detail.maKiemKe() + "?\n"
                            + lblSummary.getText() + "\nTồn kho từng lô sẽ được điều chỉnh theo chênh lệch. "
                            + "Không thể hoàn tác.",
                    "Hoàn tất kiểm kê", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }
        }
        InventoryActor actor = session.get();
        String ma = detail.maKiemKe();
        setBusy(true);
        lblError.setText(" ");
        new SwingWorker<StocktakeDetailResponse, Void>() {
            private boolean saved;

            @Override
            protected StocktakeDetailResponse doInBackground() {
                StocktakeDetailResponse result = service.saveCounts(actor, ma, counts);
                saved = true;
                return finish ? service.approve(actor, ma) : result;
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    detail = get();
                    changed = true;
                    if (finish) {
                        JOptionPane.showMessageDialog(StocktakeCountDialog.this, "Đã hoàn tất đợt " + ma
                                + " và điều chỉnh tồn kho.", "Kiểm kê", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        lblError.setForeground(GREEN);
                        lblError.setText("Đã lưu số đếm lúc " + InventoryFormat.dateTime(java.time.LocalDateTime.now()) + ".");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    changed |= saved;
                    lblError.setForeground(RED);
                    lblError.setText((saved ? "Đã lưu số đếm nhưng chưa hoàn tất: " : "")
                            + InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        btnSave.setEnabled(!busy);
        btnFinish.setEnabled(!busy);
        btnSave.setText(busy ? "Đang lưu…" : "Lưu số đếm");
    }

    private final class CountModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<StocktakeLineResponse> lines = new ArrayList<>();
        private final List<Integer> actual = new ArrayList<>();
        private final List<String> reasons = new ArrayList<>();

        void load(List<StocktakeLineResponse> data) {
            lines.clear();
            actual.clear();
            reasons.clear();
            for (StocktakeLineResponse l : data) {
                lines.add(l);
                actual.add(l.thucTe());
                reasons.add(l.ghiChu());
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return lines.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == COL_ACTUAL || column == COL_REASON;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            String text = value == null ? "" : value.toString().trim();
            if (column == COL_REASON) {
                reasons.set(row, text.isEmpty() ? null : text);
                fireTableRowsUpdated(row, row);
                return;
            }
            if (column != COL_ACTUAL) {
                return;
            }
            if (text.isEmpty()) {
                actual.set(row, null);
            } else {
                try {
                    int number = Integer.parseInt(text.replace(".", "").replace(",", ""));
                    if (number < 0) {
                        throw new NumberFormatException();
                    }
                    actual.set(row, number);
                    lblError.setText(" ");
                } catch (NumberFormatException e) {
                    lblError.setForeground(RED);
                    lblError.setText("Lô " + lines.get(row).soLo() + ": \"" + text + "\" không phải số lượng hợp lệ.");
                    return;
                }
            }
            fireTableRowsUpdated(row, row);
        }

        @Override
        public Object getValueAt(int row, int column) {
            StocktakeLineResponse l = lines.get(row);
            Integer a = actual.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> l.maMatHang() + " — " + l.tenMatHang();
                case 2 -> l.soLo();
                case 3 -> InventoryFormat.date(l.hanSuDung());
                case 4 -> l.soSach() + " " + l.donViTinh();
                case COL_ACTUAL -> a == null ? "" : String.valueOf(a);
                case COL_DIFF -> a == null ? "—" : a - l.soSach();
                default -> reasons.get(row) == null ? "" : reasons.get(row);
            };
        }
    }

    /** Chênh lệch: âm (thiếu) đỏ, dương (thừa) cam, 0 xanh. */
    private static final class DiffRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, selected, false, row, column);
            lbl.setHorizontalAlignment(JLabel.RIGHT);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            if (value instanceof Integer diff) {
                lbl.setText(diff > 0 ? "+" + diff : String.valueOf(diff));
                lbl.setForeground(diff < 0 ? RED : diff > 0 ? ORANGE : GREEN);
            } else {
                lbl.setForeground(TEXT_MUTED);
            }
            return lbl;
        }
    }
}
