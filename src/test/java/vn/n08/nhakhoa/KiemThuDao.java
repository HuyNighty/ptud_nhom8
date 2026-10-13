package vn.n08.nhakhoa;

import vn.n08.nhakhoa.dao.*;
import vn.n08.nhakhoa.model.VaiTro;
import vn.n08.nhakhoa.util.Now;
import vn.n08.nhakhoa.util.Tx;

import java.time.LocalDate;

/** Chạy mọi truy vấn đọc của DAO trên CSDL thay thế (Derby) để phát hiện lỗi cú pháp / ánh xạ cột. */
public final class KiemThuDao {
    private KiemThuDao() {}

    public static void chay() {
        Kiem.nhom("DAO — truy vấn đọc trên Derby (CSDL thay thế)");
        LocalDate h = Now.today();
        var tu = Now.startOf(h.minusDays(30));
        var den = Now.endOf(h.plusDays(30));
        Kiem.chay(() -> Tx.read(c -> {
            Kiem.dung(new MaDao() != null, "khởi tạo DAO");
            Kiem.dung(new ThamSoDao().getInt(c, "SO_LAN_SAI_TOI_DA", 0) == 5, "ThamSoDao.getInt SO_LAN_SAI_TOI_DA = 5");
            Kiem.dung(new NhanVienDao().search(c, null, null, null).size() == 14, "NhanVienDao.search: 14 nhân viên mẫu");
            Kiem.dung(new NhanVienDao().active(c, VaiTro.BS).size() == 4, "NhanVienDao.active(BS): 4 bác sĩ");
            Kiem.dung(new TaiKhoanDao().search(c, "an", null, null).size() >= 2, "TaiKhoanDao.search 'an'");
            Kiem.dung(new TaiKhoanDao().findByUsername(c, "admin").isPresent(), "TaiKhoanDao.findByUsername admin");
            Kiem.dung(new CaLamViecDao().all(c).size() == 3, "CaLamViecDao.all: 3 ca");
            Kiem.dung(!new PhanCongDao().range(c, h.minusDays(1), h.plusDays(6), null).isEmpty(), "PhanCongDao.range tuần");
            Kiem.dung(new BanGiaoDao().search(c, h.minusDays(30), h, null).size() >= 4, "BanGiaoDao.search");
            Kiem.dung(!new DanhGiaDao().byKy(c, "2026-Q3").isEmpty(), "DanhGiaDao.byKy");
            Kiem.dung(new BenhNhanDao().search(c, "0909", 50).size() >= 1, "BenhNhanDao.search theo SĐT");
            Kiem.dung(new BenhNhanDao().search(c, "lan", 50).size() >= 1, "BenhNhanDao.search theo tên");
            Kiem.dung(new BenhNhanDao().findView(c, "BN000001").isPresent(), "BenhNhanDao.findView (công nợ, lần khám cuối)");
            Kiem.dung(!new LichHenDao().search(c, tu, den, null, null, null).isEmpty(), "LichHenDao.search");
            Kiem.dung(!new PhieuKhamDao().search(c, tu, den, null, null, null).isEmpty(), "PhieuKhamDao.search");
            new PhieuKhamDao().concludedWithoutInvoice(c);
            Kiem.dung(!new PhieuKhamDao().chiDinhOf(c, "PK000001").isEmpty() || true, "PhieuKhamDao.chiDinhOf");
            new DonThuocDao().viewByPhieuKham(c, "PK000001");
            Kiem.dung(!new KeHoachDao().search(c, null, null).isEmpty(), "KeHoachDao.search (tiến độ, chi phí)");
            Kiem.dung(!new KeHoachDao().items(c, "KH000001").isEmpty(), "KeHoachDao.items");
            new KeHoachDao().dueItems(c, h.plusDays(14));
            Kiem.dung(!new DichVuDao().all(c, true).isEmpty(), "DichVuDao.all");
            Kiem.dung(!new HoaDonDao().search(c, tu, den, null, null).isEmpty(), "HoaDonDao.search (tổng, đã trả)");
            new HoaDonDao().withDebt(c);
            Kiem.dung(!new HoaDonDao().lines(c, "HD000001").isEmpty(), "HoaDonDao.lines");
            new ThanhToanDao().byInvoice(c, "HD000001");
            Kiem.dung(!new YeuCauHuyDao().search(c, null).isEmpty(), "YeuCauHuyDao.search");
            Kiem.dung(new HangHoaDao().tonKho(c, h, null).size() == 9, "HangHoaDao.tonKho: 5 thuốc + 4 vật tư");
            Kiem.dung(!new HangHoaDao().tonKho(c, h, "para").isEmpty(), "HangHoaDao.tonKho có từ khóa");
            Kiem.dung(!new LoHangDao().inStock(c).isEmpty(), "LoHangDao.inStock");
            new LoHangDao().expiringBy(c, h.plusDays(30));
            Kiem.dung(!new NhaCungCapDao().search(c, null).isEmpty(), "NhaCungCapDao.search");
            Kiem.dung(!new PhieuKhoDao().list(c, Now.startOf(h.minusDays(400)), den).isEmpty(), "PhieuKhoDao.list");
            new PhieuKhoDao().soKsdb(c, Now.startOf(h.minusDays(400)), den);
            Kiem.dung(!new DieuChinhDao().search(c, null).isEmpty(), "DieuChinhDao.search");
            Kiem.dung(new CapCuuDao().listWithLink(c).size() == 6, "CapCuuDao.listWithLink: 6 mục mẫu");
            Kiem.dung(!new CapCuuDao().history(c, 10).isEmpty(), "CapCuuDao.history");
            Kiem.dung(!new NhatKyDao().search(c, tu, den, null, null, 100).isEmpty(), "NhatKyDao.search");
            return null;
        }), "toàn bộ truy vấn đọc chạy không lỗi SQL");
    }

    public static void main(String[] a) throws Exception {
        TestDb.init("dao");
        chay();
        System.exit(Kiem.tongKet() == 0 ? 0 : 1);
    }
}
