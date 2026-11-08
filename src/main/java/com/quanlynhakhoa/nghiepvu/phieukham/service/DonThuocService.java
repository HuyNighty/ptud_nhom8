package com.quanlynhakhoa.nghiepvu.phieukham.service;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.quantri.vaitro.entity.Role;
import com.quanlynhakhoa.nghiepvu.phieukham.dao.CanLamSangDao;
import com.quanlynhakhoa.nghiepvu.phieukham.dao.DonThuocDao;
import com.quanlynhakhoa.nghiepvu.phieukham.dao.PhieuKhamDao;
import com.quanlynhakhoa.nghiepvu.phieukham.entity.ChiTietDonThuoc;
import com.quanlynhakhoa.nghiepvu.phieukham.entity.DonThuocData;
import com.quanlynhakhoa.nghiepvu.phieukham.entity.PhieuKham;
import com.quanlynhakhoa.nghiepvu.phieukham.entity.Thuoc;
import com.quanlynhakhoa.hethong.phanquyen.AuthorizationService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import com.quanlynhakhoa.hethong.phanquyen.RolePermissionPolicy;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class DonThuocService {

    private static final int MAX_LOI_DAN = 500;
    private static final int MAX_LIEU_DUNG = 100;
    private static final int MAX_CACH_DUNG = 200;
    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    public static final class CanhBaoCanXacNhanException extends IllegalStateException {
        public CanhBaoCanXacNhanException(String message) {
            super(message);
        }
    }

    private final DesktopSession session;
    private final DonThuocDao dao;
    private final PhieuKhamDao phieuKhamDao;
    private final CanLamSangDao canLamSangDao;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());

    public DonThuocService(DesktopSession session) {
        this(session, new DonThuocDao(), new PhieuKhamDao(), new CanLamSangDao());
    }

    public DonThuocService(DesktopSession session, DonThuocDao dao,
                           PhieuKhamDao phieuKhamDao, CanLamSangDao canLamSangDao) {
        this.session = Objects.requireNonNull(session, "session");
        this.dao = Objects.requireNonNull(dao, "dao");
        this.phieuKhamDao = Objects.requireNonNull(phieuKhamDao, "phieuKhamDao");
        this.canLamSangDao = Objects.requireNonNull(canLamSangDao, "canLamSangDao");
    }

    public DonThuocData taiDuLieu(String maPhieuKham, String maBacSiDuocChon)
            throws SQLException {
        authorization.require(session, Permission.MEDICAL_RECORD_READ);
        String visitId = validateId(maPhieuKham, "Mã phiếu khám");
        try (Connection connection = TeamDevConnection.open()) {
            PhieuKham visit = loadAndVerifyVisit(connection, visitId, maBacSiDuocChon, false);
            List<Thuoc> medicines = dao.danhSachThuoc(connection);
            Optional<DonThuocDao.DonThuocHeader> header =
                    dao.timDonThuocTheoPhieu(connection, visitId);
            if (header.isEmpty()) {
                return new DonThuocData(medicines, List.of(), "", false);
            }
            List<ChiTietDonThuoc> details = dao.danhSachChiTiet(
                    connection, header.get().maDonThuoc());
            return new DonThuocData(medicines, details, header.get().loiDan(), true);
        }
    }

    public void luuDonThuoc(String maPhieuKham, String maBacSiDuocChon,
                            List<ChiTietDonThuoc> requestedItems, String loiDan,
                            boolean daXacNhanCanhBao) throws SQLException {
        authorization.require(session, Permission.MEDICAL_RECORD_UPDATE);
        String visitId = validateId(maPhieuKham, "Mã phiếu khám");
        String instructions = normalize(loiDan);
        if (instructions.length() > MAX_LOI_DAN) {
            throw new IllegalArgumentException("Lời dặn tối đa 500 ký tự.");
        }
        List<ChiTietDonThuoc> items = validateItems(requestedItems);

        try (Connection connection = TeamDevConnection.open()) {
            connection.setAutoCommit(false);
            try {
                PhieuKham visit = loadAndVerifyVisit(connection, visitId, maBacSiDuocChon, true);
                if (dao.timDonThuocTheoPhieu(connection, visitId).isPresent()) {
                    throw new IllegalStateException("Phiếu khám này đã có đơn thuốc. Không thể tạo đơn thứ hai.");
                }

                Map<String, Thuoc> catalog = new HashMap<>();
                for (Thuoc medicine : dao.danhSachThuoc(connection)) {
                    catalog.put(medicine.maThuoc(), medicine);
                }

                List<ChiTietDonThuoc> canonicalItems = new ArrayList<>();
                for (ChiTietDonThuoc item : items) {
                    Thuoc medicine = catalog.get(item.maThuoc());
                    if (medicine == null) {
                        throw new IllegalArgumentException("Không tìm thấy thuốc " + item.maThuoc() + ".");
                    }
                    canonicalItems.add(new ChiTietDonThuoc(
                            medicine.maThuoc(), medicine.tenThuoc(), medicine.donViTinh(),
                            item.soLuong(), item.lieuDung(), item.cachDung(), item.soNgayDung(),
                            medicine.kiemSoatDacBiet(), medicine.hoatChat()));
                }

                List<String> warnings = buildWarnings(visit.diUng(), canonicalItems);
                if (!warnings.isEmpty() && !daXacNhanCanhBao) {
                    throw new CanhBaoCanXacNhanException(
                            "Vui lòng đọc và xác nhận các cảnh báo sau trước khi lưu đơn thuốc:\n\n"
                            + String.join("\n", warnings));
                }

                String prescriptionId = generateId();
                String doctorId = visit.maNhanVien();
                dao.taoDonThuoc(connection, prescriptionId, visitId,
                        visit.maBenhNhan(), doctorId, instructions);
                for (ChiTietDonThuoc item : canonicalItems) {
                    dao.taoChiTiet(connection, prescriptionId, item);
                }

                String auditNote = "Đơn thuốc gồm " + canonicalItems.size() + " loại thuốc.";
                if (!warnings.isEmpty()) {
                    auditNote += " Người kê đơn đã xác nhận cảnh báo: "
                            + abbreviate(String.join("; ", warnings), 700);
                }
                dao.ghiNhatKy(connection, session.accountId(), prescriptionId,
                        doctorId, auditNote);
                authorization.require(session, Permission.MEDICAL_RECORD_UPDATE);
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        }
    }

    private PhieuKham loadAndVerifyVisit(Connection connection, String visitId,
                                         String selectedDoctorId, boolean mustBeOpen)
            throws SQLException {
        var assignment = (mustBeOpen
                ? canLamSangDao.khoaPhieuKham(connection, visitId)
                : canLamSangDao.timPhieuKham(connection, visitId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu khám."));

        String assignedDoctor = safe(assignment.maNhanVien());
        if (assignedDoctor.isEmpty()) {
            throw new IllegalStateException("Phiếu khám chưa có bác sĩ chịu trách nhiệm.");
        }
        if (mustBeOpen && !"CHO_KHAM".equals(assignment.trangThai())
                && !"DANG_KHAM".equals(assignment.trangThai())) {
            throw new IllegalStateException("Chỉ được kê đơn khi phiếu đang Chờ khám hoặc Đang khám.");
        }

        PhieuKham visit = phieuKhamDao.timTheoMa(connection, visitId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu khám."));
        if (session.role() == Role.DOCTOR) {
            Optional<String> employeeId = phieuKhamDao.maNhanVienTheoTaiKhoan(
                    connection, session.accountId());
            String signedInEmployee = employeeId.filter(value -> !value.isBlank())
                    .orElseThrow(() -> new IllegalStateException(
                            "Tài khoản bác sĩ chưa được liên kết với hồ sơ nhân viên."));
            if (!signedInEmployee.equals(assignedDoctor)) {
                throw new IllegalStateException("Phiếu khám do bác sĩ khác phụ trách.");
            }
        } else if (session.role() == Role.SYSTEM_ADMIN) {
            if (!assignedDoctor.equals(safe(selectedDoctorId))) {
                throw new IllegalArgumentException(
                        "Quản trị hệ thống phải xác nhận đúng bác sĩ đang phụ trách phiếu.");
            }
        } else {
            throw new IllegalStateException("Tài khoản không được phép kê đơn thuốc.");
        }
        return visit;
    }

    private static List<ChiTietDonThuoc> validateItems(List<ChiTietDonThuoc> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new IllegalArgumentException("Đơn thuốc phải có ít nhất một loại thuốc.");
        }
        if (requested.size() > 50) {
            throw new IllegalArgumentException("Một đơn thuốc tối đa 50 loại thuốc.");
        }
        List<ChiTietDonThuoc> result = new ArrayList<>();
        Set<String> medicineIds = new HashSet<>();
        for (ChiTietDonThuoc item : requested) {
            if (item == null) throw new IllegalArgumentException("Dòng thuốc không hợp lệ.");
            String medicineId = validateId(item.maThuoc(), "Mã thuốc");
            if (!medicineIds.add(medicineId)) {
                throw new IllegalArgumentException("Một thuốc chỉ được xuất hiện một lần trong đơn.");
            }
            String dosage = normalize(item.lieuDung());
            String usage = normalize(item.cachDung());
            if (item.soLuong() <= 0 || item.soLuong() > 10000) {
                throw new IllegalArgumentException("Số lượng thuốc phải từ 1 đến 10000.");
            }
            if (item.soNgayDung() <= 0 || item.soNgayDung() > 365) {
                throw new IllegalArgumentException("Số ngày dùng phải từ 1 đến 365.");
            }
            if (dosage.isEmpty() || dosage.length() > MAX_LIEU_DUNG) {
                throw new IllegalArgumentException("Liều dùng không được để trống và tối đa 100 ký tự.");
            }
            if (usage.isEmpty() || usage.length() > MAX_CACH_DUNG) {
                throw new IllegalArgumentException("Cách dùng không được để trống và tối đa 200 ký tự.");
            }
            result.add(new ChiTietDonThuoc(medicineId, "", "", item.soLuong(),
                    dosage, usage, item.soNgayDung(), false, ""));
        }
        return List.copyOf(result);
    }

    private static List<String> buildWarnings(String allergies, List<ChiTietDonThuoc> items) {
        List<String> warnings = new ArrayList<>();
        String allergyText = safe(allergies).toLowerCase(Locale.ROOT);
        for (ChiTietDonThuoc item : items) {
            if (item.kiemSoatDacBiet()) {
                warnings.add("Thuốc " + item.tenThuoc() + " thuộc nhóm kiểm soát đặc biệt.");
            }
            if (!allergyText.isEmpty()) {
                boolean nameMatch = hasIngredientMatch(allergyText, item.tenThuoc());
                boolean ingredientMatch = hasIngredientMatch(allergyText, item.hoatChat());
                if (nameMatch || ingredientMatch) {
                    String ingredient = safe(item.hoatChat());
                    warnings.add("Thuốc " + item.tenThuoc()
                            + (ingredient.isEmpty() ? "" : " (hoạt chất: " + ingredient + ")")
                            + " có từ khóa trùng với ghi chú dị ứng của hồ sơ: " + safe(allergies));
                }
            }
        }
        return List.copyOf(warnings);
    }

    private static boolean hasIngredientMatch(String allergyText, String ingredients) {
        String text = safe(ingredients);
        if (text.isEmpty()) return false;
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (token.length() >= 4 && allergyText.contains(token)) return true;
        }
        return false;
    }

    private static String generateId() {
        StringBuilder value = new StringBuilder(10);
        for (int i = 0; i < 10; i++) value.append(BASE32[RANDOM.nextInt(BASE32.length)]);
        return value.toString();
    }

    private static String validateId(String value, String field) {
        String text = safe(value);
        if (text.isEmpty() || text.length() > 10) {
            throw new IllegalArgumentException(field + " phải có từ 1 đến 10 ký tự.");
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))) {
                throw new IllegalArgumentException(field + " chỉ được dùng chữ in hoa không dấu và chữ số.");
            }
        }
        return text;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String abbreviate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            failure.addSuppressed(rollbackError);
        }
    }
}
