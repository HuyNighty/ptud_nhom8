package com.quanlynhakhoa.hethong.tinhnang;

import com.quanlynhakhoa.enums.CheDoSuaDanhMucDichVu;
import com.quanlynhakhoa.quantri.taikhoan.ui.AccountPanel;
import com.quanlynhakhoa.quantri.nhatky.ui.AuditPanel;
import com.quanlynhakhoa.view.benhnhan.BenhNhanPanel;
import com.quanlynhakhoa.view.benhnhan.HoSoBenhAnPanel;
import com.quanlynhakhoa.view.lichhen.LichHenPanel;
import com.quanlynhakhoa.view.lichhen.TiepNhanPanel;
import com.quanlynhakhoa.nghiepvu.kho.InventoryPanel;
import com.quanlynhakhoa.quantri.nhanvien.ui.EmployeePanel;
import com.quanlynhakhoa.quantri.vaitro.ui.RolePanel;
import com.quanlynhakhoa.view.phieukham.PhieuKhamPanel;
import com.quanlynhakhoa.nghiepvu.dieutri.view.TreatmentPlanPanel;
import com.quanlynhakhoa.enums.Module;
import com.quanlynhakhoa.view.hoadon.HoaDonPanel;
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
                .register(Module.REVENUE_REPORTS, com.quanlynhakhoa.quantri.baocao.RevenueReportPanel::new)
                .register(Module.EXAMINATIONS, PhieuKhamPanel::new)
                .register(Module.TREATMENT_PLANS, TreatmentPlanPanel::new)
                .register(Module.PATIENTS, BenhNhanPanel::new)
                .register(Module.APPOINTMENTS, LichHenPanel::new)
                .register(Module.RECEPTION, TiepNhanPanel::new)
                .register(Module.INVENTORY, InventoryPanel::new)
                .register(Module.SUPPLY_REQUESTS, com.quanlynhakhoa.nghiepvu.kho.supplyrequest.view.SupplyRequestPanel::new)
                .register(Module.MEDICAL_RECORDS, HoSoBenhAnPanel::new)
                .register(Module.SERVICE_CATALOG, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session) {
                        return new ServiceCatalogPanel(session, TeamDevConnection::open, CheDoSuaDanhMucDichVu.COMBINED);
                    }
                })
                .register(Module.BILLING, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session) {
                        return new HoaDonPanel(session, TeamDevConnection::open);
                    }
                })
                .register(Module.DEBTS, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session){return new com.quanlynhakhoa.view.hoadon.CongNoPanel(session,TeamDevConnection::open);}
                })
                .register(Module.PAYMENTS, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session){return new com.quanlynhakhoa.view.hoadon.ThanhToanPanel(session,TeamDevConnection::open);}
                })
                .register(Module.SERVICES, new FeaturePanelFactory() {
                    @Override public JPanel create(DesktopSession session){return new ServiceCatalogPanel(session,TeamDevConnection::open,CheDoSuaDanhMucDichVu.COMBINED);}
                })
                .register(Module.INVOICE_CANCELLATIONS,new FeaturePanelFactory(){
                    @Override public JPanel create(DesktopSession session){return new com.quanlynhakhoa.view.hoadon.HuyHoaDonPanel(session,TeamDevConnection::open);}
                })
                .build();
    }
}
