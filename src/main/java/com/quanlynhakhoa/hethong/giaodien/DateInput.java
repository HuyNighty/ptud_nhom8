package com.quanlynhakhoa.hethong.giaodien;

import com.quanlynhakhoa.view.benhnhan.ChonNgayDialog;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Calendar beside the existing input; the original format and validation stay intact. */
public final class DateInput {
    private DateInput() { }

    public static JPanel iso(JTextField input, String caption) {
        return wrap(input, caption, DateTimeFormatter.ISO_LOCAL_DATE,
                LocalDate.of(1900, 1, 1), LocalDate.of(9999, 12, 31));
    }

    public static JPanel wrap(final JTextField input, final String caption,
                             final DateTimeFormatter format, final LocalDate minimum,
                             final LocalDate maximum) {
        final JButton calendar = new JButton("Lịch");
        calendar.setToolTipText("Chọn " + caption);
        calendar.getAccessibleContext().setAccessibleName("Chọn lịch: " + caption);
        calendar.setEnabled(input.isEnabled());
        calendar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) {
                if (!input.isEnabled()) return;
                LocalDate current = null;
                try { current = LocalDate.parse(input.getText().trim(), format); }
                catch (DateTimeParseException ignored) { /* Calendar also repairs invalid input. */ }
                LocalDate chosen = ChonNgayDialog.chon(SwingUtilities.getWindowAncestor(input),
                        "Chọn " + caption, current, minimum, maximum);
                if (chosen != null) input.setText(format.format(chosen));
            }
        });
        input.addPropertyChangeListener("enabled", new java.beans.PropertyChangeListener() {
            @Override public void propertyChange(java.beans.PropertyChangeEvent event) {
                calendar.setEnabled(input.isEnabled());
            }
        });
        JPanel control = new JPanel(new BorderLayout(4, 0));
        control.setOpaque(false);
        control.add(input, BorderLayout.CENTER);
        control.add(calendar, BorderLayout.EAST);
        return control;
    }
}
