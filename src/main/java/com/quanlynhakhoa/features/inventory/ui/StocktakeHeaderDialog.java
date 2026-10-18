package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.inventory.dto.request.StocktakeRequest;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeDetailResponse;
import com.quanlynhakhoa.features.inventory.enums.StocktakeScope;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.StocktakeService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

/**
 * Tạo đợt kiểm kê (chọn ngày, phạm vi → chụp tồn sổ sách) hoặc sửa đợt:
 * đang kiểm kê sửa được ngày + ghi chú; đã hoàn thành chỉ sửa ghi chú.
 */
final class StocktakeHeaderDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final transient StocktakeService service;
    private final transient Supplier<InventoryActor> session;
    private final transient StocktakeDetailResponse existing;

    private final JSpinner spnDate = new JSpinner(new SpinnerDateModel());
    private final JComboBox<StocktakeScope> cmbScope = new JComboBox<>(StocktakeScope.values());
    private final JTextField txtNote = styleInput(new JTextField(28));
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnSave;
    private final JButton btnCancel = createButton("Hủy", ButtonKind.OUTLINE);

    private transient StocktakeDetailResponse saved;

    StocktakeHeaderDialog(Window owner, StocktakeService service, Supplier<InventoryActor> session,
                          StocktakeDetailResponse existing) {
        super(owner, existing == null ? "Tạo đợt kiểm kê" : "Sửa đợt kiểm kê " + existing.maKiemKe(),
                ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.session = Objects.requireNonNull(session);
        this.existing = existing;
        btnSave = createButton(existing == null ? "Tạo đợt & chụp tồn sổ sách" : "Lưu", ButtonKind.PRIMARY);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> save());
        pack();
        setSize(Math.max(getWidth(), 620), getHeight());
        setLocationRelativeTo(owner);
    }

    Optional<StocktakeDetailResponse> saved() {
        return Optional.ofNullable(saved);
    }

    private JComponent buildContent() {
        JPanel pnlRoot = new JPanel(new BorderLayout(0, 14));
        pnlRoot.setBackground(SURFACE);
        pnlRoot.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));
        JLabel lblTitle = new JLabel(getTitle());
        lblTitle.setFont(font(Font.BOLD, 20));
        lblTitle.setForeground(TEXT);
        pnlRoot.add(lblTitle, BorderLayout.NORTH);

        spnDate.setEditor(new JSpinner.DateEditor(spnDate, "dd/MM/yyyy"));
        spnDate.setFont(font(Font.PLAIN, 14));
        styleCombo(cmbScope);
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setOpaque(false);
        if (existing == null) {
            spnDate.setValue(toDate(LocalDate.now()));
            addRow(pnlForm, 0, "Ngày kiểm kê *", spnDate);
            addRow(pnlForm, 1, "Phạm vi *", cmbScope);
            addRow(pnlForm, 2, "Ghi chú", txtNote);
            JLabel lblHint = new JLabel("<html>Hệ thống chụp tồn sổ sách của mọi lô còn tồn trong phạm vi. Mỗi lúc chỉ "
                    + "có một đợt đang kiểm kê; người hoàn tất phải khác người lập.</html>");
            lblHint.setFont(font(Font.PLAIN, 13));
            lblHint.setForeground(TEXT_MUTED);
            addRow(pnlForm, 3, "", lblHint);
        } else {
            spnDate.setValue(toDate(existing.ngayKiemKe()));
            boolean open = existing.trangThai() == VoucherStatus.CHO_XU_LY;
            spnDate.setEnabled(open);
            txtNote.setText(existing.ghiChu() == null ? "" : existing.ghiChu());
            addRow(pnlForm, 0, "Ngày kiểm kê", spnDate);
            addRow(pnlForm, 1, "Phạm vi", new JLabel(existing.phamVi().label()));
            addRow(pnlForm, 2, "Ghi chú", txtNote);
            if (!open) {
                JLabel lblHint = new JLabel("Đợt đã hoàn tất — chỉ sửa được ghi chú, tồn kho giữ nguyên.");
                lblHint.setFont(font(Font.PLAIN, 13));
                lblHint.setForeground(TEXT_MUTED);
                addRow(pnlForm, 3, "", lblHint);
            }
        }
        pnlRoot.add(pnlForm, BorderLayout.CENTER);

        lblError.setFont(font(Font.PLAIN, 13));
        lblError.setForeground(RED);
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

    private static void addRow(JPanel target, int row, String label, JComponent field) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(5, 0, 5, 14);
        target.add(fieldLabel(label), gc);
        gc.gridx = 1;
        gc.insets = new Insets(5, 0, 5, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        field.setFont(font(Font.PLAIN, 14));
        target.add(field, gc);
    }

    private void save() {
        if (!commitSpinners(lblError, spnDate)) {
            return;
        }
        LocalDate date = toLocalDate((Date) spnDate.getValue());
        StocktakeScope scope = (StocktakeScope) cmbScope.getSelectedItem();
        String note = txtNote.getText();
        InventoryActor actor = session.get();
        btnSave.setEnabled(false);
        lblError.setText(" ");
        new SwingWorker<StocktakeDetailResponse, Void>() {
            @Override
            protected StocktakeDetailResponse doInBackground() {
                return existing == null
                        ? service.create(actor, new StocktakeRequest(date, scope, note))
                        : service.update(actor, existing.maKiemKe(), date, note);
            }

            @Override
            protected void done() {
                try {
                    saved = get();
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    btnSave.setEnabled(true);
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
