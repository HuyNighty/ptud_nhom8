package com.quanlynhakhoa.features;

import com.quanlynhakhoa.account.ui.AccountPanel;
import com.quanlynhakhoa.benhnhan.view.BenhNhanPanel;
import com.quanlynhakhoa.benhnhan.view.HoSoBenhAnPanel;
import com.quanlynhakhoa.lichhen.view.LichHenPanel;
import com.quanlynhakhoa.workforce.ui.EmployeePanel;
import com.quanlynhakhoa.roles.ui.RolePanel;
import com.quanlynhakhoa.examination.ui.ExamQueuePanel;
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
                .register(Module.WORKFORCE, EmployeePanel::new)
                .register(Module.ROLES, RolePanel::new)
                .register(Module.EXAMINATIONS, ExamQueuePanel::new)
                .register(Module.PATIENTS, BenhNhanPanel::new)
                .register(Module.APPOINTMENTS, LichHenPanel::new)
                .register(Module.MEDICAL_RECORDS, HoSoBenhAnPanel::new)
                .register(Module.SERVICE_CATALOG, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session) {
                        return new DichVuPanel(session, TeamDevConnection::open, DichVuPanel.EditorMode.COMBINED);
                    }
                })
                .register(Module.BILLING, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session) {
                        return new HoaDonPanel(session, TeamDevConnection::open);
                    }
                })
                .register(Module.DEBTS, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session){return new com.quanlynhakhoa.billing.ui.DebtPanel(session,TeamDevConnection::open);}
                })
                .register(Module.PAYMENTS, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session){return new com.quanlynhakhoa.billing.ui.PaymentPanel(session,TeamDevConnection::open);}
                })
                .register(Module.SERVICES, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session){return new DichVuPanel(session,TeamDevConnection::open,DichVuPanel.EditorMode.COMBINED);}
                })
                .register(Module.INVOICE_CANCELLATIONS,new FeaturePanelFactory(){
                    @Override public JPanel create(DesktopSession session){return new com.quanlynhakhoa.billing.ui.CancellationPanel(session,TeamDevConnection::open);}
                })
                .build();
    }
}
