package com.quanlynhakhoa.features.inventory.entity;

import com.quanlynhakhoa.features.inventory.enums.ControlledDrugGroup;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;
import org.springframework.data.domain.Persistable;

/** Danh mục thuốc — bảng dbo.Thuoc. */
@Entity(name = "ThuocJpaEntity")
@Table(name = "Thuoc", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medicine implements Persistable<String> {

    @Id
    @Column(name = "maThuoc", length = 10, nullable = false)
    private String maThuoc;

    @Nationalized
    @Column(name = "tenThuoc", length = 150, nullable = false)
    private String tenThuoc;

    @Nationalized
    @Column(name = "donViTinh", length = 30, nullable = false)
    private String donViTinh;

    @Column(name = "kiemSoatDacBiet", nullable = false)
    private boolean kiemSoatDacBiet;

    @Nationalized
    @Column(name = "hamLuong", length = 50)
    private String hamLuong;

    @Nationalized
    @Column(name = "hoatChat", length = 150)
    private String hoatChat;

    @Column(name = "dinhMucToiThieu", nullable = false)
    private int dinhMucToiThieu;

    @Column(name = "daDatHang", nullable = false)
    private boolean daDatHang;

    /** Vị trí lưu trữ trong kho (V12), vd. "Kệ A-01"; null = chưa xếp vị trí. */
    @Nationalized
    @Column(name = "viTriLuuTru", length = 50)
    private String viTriLuuTru;

    /** Nhóm kiểm soát (V11); chỉ có khi kiemSoatDacBiet = true, null = chưa phân loại. */
    @Enumerated(EnumType.STRING)
    @Column(name = "nhomKiemSoat", length = 20)
    private ControlledDrugGroup nhomKiemSoat;

    /** Mã do người dùng nhập: buộc INSERT, không để save() âm thầm UPDATE dòng trùng mã. */
    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static Medicine create(String maThuoc, String tenThuoc, String donViTinh, boolean kiemSoatDacBiet,
                                  String hamLuong, String hoatChat, int dinhMucToiThieu) {
        Medicine medicine = new Medicine();
        medicine.maThuoc = maThuoc;
        medicine.updateInfo(tenThuoc, donViTinh, kiemSoatDacBiet, hamLuong, hoatChat, dinhMucToiThieu);
        return medicine;
    }

    public void updateInfo(String tenThuoc, String donViTinh, boolean kiemSoatDacBiet,
                           String hamLuong, String hoatChat, int dinhMucToiThieu) {
        this.tenThuoc = tenThuoc;
        this.donViTinh = donViTinh;
        this.kiemSoatDacBiet = kiemSoatDacBiet;
        this.hamLuong = hamLuong;
        this.hoatChat = hoatChat;
        this.dinhMucToiThieu = dinhMucToiThieu;
        if (!kiemSoatDacBiet) {
            this.nhomKiemSoat = null;
        }
    }

    /** Phân loại nhóm kiểm soát; chỉ áp dụng cho thuốc kiểm soát đặc biệt. */
    public void classify(ControlledDrugGroup group) {
        if (!kiemSoatDacBiet && group != null) {
            throw new IllegalStateException("Thuốc " + maThuoc + " không thuộc diện kiểm soát đặc biệt");
        }
        this.nhomKiemSoat = group;
    }

    /**
     * Điều chỉnh nhanh thông tin theo dõi tồn (UC36): định mức tối thiểu, vị trí lưu trữ, cờ đã đặt hàng.
     * Không đổi số tồn — tồn chỉ đổi qua chứng từ kho.
     */
    public void updateMonitoring(int dinhMucToiThieu, String viTriLuuTru, boolean daDatHang) {
        if (dinhMucToiThieu < 0) {
            throw new IllegalArgumentException("Định mức không được âm");
        }
        this.dinhMucToiThieu = dinhMucToiThieu;
        this.viTriLuuTru = viTriLuuTru;
        this.daDatHang = daDatHang;
    }

    @Override
    public String getId() {
        return maThuoc;
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
