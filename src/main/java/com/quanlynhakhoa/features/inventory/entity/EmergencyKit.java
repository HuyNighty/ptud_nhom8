package com.quanlynhakhoa.features.inventory.entity;

import com.quanlynhakhoa.features.inventory.enums.EmergencyKitType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;
import org.springframework.data.domain.Persistable;

/** Hộp thuốc cấp cứu — bảng dbo.HopCapCuu (UC34). */
@Entity(name = "HopCapCuuJpaEntity")
@Table(name = "HopCapCuu", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmergencyKit implements Persistable<String> {

    @Id
    @Column(name = "maHop", length = 10, nullable = false)
    private String maHop;

    @Nationalized
    @Column(name = "tenHop", length = 150, nullable = false)
    private String tenHop;

    @Enumerated(EnumType.STRING)
    @Column(name = "loaiHop", length = 20, nullable = false)
    private EmergencyKitType loaiHop;

    @Nationalized
    @Column(name = "viTri", length = 100, nullable = false)
    private String viTri;

    @Column(name = "chuKyKiemTraNgay", nullable = false)
    private int chuKyKiemTraNgay;

    @Column(name = "nguongHanDungNgay", nullable = false)
    private int nguongHanDungNgay;

    @Nationalized
    @Column(name = "ghiChu", length = 300)
    private String ghiChu;

    @Column(name = "maNhanVienTao", length = 10, nullable = false)
    private String maNhanVienTao;

    @Column(name = "thoiDiemTao", nullable = false)
    private LocalDateTime thoiDiemTao;

    @OneToMany(mappedBy = "kit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("maChiTiet")
    @Getter(AccessLevel.NONE)
    private List<EmergencyKitItem> items = new ArrayList<>();

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static EmergencyKit create(String ma, String ten, EmergencyKitType loai, String viTri, int chuKy,
                                      int nguongHan, String ghiChu, String maNhanVienTao, LocalDateTime now) {
        EmergencyKit kit = new EmergencyKit();
        kit.maHop = ma;
        kit.maNhanVienTao = maNhanVienTao;
        kit.thoiDiemTao = now;
        kit.updateInfo(ten, loai, viTri, chuKy, nguongHan, ghiChu);
        return kit;
    }

    public void updateInfo(String ten, EmergencyKitType loai, String viTri, int chuKy, int nguongHan, String ghiChu) {
        this.tenHop = ten;
        this.loaiHop = loai;
        this.viTri = viTri;
        this.chuKyKiemTraNgay = chuKy;
        this.nguongHanDungNgay = nguongHan;
        this.ghiChu = ghiChu;
    }

    public List<EmergencyKitItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Optional<EmergencyKitItem> findItem(String maThuoc, String maVatTu) {
        return items.stream().filter(i -> i.sameItem(maThuoc, maVatTu)).findFirst();
    }

    public Optional<EmergencyKitItem> findItem(Integer maChiTiet) {
        return items.stream().filter(i -> i.getMaChiTiet() != null && i.getMaChiTiet().equals(maChiTiet)).findFirst();
    }

    /**
     * Đặt lại thành phần hộp: mặt hàng đã có giữ nguyên số hiện có/hạn dùng, chỉ đổi số tối thiểu;
     * mặt hàng mới bắt đầu từ 0; mặt hàng không còn trong danh sách bị bỏ khỏi hộp.
     */
    public void replaceComposition(List<EmergencyKitItem> wanted) {
        List<EmergencyKitItem> next = new ArrayList<>();
        for (EmergencyKitItem w : wanted) {
            EmergencyKitItem existing = findItem(w.getMaThuoc(), w.getMaVatTu()).orElse(null);
            if (existing != null) {
                existing.changeMinimum(w.getSoLuongToiThieu());
                next.add(existing);
            } else {
                w.attachTo(this);
                next.add(w);
            }
        }
        items.clear();
        items.addAll(next);
    }

    @Override
    public String getId() {
        return maHop;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        newEntity = false;
    }
}
