package com.quanlynhakhoa.hethong.tinhnang;

import com.quanlynhakhoa.quantri.taikhoan.ui.AccountPanel;
import com.quanlynhakhoa.quantri.nhatky.ui.AuditPanel;
import com.quanlynhakhoa.nghiepvu.benhnhan.view.BenhNhanPanel;
import com.quanlynhakhoa.nghiepvu.benhnhan.view.HoSoBenhAnPanel;
import com.quanlynhakhoa.nghiepvu.lichhen.view.LichHenPanel;
import com.quanlynhakhoa.nghiepvu.lichhen.view.TiepNhanPanel;
import com.quanlynhakhoa.nghiepvu.kho.InventoryPanel;
import com.quanlynhakhoa.quantri.nhanvien.ui.EmployeePanel;
import com.quanlynhakhoa.quantri.vaitro.ui.RolePanel;
import com.quanlynhakhoa.nghiepvu.phieukham.view.PhieuKhamPanel;
import com.quanlynhakhoa.nghiepvu.dieutri.view.TreatmentPlanPanel;
import com.quanlynhakhoa.hethong.dieuhuong.Module;
import com.quanlynhakhoa.nghiepvu.hoadon.ui.InvoicePanel;
import com.quanlynhakhoa.nghiepvu.dichvu.ui.ServiceCatalogPanel;
import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.quantri.lichlamviec.view.LichLamViecPanel;
import javax.swing.JPanel;

public final class FeatureBootstrap {
    private FeatureBootstrap() { }

    public static FeatureRegistry createRegistry() {
        return FeatureRegistry.builder()
                .register(Module.ACCOUNTS, AccountPanel::new)
                .register(Module.WORKFORCE, EmployeePanel::new)
                .register(Module.SHIFT_ASSIGNMENTS, session -> new LichLamViecPanel(session, true))
                .register(Module.MY_SCHEDULE, session -> new LichLamViecPanel(session, false))
                .register(Module.ROLES, RolePanel::new)
                .register(Module.AUDIT, AuditPanel::new)
                .register(Module.EXAMINATIONS, PhieuKhamPanel::new)
                .register(Module.TREATMENT_PLANS, TreatmentPlanPanel::new)
                .register(Module.PATIENTS, BenhNhanPanel::new)
                .register(Module.APPOINTMENTS, LichHenPanel::new)
                .register(Module.RECEPTION, TiepNhanPanel::new)
                .register(Module.INVENTORY, InventoryPanel::new)
                .register(Module.MEDICAL_RECORDS, HoSoBenhAnPanel::new)
                .register(Module.SERVICE_CATALOG, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session) {
                        return new ServiceCatalogPanel(session, TeamDevConnection::open, ServiceCatalogPanel.EditorMode.COMBINED);
                    }
                })
                .register(Module.BILLING, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session) {
                        return new InvoicePanel(session, TeamDevConnection::open);
                    }
                })
                .register(Module.DEBTS, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session){return new com.quanlynhakhoa.nghiepvu.hoadon.ui.DebtPanel(session,TeamDevConnection::open);}
                })
                .register(Module.PAYMENTS, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session){return new com.quanlynhakhoa.nghiepvu.hoadon.ui.PaymentPanel(session,TeamDevConnection::open);}
                })
                .register(Module.SERVICES, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session){return new ServiceCatalogPanel(session,TeamDevConnection::open,ServiceCatalogPanel.EditorMode.COMBINED);}
                })
                .register(Module.INVOICE_CANCELLATIONS,new FeaturePanelFactory(){
                    @Override public JPanel create(DesktopSession session){return new com.quanlynhakhoa.nghiepvu.hoadon.ui.CancellationPanel(session,TeamDevConnection::open);}
                })
                .build();
    }
}
