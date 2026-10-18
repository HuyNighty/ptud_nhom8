package com.quanlynhakhoa.features.inventory.entity;

import com.quanlynhakhoa.features.inventory.enums.SupplierStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Nhà cung cấp — bảng dbo.NhaCungCap. */
@Entity(name = "NhaCungCapJpaEntity")
@Table(name = "NhaCungCap", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Supplier implements Persistable<String> {

    @Id
    @Column(name = "maNhaCungCap", length = 10, nullable = false)
    private String maNhaCungCap;

    @Nationalized
    @Column(name = "tenNhaCungCap", length = 150, nullable = false)
    private String tenNhaCungCap;

    @Column(name = "soDienThoai", length = 20, nullable = false)
    private String soDienThoai;

    @Nationalized
    @Column(name = "diaChi", length = 255)
    private String diaChi;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", length = 20, nullable = false)
    private SupplierStatus trangThai;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static Supplier create(String ma, String ten, String soDienThoai, String diaChi, String email) {
        Supplier supplier = new Supplier();
        supplier.maNhaCungCap = ma;
        supplier.tenNhaCungCap = ten;
        supplier.soDienThoai = soDienThoai;
        supplier.diaChi = diaChi;
        supplier.email = email;
        supplier.trangThai = SupplierStatus.DANG_HOP_TAC;
        return supplier;
    }

    @Override
    public String getId() {
        return maNhaCungCap;
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
