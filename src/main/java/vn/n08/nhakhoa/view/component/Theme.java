package vn.n08.nhakhoa.view.component;

import vn.n08.nhakhoa.model.Tone;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Set;

/** Bảng màu, phông chữ và mặc định Swing — theo token của prototype v0.3. */
public final class Theme {
    public static final Color BG = hex("#F3F5F9"), SURFACE = Color.WHITE, LINE = hex("#E4E8EF"), LINE2 = hex("#EEF1F6"), HEAD = hex("#F5F7FA");
    public static final Color INK = hex("#0F172A"), INK2 = hex("#334155"), MUTED = hex("#64748B"), FAINT = hex("#94A3B8");
    public static final Color PRI = hex("#2563EB"), PRI_INK = hex("#1D4ED8"), PRI_SOFT = hex("#E8EEFD"), NAV_ACT = hex("#E5EBF8");
    public static final Color LOGO = hex("#3B5594"), ADMIN = hex("#1E293B");
    public static final Color GREEN = hex("#15803D"), GREEN_S = hex("#E5F5EA"), AMBER = hex("#B45309"), AMBER_S = hex("#FEF3D7");
    public static final Color RED = hex("#DC2626"), RED_S = hex("#FDECEC"), PURPLE = hex("#7C3AED"), PURPLE_S = hex("#F1EAFE");
    public static final Color TEAL = hex("#0F766E"), TEAL_S = hex("#E1F4F1"), GRAY = hex("#475569"), GRAY_S = hex("#EEF1F5");
    public static final Color ORANGE = hex("#C2410C"), ORANGE_S = hex("#FFEDD5");

    public static final String FAMILY = pickFamily();
    public static final Font BODY = new Font(FAMILY, Font.PLAIN, 13);
    public static final Font BODY_B = BODY.deriveFont(Font.BOLD);
    public static final Font SMALL = new Font(FAMILY, Font.PLAIN, 12);
    public static final Font SMALL_B = SMALL.deriveFont(Font.BOLD);
    public static final Font H1 = new Font(FAMILY, Font.BOLD, 20);
    public static final Font H2 = new Font(FAMILY, Font.BOLD, 15);
    public static final Font BIG = new Font(FAMILY, Font.BOLD, 24);

    private Theme() {}

    public static Color hex(String h) { return Color.decode(h); }

    public static Color fg(Tone t) {
        return switch (t) {
            case GREEN -> GREEN; case AMBER -> AMBER; case RED -> RED; case BLUE -> PRI_INK; case PURPLE -> PURPLE;
            case TEAL -> TEAL; case ORANGE -> ORANGE; default -> GRAY;
        };
    }

    public static Color bg(Tone t) {
        return switch (t) {
            case GREEN -> GREEN_S; case AMBER -> AMBER_S; case RED -> RED_S; case BLUE -> PRI_SOFT; case PURPLE -> PURPLE_S;
            case TEAL -> TEAL_S; case ORANGE -> ORANGE_S; default -> GRAY_S;
        };
    }

    private static String pickFamily() {
        Set<String> have = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String f : new String[]{"Segoe UI", "Inter", "Noto Sans", "DejaVu Sans", "Arial"}) if (have.contains(f)) return f;
        return Font.SANS_SERIF;
    }

    /** Cài mặc định cho toàn ứng dụng (gọi một lần trước khi tạo cửa sổ). */
    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignore) {
            // giữ L&F mặc định
        }
        FontUIResource f = new FontUIResource(BODY);
        for (Object k : UIManager.getLookAndFeelDefaults().keySet().toArray()) {
            if (k != null && k.toString().endsWith(".font")) UIManager.put(k, f);
        }
        UIManager.put("Panel.background", new ColorUIResource(BG));
        UIManager.put("OptionPane.background", new ColorUIResource(SURFACE));
        UIManager.put("OptionPane.messageFont", f);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.selectionBackground", new ColorUIResource(PRI_SOFT));
        UIManager.put("Table.selectionForeground", new ColorUIResource(INK));
        UIManager.put("Table.gridColor", new ColorUIResource(LINE2));
        UIManager.put("TableHeader.background", new ColorUIResource(HEAD));
        UIManager.put("TableHeader.foreground", new ColorUIResource(MUTED));
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        UIManager.put("PasswordField.border", UIManager.get("TextField.border"));
        UIManager.put("TextArea.border", BorderFactory.createEmptyBorder(6, 8, 6, 8));
        UIManager.put("ComboBox.background", new ColorUIResource(SURFACE));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(PRI_SOFT));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(INK));
        UIManager.put("TabbedPane.selected", new ColorUIResource(SURFACE));
        UIManager.put("TabbedPane.contentAreaColor", new ColorUIResource(SURFACE));
        UIManager.put("TabbedPane.background", new ColorUIResource(BG));
        UIManager.put("TabbedPane.borderHightlightColor", new ColorUIResource(LINE));
        UIManager.put("TabbedPane.darkShadow", new ColorUIResource(LINE));
        UIManager.put("TabbedPane.shadow", new ColorUIResource(LINE));
        UIManager.put("TabbedPane.light", new ColorUIResource(LINE));
        UIManager.put("TabbedPane.highlight", new ColorUIResource(LINE));
        UIManager.put("TabbedPane.focus", new ColorUIResource(SURFACE));
        UIManager.put("TabbedPane.tabInsets", new java.awt.Insets(6, 14, 6, 14));
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(LINE));
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Button.focus", new ColorUIResource(SURFACE));
        UIManager.put("CheckBox.background", new ColorUIResource(SURFACE));
        UIManager.put("ToolTip.background", new ColorUIResource(ADMIN));
        UIManager.put("ToolTip.foreground", new ColorUIResource(Color.WHITE));
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(4, 8, 4, 8));
    }
}
