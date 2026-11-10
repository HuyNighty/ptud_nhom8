package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.dao.hoadon.HuyHoaDonDao;
import com.quanlynhakhoa.dao.hoadon.HoaDonDao;
import com.quanlynhakhoa.config.ConnectionProvider;
import com.quanlynhakhoa.enums.TrangThaiYeuCauHuyHoaDon;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinYeuCauHuyHoaDon;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.security.KetQuaDangNhap;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.security.DesktopSession;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import static com.quanlynhakhoa.service.hoadon.ServiceBillingSelfTest.*;

/** Simulated transactional locks, not a SQL Server lock/concurrency integration test. */
public final class CancellationConcurrencySelfTest {
    private static final class Store implements ConnectionProvider {
        final ReentrantLock invoiceLock=new ReentrantLock();final Map<Connection,Tx> txs=Collections.synchronizedMap(new IdentityHashMap<>());
        ThongTinYeuCauHuyHoaDon request;boolean cancelled;int audits;final AtomicInteger ids=new AtomicInteger();
        final class Tx implements InvocationHandler {
            boolean auto=true,locked;final List<Runnable> pending=new ArrayList<>();
            void unlock(){if(locked){locked=false;invoiceLock.unlock();}}
            public Object invoke(Object proxy,Method method,Object[] args)throws Exception {
                return switch(method.getName()){
                    case "getAutoCommit" -> auto;
                    case "setAutoCommit" -> {auto=(boolean)args[0];yield null;}
                    case "setTransactionIsolation" -> null;
                    case "prepareStatement" -> new FeatureJdbcDouble().open().prepareStatement((String)args[0]);
                    case "commit" -> {for(Runnable action:pending)action.run();pending.clear();unlock();yield null;}
                    case "rollback" -> {pending.clear();unlock();yield null;}
                    case "close" -> {pending.clear();unlock();txs.remove(proxy);yield null;}
                    default -> throw new AssertionError("Unexpected connection method "+method.getName());
                };
            }
        }
        public Connection open(){Tx tx=new Tx();Connection c=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},tx);txs.put(c,tx);return c;}
        final HuyHoaDonDao dao=new HuyHoaDonDao(){
            public void lockInvoice(Connection c,String id)throws SQLException{try{if(!invoiceLock.tryLock(5,TimeUnit.SECONDS))throw new SQLException("Timed out waiting for synthetic invoice lock");txs.get(c).locked=true;}catch(InterruptedException e){Thread.currentThread().interrupt();throw new SQLException(e);}}
            public void lockRequest(Connection c,String id){check(txs.get(c).locked,"invoice locked before request");}
            public boolean pending(Connection c,String invoice){check(txs.get(c).locked,"pending checked under invoice lock");return request!=null&&request.trangThai()==TrangThaiYeuCauHuyHoaDon.CHO_DUYET;}
            public String invoiceId(Connection c,String id){return "H1";}
            public ThongTinYeuCauHuyHoaDon get(Connection c,String id){return request;}
            public void insert(Connection c,final String id,String invoice,final String account,final String reason){txs.get(c).pending.add(new Runnable(){public void run(){request=record(id,account,reason,TrangThaiYeuCauHuyHoaDon.CHO_DUYET,null,null);}});}
            public void cancelInvoice(Connection c,String id){txs.get(c).pending.add(new Runnable(){public void run(){cancelled=true;}});}
            public void decide(Connection c,String id,final String account,final String reason,final boolean approve){txs.get(c).pending.add(new Runnable(){public void run(){request=record(request.maYeuCau(),request.maTaiKhoanTao(),request.lyDo(),approve?TrangThaiYeuCauHuyHoaDon.DA_DUYET:TrangThaiYeuCauHuyHoaDon.TU_CHOI,account,reason);}});}
            public void audit(Connection c,String account,String id,String action){txs.get(c).pending.add(new Runnable(){public void run(){audits++;}});}
        };
        final HoaDonDao invoices=new HoaDonDao(){public ThongTinHoaDon getForPayment(Connection c,String id){return new ThongTinHoaDon("H1","NV1","BN1","Synthetic",null,LocalDateTime.now(),cancelled?TrangThaiHoaDon.DA_HUY:TrangThaiHoaDon.CHUA_THANH_TOAN,BigDecimal.TEN,BigDecimal.ZERO,BigDecimal.ZERO);}};
        ThongTinYeuCauHuyHoaDon record(String id,String account,String reason,TrangThaiYeuCauHuyHoaDon status,String reviewer,String decision){return new ThongTinYeuCauHuyHoaDon(id,"H1","BN1","Synthetic",BigDecimal.TEN,BigDecimal.ZERO,account,LocalDateTime.now(),reason,status,reviewer,reviewer==null?null:LocalDateTime.now(),decision);}
        HuyHoaDonService service(String account,VaiTroHeThong role){DesktopSession session=DesktopSession.fromSuccessfulLogin(new KetQuaDangNhap(true,account,role,false));return new HuyHoaDonService(session,this,dao,invoices,new TaoMaHoaDon(){public String next(Connection c){return "Y"+ids.incrementAndGet();}});}
    }
    private static void race(final Action first,final Action second)throws Exception {
        final CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);final AtomicInteger successes=new AtomicInteger(),rejected=new AtomicInteger();final List<Throwable> failures=Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try{
            for(final Action action:List.of(first,second))pool.submit(new Runnable(){public void run(){ready.countDown();try{go.await();action.run();successes.incrementAndGet();}catch(IllegalArgumentException expected){rejected.incrementAndGet();}catch(Throwable e){failures.add(e);}}});
            check(ready.await(5,TimeUnit.SECONDS),"both actors ready");go.countDown();pool.shutdown();check(pool.awaitTermination(10,TimeUnit.SECONDS),"race finished");
            check(failures.isEmpty(),"unexpected race error "+failures);check(successes.get()==1&&rejected.get()==1,"exactly one actor wins and one sees committed state");
        }finally{pool.shutdownNow();}
    }
    public static void main(String[] args)throws Exception {
        final Store store=new Store();final HuyHoaDonService first=store.service("REQ1",VaiTroHeThong.RECEPTIONIST),second=store.service("REQ2",VaiTroHeThong.RECEPTIONIST);
        race(new Action(){public void run()throws Exception{first.request("H1","First reason");}},new Action(){public void run()throws Exception{second.request("H1","Second reason");}});
        check(store.request!=null&&store.request.trangThai()==TrangThaiYeuCauHuyHoaDon.CHO_DUYET&&store.audits==1&&!store.cancelled,"one pending request, invoice unchanged");
        final String id=store.request.maYeuCau();final HuyHoaDonService managerA=store.service("MANAGER1",VaiTroHeThong.CLINIC_MANAGER),managerB=store.service("MANAGER2",VaiTroHeThong.CLINIC_MANAGER);
        race(new Action(){public void run()throws Exception{managerA.decide(id,true,"");}},new Action(){public void run()throws Exception{managerB.decide(id,false,"Reject concurrently");}});
        check(store.audits==2&&store.request.trangThai()!=TrangThaiYeuCauHuyHoaDon.CHO_DUYET,"decision and audit persisted once");
        check(store.cancelled==(store.request.trangThai()==TrangThaiYeuCauHuyHoaDon.DA_DUYET),"invoice cancelled iff approval won");
        System.out.println("PASS: CancellationConcurrencySelfTest (two requesters and two reviewers; simulated transactions/locks, no real SQL).");
    }
}
