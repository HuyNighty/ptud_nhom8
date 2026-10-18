package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.dto.request.InventoryItemRequest;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryItemResponse;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

/** Hộp thoại thêm/sửa một mặt hàng. Lưu qua InventoryItemService trong SwingWorker. */
final class InventoryItemDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String CARD_MEDICINE = "medicine";
    private static final String CARD_SUPPLY = "supply";

    private final transient InventoryItemService service;
    private final transient Supplier<Set<Role>> sessionRoles;
    private final boolean editing;
    private final JLabel lblTitle = new JLabel();

    private final JComboBox<InventoryItemType> cmbItemType = new JComboBox<>(InventoryItemType.values());
    private final JTextField txtCode = styleInput(new JTextField(12));
    private final JButton btnSuggestCode = createButton("Gợi ý mã", ButtonKind.OUTLINE);
    private final JTextField txtName = styleInput(new JTextField(28));
    private final JTextField txtUnit = styleInput(new JTextField(12));
    private final JSpinner spnMinStock = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));
    private final JTextField txtStrength = styleInput(new JTextField(14));
    private final JTextField txtActiveIngredient = styleInput(new JTextField(28));
    private final JCheckBox chkControlled = new JCheckBox("Thuốc kiểm soát đặc biệt");
    private final JTextArea txaDescription = styleInput(new JTextArea(4, 28));
    private final CardLayout typeCards = new CardLayout();
    private final JPanel pnlTypeFields = new JPanel(typeCards);
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnSave = createButton("Lưu", ButtonKind.PRIMARY);
    private final JButton btnCancel = createButton("Hủy", ButtonKind.OUTLINE);
    private final JButton btnSwitchToEdit = createButton("Sửa", ButtonKind.PRIMARY);

    private transient InventoryItemResponse savedItem;

    /**
     * @param existing null = thêm mới; khác null = xem/sửa (mã và loại bị khóa)
     * @param viewOnly mở ở chế độ xem chi tiết; có nút chuyển sang sửa
     */
    InventoryItemDialog(Window owner, InventoryItemService service, Supplier<Set<Role>> sessionRoles,
                        InventoryItemResponse existing, boolean viewOnly) {
        super(owner, titleFor(existing, viewOnly), ModalityType.APPLICATION_MODAL);
        this.service = Objects.requireNonNull(service);
        this.sessionRoles = Objects.requireNonNull(sessionRoles);
        this.editing = existing != null;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        wireEvents();
        if (editing) {
            fill(existing);
            setViewMode(viewOnly, existing);
        } else {
            showTypeFields();
            suggestCode();
        }
        pack();
        setMinimumSize(new Dimension(560, getHeight()));
        setLocationRelativeTo(owner);
    }

    private static String titleFor(InventoryItemResponse existing, boolean viewOnly) {
        if (existing == null) {
            return "Thêm mặt hàng";
        }
        return (viewOnly ? "Chi tiết mặt hàng " : "Sửa mặt hàng ") + existing.ma();
    }

    /** Chế độ xem: khóa các ô nhập, ẩn "Lưu", hiện "Sửa". Chuyển sang sửa ngay trong hộp thoại. */
    private void setViewMode(boolean view, InventoryItemResponse item) {
        for (JComponent input : new JComponent[] {txtName, txtUnit, txtStrength, txtActiveIngredient, txaDescription}) {
            ((javax.swing.text.JTextComponent) input).setEditable(!view);
        }
        spnMinStock.setEnabled(!view);
        chkControlled.setEnabled(!view);
        btnSave.setVisible(!view);
        btnSwitchToEdit.setVisible(view);
        btnCancel.setText(view ? "Đóng" : "Hủy");
        setTitle(titleFor(item, view));
        lblTitle.setText(getTitle());
        getRootPane().setDefaultButton(view ? btnSwitchToEdit : btnSave);
        if (!view) {
            txtName.requestFocusInWindow();
        }
    }

    Optional<InventoryItemResponse> savedItem() {
        return Optional.ofNullable(savedItem);
    }

    // ------------------------------------------------------------------ layout

    private JComponent buildContent() {
        JPanel pnlRoot = new JPanel(new BorderLayout(0, 16));
        pnlRoot.setBackground(SURFACE);
        pnlRoot.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));

        lblTitle.setText(getTitle());
        lblTitle.setFont(font(Font.BOLD, 20));
        lblTitle.setForeground(TEXT);
        pnlRoot.add(lblTitle, BorderLayout.NORTH);

        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setOpaque(false);
        styleCombo(cmbItemType);
        spnMinStock.setFont(font(Font.PLAIN, 14));
        chkControlled.setFont(font(Font.PLAIN, 14));
        chkControlled.setOpaque(false);
        txaDescription.setLineWrap(true);
        txaDescription.setWrapStyleWord(true);

        JPanel pnlCode = new JPanel(new BorderLayout(8, 0));
        pnlCode.setOpaque(false);
        pnlCode.add(txtCode, BorderLayout.CENTER);
        pnlCode.add(btnSuggestCode, BorderLayout.EAST);

        int row = 0;
        addRow(pnlForm, row++, "Loại *", cmbItemType);
        addRow(pnlForm, row++, "Mã *", pnlCode);
        addRow(pnlForm, row++, "Tên *", txtName);
        addRow(pnlForm, row++, "Đơn vị tính *", txtUnit);
        addRow(pnlForm, row++, "Định mức tối thiểu", spnMinStock);

        JPanel pnlMedicine = new JPanel(new GridBagLayout());
        pnlMedicine.setOpaque(false);
        addRow(pnlMedicine, 0, "Hàm lượng", txtStrength);
        addRow(pnlMedicine, 1, "Hoạt chất", txtActiveIngredient);
        addRow(pnlMedicine, 2, "", chkControlled);

        JPanel pnlSupply = new JPanel(new GridBagLayout());
        pnlSupply.setOpaque(false);
        JScrollPane scrDescription = new JScrollPane(txaDescription);
        scrDescription.setBorder(BorderFactory.createEmptyBorder());
        addRow(pnlSupply, 0, "Mô tả", scrDescription);

        pnlTypeFields.setOpaque(false);
        pnlTypeFields.add(pnlMedicine, CARD_MEDICINE);
        pnlTypeFields.add(pnlSupply, CARD_SUPPLY);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        pnlForm.add(pnlTypeFields, gc);
        pnlRoot.add(pnlForm, BorderLayout.CENTER);

        JPanel pnlFooter = new JPanel(new BorderLayout(12, 0));
        pnlFooter.setOpaque(false);
        lblError.setFont(font(Font.PLAIN, 13));
        lblError.setForeground(RED);
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlButtons.setOpaque(false);
        pnlButtons.add(btnCancel);
        pnlButtons.add(btnSwitchToEdit);
        pnlButtons.add(btnSave);
        btnSwitchToEdit.setVisible(false);
        pnlFooter.add(lblError, BorderLayout.CENTER);
        pnlFooter.add(pnlButtons, BorderLayout.EAST);
        pnlRoot.add(pnlFooter, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnSave);
        return pnlRoot;
    }

    private static void addRow(JPanel pnlTarget, int row, String label, JComponent field) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;
        gc.insets = new Insets(5, 0, 5, 12);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0;
        JLabel lblField = fieldLabel(label);
        lblField.setPreferredSize(new Dimension(130, lblField.getPreferredSize().height));
        pnlTarget.add(lblField, gc);
        gc.gridx = 1;
        gc.insets = new Insets(5, 0, 5, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        pnlTarget.add(field, gc);
    }

    private void wireEvents() {
        cmbItemType.addActionListener(e -> {
            showTypeFields();
            if (!editing) {
                suggestCode();
            }
        });
        btnSuggestCode.addActionListener(e -> suggestCode());
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> save());
        btnSwitchToEdit.addActionListener(e -> setViewMode(false, currentSnapshot()));
    }

    private void fill(InventoryItemResponse item) {
        cmbItemType.setSelectedItem(item.loai());
        cmbItemType.setEnabled(false);
        txtCode.setText(item.ma());
        txtCode.setEditable(false);
        btnSuggestCode.setVisible(false);
        txtName.setText(item.ten());
        txtUnit.setText(item.donViTinh());
        spnMinStock.setValue(item.dinhMucToiThieu());
        txtStrength.setText(item.hamLuong() == null ? "" : item.hamLuong());
        txtActiveIngredient.setText(item.hoatChat() == null ? "" : item.hoatChat());
        chkControlled.setSelected(item.kiemSoatDacBiet());
        txaDescription.setText(item.moTa() == null ? "" : item.moTa());
        showTypeFields();
    }

    private InventoryItemResponse currentSnapshot() {
        return new InventoryItemResponse(selectedType(), txtCode.getText(), txtName.getText(), txtUnit.getText(),
                ((Number) spnMinStock.getValue()).intValue(), false, txtStrength.getText(),
                txtActiveIngredient.getText(), chkControlled.isSelected(), txaDescription.getText(),
                0, null, com.quanlynhakhoa.features.inventory.enums.StockLevel.HET_HANG);
    }

    private void showTypeFields() {
        typeCards.show(pnlTypeFields, selectedType() == InventoryItemType.THUOC ? CARD_MEDICINE : CARD_SUPPLY);
    }

    private InventoryItemType selectedType() {
        return (InventoryItemType) cmbItemType.getSelectedItem();
    }

    // ----------------------------------------------------------------- actions

    private void suggestCode() {
        InventoryItemType type = selectedType();
        Set<Role> roles = sessionRoles.get();
        btnSuggestCode.setEnabled(false);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return service.suggestNextCode(roles, type);
            }

            @Override
            protected void done() {
                btnSuggestCode.setEnabled(true);
                try {
                    if (type == selectedType()) {
                        txtCode.setText(get());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void save() {
        if (!commitSpinners(lblError, spnMinStock)) {
            return;
        }
        InventoryItemRequest request = new InventoryItemRequest(
                selectedType(), txtCode.getText(), txtName.getText(), txtUnit.getText(),
                ((Number) spnMinStock.getValue()).intValue(),
                txtStrength.getText(), txtActiveIngredient.getText(), chkControlled.isSelected(),
                txaDescription.getText());
        Set<Role> roles = sessionRoles.get();
        setSaving(true);
        new SwingWorker<InventoryItemResponse, Void>() {
            @Override
            protected InventoryItemResponse doInBackground() {
                return editing ? service.update(roles, request) : service.create(roles, request);
            }

            @Override
            protected void done() {
                try {
                    savedItem = get();
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    setSaving(false);
                    lblError.setText(InventoryItemPanel.describe(e.getCause()));
                }
            }
        }.execute();
    }

    private void setSaving(boolean saving) {
        btnSave.setEnabled(!saving);
        btnCancel.setEnabled(!saving);
        btnSave.setText(saving ? "Đang lưu…" : "Lưu");
        if (saving) {
            lblError.setText(" ");
        }
    }
}
