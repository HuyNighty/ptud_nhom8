package com.quanlynhakhoa.features.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Một mặt hàng trong hộp cấp cứu — bảng dbo.ChiTietHopCapCuu. */
@Entity(name = "ChiTietHopCapCuuJpaEntity")
@Table(name = "ChiTietHopCapCuu", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmergencyKitItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maChiTiet", nullable = false)
    private Integer maChiTiet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maHop", nullable = false)
    @Getter(AccessLevel.NONE)
    private EmergencyKit kit;

    @Column(name = "maThuoc", length = 10)
    private String maThuoc;

    @Column(name = "maVatTu", length = 10)
    private String maVatTu;

    @Column(name = "soLuongToiThieu", nullable = false)
    private int soLuongToiThieu;

    @Column(name = "soLuongHienCo", nullable = false)
    private int soLuongHienCo;

    @Column(name = "hanDung")
    private LocalDate hanDung;

    public static EmergencyKitItem of(String maThuoc, String maVatTu, int soLuongToiThieu) {
        if ((maThuoc == null) == (maVatTu == null)) {
            throw new IllegalArgumentException("Cần đúng một trong mã thuốc / mã vật tư");
        }
        EmergencyKitItem item = new EmergencyKitItem();
        item.maThuoc = maThuoc;
        item.maVatTu = maVatTu;
        item.changeMinimum(soLuongToiThieu);
        return item;
    }

    void attachTo(EmergencyKit owner) {
        this.kit = owner;
    }

    void changeMinimum(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Số lượng tối thiểu phải lớn hơn 0");
        }
        this.soLuongToiThieu = value;
    }

    public String maMatHang() {
        return maThuoc != null ? maThuoc : maVatTu;
    }

    boolean sameItem(String thuoc, String vatTu) {
        return Objects.equals(maThuoc, thuoc) && Objects.equals(maVatTu, vatTu);
    }

    /** Ghi nhận số thực có trong hộp (khi kiểm tra). */
    public void record(int soLuong, LocalDate han) {
        if (soLuong < 0) {
            throw new IllegalArgumentException("Số lượng không được âm");
        }
        this.soLuongHienCo = soLuong;
        this.hanDung = soLuong == 0 ? null : han;
    }

    /** Bổ sung thêm hàng từ kho: hạn dùng của hộp là hạn sớm nhất của số đang có. */
    public void add(int soLuong, LocalDate han) {
        if (soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng bổ sung phải lớn hơn 0");
        }
        LocalDate next = soLuongHienCo == 0 ? han : earliest(hanDung, han);
        this.soLuongHienCo += soLuong;
        this.hanDung = next;
    }

    /** Thay toàn bộ số đang có bằng hàng mới từ kho (hàng cũ đã cận/hết hạn được lấy ra khỏi hộp). */
    public void replace(int soLuong, LocalDate han) {
        if (soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng thay thế phải lớn hơn 0");
        }
        this.soLuongHienCo = soLuong;
        this.hanDung = han;
    }

    private static LocalDate earliest(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isBefore(b) ? a : b;
    }
}
