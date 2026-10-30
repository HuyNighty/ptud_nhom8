package com.quanlynhakhoa.billing.service;

import com.quanlynhakhoa.billing.dao.ClinicalChargeDao;
import com.quanlynhakhoa.billing.dao.ClinicalChargeDao.Evidence;
import com.quanlynhakhoa.billing.model.InvoiceDraft;
import java.sql.*;
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
        return build(patient,visit,dao.load(c,patient,visit));
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
