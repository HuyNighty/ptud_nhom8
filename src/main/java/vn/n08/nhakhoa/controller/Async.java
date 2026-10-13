package vn.n08.nhakhoa.controller;

import vn.n08.nhakhoa.view.component.DataTable;
import vn.n08.nhakhoa.view.component.Dialogs;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Chạy lời gọi Service ngoài luồng giao diện (SwingWorker) để màn hình không bị treo khi truy vấn CSDL,
 * rồi trả kết quả / lỗi về luồng giao diện.
 */
public final class Async {
    private Async() {}

    public static <R> void run(Component owner, Supplier<R> work, Consumer<R> ok) {
        run(owner, work, ok, e -> Dialogs.error(owner, ErrorHandler.message(e)));
    }

    public static <R> void run(Component owner, Supplier<R> work, Consumer<R> ok, Consumer<Throwable> fail) {
        Window w = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        if (w != null) w.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<R, Void>() {
            @Override protected R doInBackground() { return work.get(); }

            @Override protected void done() {
                if (w != null) w.setCursor(Cursor.getDefaultCursor());
                R r;
                try {
                    r = get();
                } catch (Exception e) {
                    fail.accept(ErrorHandler.unwrap(e));
                    return;
                }
                ok.accept(r);
            }
        }.execute();
    }

    /** Tải danh sách vào bảng với trạng thái đang tải / lỗi. */
    public static <T> void load(DataTable<T> table, Supplier<List<T>> work) {
        load(table, work, r -> { });
    }

    public static <T> void load(DataTable<T> table, Supplier<List<T>> work, Consumer<List<T>> after) {
        if (table.rows().isEmpty()) table.loading();
        run(table, work, r -> {
            table.setRows(r);
            after.accept(r);
        }, e -> table.error(ErrorHandler.message(e)));
    }
}
