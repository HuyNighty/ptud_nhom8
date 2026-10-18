package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.response.ControlledDrugDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ControlledDrugRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ControlledLotResponse;
import com.quanlynhakhoa.features.inventory.enums.ControlledDrugGroup;
import com.quanlynhakhoa.features.inventory.enums.ExpiryLevel;
import com.quanlynhakhoa.features.inventory.service.ControlledDrugService;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

/** Chi tiết một thuốc kiểm soát đặc biệt: thông tin, trạng thái, các lô còn tồn và phân loại nhóm kiểm soát. */
final class ControlledDrugDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String NO_GROUP = "Chưa phân loại";
    private static final String[] COLUMNS = {"#", "Số lô", "Hạn sử dụng", "Tồn", "Tình trạng hạn"};

    private final transient ControlledDrugService service;
    private final transient Supplier<InventoryActor> session;
    private final transient Consumer<String> openLedger;
    private final String maThuoc;
    private boolean changed;

    private final JPanel pnlBody = new JPanel(new BorderLayout(0, 14));
    private final JComboBox<Object> cmbGroup = new JComboBox<>();
    private final JButton btnSaveGroup = createButton("Lưu phân loại", ButtonKind.OUTLINE);
    private final JLabel lblMessage = new JLabel(" ");

    ControlledDrugDetailDialog(Window owner, ControlledDrugService service, Supplier<InventoryActor> session,
                               String maThuoc, Consumer<String> openLedger) {
        super(owner, "Thuốc kiểm soát " + maThuoc, ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        this.openLedger = Objects.requireNonNull(openLedger);
        this.maThuoc = maThuoc;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pnlBody.setBackground(SURFACE);
        pnlBody.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        pnlBody.add(new JLabel("Đang tải…"), BorderLayout.CENTER);
        setContentPane(pnlBody);
        cmbGroup.addItem(NO_GROUP);
        for (ControlledDrugGroup g : ControlledDrugGroup.values()) {
            cmbGroup.addItem(g);
        }
        styleCombo(cmbGroup);
        btnSaveGroup.addActionListener(e -> saveGroup());
        setSize(900, 560);
        setLocationRelativeTo(owner);
        load(() -> service.getDetail(session.get().roles(), maThuoc));
    }

    boolean changed() {
        return changed;
    }

    private void load(Supplier<ControlledDrugDetailResponse> call) {
        btnSaveGroup.setEnabled(false);
        new SwingWorker<ControlledDrugDetailResponse, Void>() {
            @Override
            protected ControlledDrugDetailResponse doInBackground() {
                return call.get();
            }

            @Override
            protected void done() {
                btnSaveGroup.setEnabled(true);
                try {
                    render(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblMessage.setForeground(RED);
                    lblMessage.setText(InventoryItemPanel.describe(e.getCause()));
                    if (pnlBody.getComponentCount() == 1) {
                        pnlBody.removeAll();
                        pnlBody.add(lblMessage, BorderLayout.CENTER);
                        pnlBody.revalidate();
                    }
                }
            }
        }.execute();
    }

    private void render(ControlledDrugDetailResponse d) {
        ControlledDrugRowResponse t = d.thuoc();
        pnlBody.removeAll();
        JPanel pnlTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlTitle.setOpaque(false);
        JLabel lblTitle = new JLabel(t.maThuoc() + " — " + t.tenDayDu());
        lblTitle.setFont(font(Font.BOLD, 22));
        lblTitle.setForeground(TEXT);
        StatusPill lblStatus = new StatusPill();
        Color[] c = ControlledDrugPanel.severityColors(t.mucDo());
        lblStatus.apply(t.trangThai(), c[0], c[1], c[2]);
        pnlTitle.add(lblTitle);
        pnlTitle.add(lblStatus);

        JPanel pnlInfo = new JPanel(new GridBagLayout());
        pnlInfo.setOpaque(false);
        info(pnlInfo, 0, 0, "Hoạt chất", t.hoatChat() == null ? "—" : t.hoatChat());
        info(pnlInfo, 0, 2, "Đơn vị tính", t.donViTinh());
        info(pnlInfo, 1, 0, "Tồn kho", t.tonKho() + " " + t.donViTinh());
        info(pnlInfo, 1, 2, "Định mức tối thiểu", t.dinhMucToiThieu() + " " + t.donViTinh());
        info(pnlInfo, 2, 0, "Hạn gần nhất", InventoryFormat.date(t.hanGanNhat()));
        cmbGroup.setSelectedItem(t.nhom() == null ? NO_GROUP : t.nhom());
        JPanel pnlGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlGroup.setOpaque(false);
        pnlGroup.add(cmbGroup);
        pnlGroup.add(btnSaveGroup);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 2;
        gc.gridx = 2;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(3, 24, 3, 10);
        pnlInfo.add(fieldLabel("Nhóm kiểm soát"), gc);
        gc.gridx = 3;
        gc.insets = new Insets(0, 0, 0, 0);
        pnlInfo.add(pnlGroup, gc);

        JPanel pnlTop = new JPanel(new BorderLayout(0, 12));
        pnlTop.setOpaque(false);
        pnlTop.add(pnlTitle, BorderLayout.NORTH);
        pnlTop.add(pnlInfo, BorderLayout.CENTER);
        pnlBody.add(pnlTop, BorderLayout.NORTH);

        JTable tblLots = new JTable(new LotModel(d.lo()));
        tblLots.setFocusable(false);
        tblLots.setRowSelectionAllowed(false);
        styleTable(tblLots, 38, new int[] {40, 160, 140, 100, 180}, Set.of(0, 3));
        JLabel lblLots = new JLabel("CÁC LÔ CÒN TỒN (hết hạn sớm nhất trước)");
        lblLots.setFont(font(Font.PLAIN, 13));
        lblLots.setForeground(TEXT_MUTED);
        JPanel pnlLots = new JPanel(new BorderLayout(0, 6));
        pnlLots.setOpaque(false);
        pnlLots.add(lblLots, BorderLayout.NORTH);
        pnlLots.add(tableScroll(tblLots), BorderLayout.CENTER);
        pnlBody.add(pnlLots, BorderLayout.CENTER);

        lblMessage.setFont(font(Font.PLAIN, 13));
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlButtons.setOpaque(false);
        JButton btnLedger = createButton("Nhật ký sử dụng", ButtonKind.OUTLINE);
        btnLedger.addActionListener(e -> {
            dispose();
            openLedger.accept(maThuoc);
        });
        JButton btnClose = createButton("Đóng", ButtonKind.PRIMARY);
        btnClose.addActionListener(e -> dispose());
        pnlButtons.add(btnLedger);
        pnlButtons.add(btnClose);
        JPanel pnlFooter = new JPanel(new BorderLayout(12, 0));
        pnlFooter.setOpaque(false);
        pnlFooter.add(lblMessage, BorderLayout.CENTER);
        pnlFooter.add(pnlButtons, BorderLayout.EAST);
        pnlBody.add(pnlFooter, BorderLayout.SOUTH);
        pnlBody.revalidate();
        pnlBody.repaint();
    }

    private static void info(JPanel target, int row, int column, String label, String value) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;
        gc.gridx = column;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(3, column == 0 ? 0 : 24, 3, 10);
        target.add(fieldLabel(label), gc);
        gc.gridx = column + 1;
        gc.insets = new Insets(3, 0, 3, 0);
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(font(Font.PLAIN, 14));
        lblValue.setForeground(TEXT);
        target.add(lblValue, gc);
    }

    private void saveGroup() {
        ControlledDrugGroup group = cmbGroup.getSelectedItem() instanceof ControlledDrugGroup g ? g : null;
        InventoryActor actor = session.get();
        changed = true;
        lblMessage.setForeground(GREEN);
        lblMessage.setText("Đã lưu phân loại: " + (group == null ? NO_GROUP : group.label()) + ".");
        load(() -> service.classify(actor, maThuoc, group));
    }

    private static final class LotModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<ControlledLotResponse> rows;

        LotModel(List<ControlledLotResponse> rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows.size();
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
        public Object getValueAt(int row, int column) {
            ControlledLotResponse l = rows.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> l.soLo();
                case 2 -> InventoryFormat.date(l.hanSuDung());
                case 3 -> l.tonKho();
                default -> l.mucHan() == ExpiryLevel.CON_HAN ? "Còn hạn" : l.mucHan().label();
            };
        }
    }
}
