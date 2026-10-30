package com.quanlynhakhoa.benhnhan.view;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.benhnhan.controller.BenhNhanController;
import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.Cursor;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Real SwingWorker/controller/view regressions; all dialogs stay hidden; no database. */
public final class BenhNhanAsyncSelfTest {
    private static int checks;

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static <T> T edt(Callable<T> task) throws Exception {
        FutureTask<T> future = new FutureTask<>(task);
        SwingUtilities.invokeAndWait(future);
        return future.get(8, TimeUnit.SECONDS);
    }

    private static void await(BooleanSupplier condition, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (!edt(condition::getAsBoolean)) {
            if (System.nanoTime() > deadline) throw new AssertionError(message);
            Thread.sleep(10);
        }
    }

    private static BenhNhan patient(String id, String name) {
        return new BenhNhan(id, name, LocalDate.of(2000, 1, 1),
                "NAM", "0909123456", null, null, null);
    }

    private static ArrayList<BenhNhan> rows(BenhNhan... patients) {
        return new ArrayList<>(List.of(patients));
    }

    private static BenhNhanController controller(TestPanel panel) {
        for (var listener : panel.getBtnLoc().getActionListeners()) {
            if (listener instanceof BenhNhanController c) return c;
        }
        throw new AssertionError("Controller missing");
    }

    private static void refresh(TestPanel panel, String keyword, int gender, String selected) throws Exception {
        edt(() -> {
            panel.getTxtTimKiem().setText(keyword);
            panel.getCmbGioiTinh().setSelectedIndex(gender);
            Method method = BenhNhanController.class.getDeclaredMethod("docDuLieuVaoTable", String.class);
            method.setAccessible(true);
            method.invoke(controller(panel), selected);
            return null;
        });
    }

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) throw new AssertionError("Hidden-dialog tests require a desktop");
        try {
            pendingAndStaleDetails();
            failedLoadAndNullSelection();
            for (String phase : List.of("duplicate", "create", "update")) saveTimeout(phase);
            System.out.println("PASS: BENH NHAN ASYNC (" + checks + " checks; hidden Swing, no DB)");
        } finally {
            edt(() -> {
                for (Window window : Window.getWindows()) window.dispose();
                return null;
            });
        }
    }

    private static void pendingAndStaleDetails() throws Exception {
        FakeService service = new FakeService();
        Gate<ArrayList<BenhNhan>> first = new Gate<>(rows(patient("BN000001", "Old List")));
        BenhNhan selected = patient("BN000003", "Selected Patient");
        Gate<ArrayList<BenhNhan>> latest = new Gate<>(rows(selected, patient("BN000004", "Other Patient")));
        service.loads.add(first);
        service.loads.add(latest);
        TestPanel panel = edt(() -> new TestPanel(service));
        first.awaitStarted();
        refresh(panel, "Middle", 1, "BN000002");
        refresh(panel, "Latest", 2, selected.getMaBenhNhan());
        edt(() -> { panel.getTxtTimKiem().setText("Unsubmitted edit"); return null; });
        first.release.countDown();
        latest.awaitStarted();
        check(service.loadArguments.equals(List.of("|", "Latest|NU")), "latest filter snapshot wins");
        check(edt(() -> panel.rendered.isEmpty()), "superseded list never rendered");
        check(edt(() -> !panel.getBtnTaiLai().isEnabled()), "loading stays active through pending refresh");
        Gate<BenhNhan> oldDetail = new Gate<>(patient(selected.getMaBenhNhan(), "Stale Detail"));
        service.details.add(oldDetail);
        latest.release.countDown();
        oldDetail.awaitStarted();
        check(edt(() -> panel.getTblBenhNhan().getSelectedRow() == 0), "pending patient selection retained");
        check(edt(() -> panel.getBnDangXem() == null), "detail waits for worker");

        BenhNhan newest = patient(selected.getMaBenhNhan(), "Newest Detail");
        Gate<ArrayList<BenhNhan>> reload = new Gate<>(rows(newest));
        Gate<BenhNhan> newDetail = new Gate<>(newest);
        service.loads.add(reload);
        service.details.add(newDetail);
        refresh(panel, "Again", 0, newest.getMaBenhNhan());
        reload.awaitStarted();
        reload.release.countDown();
        newDetail.awaitStarted();
        newDetail.release.countDown();
        await(() -> panel.getBnDangXem() == newest && panel.getBtnTaiLai().isEnabled(), "new detail not rendered");
        oldDetail.release.countDown();
        oldDetail.awaitReturned();
        // SwingWorker batches done callbacks on a Swing timer; let the older callback drain.
        CountDownLatch drained = new CountDownLatch(1);
        edt(() -> {
            Timer timer = new Timer(200, e -> drained.countDown());
            timer.setRepeats(false);
            timer.start();
            return null;
        });
        check(drained.await(8, TimeUnit.SECONDS), "EDT completion barrier");
        check(edt(() -> panel.getBnDangXem() == newest), "late detail for the same ID cannot overwrite new data");
        check(edt(() -> panel.details.stream().noneMatch(b -> "Stale Detail".equals(b.getHoTen()))),
                "stale detail was never rendered");
        check(service.maxLoads.get() == 1, "list loads never overlap");
        check(edt(() -> panel.errors == 0 && panel.rendered.size() == 2), "only current lists rendered");
    }

    private static void failedLoadAndNullSelection() throws Exception {
        FakeService service = new FakeService();
        Gate<ArrayList<BenhNhan>> failed = new Gate<>(null);
        failed.failure = new SQLTimeoutException("Synthetic list timeout");
        Gate<ArrayList<BenhNhan>> pending = new Gate<>(rows(patient("BN000008", "Pending Patient")));
        service.loads.add(failed);
        service.loads.add(pending);
        TestPanel panel = edt(() -> new TestPanel(service));
        failed.awaitStarted();
        refresh(panel, "Superseded", 0, "BN000008");
        refresh(panel, "No selection", 0, null);
        failed.release.countDown();
        pending.awaitStarted();
        pending.release.countDown();
        await(() -> panel.getBtnTaiLai().isEnabled(), "pending load after failure did not complete");
        check(service.loadArguments.equals(List.of("|", "No selection|")), "pending refresh runs after failure");
        check(edt(() -> panel.getTblBenhNhan().getSelectedRow() == -1 && panel.getBnDangXem() == null),
                "latest null selection replaces previous requested patient");
        check(edt(() -> panel.errors == 0), "superseded load error does not interrupt pending refresh");

        Gate<ArrayList<BenhNhan>> currentFailure = new Gate<>(null);
        currentFailure.failure = new SQLTimeoutException("Synthetic current timeout");
        service.loads.add(currentFailure);
        refresh(panel, "Failure", 0, null);
        currentFailure.awaitStarted();
        currentFailure.release.countDown();
        await(() -> panel.errors == 1 && panel.getBtnTaiLai().isEnabled(), "load failure left controls disabled");
        Gate<ArrayList<BenhNhan>> retry = new Gate<>(rows());
        service.loads.add(retry);
        refresh(panel, "Retry", 0, null);
        retry.awaitStarted();
        retry.release.countDown();
        await(() -> panel.getBtnTaiLai().isEnabled() && panel.rendered.size() == 2, "load retry did not complete");
        check(service.maxLoads.get() == 1, "failed load does not overlap pending or retry");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = BenhNhanController.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void saveTimeout(String phase) throws Exception {
        FakeService service = new FakeService();
        Gate<ArrayList<BenhNhan>> initial = new Gate<>(rows());
        initial.release.countDown();
        service.loads.add(initial);
        Gate<?> timeoutGate;
        if (phase.equals("duplicate")) {
            service.duplicate.failure = new SQLTimeoutException("Synthetic duplicate timeout");
            timeoutGate = service.duplicate;
        } else {
            service.duplicate.release.countDown();
            service.write.failure = new SQLTimeoutException("Synthetic write timeout");
            timeoutGate = service.write;
        }
        TestPanel panel = edt(() -> new TestPanel(service));
        await(() -> panel.getBtnTaiLai().isEnabled(), "initial list did not complete");
        BenhNhanController controller = controller(panel);
        TestDialog dialog = edt(() -> {
            TestDialog d = new TestDialog();
            d.addNotify(); // Displayable so production cleanup runs, but never visible.
            setField(controller, "winBenhNhan", d);
            setField(controller, "maDangSua", phase.equals("update") ? "BN000001" : null);
            controller.actionPerformed(new ActionEvent(d.getBtnLuu(), ActionEvent.ACTION_PERFORMED, "save"));
            return d;
        });
        try {
            timeoutGate.awaitStarted();
            check(edt(() -> !dialog.getBtnLuu().isEnabled() && !dialog.getBtnHuy().isEnabled()
                    && dialog.getDefaultCloseOperation() == JDialog.DO_NOTHING_ON_CLOSE), phase + " saving guarded");
            edt(() -> {
                controller.actionPerformed(new ActionEvent(dialog.getBtnLuu(), ActionEvent.ACTION_PERFORMED, "save"));
                return null;
            });
            check(service.duplicateCalls.get() == 1, phase + " duplicate save event ignored");
            check(!edt(dialog::isVisible), "test dialog stays hidden; EDT remains responsive");
            timeoutGate.release.countDown();
            await(() -> dialog.errors == 1, phase + " timeout not reported");
            check(edt(() -> dialog.getBtnLuu().isEnabled() && dialog.getBtnHuy().isEnabled()
                    && dialog.getBtnChonNgay().isEnabled()
                    && dialog.getDefaultCloseOperation() == JDialog.DISPOSE_ON_CLOSE
                    && dialog.getCursor().getType() == Cursor.DEFAULT_CURSOR), phase + " saving UI released");
            check(edt(dialog::isDisplayable), phase + " timeout keeps form open");
            edt(() -> {
                controller.actionPerformed(new ActionEvent(dialog.getBtnLuu(), ActionEvent.ACTION_PERFORMED, "retry"));
                return null;
            });
            await(() -> dialog.errors == 2, phase + " saving state blocked retry");
            check(service.duplicateCalls.get() == 2, phase + " retry accepted");
            check(service.writeCalls.get() == (phase.equals("duplicate") ? 0 : 2), phase + " expected write attempts");
        } finally {
            edt(() -> { dialog.dispose(); return null; });
        }
    }

    private static final class Gate<T> {
        final T value;
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch returned = new CountDownLatch(1);
        SQLException failure;
        Gate(T value) { this.value = value; }
        T run() throws SQLException {
            if (SwingUtilities.isEventDispatchThread()) throw new AssertionError("SQL operation ran on EDT");
            started.countDown();
            try {
                if (!release.await(8, TimeUnit.SECONDS)) throw new AssertionError("Worker gate timed out");
                if (failure != null) throw failure;
                return value;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException(e);
            } finally {
                returned.countDown();
            }
        }
        void awaitStarted() throws InterruptedException {
            if (!started.await(8, TimeUnit.SECONDS)) throw new AssertionError("Worker did not start");
        }
        void awaitReturned() throws InterruptedException {
            if (!returned.await(8, TimeUnit.SECONDS)) throw new AssertionError("Worker did not return");
        }
    }

    private static final class FakeService extends BenhNhanService {
        final LinkedBlockingQueue<Gate<ArrayList<BenhNhan>>> loads = new LinkedBlockingQueue<>();
        final LinkedBlockingQueue<Gate<BenhNhan>> details = new LinkedBlockingQueue<>();
        final List<String> loadArguments = new CopyOnWriteArrayList<>();
        final AtomicInteger activeLoads = new AtomicInteger();
        final AtomicInteger maxLoads = new AtomicInteger();
        final AtomicInteger duplicateCalls = new AtomicInteger();
        final AtomicInteger writeCalls = new AtomicInteger();
        final Gate<ArrayList<BenhNhan>> duplicate = new Gate<>(rows());
        final Gate<String> write = new Gate<>("BN000001");
        FakeService() {
            super(DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTHETIC", Role.RECEPTIONIST, false)),
                    () -> { throw new AssertionError("Unexpected database connection"); });
        }
        @Override public ArrayList<BenhNhan> timKiem(String keyword, String gender) throws SQLException {
            int active = activeLoads.incrementAndGet();
            maxLoads.accumulateAndGet(active, Math::max);
            try {
                loadArguments.add(keyword + "|" + gender);
                Gate<ArrayList<BenhNhan>> gate = loads.poll();
                if (gate == null) throw new AssertionError("Unexpected list load");
                return gate.run();
            } finally { activeLoads.decrementAndGet(); }
        }
        @Override public BenhNhan xemChiTiet(String id) throws SQLException {
            Gate<BenhNhan> gate = details.poll();
            if (gate == null || !id.equals(gate.value.getMaBenhNhan())) throw new AssertionError("Unexpected detail ID");
            return gate.run();
        }
        @Override public ArrayList<BenhNhan> timNghiTrung(BenhNhan bn) throws SQLException {
            duplicateCalls.incrementAndGet();
            return duplicate.run();
        }
        @Override public String themBenhNhan(BenhNhan bn) throws SQLException {
            writeCalls.incrementAndGet();
            return write.run();
        }
        @Override public void capNhatBenhNhan(BenhNhan bn) throws SQLException {
            writeCalls.incrementAndGet();
            write.run();
        }
    }

    private static final class TestPanel extends BenhNhanPanel {
        final List<ArrayList<BenhNhan>> rendered = new ArrayList<>();
        final List<BenhNhan> details = new ArrayList<>();
        int errors;
        TestPanel(FakeService service) { super(service); }
        @Override public void docDuLieuVaoTable(ArrayList<BenhNhan> rows) {
            check(SwingUtilities.isEventDispatchThread(), "table rendered on EDT");
            rendered.add(rows);
            super.docDuLieuVaoTable(rows);
        }
        @Override public void hienChiTiet(BenhNhan bn) {
            check(SwingUtilities.isEventDispatchThread(), "detail rendered on EDT");
            if (bn != null) details.add(bn);
            super.hienChiTiet(bn);
        }
        @Override public void hienLoi(String message) { errors++; }
    }

    private static final class TestDialog extends BenhNhanDialog {
        int errors;
        TestDialog() { super(null, patient("BN000001", "Synthetic Patient")); }
        @Override public void hienLoi(String message) {
            check(SwingUtilities.isEventDispatchThread(), "save error shown on EDT");
            check(getBtnLuu().isEnabled() && getBtnHuy().isEnabled(), "saving released before error display");
            errors++;
        }
    }
}
