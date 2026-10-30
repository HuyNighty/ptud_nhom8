package com.quanlynhakhoa.billing.controller;

import com.quanlynhakhoa.security.DesktopSession;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

/** All callbacks run on EDT. An invalid session never receives a result or error payload. */
public abstract class SessionWorker<T> extends SwingWorker<T, Void> {
    private final DesktopSession session;
    protected SessionWorker(DesktopSession session) { this.session = session; }
    @Override protected final void done() {
        T result = null;
        Throwable failure = null;
        try { result = get(); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); failure = error; }
        catch (ExecutionException error) { failure = error.getCause(); }
        catch (java.util.concurrent.CancellationException error) { failure = error; }
        finished();
        if (!session.isActive()) expired();
        else if (failure == null) succeeded(result);
        else failed(failure);
    }
    protected abstract void finished();
    protected abstract void expired();
    protected abstract void succeeded(T result);
    protected abstract void failed(Throwable failure);
}
