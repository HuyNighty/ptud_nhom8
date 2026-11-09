package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.model.hoadon.BanNhapHoaDon;
import java.sql.Connection;
import java.sql.SQLException;

/** Trusted composition dependency, never supplied by a form.
 * Adapter must validate clinical eligibility and return ONLY performed, chargeable services,
 * quantities and trusted prices. It must enforce the approved inactive-service policy.
 * Under lock=true, source rows/prices must remain protected until the caller commits.
 * revision must change when the clinical eligibility or pricing evidence changes.
 * Uses caller's connection/transaction; no writes, commit or connection close.
 */
public interface NguonTinhPhiLamSang {
    BanNhapHoaDon load(Connection connection, String patientId, String visitId, boolean lock) throws SQLException;
    /** Must apply the reviewed historical/inactive catalogue policy, or throw to reject. */
    void validateServiceState(Connection connection, BanNhapHoaDon draft, String serviceId, boolean currentlyActive) throws SQLException;
}
