package vn.n08.nhakhoa.service;

import vn.n08.nhakhoa.dao.BenhNhanDao;
import vn.n08.nhakhoa.dao.LichHenDao;
import vn.n08.nhakhoa.dao.MaDao;
import vn.n08.nhakhoa.dao.PhieuKhamDao;
import vn.n08.nhakhoa.dto.LichHenView;
import vn.n08.nhakhoa.dto.TiepNhanData;
import vn.n08.nhakhoa.model.BenhNhan;
import vn.n08.nhakhoa.model.HoSoBenhAn;
import vn.n08.nhakhoa.model.LichHen;
import vn.n08.nhakhoa.model.PhieuKham;
import vn.n08.nhakhoa.model.TrangThaiLichHen;
import vn.n08.nhakhoa.model.TrangThaiPhieuKham;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Quyen;
import vn.n08.nhakhoa.util.Session;
import vn.n08.nhakhoa.util.Tx;
import vn.n08.nhakhoa.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** UC02 Tiếp nhận bệnh nhân: tiếp nhận trực tiếp (không hẹn) hoặc check-in lịch hẹn → tạo phiếu khám. */
public class TiepNhanService {
    private final PhieuKhamDao pkDao = new PhieuKhamDao();
    private final LichHenDao lhDao = new LichHenDao();
    private final BenhNhanDao bnDao = new BenhNhanDao();
    private final MaDao maDao = new MaDao();

    public TiepNhanData trongNgay(LocalDate ngay) {
        Session.requireAny(Quyen.TIEP_NHAN, Quyen.KHAM);
        return Tx.read(c -> {
            List<LichHenView> lh = lhDao.search(c, Now.startOf(ngay), Now.endOf(ngay), null, null, null).stream()
                    .filter(v -> v.lh().dangMo()).toList();
            return new TiepNhanData(lh, pkDao.search(c, Now.startOf(ngay), Now.endOf(ngay), null, null, null));
        });
    }

    public String tiepNhanTrucTiep(String maBn, String maBs, String trieuChung) {
        Session.require(Quyen.TIEP_NHAN);
        String tc = Validator.maxLen(Validator.optional(trieuChung), 500, "trieuChung", "Triệu chứng");
        return Tx.write(c -> {
            BenhNhan bn = bnDao.find(c, maBn).orElseThrow(() -> new BusinessException("benhNhan", "Chọn bệnh nhân."));
            BacSiPhuTrach.requireActiveDoctor(c, maBs);
            String ma = taoPhieu(c, bn, maBs, null, tc);
            Audit.log(c, "Tiếp nhận", "Tiếp nhận trực tiếp — tạo phiếu khám", ma, bn.hoTen(), maBs);
            return ma;
        });
    }

    public String checkIn(String maLichHen, String trieuChung) {
        Session.require(Quyen.TIEP_NHAN);
        String tc = Validator.maxLen(Validator.optional(trieuChung), 500, "trieuChung", "Triệu chứng");
        return Tx.write(c -> {
            LichHen lh = lhDao.find(c, maLichHen).orElseThrow(() -> new BusinessException("Không tìm thấy lịch hẹn."));
            if (!lh.thoiGianHen().toLocalDate().equals(Now.today())) throw new BusinessException("Chỉ check-in lịch hẹn của hôm nay.");
            BenhNhan bn = bnDao.find(c, lh.maBenhNhan()).orElseThrow();
            BacSiPhuTrach.requireActiveDoctor(c, lh.maNhanVien());
            if (lhDao.transition(c, maLichHen, TrangThaiLichHen.DA_DEN, null, TrangThaiLichHen.CHO_XAC_NHAN, TrangThaiLichHen.DA_XAC_NHAN) == 0) {
                throw new BusinessException("Lịch hẹn không ở trạng thái chờ check-in.");
            }
            String ma = taoPhieu(c, bn, lh.maNhanVien(), maLichHen, tc == null ? lh.lyDoHen() : tc);
            Audit.log(c, "Tiếp nhận", "Check-in lịch hẹn — tạo phiếu khám", maLichHen, ma, lh.maNhanVien());
            return ma;
        });
    }

    private String taoPhieu(Connection c, BenhNhan bn, String maBs, String maLh, String trieuChung) throws SQLException {
        if (pkDao.patientHasOpen(c, bn.maBenhNhan())) {
            throw new BusinessException(bn.hoTen() + " đang có phiếu khám chưa kết thúc (chờ khám / đang khám).");
        }
        HoSoBenhAn hs = bnDao.hoSoOf(c, bn.maBenhNhan()).orElseThrow(() -> new BusinessException("Bệnh nhân chưa có hồ sơ bệnh án."));
        String ma = maDao.next(c, "PK");
        pkDao.insert(c, new PhieuKham(ma, bn.maBenhNhan(), hs.maHoSo(), maBs, maLh, Now.now(), trieuChung, null, null, TrangThaiPhieuKham.CHO_KHAM, null));
        return ma;
    }

    /** Đổi bác sĩ khi bệnh nhân còn chờ khám. */
    public void doiBacSi(String maPk, String maBsMoi) {
        Session.require(Quyen.TIEP_NHAN);
        Tx.run(c -> {
            BacSiPhuTrach.requireActiveDoctor(c, maBsMoi);
            if (pkDao.changeDoctor(c, maPk, maBsMoi) == 0) throw new BusinessException("Chỉ đổi bác sĩ khi phiếu đang chờ khám.");
            Audit.log(c, "Tiếp nhận", "Đổi bác sĩ khám", maPk, null, maBsMoi);
        });
    }

    /** Hủy phiếu chờ khám (bệnh nhân về / tiếp nhận nhầm). Lịch hẹn gắn kèm chuyển DA_HUY cùng lý do. */
    public void huyPhieu(String maPk, String lyDo) {
        Session.requireAny(Quyen.TIEP_NHAN, Quyen.KHAM);
        String ly = Validator.required(lyDo, "lyDo", "Lý do hủy");
        Tx.run(c -> {
            PhieuKham pk = pkDao.find(c, maPk).orElseThrow(() -> new BusinessException("Không tìm thấy phiếu khám."));
            if (pkDao.cancel(c, maPk, ly) == 0) throw new BusinessException("Chỉ hủy được phiếu đang chờ khám.");
            if (pk.maLichHen() != null) lhDao.transition(c, pk.maLichHen(), TrangThaiLichHen.DA_HUY, "Hủy lượt khám " + maPk + ": " + ly, TrangThaiLichHen.DA_DEN);
            Audit.log(c, "Tiếp nhận", "Hủy phiếu khám", maPk, ly);
        });
    }
}
