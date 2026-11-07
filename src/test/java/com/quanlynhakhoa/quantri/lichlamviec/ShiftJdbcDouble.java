package com.quanlynhakhoa.quantri.lichlamviec;
import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Logic fixture only. It does not simulate SQL Server's locking engine. */
public final class ShiftJdbcDouble {
    public final List<String> events=new CopyOnWriteArrayList<>();
    public List<Map<String,Object>> assignments=new ArrayList<>();
    public String database="N08Dev_1234abcd",linkedEmployee="NV1",employeeStatus="DANG_LAM_VIEC";
    public boolean failAudit,failCommit,missingShift,missingEmployee;
    public int maximum=2,opens,commits,rollbacks;
    public String concurrentCancelState;
    public Integer cancelAffectedRows;
    public Runnable onAudit=()->{};
    public String queriedAccount,queriedEmployee;
    public static Map<String,Object> row(Object...pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
    public static Map<String,Object> shift(){return row("maCa","CA1","tenCa","Ca sáng","gioBatDau",Time.valueOf("07:30:00"),"gioKetThuc",Time.valueOf("11:30:00"));}
    public static Map<String,Object> assignment(String id,String employee,String state){var m=row("maPhanCong",id,"maNhanVien",employee,"hoTen",employee.equals("NV1")?"Nguyễn An":"Trần Bình","ngayLam",java.sql.Date.valueOf(LocalDate.now()),"nhiemVu","Khám theo phân công","trangThai",state,"lyDoHuy",null);m.putAll(shift());return m;}
    public Connection open(){
        if(javax.swing.SwingUtilities.isEventDispatchThread())throw new AssertionError("JDBC on EDT");
        opens++;events.add("open");
        List<Map<String,Object>> local=new ArrayList<>();for(var r:assignments)local.add(new LinkedHashMap<>(r));
        boolean[] auto={true};
        return proxy(Connection.class,(o,m,a)->switch(m.getName()){
            case "getAutoCommit" -> auto[0];
            case "setAutoCommit" -> {auto[0]=(boolean)a[0];events.add("transaction");yield null;}
            case "setTransactionIsolation" -> null;
            case "prepareStatement" -> statement((String)a[0],local);
            case "commit" -> {events.add("commit");if(failCommit)throw new SQLException("synthetic commit outcome unknown");assignments=local;commits++;yield null;}
            case "rollback" -> {events.add("rollback");rollbacks++;yield null;}
            case "close" -> {events.add("close");yield null;}
            default -> defaultValue(m.getReturnType());
        });
    }
    private PreparedStatement statement(String sql,List<Map<String,Object>> local){
        Map<Integer,Object> params=new HashMap<>();
        return proxy(PreparedStatement.class,(o,m,a)->{
            String n=m.getName();if(n.startsWith("set")&&a!=null&&a.length>=2&&a[0] instanceof Integer){params.put((Integer)a[0],a[1]);return null;}
            if(n.equals("executeQuery")){
                events.add(sql);
                if(sql.equals("SELECT DB_NAME()"))return result(List.of(row("db",database)));
                if(sql.contains("FROM dbo.TaiKhoan a")){queriedAccount=(String)params.get(1);return result(linkedEmployee==null?List.of():List.of(row("maNhanVien",linkedEmployee)));}
                if(sql.contains("UPDLOCK,HOLDLOCK"))return result(missingEmployee?List.of():List.of(row("trangThai",employeeStatus,"ngayVaoLam",null)));
                if(sql.contains("FROM dbo.ThamSoHeThong"))return result(List.of(row("giaTri",Integer.toString(maximum))));
                if(sql.startsWith("SELECT maNhanVien FROM dbo.PhanCong"))return result(local.stream().filter(r->r.get("maPhanCong").equals(params.get(1))).map(r->row("maNhanVien",r.get("maNhanVien"))).toList());
                if(sql.contains("FROM dbo.PhanCong p")){
                    queriedEmployee=(String)params.get(3);List<Map<String,Object>> found=new ArrayList<>();
                    for(var r:local)if((params.get(3)==null||params.get(3).equals(r.get("maNhanVien")))&& !((java.sql.Date)r.get("ngayLam")).before((java.sql.Date)params.get(1)) && !((java.sql.Date)r.get("ngayLam")).after((java.sql.Date)params.get(2)))found.add(r);
                    return result(found);
                }
                if(sql.contains("FROM dbo.CaLamViec"))return result(missingShift?List.of():List.of(shift()));
                if(sql.contains("FROM dbo.NhanVien")){String search=((String)params.get(1)).toLowerCase(java.util.Locale.ROOT);return result(List.of(row("maNhanVien","NV1","hoTen","Nguyễn An"),row("maNhanVien","NV2","hoTen","Trần Bình")).stream().filter(r->r.values().stream().anyMatch(v->v.toString().toLowerCase(java.util.Locale.ROOT).contains(search))).toList());}
                throw new AssertionError("Unexpected query "+sql);
            }
            if(n.equals("executeUpdate")){
                events.add(sql);
                if(sql.startsWith("INSERT INTO dbo.CaLamViec"))return 1;
                if(sql.startsWith("INSERT INTO dbo.PhanCong")){var r=assignment((String)params.get(1),(String)params.get(2),"CHO_HIEU_LUC");r.put("maCa",params.get(3));r.put("ngayLam",params.get(4));r.put("nhiemVu",params.get(5));local.add(r);return 1;}
                if(sql.startsWith("UPDATE dbo.PhanCong")){
                    if(cancelAffectedRows!=null)return cancelAffectedRows;
                    if(concurrentCancelState!=null){
                        // Simulate another writer changing the state immediately before UPDATE evaluation.
                        for(var r:assignments)if(r.get("maPhanCong").equals(params.get(2)))r.put("trangThai",concurrentCancelState);
                        for(var r:local)if(r.get("maPhanCong").equals(params.get(2)))r.put("trangThai",concurrentCancelState);
                    }
                    for(var r:local)if(r.get("maPhanCong").equals(params.get(2))&&r.get("trangThai").equals("CHO_HIEU_LUC")){r.put("trangThai","DA_HUY");r.put("lyDoHuy",params.get(1));return 1;}return 0;}
                if(sql.startsWith("INSERT INTO dbo.NhatKyThaoTac")){onAudit.run();if(failAudit)throw new SQLException("synthetic audit failure");return 1;}
                throw new AssertionError("Unexpected update "+sql);
            }
            return defaultValue(m.getReturnType());
        });
    }
    private static ResultSet result(List<Map<String,Object>> rows){int[] i={-1};return proxy(ResultSet.class,(o,m,a)->{
        if(m.getName().equals("next"))return ++i[0]<rows.size();
        if(m.getName().startsWith("get")&&a!=null){Object k=a[0];return k instanceof Integer index?new ArrayList<>(rows.get(i[0]).values()).get(index-1):rows.get(i[0]).get(k);}
        return defaultValue(m.getReturnType());
    });}
    private static Object defaultValue(Class<?> type){if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;return null;}
    private static <T>T proxy(Class<T> type,InvocationHandler h){return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},h));}
}
