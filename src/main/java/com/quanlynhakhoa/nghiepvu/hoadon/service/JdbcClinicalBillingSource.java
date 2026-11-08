package com.quanlynhakhoa.nghiepvu.hoadon.service;

import com.quanlynhakhoa.nghiepvu.hoadon.dao.ClinicalChargeDao;
import com.quanlynhakhoa.nghiepvu.hoadon.dao.ClinicalChargeDao.Evidence;
import com.quanlynhakhoa.nghiepvu.hoadon.entity.InvoiceDraft;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;

/** User-authorized contract: completed visit/plan, one unit per performed source row,
 * current catalogue price locked at invoice creation; ambiguous cross-source duplicates rejected.
 */
public final class JdbcClinicalBillingSource implements ClinicalBillingSource {
    private final ClinicalChargeDao dao;
    public JdbcClinicalBillingSource(){this(new ClinicalChargeDao());}
    public JdbcClinicalBillingSource(ClinicalChargeDao dao){this.dao=Objects.requireNonNull(dao);}
    @Override public InvoiceDraft load(Connection c,String patient,String visit,boolean lock)throws SQLException {
        if(!lock || c.getAutoCommit() || c.getTransactionIsolation()!=Connection.TRANSACTION_SERIALIZABLE)
            throw new SQLException("Nguồn tính phí cần transaction SERIALIZABLE.");
        var evidence = dao.load(c,patient,visit);
        InvoiceDraft base = evidence.isEmpty() ? new InvoiceDraft(patient,visit,"PERFORMED_V1",List.of()) : build(patient,visit,evidence);
        List<InvoiceDraft.Line> lines = new ArrayList<>(base.lines());
        StringBuilder revision = new StringBuilder(base.revision());
        try (PreparedStatement s = c.prepareStatement("""
                SELECT t.maThuoc,t.tenThuoc,t.donGiaBan,x.soLuong
                FROM dbo.DonThuoc d WITH (HOLDLOCK)
                JOIN dbo.ChiTietDonThuoc x WITH (HOLDLOCK) ON x.maDonThuoc=d.maDonThuoc
                JOIN dbo.Thuoc t WITH (HOLDLOCK) ON t.maThuoc=x.maThuoc
                WHERE d.maPhieuKham=? AND d.maBenhNhan=? ORDER BY t.maThuoc
                """)) {
            s.setQueryTimeout(15); s.setString(1,visit); s.setString(2,patient);
            try (ResultSet r=s.executeQuery()) {
                while(r.next()) {
                    String id=r.getString("maThuoc"),name=r.getNString("tenThuoc");
                    BigDecimal price=r.getBigDecimal("donGiaBan"); int quantity=r.getInt("soLuong");
                    if(price==null)throw new IllegalArgumentException("Thuốc "+id+" chưa có giá bán. Cập nhật giá trong Danh mục thuốc rồi tải lại hóa đơn.");
                    if(quantity<=0||price.signum()<0||price.stripTrailingZeros().scale()>0||price.precision()>18)
                        throw new IllegalArgumentException("Số lượng hoặc giá bán thuốc không hợp lệ: "+id);
                    lines.add(new InvoiceDraft.Line(id,name,quantity,price.setScale(0),true));
                    for(String value:List.of(id,name,Integer.toString(quantity),price.toPlainString()))revision.append('|').append(value.length()).append(':').append(value);
                }
            }
        }
        if(lines.isEmpty())throw new IllegalArgumentException("Lượt khám chưa có dịch vụ hoặc thuốc được phép tính phí.");
        return new InvoiceDraft(patient,visit,revision.toString(),lines);
    }
    public static InvoiceDraft build(String patient,String visit,List<Evidence> evidence) {
        List<InvoiceDraft.Line> lines=new ArrayList<>();
        Map<String,String> serviceKinds=new HashMap<>();Set<String> sourceIds=new HashSet<>();
        StringBuilder revision=new StringBuilder("PERFORMED_V1");
        for(Evidence row:evidence) {
            if(!sourceIds.add(row.kind()+":"+row.sourceId()))throw new IllegalArgumentException("Trùng bằng chứng tính phí.");
            String old=serviceKinds.putIfAbsent(row.serviceId(),row.kind());
            if(old!=null && !old.equals(row.kind()))throw new IllegalArgumentException("Dịch vụ có ở cả CLS và điều trị; cần đối soát tránh tính hai lần.");
            if(!row.kind().equals("CLS")&&!row.kind().equals("DIEU_TRI"))throw new IllegalArgumentException("Nguồn tính phí không hỗ trợ.");
            if(row.kind().equals("CLS")&&!row.clinicalService())throw new IllegalArgumentException("Chỉ định CLS gắn dịch vụ không phải cận lâm sàng.");
            if(row.price()==null||row.price().signum()<0||row.price().stripTrailingZeros().scale()>0||row.price().precision()>18)
                throw new IllegalArgumentException("Giá danh mục không hợp lệ.");
            lines.add(new InvoiceDraft.Line(row.serviceId(),row.name(),1,row.price().setScale(0)));
            // Length-prefixed fields avoid ambiguous concatenation, without exposing clinical notes.
            for(String value:List.of(row.kind(),row.sourceId(),row.serviceId(),row.name(),row.price().toPlainString(),Boolean.toString(row.active())))
                revision.append('|').append(value.length()).append(':').append(value);
        }
        if(lines.isEmpty())throw new IllegalArgumentException("Lượt khám không có dịch vụ đã thực hiện được phép tính phí.");
        try {
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(revision.toString().getBytes(StandardCharsets.UTF_8)));
            return new InvoiceDraft(patient,visit,hash,lines);
        } catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    @Override public void validateServiceState(Connection c,InvoiceDraft draft,String serviceId,boolean active) {
        // Historical performed evidence is billable even when catalogue was subsequently stopped.
        // load() has verified that evidence under the same transaction; never permit manual additions.
        boolean found=false;for(InvoiceDraft.Line line:draft.lines())if(line.serviceId().equals(serviceId))found=true;
        if(!found)throw new IllegalArgumentException("Dịch vụ không thuộc bằng chứng thực hiện.");
    }
}
