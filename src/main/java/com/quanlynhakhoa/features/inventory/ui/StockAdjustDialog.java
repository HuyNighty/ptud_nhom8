package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.request.StockMonitorAdjustRequest;
import com.quanlynhakhoa.features.inventory.dto.response.StockMonitorRowResponse;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.StockMonitorService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

/** Điều chỉnh nhanh định mức, vị trí lưu trữ và cờ "đã đặt hàng" của một mặt hàng (UC36). Không đổi số tồn. */
final class StockAdjustDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final transient StockMonitorService service;
    private final transient Supplier<InventoryActor> session;
    private final transient StockMonitorRowResponse item;
    private transient StockMonitorRowResponse saved;

    private final JSpinner spnMinimum;
    private final JComboBox<String> cmbLocation = new JComboBox<>();
    private final JCheckBox chkOrdered = new JCheckBox("Đã đặt hàng với nhà cung cấp");
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnSave = createButton("Lưu điều chỉnh", ButtonKind.PRIMARY);

    StockAdjustDialog(Window owner, StockMonitorService service, Supplier<InventoryActor> session,
                      StockMonitorRowResponse item, List<String> knownLocations) {
        super(owner, "Điều chỉnh nhanh " + item.ma(), ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        this.item = item;
        spnMinimum = new JSpinner(new SpinnerNumberModel(item.dinhMucToiThieu(), 0, 1_000_000, 1));
        cmbLocation.setEditable(true);
        knownLocations.forEach(cmbLocation::addItem);
        cmbLocation.setSelectedItem(item.viTriLuuTru() == null ? "" : item.viTriLuuTru());
        chkOrdered.setSelected(item.daDatHang());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        pack();
        setMinimumSize(new Dimension(560, getHeight()));
        setLocationRelativeTo(owner);
    }

    Optional<StockMonitorRowResponse> saved() {
        return Optional.ofNullable(saved);
    }

    private JComponent buildContent() {
        JPanel pnlRoot = new JPanel(new BorderLayout(0, 14));
        pnlRoot.setBackground(SURFACE);
        pnlRoot.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        JLabel lblTitle = new JLabel(item.ma() + " — " + item.ten());
        lblTitle.setFont(font(Font.BOLD, 19));
        lblTitle.setForeground(TEXT);
        JLabel lblStock = new JLabel("Tồn thực tế " + item.tonThucTe() + " " + item.donViTinh() + " · tồn còn hạn "
                + item.tonKhaDung() + " · " + StockMonitorPanel.levelLabel(item.mucTon()));
        lblStock.setFont(font(Font.PLAIN, 14));
        lblStock.setForeground(TEXT_MUTED);
        JPanel pnlTop = new JPanel(new BorderLayout(0, 6));
        pnlTop.setOpaque(false);
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        pnlTop.add(lblStock, BorderLayout.CENTER);
        pnlRoot.add(pnlTop, BorderLayout.NORTH);

        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(5, 0, 5, 12);
        gc.gridx = 0;
        gc.gridy = 0;
        pnlForm.add(fieldLabel("Định mức tối thiểu (" + item.donViTinh() + ")"), gc);
        gc.gridy = 1;
        pnlForm.add(fieldLabel("Vị trí lưu trữ"), gc);
        gc.gridx = 1;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        spnMinimum.setFont(font(Font.PLAIN, 14));
        spnMinimum.setPreferredSize(new Dimension(140, 34));
        pnlForm.add(spnMinimum, gc);
        gc.gridy = 1;
        cmbLocation.setFont(font(Font.PLAIN, 14));
        cmbLocation.setPreferredSize(new Dimension(260, 34));
        pnlForm.add(cmbLocation, gc);
        gc.gridy = 2;
        chkOrdered.setOpaque(false);
        chkOrdered.setFont(font(Font.PLAIN, 14));
        pnlForm.add(chkOrdered, gc);
        gc.gridx = 0;
        gc.gridy = 3;
        gc.gridwidth = 2;
        JLabel lblHint = new JLabel("Số tồn chỉ thay đổi qua phiếu nhập / xuất / kiểm kê / hủy để giữ lịch sử chứng từ.");
        lblHint.setFont(font(Font.PLAIN, 13));
        lblHint.setForeground(TEXT_MUTED);
        pnlForm.add(lblHint, gc);
        pnlRoot.add(pnlForm, BorderLayout.CENTER);

        lblError.setFont(font(Font.PLAIN, 13));
        lblError.setForeground(RED);
        JButton btnCancel = createButton("Hủy", ButtonKind.OUTLINE);
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> save());
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlButtons.setOpaque(false);
        pnlButtons.add(btnCancel);
        pnlButtons.add(btnSave);
        JPanel pnlFooter = new JPanel(new BorderLayout(12, 0));
        pnlFooter.setOpaque(false);
        pnlFooter.add(lblError, BorderLayout.CENTER);
        pnlFooter.add(pnlButtons, BorderLayout.EAST);
        pnlRoot.add(pnlFooter, BorderLayout.SOUTH);
        return pnlRoot;
    }

    private void save() {
        if (!commitSpinners(lblError, spnMinimum)) {
            return;
        }
        Object location = cmbLocation.getEditor().getItem();
        StockMonitorAdjustRequest request = new StockMonitorAdjustRequest((Integer) spnMinimum.getValue(),
                location == null ? null : location.toString(), chkOrdered.isSelected());
        InventoryActor actor = session.get();
        btnSave.setEnabled(false);
        lblError.setText(" ");
        new SwingWorker<StockMonitorRowResponse, Void>() {
            @Override
            protected StockMonitorRowResponse doInBackground() {
                return service.adjust(actor, item.loai(), item.ma(), request);
            }

            @Override
            protected void done() {
                btnSave.setEnabled(true);
                try {
                    saved = get();
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }
}
