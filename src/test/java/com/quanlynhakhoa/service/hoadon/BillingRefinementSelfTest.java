package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.view.hoadon.ThanhToanPanel;
import com.quanlynhakhoa.view.hoadon.ThanhToanForm;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import com.quanlynhakhoa.enums.PhuongThucNhapThanhToan;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.model.hoadon.YeuCauThanhToan;
import com.quanlynhakhoa.model.hoadon.PhanThanhToan;
import com.quanlynhakhoa.service.hoadon.ThanhToanService;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.nio.file.*;
import javax.swing.*;

/** Synthetic fixtures strictly for automated assertions. Does not generate screenshots or use SQL. */
public final class BillingRefinementSelfTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static ThanhToanForm current(ThanhToanPanel panel){return find(panel,ThanhToanForm.class);}
    private static <T>T find(java.awt.Container root,Class<T> type){for(var child:root.getComponents()){if(type.isInstance(child))return type.cast(child);if(child instanceof java.awt.Container nested){T value=find(nested,type);if(value!=null)return value;}}return null;}
    private static void edt(Runnable action)throws Exception{SwingUtilities.invokeAndWait(action);}
    private static void waitFor(final ThanhToanPanel panel,final String text)throws Exception{long deadline=System.nanoTime()+10_000_000_000L;while(System.nanoTime()<deadline){boolean[] done={false};edt(new Runnable(){public void run(){done[0]=panel.lblTrangThai.getText().contains(text)&&panel.btnTim.isEnabled();}});if(done[0])return;Thread.sleep(20);}throw new AssertionError("Timed out: "+text);}
    private static String state(ThanhToanPanel panel,FeatureJdbcDouble jdbc){
        ThanhToanForm form=current(panel);
        return "search="+panel.lblTrangThai.getText()+"; pickerEnabled="+panel.btnTim.isEnabled()
                +"; detail="+form.lblTrangThai.getText()+"; busy="+form.isWorking()
                +"; uncertain="+form.hasUncertainResult()+"; collectionAllowed="+form.collectionContextAllowed()
                +"; collectEnabled="+form.btnThuTien.isEnabled()+"; fixtureQueries="+new ArrayList<>(jdbc.sql);
    }
    private static void waitForDetail(final ThanhToanPanel panel,final FeatureJdbcDouble jdbc)throws Exception{
        long deadline=System.nanoTime()+10_000_000_000L;String[] last={"not sampled"};
        while(System.nanoTime()<deadline){
            boolean[] ready={false};
            edt(()->{ThanhToanForm form=current(panel);ready[0]=panel.lblTrangThai.getText().contains("Đang xem hóa đơn H1")
                    &&panel.btnTim.isEnabled()&&!form.isWorking()&&!form.hasUncertainResult()
                    &&form.collectionContextAllowed()&&form.btnThuTien.isEnabled();last[0]=state(panel,jdbc);});
            if(ready[0])return;
            Thread.sleep(20); // Poll a bounded condition; never assume a fixed load duration.
        }
        throw new AssertionError("Timed out waiting for loaded eligible detail: "+last[0]);
    }
    public static void main(String[] args)throws Exception{
        // PaymentPanel uses PendingPaymentStore.local(): never share a real recovery journal
        // or local database config with this synthetic test.
        String previousHome=System.getProperty("user.home"),previousDir=System.getProperty("user.dir");
        Path sandbox=Files.createTempDirectory("n08-billing-refinement-").toAbsolutePath().normalize();
        try{
            System.setProperty("user.home",sandbox.toString());System.setProperty("user.dir",sandbox.toString());
            run();
        }finally{
            System.setProperty("user.home",previousHome);System.setProperty("user.dir",previousDir);
            // Only this freshly created synthetic directory is eligible for cleanup.
            try(var paths=Files.walk(sandbox)){
                for(Path path:paths.sorted(Comparator.reverseOrder()).toList()){
                    if(!path.toAbsolutePath().normalize().startsWith(sandbox))throw new AssertionError("Unsafe cleanup target");
                    Files.delete(path);
                }
            }
        }
    }
    private static void run()throws Exception{
        edt(new Runnable(){public void run(){
            ThanhToanForm view=new ThanhToanForm();
            ThongTinHoaDon h=new ThongTinHoaDon("H1","NV1","BN1","Fixture",null,LocalDateTime.now(),TrangThaiHoaDon.CHUA_THANH_TOAN,new BigDecimal("1000000"),BigDecimal.ZERO,BigDecimal.ZERO);
            view.showInvoice(new DuLieuChiTietHoaDon(h,List.of(),List.of()));view.busy(false,true,false);
            view.txtSoTien.setText("600.000");view.txtKhachDua.setText("1.000.000");
            check(view.cashAmount().intValueExact()==600000&&view.bankAmount().signum()==0,"Cash auto allocation");
            check(!view.txtTienMat.isEditable()&&!view.chkDaNhanChuyenKhoan.isVisible(),"Single method is locked and no bank confirmation");
            check(view.lblTienThua.getText().contains("400.000")&&view.lblNoSauThu.getText().contains("400.000"),"Change uses cash part, debt uses amount collected");
            view.cmbPhuongThuc.setSelectedItem(PhuongThucNhapThanhToan.BANK);
            check(view.bankAmount().intValueExact()==600000&&view.cashAmount().signum()==0,"Bank auto allocation");
            check(!view.lblTienThua.isVisible()&&view.chkDaNhanChuyenKhoan.isVisible(),"Bank-only shows relevant controls");
            view.txtKhachDua.setText("unused invalid cash input");check(view.lblPhanBo.getText().contains("Khớp"),"Hidden tender does not affect bank preview");
            view.chkDaNhanChuyenKhoan.setSelected(true);view.txtSoTien.setText("700.000");check(!view.chkDaNhanChuyenKhoan.isSelected(),"Changed bank amount requires reconfirmation");
            view.txtMaGiaoDich.setText("BANK-A");view.chkDaNhanChuyenKhoan.setSelected(true);
            view.txtMaGiaoDich.setText("BANK-B");check(!view.chkDaNhanChuyenKhoan.isSelected(),"Changed transfer reference requires reconfirmation");
            view.chkDaNhanChuyenKhoan.setSelected(true);view.txtMaGiaoDich.setText("");
            check(!view.chkDaNhanChuyenKhoan.isSelected(),"Cleared transfer reference requires reconfirmation");
            check(view.cmbPhuongThuc.getItemCount()==2,"Only cash and bank methods available");
            List<PhanThanhToan> parts=List.of(new PhanThanhToan("P2",new BigDecimal("700000"),PhuongThucThanhToan.CHUYEN_KHOAN,"TESTREF",null));
            try{ThanhToanService.validate(new YeuCauThanhToan("H1",new BigDecimal("700000"),parts,false));throw new AssertionError("Bank confirmation bypassed");}catch(IllegalArgumentException expected){checks++;}
            ThanhToanService.validate(new YeuCauThanhToan("H1",new BigDecimal("700000"),parts,true));checks++;
            view.cmbPhuongThuc.setSelectedItem(PhuongThucNhapThanhToan.CASH);view.txtKhachDua.setText("100.000");
            check(view.lblTienThua.getText().contains("chưa đủ"),"Insufficient tender visible");
            view.txtSoTien.setText("1.000.001");check(view.lblNoSauThu.getText().contains("vượt"),"Overcollection visible");
            view.busy(true,false,true);check(!view.cmbPhuongThuc.isEnabled()&&!view.btnThuTien.isEnabled(),"Write locks method and submit");
            view.busy(false,false,true);check(view.btnKiemTra.isVisible()&&view.btnKiemTra.isEnabled()&&!view.btnThuTien.isEnabled(),"Unknown commit offers read verification only");
            view.setCollectionContextAllowed(false);view.busy(false,true,false);check(!view.btnThuTien.isEnabled(),"Late detail response cannot enable an invoice outside current results");view.setCollectionContextAllowed(true);check(view.btnThuTien.isEnabled(),"Matching current results restore an otherwise eligible invoice");
        }});
        final FeatureJdbcDouble jdbc=new FeatureJdbcDouble();Map<String,Object> header=new HashMap<>();
        header.put("maHoaDon","H1");header.put("maNhanVien","NV1");header.put("maBenhNhan","BN1");header.put("tenBenhNhan","Fixture");header.put("ngayLap",Timestamp.valueOf("2026-10-31 10:00:00"));header.put("trangThai","CHUA_THANH_TOAN");header.put("tongTien",new BigDecimal("1000000"));header.put("daThu",BigDecimal.ZERO);header.put("congNoBenhNhan",new BigDecimal("1000000"));jdbc.invoiceRows=List.of(header);
        Map<String,Object> line=new HashMap<>();line.put("maChiTiet",1);line.put("maHoaDon","H1");line.put("maDichVu","DV1");line.put("soLuong",1);line.put("donGiaApDung",new BigDecimal("1000000"));jdbc.lineRows=List.of(line);
        var session=ServiceBillingSelfTest.session(VaiTroHeThong.RECEPTIONIST);final ThanhToanPanel[] page={null};
        edt(new Runnable(){public void run(){page[0]=new ThanhToanPanel(session,jdbc);page[0].openInvoice("H1");}});waitForDetail(page[0],jdbc);
        edt(new Runnable(){public void run(){check(current(page[0]).btnThuTien.isEnabled(),"Selected real-shaped fixture loaded");jdbc.invoiceRows=List.of();page[0].txtTimHoaDon.setText("NO_MATCH");page[0].btnTim.doClick();}});waitFor(page[0],"Không tìm thấy");
        edt(new Runnable(){public void run(){check(page[0].cmbHoaDon.getSelectedItem()==null&&!current(page[0]).btnThuTien.isEnabled(),"Fresh unmatched search cannot collect old invoice");check(current(page[0]).txtSoTien.getText().isBlank(),"Old receipt cleared on explicit search");}});
        check(!jdbc.onEdt,"JDBC remains outside EDT");session.logout();
        System.out.println("PASS: BillingRefinementSelfTest "+checks+" assertions (single method allocation, change, validation, busy/unknown result, fresh search; synthetic logic tests only, no screenshots/SQL).");
    }
}
