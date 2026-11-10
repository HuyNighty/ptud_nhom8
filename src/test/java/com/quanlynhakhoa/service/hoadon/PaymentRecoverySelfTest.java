package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import com.quanlynhakhoa.model.hoadon.YeuCauThanhToan;
import com.quanlynhakhoa.model.hoadon.PhanThanhToan;
import com.quanlynhakhoa.controller.hoadon.ThanhToanController;
import com.quanlynhakhoa.service.hoadon.LuuThanhToanDangCho;
import com.quanlynhakhoa.view.hoadon.ThanhToanForm;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

/** File recovery + real Swing controller with synthetic JDBC. Never connects to a database. */
public final class PaymentRecoverySelfTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static YeuCauThanhToan request(String id){return new YeuCauThanhToan("H1",new BigDecimal("100"),List.of(
            new PhanThanhToan(id,new BigDecimal("100"),PhuongThucThanhToan.TIEN_MAT,null,new BigDecimal("200"))),false);}
    private static FeatureJdbcDouble fixture(String paymentId,int paid){
        FeatureJdbcDouble db=new FeatureJdbcDouble();Map<String,Object> header=new HashMap<>();
        header.put("maHoaDon","H1");header.put("maNhanVien","NV1");header.put("maBenhNhan","BN1");header.put("tenBenhNhan","Fixture");
        header.put("ngayLap",Timestamp.valueOf("2026-11-01 10:00:00"));header.put("trangThai",paid==0?"CHUA_THANH_TOAN":"THANH_TOAN_MOT_PHAN");
        header.put("tongTien",new BigDecimal("1000"));header.put("daThu",BigDecimal.valueOf(paid));header.put("congNoBenhNhan",BigDecimal.valueOf(1000-paid));
        db.invoiceRows=List.of(header);
        db.lineRows=List.of(Map.of("maChiTiet",1,"maHoaDon","H1","maDichVu","DV1","soLuong",1,"donGiaApDung",new BigDecimal("1000")));
        if(paymentId!=null){Map<String,Object> row=new HashMap<>();row.put("maThanhToan",paymentId);row.put("maHoaDon","H1");row.put("thoiGian",Timestamp.valueOf(LocalDateTime.now()));
            row.put("soTien",BigDecimal.valueOf(paid));row.put("phuongThuc","TIEN_MAT");row.put("tienKhachDua",new BigDecimal("200"));row.put("trangThai","THANH_CONG");row.put("maNhanVien","NV1");db.paymentRows=List.of(row);}
        return db;
    }
    private static void edt(Runnable action)throws Exception{SwingUtilities.invokeAndWait(action);}
    private static void await(ThanhToanForm form)throws Exception{
        long limit=System.nanoTime()+5_000_000_000L;
        while(System.nanoTime()<limit){boolean[] done={false};edt(new Runnable(){public void run(){done[0]=!form.isWorking();}});if(done[0])return;Thread.sleep(15);}
        throw new AssertionError("Recovery load timed out");
    }
    public static void main(String[] args)throws Exception{
        Path folder=Files.createTempDirectory("n08-payment-recovery-");LuuThanhToanDangCho store=new LuuThanhToanDangCho(folder);
        YeuCauThanhToan pending=request("PENDING1");store.create(pending);
        check(new LuuThanhToanDangCho(folder).read("H1").equals(pending),"new store instance recovers exact ID, amount and tender from disk");
        try{new LuuThanhToanDangCho(folder).create(request("OTHER"));throw new AssertionError("Overwrote unresolved attempt");}catch(java.nio.file.FileAlreadyExistsException expected){}
        try{store.clear(request("OTHER"));throw new AssertionError("Cleared another request");}catch(java.io.IOException expected){}
        var session=ServiceBillingSelfTest.session(VaiTroHeThong.RECEPTIONIST);final ThanhToanForm[] first={null};FeatureJdbcDouble absent=fixture(null,0);
        edt(new Runnable(){public void run(){first[0]=new ThanhToanForm();new ThanhToanController(first[0],session,absent,"H1",new Runnable(){public void run(){}},new LuuThanhToanDangCho(folder));}});await(first[0]);
        edt(new Runnable(){public void run(){check(first[0].hasUncertainResult()&&!first[0].btnThuTien.isEnabled(),"reopening unresolved attempt blocks a new collection");check(first[0].btnThuLai.isVisible()&&first[0].btnThuLai.isEnabled(),"absent result offers retry of same attempt only");check(!first[0].txtSoTien.isEnabled(),"cannot edit an unresolved attempt");check(first[0].txtSoTien.getText().equals("100")&&first[0].txtKhachDua.getText().equals("200"),"original amount and cash tender restored for review");}});
        check(store.read("H1").equals(pending)&&absent.updates==0,"read-only check never clears an absent attempt or writes payment");
        final ThanhToanForm[] second={null};AtomicInteger completed=new AtomicInteger();FeatureJdbcDouble committed=fixture("PENDING1",100);
        edt(new Runnable(){public void run(){second[0]=new ThanhToanForm();second[0].onClose(new Runnable(){public void run(){}});new ThanhToanController(second[0],session,committed,"H1",new Runnable(){public void run(){completed.incrementAndGet();}},new LuuThanhToanDangCho(folder));}});await(second[0]);
        check(completed.get()==1&&store.read("H1")==null&&committed.updates==0,"matching committed payment clears journal without collecting again");
        store.create(pending);final ThanhToanForm[] third={null};FeatureJdbcDouble mismatch=fixture("PENDING1",101);
        edt(new Runnable(){public void run(){third[0]=new ThanhToanForm();new ThanhToanController(third[0],session,mismatch,"H1",new Runnable(){public void run(){}},store);}});await(third[0]);
        edt(new Runnable(){public void run(){check(third[0].hasUncertainResult()&&!third[0].btnThuTien.isEnabled()&&!third[0].btnThuLai.isEnabled(),"mismatched payment stays locked without retry");}});
        check(store.read("H1").equals(pending),"mismatch preserves journal");store.clear(pending);
        // A corrupt journal must fail closed rather than becoming a new payment.
        store.create(pending);Path journal;try(var files=Files.list(folder)){journal=files.filter(path->path.toString().endsWith(".properties")).findFirst().orElseThrow();}Files.writeString(journal,"broken=true");
        try{store.read("H1");throw new AssertionError("Corrupt journal accepted");}catch(java.io.IOException expected){}
        // Drive the actual submit path without a modal dialog. The failing connection sees the flushed journal.
        Files.delete(journal);final boolean[] disconnected={false};final YeuCauThanhToan[] sent={null};
        FeatureJdbcDouble submissionDb=fixture(null,0);final LuuThanhToanDangCho recoveryStore=new LuuThanhToanDangCho(folder);
        com.quanlynhakhoa.dao.hoadon.ConnectionProvider connection=new com.quanlynhakhoa.dao.hoadon.ConnectionProvider(){
            public java.sql.Connection open()throws java.sql.SQLException{
                if(disconnected[0]){try{YeuCauThanhToan stored=recoveryStore.read("H1");check(stored!=null,"journal is durable before SQL opens");sent[0]=stored;}catch(java.io.IOException failure){throw new java.sql.SQLException(failure);}throw new java.sql.SQLException("Simulated lost connection");}
                return submissionDb.open();
            }
        };
        final ThanhToanForm[] submit={null};
        edt(new Runnable(){public void run(){submit[0]=new ThanhToanForm();new ThanhToanController(submit[0],session,connection,"H1",new Runnable(){public void run(){}},recoveryStore,new java.util.function.Predicate<YeuCauThanhToan>(){public boolean test(YeuCauThanhToan request){return true;}});}});await(submit[0]);
        edt(new Runnable(){public void run(){submit[0].txtSoTien.setText("100");submit[0].txtKhachDua.setText("200");disconnected[0]=true;submit[0].btnThuTien.doClick();}});await(submit[0]);
        check(sent[0]!=null&&recoveryStore.read("H1").equals(sent[0]),"connection failure preserves exact submitted request");
        edt(new Runnable(){public void run(){check(submit[0].hasUncertainResult()&&!submit[0].btnThuTien.isEnabled(),"failed submit locks new collection");disconnected[0]=false;submit[0].btnKiemTra.doClick();}});await(submit[0]);
        YeuCauThanhToan original=sent[0];
        edt(new Runnable(){public void run(){disconnected[0]=true;submit[0].btnThuLai.doClick();}});await(submit[0]);
        check(original.equals(sent[0])&&recoveryStore.read("H1").equals(original),"retry reuses original ID and amount instead of creating another attempt");
        recoveryStore.clear(original);
        session.logout();try(var files=Files.list(folder)){for(Path file:files.toList())Files.delete(file);}Files.delete(folder);
        System.out.println("PASS: durable payment recovery, reopen locks, exact-match reconciliation, no-write checks, corruption and request isolation.");
    }
}
