package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.security.JdbcSessionVerifier;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.dao.hoadon.ConnectionProvider;
import com.quanlynhakhoa.enums.TrangThaiThanhToan;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import com.quanlynhakhoa.enums.LoaiDaoThanhToan;
import com.quanlynhakhoa.model.hoadon.YeuCauThanhToan;
import com.quanlynhakhoa.model.hoadon.PhanThanhToan;
import com.quanlynhakhoa.model.hoadon.DongBanNhapHoaDon;
import com.quanlynhakhoa.model.hoadon.BanNhapHoaDon;
import com.quanlynhakhoa.dao.dieutri.KeHoachDieuTriDao;
import com.quanlynhakhoa.security.KetQuaDangNhap;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.*;

/** Real SQL writes ONLY to a separate synthetic database created by the companion test script. */
public final class BillingHardeningSqlIT {
    private static ConnectionProvider connections;private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private interface Action{void run()throws Exception;}
    private static void denied(Action a,String label)throws Exception{boolean rejected=false;try{a.run();}catch(IllegalArgumentException|AccessDeniedException expected){rejected=true;}check(rejected,label);}
    private static void sql(String text)throws Exception{try(Connection c=connections.open();Statement s=c.createStatement()){s.executeUpdate(text);}}
    private static BigDecimal scalar(String text)throws Exception{try(Connection c=connections.open();Statement s=c.createStatement();ResultSet r=s.executeQuery(text)){r.next();return r.getBigDecimal(1);}}
    private static DesktopSession session(String account,VaiTroHeThong role){var s=DesktopSession.fromSuccessfulLogin(new KetQuaDangNhap(true,account,role,false));s.bindCurrentAccountCheck(new JdbcSessionVerifier(connections::open));return s;}
    private static YeuCauThanhToan cash(String invoice,String id,int amount){var m=BigDecimal.valueOf(amount);return new YeuCauThanhToan(invoice,m,List.of(new PhanThanhToan(id,m,PhuongThucThanhToan.TIEN_MAT,null,m)),false);}
    private static void header(String id,String visit,int total)throws Exception{
        sql("INSERT dbo.HoaDon(maHoaDon,maNhanVien,maBenhNhan,maPhieuKham,ngayLap,trangThai) VALUES('"+id+"','N1','B1',"+(visit==null?"NULL":"'"+visit+"'")+",CURRENT_TIMESTAMP,'CHUA_THANH_TOAN')");
        sql("INSERT dbo.ChiTietHoaDon(maHoaDon,maDichVu,soLuong,donGiaApDung) VALUES('"+id+"','D1',1,"+total+")");
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=2||!args[0].equals("--synthetic-database")||!args[1].matches("N08Dev_[0-9a-f]{8}"))throw new IllegalArgumentException("Requires separate synthetic database");
        Properties config=new Properties();try(var reader=Files.newBufferedReader(Path.of(".n08-dev.local.properties"))){config.load(reader);}
        if(args[1].equals(config.getProperty("dev.database")))throw new IllegalArgumentException("Refusing configured application database");
        String url="jdbc:sqlserver://127.0.0.1:"+config.getProperty("dev.port")+";databaseName="+args[1]+";encrypt=true;trustServerCertificate=true;loginTimeout=8";
        connections=()->DriverManager.getConnection(url,config.getProperty("dev.user"),config.getProperty("dev.password"));
        sql("INSERT dbo.NhanVien(maNhanVien,hoTen,soDienThoai,chucDanh) VALUES('N1',N'Synthetic cashier','0990000001','LT'),('N2',N'Synthetic cashier two','0990000002','LT'),('NM',N'Synthetic manager','0990000003','QL'),('ND',N'Synthetic doctor','0990000004','BS')");
        sql("INSERT dbo.TaiKhoan(maTaiKhoan,maNhanVien,tenDangNhap,tenHienThi,matKhauHash,vaiTro,matKhauCapNhatLuc) VALUES('A1','N1','test.cashier',N'Synthetic','not-a-login-hash','RECEPTIONIST',SYSUTCDATETIME()),('A2','N2','test.cashier2',N'Synthetic','not-a-login-hash','RECEPTIONIST',SYSUTCDATETIME()),('AM','NM','test.manager',N'Synthetic','not-a-login-hash','CLINIC_MANAGER',SYSUTCDATETIME()),('AD','ND','test.doctor',N'Synthetic','not-a-login-hash','DOCTOR',SYSUTCDATETIME())");
        sql("INSERT dbo.BenhNhan(maBenhNhan,hoTen,ngaySinh,gioiTinh,soDienThoai) VALUES('B1',N'Original synthetic name','2000-01-01','NAM','0991111111'); INSERT dbo.HoSoBenhAn(maHoSo,maBenhNhan,ngayLap) VALUES('C1','B1','2026-01-01'); INSERT dbo.DichVu(maDichVu,tenDichVu,donGia) VALUES('D1',N'Original service',100)");
        sql("INSERT dbo.PhieuKham(maPhieuKham,maBenhNhan,maHoSo,maNhanVien,thoiGianTiepNhan,trangThai,chanDoan,ketLuan) VALUES('V1','B1','C1','ND',CURRENT_TIMESTAMP,'DA_KET_LUAN',N'test',N'test'),('V0','B1','C1','ND',CURRENT_TIMESTAMP,'DA_KET_LUAN',N'test',N'test')");
        var cashier=session("A1",VaiTroHeThong.RECEPTIONIST);var second=session("A2",VaiTroHeThong.RECEPTIONIST);var manager=session("AM",VaiTroHeThong.CLINIC_MANAGER);
        header("H1","V1",300);header("H2",null,300);header("HC",null,300);
        try(Connection c=connections.open()){
            c.setAutoCommit(false);
            denied(()->new KeHoachDieuTriDao().taoKeHoach(c,"AD","KNEW","C1","V1",LocalDate.now(),"Extra care"),"reject treatment after invoice");c.rollback();
        }
        boolean databaseSeal=false;
        try{sql("INSERT dbo.KeHoachDieuTri(maKeHoach,maHoSo,maPhieuKham,ngayLap,mucTieu,maNhanVien) VALUES('KSQL','C1','V1',CURRENT_TIMESTAMP,N'Late treatment','ND')");}catch(SQLException expected){databaseSeal=expected.getErrorCode()==51182;}
        check(databaseSeal,"database also rejects late plans from older clients");
        var invoiceService=new HoaDonService(cashier,connections);
        var before=invoiceService.detail("H1");sql("UPDATE dbo.BenhNhan SET hoTen=N'Changed patient' WHERE maBenhNhan='B1'; UPDATE dbo.DichVu SET tenDichVu=N'Changed service' WHERE maDichVu='D1'");
        var after=invoiceService.detail("H1");check(before.hoaDon().tenBenhNhan().equals(after.hoaDon().tenBenhNhan()),"patient snapshot immutable after rename");check(before.chiTiet().getFirst().tenHienThi().equals(after.chiTiet().getFirst().tenHienThi()),"service snapshot immutable after rename");
        var freeSource=new NguonTinhPhiLamSang(){public void validateServiceState(Connection c,BanNhapHoaDon d,String id,boolean active){} public BanNhapHoaDon load(Connection c,String patient,String visit,boolean lock){return new BanNhapHoaDon(patient,visit,"free",List.of(new DongBanNhapHoaDon("D1","Free",1,BigDecimal.ZERO)));}};
        new TaoHoaDonService(cashier,connections,freeSource,c->"FREE").create(new BanNhapHoaDon("B1","V0","free",List.of(new DongBanNhapHoaDon("D1","Free",1,BigDecimal.ZERO))));
        check(invoiceService.detail("FREE").hoaDon().trangThai()==TrangThaiHoaDon.DA_THANH_TOAN,"free invoice finalized");check(invoiceService.detail("FREE").hoaDon().canhBao().isEmpty(),"free invoice consistent");
        var attempts=new LanThanhToanService(cashier,connections);var request=cash("H1","P1",100);attempts.reserve(request);
        check(new LanThanhToanService(second,connections).pending("H1")!=null,"second workstation sees reservation");
        denied(()->new ThanhToanService(second,connections).collect(cash("H1","OTHER",100)),"second attempt blocked");
        new ThanhToanService(cashier,connections).collect(request);new ThanhToanService(cashier,connections).collect(request);
        check(invoiceService.detail("H1").hoaDon().daThu().compareTo(BigDecimal.valueOf(100))==0,"retry records money exactly once");
        denied(()->new DaoThanhToanService(cashier,connections).reverse("H1","P1",LoaiDaoThanhToan.HOAN_TIEN,"reason","receipt"),"cashier cannot refund");
        sql("UPDATE dbo.ThanhToan SET thoiGian=DATEADD(day,-1,CURRENT_TIMESTAMP) WHERE maThanhToan='P1'");
        new DaoThanhToanService(manager,connections).reverse("H1","P1",LoaiDaoThanhToan.HOAN_TIEN,"Synthetic refund","receipt-test");
        check(invoiceService.detail("H1").hoaDon().daThu().signum()==0,"refund reopens debt");check(invoiceService.detail("H1").thanhToan().getFirst().trangThai()==TrangThaiThanhToan.DA_HOAN_TIEN,"refund preserves original row");
        denied(()->new DaoThanhToanService(manager,connections).reverse("H1","P1",LoaiDaoThanhToan.HOAN_TIEN,"again","receipt"),"double refund rejected");
        new ThanhToanService(cashier,connections).collect(cash("H1","P2",50));new DaoThanhToanService(manager,connections).reverse("H1","P2",LoaiDaoThanhToan.HUY_GHI_NHAN,"Synthetic wrong entry","proof-test");
        check(scalar("SELECT SUM(soTien) FROM dbo.DongTienHoaDon WHERE maHoaDon='H1'").signum()==0,"signed movements reconcile refund and correction");
        String cancel=new HuyHoaDonService(cashier,connections).request("H1","No remaining services");new HuyHoaDonService(manager,connections).decide(cancel,true,"");
        check(invoiceService.detail("H1").hoaDon().trangThai()==TrangThaiHoaDon.DA_HUY,"fully refunded invoice can be cancelled");
        attempts.reserve(cash("H2","RELEASE",100));new LanThanhToanService(manager,connections).release("H2","RELEASE","Verified no money received");
        denied(()->new ThanhToanService(cashier,connections).collect(cash("H2","RELEASE",100)),"released attempt cannot later collect");
        header("HLOST",null,300);
        java.util.concurrent.atomic.AtomicInteger commitNumber=new java.util.concurrent.atomic.AtomicInteger();
        ConnectionProvider lostResponse=()->{
            Connection real=connections.open();
            return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,arguments)->{
                try{
                    Object result=method.invoke(real,arguments);
                    if(method.getName().equals("commit")&&commitNumber.incrementAndGet()==2)throw new SQLException("Synthetic connection lost AFTER actual commit");
                    return result;
                }catch(java.lang.reflect.InvocationTargetException error){throw error.getCause();}
            });
        };
        boolean uncertain=false;try{new ThanhToanService(cashier,lostResponse).collect(cash("HLOST","PLOST",100));}catch(SQLException expected){uncertain=true;}
        check(uncertain,"lost commit response reported as uncertain");
        check(new LanThanhToanService(second,connections).pending("HLOST")!=null,"reservation survives a lost payment commit response");
        denied(()->new ThanhToanService(second,connections).collect(cash("HLOST","DUPLOST",100)),"another workstation cannot collect uncertain receipt again");
        new LanThanhToanService(manager,connections).release("HLOST","PLOST","Verified committed receipt and actual money");
        check(new LanThanhToanService(second,connections).pending("HLOST")==null,"manager confirms matching committed payment without collecting again");
        check(invoiceService.detail("HLOST").hoaDon().daThu().intValueExact()==100,"unknown-outcome reconciliation preserves exact amount");
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);List<Future<Boolean>> results=new ArrayList<>();
        for(int i=0;i<2;i++){final int number=i;results.add(pool.submit(()->{start.await();try{new LanThanhToanService(number==0?cashier:second,connections).reserve(cash("HC","RACE"+number,100));return true;}catch(IllegalArgumentException expected){return false;}}));}
        start.countDown();int won=0;for(var result:results)if(result.get(20,TimeUnit.SECONDS))won++;pool.shutdownNow();check(won==1,"concurrent workstations: exactly one reservation");
        for(int i=0;i<105;i++)header("PAGE"+i,null,100);
        check(invoiceService.findPage("PAGE",null,null,null,0).size()==101,"first page bounded plus sentinel");check(invoiceService.findPage("PAGE",null,null,null,1).size()==5,"second page returns remainder");
        var report=new com.quanlynhakhoa.service.baocao.BaoCaoDoanhThuService(manager,connections::open).load(LocalDate.now().minusDays(1),LocalDate.now(),false);
        check(report.collected().intValueExact()==100,"report net collections include reversals and one reconciled receipt");
        var todayReport=new com.quanlynhakhoa.service.baocao.BaoCaoDoanhThuService(manager,connections::open).load(LocalDate.now(),LocalDate.now(),false);
        check(todayReport.collected().signum()==0,"later-day refund is deducted on refund date");
        sql("UPDATE dbo.TaiKhoan SET trangThai='BI_KHOA',nguonKhoa='ADMIN_ACTION',khoaLuc=SYSUTCDATETIME(),lyDoKhoa=N'test' WHERE maTaiKhoan='A1'");
        denied(()->invoiceService.find("",null,null,null),"account lock invalidates existing session");check(!cashier.isActive(),"locked cached session invalidated");
        sql("UPDATE dbo.TaiKhoan SET vaiTro='NURSE' WHERE maTaiKhoan='A2'");denied(()->new HoaDonService(second,connections).find("",null,null,null),"changed role invalidates cached session");
        System.out.println("PASS: "+checks+" real SQL billing hardening assertions; separate synthetic database, no application data modified.");
    }
}
