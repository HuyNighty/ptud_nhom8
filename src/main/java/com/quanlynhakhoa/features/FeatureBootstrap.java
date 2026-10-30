package com.quanlynhakhoa.features;

import com.quanlynhakhoa.account.ui.AccountPanel;
import com.quanlynhakhoa.navigation.Module;
import com.quanlynhakhoa.billing.ui.HoaDonPanel;
import com.quanlynhakhoa.dichvu.ui.DichVuPanel;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.security.DesktopSession;
import javax.swing.JPanel;

public final class FeatureBootstrap {
    private FeatureBootstrap() { }

    public static FeatureRegistry createRegistry() {
        return FeatureRegistry.builder()
                .register(Module.ACCOUNTS, AccountPanel::new)
                .register(Module.SERVICE_CATALOG, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session) {
                        return new DichVuPanel(session, TeamDevConnection::open, DichVuPanel.EditorMode.PRICES);
                    }
                })
                .register(Module.BILLING, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session) {
                        return new HoaDonPanel(session, TeamDevConnection::open);
                    }
                })
                .register(Module.DEBTS, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session){return new HoaDonPanel(session,TeamDevConnection::open,HoaDonPanel.ViewMode.DEBTS);}
                })
                .register(Module.PAYMENTS, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session){return new HoaDonPanel(session,TeamDevConnection::open,HoaDonPanel.ViewMode.PAYMENTS);}
                })
                .register(Module.SERVICES, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session){return new DichVuPanel(session,TeamDevConnection::open,DichVuPanel.EditorMode.SERVICES);}
                })
                .register(Module.INVOICE_CANCELLATIONS,new FeaturePanelFactory(){
                    @Override public JPanel create(DesktopSession session){return new com.quanlynhakhoa.billing.ui.CancellationPanel(session,TeamDevConnection::open);}
                })
                .build();
    }
}
